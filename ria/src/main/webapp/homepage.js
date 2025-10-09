(function () {
    // =================== VARIABILI GLOBALI ===================
    let lastPlaylist = null, lastTrack = null;
    // Toast util: mostra messaggio temporaneo (simile a pure_html)
    function showToast(msg, duration=3000){
        const t = document.getElementById('toast');
        if(!t) return; // fallback silenzioso
        t.textContent = msg;
        t.classList.add('show');
        clearTimeout(t._h); t._h = setTimeout(()=>{ t.classList.remove('show'); }, duration);
    }
    // Determina (best-effort) il context path (es: /ria) se l'app è deployata non in root
    const CONTEXT_PATH = (function() {
        // Se la pagina è ad es. http://host/ria/homepage.html -> estrae "/ria"
        // Se è root (homepage.html in /) ritorna stringa vuota
        try {
            const path = window.location.pathname; // es: /ria/homepage.html
            const parts = path.split('/').filter(Boolean);
            if (parts.length > 1) {
                // heuristica: se esiste cartella 'ria' o nome progetto all'inizio
                return '/' + parts[0];
            }
        } catch(_) {}
        return '';
    })();

    function normalizeMediaPath(p) {
        if (!p) return p;
        // Se già contiene http o // o il context path esplicito, lascio stare
        if (p.startsWith('http://') || p.startsWith('https://')) return p;
        // Evita doppio slash
        if (CONTEXT_PATH && p.startsWith('/media/')) return CONTEXT_PATH + p; // server serve le risorse statiche con context prefix
        return p; // fallback
    }
    // ===== Utility: blurred album cover as page background (inside IIFE) =====
    function ensurePlayerBgLayer(){
        let el = document.getElementById('playerCoverBg');
        if(!el){
            el = document.createElement('div');
            el.id = 'playerCoverBg';
            el.className = 'player-bg';
            document.body.prepend(el);
        }
        // Helper: refresh global playlists and re-render playlist grid (used after reorder/save)
        function refreshHomePlaylists(callback) {
            makeCall('GET', 'HomePage', null, (req) => {
                if (req.readyState !== XMLHttpRequest.DONE) return;
                if (req.status === 200) {
                    try { playlists = JSON.parse(req.responseText); } catch(_) { playlists = playlists || []; }
                    try { playlistGrid(playlists); } catch(_) {}
                    if (typeof callback === 'function') callback(null, playlists);
                } else {
                    if (typeof callback === 'function') callback(new Error('Failed to refresh playlists'), null);
                }
            }, false);
        }
        return el;
    }
    function setPlayerBackground(imgPath){
        try{
            const url = imgPath ? (imgPath.startsWith('http')? imgPath : normalizeMediaPath(imgPath)) : null;
            const layer = ensurePlayerBgLayer();
            if(url){ layer.style.backgroundImage = `url('${url}')`; document.body.classList.add('has-player-bg'); }
            else { clearPlayerBackground(); }
        }catch(_){ /* no-op */ }
    }
    function clearPlayerBackground(){
        try{
            const layer = document.getElementById('playerCoverBg');
            if(layer){ layer.remove(); }
            document.body.classList.remove('has-player-bg');
        }catch(_){ /* no-op */ }
    }
    let tracklist, trackGroup = 0; // tracklist completa (solo per reorder/modal), trackGroup usato ancora per calcolo locale finché sostituito da currentGroupPlaylist
    let currentPlaylistId = null; // id playlist corrente per paginazione
    let currentGroup = 0; // indice gruppo 0-based per nuova paginazione server
    let totalGroups = 0; // totale gruppi disponibili
    const PAGE_SIZE = 5;
    let homeView = new HomeView(), playlistView = new PlaylistView(), trackView = new TrackView();
    let playlists = []; // global playlists array

    function closeModalById(id){
        const m = document.getElementById(id);
        if(!m) return;
        // Modali createModal hanno classe modal-window
        if(m.classList.contains('modal-window')){
            // Prima veniva rimossa (remove) e non poteva più essere riaperta.
            // Ora la nascondiamo soltanto così il bottone può riaprirla.
            m.classList.add('hidden');
            m.style.visibility = 'hidden';
            m.style.pointerEvents = 'none';
        } else {
            // fallback: hide generico
            m.style.display='none';
        }
    }

    // Gestione bottone indietro solo per vista traccia -> playlist
    function showBackToPlaylist(show) {
        const btn = document.getElementById("back-to-playlist-button");
        if (!btn) return;
        if (show) btn.classList.remove("hidden"); else btn.classList.add("hidden");
    }

    // Avvio pagina (DOMContentLoaded più rapido di load e evita attesa risorse pesanti)
    document.addEventListener("DOMContentLoaded", () => {
        let mainLoader = new MainLoader();
        mainLoader.start();
        mainLoader.refreshPage();
    });

    // =================== HOME VIEW ===================
    function HomeView() {
        const HOMEPAGE_LABEL = "Le mie Playlist"; // tradotto in italiano
        const HOMEPAGE_ID = "homepage";

        this.show = function () {
            // Reset semplice (stack rimosso)
            clearModals();
            clearBottomNavbar();
            loadCreatePlaylistModal();
            loadUploadTrackModal();
            loadButtons();
            loadPlaylists();
            showBackToPlaylist(false); // mai visibile in home
        };

        function loadPlaylists() {
            const main = document.getElementById("main");
            // Inserisce un box titolo standalone + contenitore playlist subito sotto
            main.innerHTML = `<div class=\"title-strip\"><div class=\"title-strip-inner\">${HOMEPAGE_LABEL}</div></div><div class=\"playlists-wrapper\"><section class=\"playlists-vertical\" id=\"playlistsGrid\"></section></div>`;
            // Ripristina h1 solo accessibile nascosto
            const mainLabel = document.getElementsByClassName("main-label").item(0);
            mainLabel.id = HOMEPAGE_ID;
            mainLabel.textContent = HOMEPAGE_LABEL;
            mainLabel.classList.remove("title-box");

            makeCall("GET", "HomePage", null, (req) => {
                if (req.readyState == XMLHttpRequest.DONE) {
                    let message = req.responseText;
                    if (req.status == 200) {
                        playlists = JSON.parse(message);
                        playlistGrid(playlists);
                    } else {
                        alert("Cannot recover data. Maybe the User has been logged out.");
                    }
                }
            });
        }

        function loadButtons() {
            // Rimpiazzo per rimuovere vecchi event listener
            let modalButton = document.getElementById("track-selector-modal-button");
            let newButton = modalButton.cloneNode(true);
            modalButton.parentNode.replaceChild(newButton, modalButton);
            newButton.textContent = "Nuova Playlist";
            newButton.addEventListener("click", () => {
                // Backend originale si aspetta il select con id track-selector-create
                loadUserTracks(document.getElementById("track-selector-create"));
                showModal(document.getElementById("create-playlist"));
            });

            // rimosso bottone chiudi globale

            // Evita invio implicito con Enter (playlist form)
            document.getElementById("create-playlist").getElementsByTagName("form").item(0)
                .addEventListener("submit", (e) => e.preventDefault());

            // Submit create playlist
            document.getElementById("create-playlist-btn").addEventListener("click", function () {
                let self = this;
                let form = this.closest("form");
                if (form.checkValidity()) {
                    makeCall("POST", "CreatePlaylist", form, function (req) {
                        if (req.readyState == XMLHttpRequest.DONE) {
                            let message = req.responseText;
                            switch (req.status) {
                                case 201: {
                                    let newPlaylist = JSON.parse(message);
                                    // Ricarico l'intera vista home per garantire ordine e consistenza (più semplice e sicuro)
                                    try { homeView.show(); } catch(_){ /* fallback silenzioso */ }
                                    showToast('Playlist creata con successo');
                                    // Chiudo modal
                                    closeModalById('create-playlist');
                                    form.reset();
                                    break; }
                                case 409:
                                    showToast(message || 'Playlist già esistente');
                                    break;
                                case 400:
                                    showToast(message || 'Dati non validi');
                                    break;
                                case 500:
                                    showToast(message || 'Errore interno');
                                    break;
                            }
                        }
                    }, false);
                } else form.reportValidity();
            });

            // Submit upload track
            document.getElementById("upload-track-btn").addEventListener("click", function () {
                let self = this;
                let form = this.closest("form");
                if (form.checkValidity()) {
                    makeCall("POST", "UploadTrack", form, function (req) {
                        if (req.readyState == XMLHttpRequest.DONE) {
                            let message = req.responseText;
                            switch (req.status) {
                                case 201:
                                    showToast('Brano caricato con successo');
                                    closeModalById('upload-track');
                                    form.getElementsByTagName("input").item(0).value = "";
                                    document.getElementById("musicTrack").value = "";
                                    break;
                                case 409:
                                    showToast(message || 'File già presente');
                                    break;
                                case 400:
                                    showToast(message || 'Dati mancanti o non validi');
                                    break;
                                case 500:
                                    showToast(message || 'Errore interno');
                                    break;
                            }
                        }
                    }, false);
                } else form.reportValidity();
            });

            // Mostra pulsanti principali
            document.getElementById("upload-track-modal-button").className = "button";
            document.getElementById("track-selector-modal-button").className = "button";
            // Bottone indietro rimosso definitivamente: nessuna creazione
        }

        function playlistGrid(playlists) {
            const container = document.getElementById("playlistsGrid");
            if (!container) return;
            container.innerHTML = "";
            playlists.forEach(p => container.appendChild(createPlaylistCard(p)));
        }

        function createPlaylistCard(playlist) {
            // Wrapper card stile pure_html adattato e reso a colonna singola
            const card = document.createElement("div");
            card.className = "playlist-card-row";
            card.setAttribute('data-playlist-id', playlist.id);
            card.addEventListener("click", (e) => { playlistView.show(playlist); currentGroup = 0; });

            const title = document.createElement("h3");
            title.className = "pl-name";
            title.textContent = playlist.title;
            card.appendChild(title);

            // Icona reorder posizionata a destra (solo titolo richiesto)
            const reorderBtn = document.createElement("button");
            reorderBtn.type = "button";
            reorderBtn.className = "playlist-reorder-inline";
            reorderBtn.setAttribute("aria-label", "Riordina " + playlist.title);
            reorderBtn.innerHTML = '<img src="img/reorder/reorder.svg" width="20" height="20" alt="Reorder" />';
            reorderBtn.addEventListener('click', (e) => {
                e.stopPropagation();
                // ensure any previous modal removed to avoid stale state
                const existing = document.getElementById('reorder-tracks-modal'); if (existing) existing.remove();
                loadReorderModal(playlist);
                const modal = document.getElementById('reorder-tracks-modal');
                if (modal) showModal(modal);
            });
            card.appendChild(reorderBtn);
            console.log("Reorder button created and appended for playlist:", playlist.title);
            return card;
        }

        // ===== Modali =====
        function loadCreatePlaylistModal() {
            let modalContainer = document.getElementById("modals"),
                modal = createModal("create-playlist", "Crea nuova playlist", "create-playlist-btn", "Crea"),
                form = modal.getElementsByTagName("form").item(0), navbar = form.firstChild;
            form.classList.add("modal-form-grid");
            let titleInput = document.createElement("input");
            titleInput.type = "text"; titleInput.className = "field"; titleInput.name = "playlistTitle"; titleInput.placeholder = "Titolo"; titleInput.required = true;
            form.insertBefore(titleInput, navbar);
            // Backend avanzato: id = track-selector-create, name = selectedTrackIds
            let label = document.createElement("label"); label.className = "label"; label.htmlFor = "track-selector-create"; label.textContent = "Seleziona i brani da aggiungere:"; form.insertBefore(label, navbar);
            // Instead of a <select multiple>, render a scrollable list of checkboxes so the user
            // can pick non-contiguous tracks. Backend expects form fields named "selectedTrackIds"
            // (multiple values) so we create inputs with that name.
            const selector = document.createElement("div");
            selector.className = "track-multi-checkbox-list";
            selector.id = "track-selector-create"; // preserved id for existing callers
            // wrapper custom scrollbar (same wrapper class used elsewhere so CSS applies)
            const wrap = document.createElement("div");
            wrap.className = "track-multi-wrapper";
            wrap.appendChild(selector);
            const sb = document.createElement("div"); sb.className = "track-multi-scrollbar";
            const track = document.createElement("div"); track.className = "track-multi-scrollbar-track";
            const thumb = document.createElement("div"); thumb.className = "track-multi-scrollbar-thumb";
            track.appendChild(thumb); sb.appendChild(track); wrap.appendChild(sb);
            form.insertBefore(wrap, navbar);
            // setup custom scrollbar sync
            initCustomScrollbar(wrap, selector);
            form.insertBefore(document.createElement("div"), navbar); // area messaggi
            modalContainer.appendChild(modal);
        }

        function loadUploadTrackModal() {
            let modalContainer = document.getElementById("modals"),
                modal = createModal("upload-track", "Carica Brano", "upload-track-btn", "Carica"),
                form = modal.getElementsByTagName("form").item(0), navbar = form.firstChild;
            form.classList.add("modal-form-grid");
            // Campi base
            const addInput = (name, placeholder) => { let i = document.createElement("input"); i.type = "text"; i.className = "field"; i.name = name; i.placeholder = placeholder; i.required = true; form.insertBefore(i, navbar); };
            addInput("title", "Titolo");
            addInput("artist", "Artista");
            addInput("album", "Album");
            // Select Year
            let yearSel = document.createElement("select"); yearSel.className = "field"; yearSel.id = "year-selection"; yearSel.name = "year"; yearSel.required = true; form.insertBefore(yearSel, navbar);
            // Select Genre
            let genreSel = document.createElement("select"); genreSel.className = "field"; genreSel.id = "genre-selection"; genreSel.name = "genre"; genreSel.required = true; form.insertBefore(genreSel, navbar);
            // File track
            let trackLabel = document.createElement("label"); trackLabel.className = "label"; trackLabel.htmlFor = "musicTrack"; trackLabel.textContent = "Brano:"; form.insertBefore(trackLabel, navbar);
            let trackInput = document.createElement("input"); trackInput.type = "file"; trackInput.name = "musicTrack"; trackInput.id = "musicTrack"; trackInput.required = true; form.insertBefore(trackInput, navbar);
            // File image
            let imageLabel = document.createElement("label"); imageLabel.className = "label"; imageLabel.htmlFor = "image"; imageLabel.textContent = "Immagine album:"; form.insertBefore(imageLabel, navbar);
            let imageInput = document.createElement("input"); imageInput.type = "file"; imageInput.name = "image"; imageInput.id = "image"; imageInput.required = true; form.insertBefore(imageInput, navbar);
            form.insertBefore(document.createElement("div"), navbar);
            modalContainer.appendChild(modal);
        }

        // Modal riordino (manuale, stile amico)
        function loadReorderModal(playlist) {
            let modalContainer = document.getElementById("modals");
            let modal_div = document.createElement("div");
            modal_div.setAttribute("id", "reorder-tracks-modal");
            modal_div.setAttribute("class", "modal-window");
            let inner_div = document.createElement("div");
            inner_div.className = "modal-content"; // assegna stile pannello bianco con bordo rosa
            let top_nav_bar = document.createElement("div"); top_nav_bar.className = "nav-bar";
            let title_div = document.createElement("div"); title_div.className = "modal-title"; title_div.textContent = "Riordina brani in " + playlist.title.toString();
            let spacer = document.createElement("div"); spacer.className = "spacer";
            let modal_close = document.createElement("button"); modal_close.type = "button"; modal_close.title = "Chiudi"; modal_close.className = "modal-close"; modal_close.textContent = "Chiudi"; modal_close.addEventListener("click", () => closeReorderModal());
            top_nav_bar.appendChild(title_div); top_nav_bar.appendChild(spacer); top_nav_bar.appendChild(modal_close);
            let main_form = document.createElement("form");
            let label = document.createElement("label"); label.className = "label"; label.htmlFor = "track-reorder"; label.textContent = "Trascina i brani per riordinarli:";
            let ordered_list = document.createElement("ol"); ordered_list.name = "reorderingTracks"; ordered_list.id = "track-reorder";
            loadUserTracksOl(ordered_list, playlist);
            let bottom_div = document.createElement("div"); bottom_div.className = "nav-bar";
            let reorder_track_btn = document.createElement("button"); reorder_track_btn.id = "track-reorder-btn"; reorder_track_btn.className = "button"; reorder_track_btn.type = "button"; reorder_track_btn.textContent = "Salva ordine"; reorder_track_btn.addEventListener("click", (e) => saveOrder(e, playlist.id.toString()));
            bottom_div.appendChild(reorder_track_btn);
            main_form.appendChild(label); main_form.appendChild(ordered_list); main_form.appendChild(document.createElement("div")); main_form.appendChild(bottom_div);
            inner_div.appendChild(top_nav_bar); inner_div.appendChild(main_form); modal_div.appendChild(inner_div); modalContainer.appendChild(modal_div);
        }
        window.loadReorderModal = loadReorderModal;

        function closeReorderModal() {
            let modal_div = document.getElementById("reorder-tracks-modal");
            if (modal_div) modal_div.remove();
        }
        window.closeReorderModal = closeReorderModal;

        // Drag & Drop per riordino
        let startElement; // elemento trascinato
        function dragStart(event) { startElement = event.target; }
        function dragOver(event) { event.preventDefault(); }
        function dragLeave(event) { /* no-op semplice */ }
        function drop(event) {
            let finalDest = event.target;
            let completeList = finalDest.closest("ol");
            let songsArray = Array.from(completeList.querySelectorAll("li"));
            let indexDest = songsArray.indexOf(finalDest);
            if (songsArray.indexOf(startElement) < indexDest) {
                startElement.parentElement.insertBefore(startElement, songsArray[indexDest + 1]);
            } else {
                startElement.parentElement.insertBefore(startElement, songsArray[indexDest]);
            }
        }
        function loadUserTracksOl(trackSelector, playlist) {
            trackSelector.innerHTML = "";
            makeCall("GET", "Playlist?playlistId=" + playlist.id, null, function (req) {
                if (req.readyState == XMLHttpRequest.DONE) {
                    if (req.status == 200) {
                        let tracks = JSON.parse(req.responseText);
                        tracks.forEach(track => {
                            let li = document.createElement("li");
                            li.draggable = true; li.addEventListener("dragstart", dragStart); li.addEventListener("dragover", dragOver); li.addEventListener("dragleave", dragLeave); li.addEventListener("drop", drop);
                            // Visualizza solo dati utente (titolo, artista, anno) senza ID globale
                            li.dataset.trackId = track.id; // conserva ID reale per salvataggio ordine
                            li.textContent = track.artist + " - " + track.title + " (" + track.year + ")";
                            //  li.title = "ID DB: " + track.id; // tooltip debug opzionale
                            trackSelector.appendChild(li);
                        });
                    } else alert("Impossibile recuperare i dati. Forse la sessione è scaduta.");
                }
            });
        }
        function saveOrder(e, _playlistId) {
            let songsContainer = document.getElementById("track-reorder");
            // Estrae gli ID reali nell'ordine corrente
            let _trackIds = Array.from(songsContainer.querySelectorAll("li")).map(li => parseInt(li.dataset.trackId));
            let req = new XMLHttpRequest();
            let target = e.target;
            // attempt to disable save button to prevent duplicates
            try { const btn = document.getElementById('track-reorder-btn'); if (btn) btn.disabled = true; } catch(_){}
            req.onreadystatechange = function () {
                if (req.readyState == XMLHttpRequest.DONE) {
                    let message = req.responseText;
                    switch (req.status) {
                        case 200:
                            showToast('Ordine riordinato con successo');
                            closeReorderModal();
                            try {
                                    // Try to open the playlist from memory
                                    const pid = parseInt(_playlistId);
                                    let found = playlists.find(p => p.id == pid || p.id === _playlistId || p.id === String(pid));
                                    if (found) {
                                        try { playlistView.show(found); } catch(_){ }
                                    }
                                    // refresh home playlists list visually
                                    refreshHomePlaylists();
                            } catch(_){}
                            // re-enable save button
                            try { const btn = document.getElementById('track-reorder-btn'); if (btn) btn.disabled = false; } catch(_){}
                            break;
                        case 500:
                            showToast(message || 'Errore nel salvataggio dell\'ordine');
                            closeReorderModal();
                            try { const btn = document.getElementById('track-reorder-btn'); if (btn) btn.disabled = false; } catch(_){}
                            break;
                    }
                }
            };
            let requestData = { trackIds: _trackIds, playlistId: _playlistId };
            req.open("POST", "TrackReorder");
            req.setRequestHeader("Content-Type", "application/json;charset=UTF-8");
            req.send(JSON.stringify(requestData));
        }
    }

    // =================== PLAYLIST VIEW ===================
    function PlaylistView() {
        const PLAYLIST_PAGE_ID = "playlist";

        this.show = function (playlist) {
            clearBottomNavbar();
            clearModals();
            loadAddTracksModal();
            loadPlaylistView(playlist);
            // Nascondi upload nella vista playlist
            document.getElementById("upload-track-modal-button").className = "button hidden";
            document.getElementById("track-selector-modal-button").className = "button";
            showBackToPlaylist(false); // non serve in lista tracce
        };

        function renderTrackPage(tracksPage, wrapper) {
            // wrapper è già stato creato con classe tracks-wrapper
            const container = document.createElement("div");
            container.className = "tracks-vertical";
            wrapper.appendChild(container);
            tracksPage.forEach(t => container.appendChild(buildTrackCard(t)));
            if (tracksPage.length === 0) {
                const empty = document.createElement("div");
                empty.textContent = "Nessuna traccia in questa playlist";
                empty.className = "empty-state";
                container.appendChild(empty);
            }
        }

        function buildTrackCard(track) {
            const card = document.createElement("div");
            card.className = "track-card";
            card.tabIndex = 0;
            // dataset per mini player
            if(track.song_path) card.dataset.audio = normalizeMediaPath(track.song_path);
            // Gestione click / doppio click robusta:
            // 1) Usiamo nativo dblclick per immediatezza.
            // 2) Manteniamo fallback timer per utenti che cliccano molto veloce ma il browser non genera dblclick.
            let singleTimer = null;
            let lastClickTime = 0;
            const SINGLE_DELAY = 280; // leggermente più ampio
            card.addEventListener('dblclick', (e)=> {
                e.preventDefault();
                if(singleTimer){ clearTimeout(singleTimer); singleTimer=null; }
                openMiniFromCard(card);
            });
            card.addEventListener('click', (e)=> {
                const now = Date.now();
                if(now - lastClickTime < 280){
                    // È già gestito da dblclick nella maggior parte dei browser, ma fallback:
                    if(singleTimer){ clearTimeout(singleTimer); singleTimer=null; }
                    openMiniFromCard(card);
                    return;
                }
                lastClickTime = now;
                if(singleTimer) clearTimeout(singleTimer);
                singleTimer = setTimeout(()=> { singleTimer=null; trackView.show(track); }, SINGLE_DELAY);
            });
            card.addEventListener("keydown", (e) => { if (e.key === "Enter" || e.key === " ") { e.preventDefault(); trackView.show(track); }});

            // Cover
            const cover = document.createElement("div");
            cover.className = "track-cover";
            if (track.image_path && track.image_path.trim() !== "") {
                const img = document.createElement("img");
                img.src = normalizeMediaPath(track.image_path);
                img.alt = "Cover album";
                cover.appendChild(img);
            } else {
                const placeholder = document.createElement("div");
                placeholder.className = "track-cover--placeholder";
                placeholder.textContent = "♪";
                cover.appendChild(placeholder);
            }
            card.appendChild(cover);

            // Info
            const info = document.createElement("div");
            info.className = "track-info";
            const titleDiv = document.createElement("div");
            titleDiv.className = "track-title";
            titleDiv.textContent = track.title;
            info.appendChild(titleDiv);
            // Seconda riga Album • Year
            const parts = [];
            if (track.album_title) parts.push(track.album_title);
            if (track.year) parts.push(track.year.toString());
            if (parts.length > 0) {
                const sub = document.createElement("div");
                sub.className = "track-sub";
                sub.textContent = parts.join(" • ");
                info.appendChild(sub);
            }
            card.appendChild(info);

            // (Placeholder azioni future) potenziale colonna azioni rimossa per semplicità
            return card;
        }

        function loadPlaylistGroup(playlist, group) {
            cleanMain();
            let mainLabel = document.getElementsByClassName("main-label").item(0);
            mainLabel.id = PLAYLIST_PAGE_ID; mainLabel.textContent = playlist.title;
            lastPlaylist = playlist;
            // Wrapper per tracce (per margini coerenti con playlist)
            const tracksWrapper = document.createElement("div");
            tracksWrapper.className = "tracks-wrapper";
            const mainEl = document.getElementById("main");
            mainEl.appendChild(tracksWrapper);
            makeCall("GET", `PlaylistGroup?playlistId=${playlist.id}&group=${group}`, null, (req) => {
                if (req.readyState === XMLHttpRequest.DONE) {
                    if (req.status === 200) {
                        const payload = JSON.parse(req.responseText);
                        currentGroup = payload.group; // in caso di clamp lato server
                        totalGroups = payload.totalGroups;
                        renderTrackPage(payload.tracks, tracksWrapper);
                        renderPagination(tracksWrapper);
                    } else {
                        alert("Errore nel recupero della pagina tracce");
                    }
                }
            });
        }

        function loadPlaylistView(playlist) {
            currentPlaylistId = playlist.id;
            currentGroup = 0;
            loadPlaylistGroup(playlist, currentGroup);
            // Rimpiazzo pulsante per usare add-tracks
            let modalButton = document.getElementById("track-selector-modal-button");
            let newButton = modalButton.cloneNode(true);
            modalButton.parentNode.replaceChild(newButton, modalButton);
            newButton.textContent = "Aggiungi brani";
            newButton.addEventListener("click", () => {
                // Usa l'id originale della select per aggiungere tracce
                loadUserTracks(document.getElementById("track-selector-add"), playlist);
                showModal(document.getElementById("add-tracks"));
            });

            document.getElementById("add-tracks-btn").addEventListener("click", function () {
                let self = this; let form = this.closest("form");
                if (form.checkValidity()) {
                    makeCall("POST", "AddTracks?playlistId=" + playlist.id, form, function (req) {
                        if (req.readyState == XMLHttpRequest.DONE) {
                            if (req.status == 201) {
                                // Dopo aggiunta tracce riparti sempre dal primo gruppo (group=0)
                                currentGroup = 0;
                                loadPlaylistGroup(playlist, 0);
                                loadUserTracks(document.getElementById("track-selector-add"), playlist);
                                showToast('Tracce aggiunte con successo');
                                closeModalById('add-tracks');
                                form.reset();
                            } else {
                                if (req.status === 409) {
                                    showToast(req.responseText || 'Alcune tracce sono già presenti');
                                } else if (req.status === 400) {
                                    showToast(req.responseText || 'Richiesta non valida');
                                } else {
                                    showToast(req.responseText || 'Errore');
                                }
                            }
                        }
                    }, false);
                } else form.reportValidity();
            });

            // bottom navbar legacy rimossa: usiamo pagination dentro wrapper
        }

        function loadAddTracksModal() {
            let modalContainer = document.getElementById("modals"),
                modal = createModal("add-tracks", "Aggiungi brani alla playlist", "add-tracks-btn", "Aggiungi"),
                form = modal.getElementsByTagName("form").item(0), navbar = form.firstChild;
            form.classList.add("modal-form-grid");
            let label = document.createElement("label"); label.className = "label"; label.htmlFor = "track-selector-add"; label.textContent = "Seleziona i brani da aggiungere:"; form.insertBefore(label, navbar);
            // Checkbox list for add-tracks modal. Inputs will carry name selectedTracksIds.
            const selector = document.createElement("div");
            selector.className = "track-multi-checkbox-list";
            selector.id = "track-selector-add"; // preserved id
            const wrap = document.createElement("div");
            wrap.className = "track-multi-wrapper";
            wrap.appendChild(selector);
            const sb = document.createElement("div"); sb.className = "track-multi-scrollbar";
            const track = document.createElement("div"); track.className = "track-multi-scrollbar-track";
            const thumb = document.createElement("div"); thumb.className = "track-multi-scrollbar-thumb";
            track.appendChild(thumb); sb.appendChild(track); wrap.appendChild(sb);
            form.insertBefore(wrap, navbar);
            initCustomScrollbar(wrap, selector);
            form.insertBefore(document.createElement("div"), navbar);
            modalContainer.appendChild(modal);
        }

        function renderPagination(wrapper) {
            let nav = document.createElement("div");
            nav.className = "tracks-pagination";
            wrapper.appendChild(nav);

            // Prev (solo se non primo gruppo)
            if(currentGroup > 0){
                const prev = document.createElement("button");
                prev.type = "button"; prev.className = "button left"; prev.textContent = "Precedenti";
                prev.addEventListener("click", () => {
                    loadPlaylistGroup({id: currentPlaylistId, title: lastPlaylist.title}, currentGroup - 1);
                });
                nav.appendChild(prev);
            }
            // Indicatore centrale
            const indicator = document.createElement("span");
            indicator.className = "center";
            indicator.textContent = totalGroups > 0 ? `Pagina ${currentGroup + 1} / ${totalGroups}` : "Nessun brano";
            nav.appendChild(indicator);
            // Next (solo se esiste gruppo successivo)
            if(currentGroup + 1 < totalGroups){
                const next = document.createElement("button");
                next.type = "button"; next.className = "button right"; next.textContent = "Successivi";
                next.addEventListener("click", () => {
                    loadPlaylistGroup({id: currentPlaylistId, title: lastPlaylist.title}, currentGroup + 1);
                });
                nav.appendChild(next);
            }
        }
    }

    // =================== CUSTOM SCROLLBAR LOGIC ===================
    function initCustomScrollbar(wrapper, selectEl){
        const thumb = wrapper.querySelector('.track-multi-scrollbar-thumb');
        if(!thumb) return;
        const trackH = ()=> wrapper.querySelector('.track-multi-scrollbar-track').clientHeight;
        const thumbH = ()=> thumb.clientHeight;
        const maxScroll = ()=> selectEl.scrollHeight - selectEl.clientHeight;
        const maxThumbOffset = ()=> trackH() - thumbH();

        function syncFromSelect(){
            if(maxScroll()<=0){ thumb.style.top = '0px'; return; }
            const ratio = selectEl.scrollTop / maxScroll();
            thumb.style.top = (ratio * maxThumbOffset()) + 'px';
        }
        selectEl.addEventListener('scroll', syncFromSelect);
        window.addEventListener('resize', syncFromSelect);
        // Drag
        let dragging=false, startY=0, startTop=0;
        thumb.addEventListener('mousedown', e=>{
            dragging=true; startY=e.clientY; startTop=parseFloat(getComputedStyle(thumb).top)||0; wrapper.classList.add('dragging'); e.preventDefault();
        });
        document.addEventListener('mousemove', e=>{
            if(!dragging) return; const dy=e.clientY-startY; let newTop=startTop+dy; if(newTop<0) newTop=0; const limit=maxThumbOffset(); if(newTop>limit) newTop=limit; thumb.style.top=newTop+'px';
            const ratio = limit>0 ? newTop/limit : 0; selectEl.scrollTop = ratio * maxScroll();
        });
        document.addEventListener('mouseup', ()=>{ if(dragging){ dragging=false; wrapper.classList.remove('dragging'); }});
        // Wheel su barra esterna
        const ext = wrapper.querySelector('.track-multi-scrollbar');
        ext.addEventListener('wheel', e=>{ e.preventDefault(); selectEl.scrollTop += e.deltaY; });
        // Prima sync
        syncFromSelect();
        // Osserva cambi numero option (mutations) per risincronizzare
        const mo = new MutationObserver(syncFromSelect); mo.observe(selectEl,{childList:true});
    }

    // =================== TRACK VIEW ===================
    function TrackView() {
        const PLAYER_PAGE_ID = "player";
        this.show = function (track) {
            clearModals();
            clearBottomNavbar();
            loadSingleTrack(track);
            showBackToPlaylist(true); // in player mostra il bottone indietro
            // Imposta sfondo blur con cover (se disponibile)
            try { setPlayerBackground(track?.image_path); } catch(_) {}
        };
        function trackPlayer(container, track) {
            container.innerHTML = "";
            // Wrapper principale (ora direttamente con bordo rosa)
            const playerWrapper = document.createElement("div");
            playerWrapper.className = "player-track-info"; // layout flessibile e box principale

            // Colonna immagine (se presente)
            if (track.image_path) {
                const coverWrap = document.createElement("div");
                coverWrap.className = "player-cover";
                const img = document.createElement("img");
                img.src = normalizeMediaPath(track.image_path);
                img.alt = "Cover";
                img.width = 200; img.height = 200;
                coverWrap.appendChild(img);
                playerWrapper.appendChild(coverWrap);
            }

            // Colonna testi + audio
            const textCol = document.createElement("div");
            textCol.className = "player-text"; // contiene righe con ellipsis
            textCol.style.minWidth = "0"; // necessario per ellipsis in flex

            function metaLine(txt, cls) {
                const d = document.createElement("div");
                d.className = cls + " ellipsis-1"; // applica ellipsis una riga
                d.textContent = txt;
                // rimosso: niente attributo title per evitare tooltip nativo su hover
                // se serve accessibilità senza tooltip, valutare: d.setAttribute('aria-label', txt);
                return d;
            }

            if (track.title) textCol.appendChild(metaLine(track.title, "player-title"));
            if (track.artist) textCol.appendChild(metaLine(track.artist, "player-artist"));
            // Album + anno + genere mantenendo una riga ciascuno (come richiesto "ogni dato su una riga")
            if (track.album_title) textCol.appendChild(metaLine(track.album_title, "player-album"));
            if (track.year) textCol.appendChild(metaLine(track.year.toString(), "player-year"));
            if (track.genre) textCol.appendChild(metaLine(track.genre, "player-genre"));

            // Separatore visivo leggero (opzionale)
            const hr = document.createElement("hr");
            hr.className = "player-sep";
            textCol.appendChild(hr);

            // Audio element
            const audio = document.createElement("audio");
            audio.controls = true;
            const src = document.createElement("source");
            src.src = normalizeMediaPath(track.song_path);
            src.type = "audio/mpeg";
            audio.appendChild(src);
            textCol.appendChild(audio);

            playerWrapper.appendChild(textCol);
            container.appendChild(playerWrapper);
        }
        function loadSingleTrack(track) {
            cleanMain();
            document.getElementById("upload-track-modal-button").className = "button hidden";
            document.getElementById("track-selector-modal-button").className = "button hidden";
            let mainLabel = document.getElementsByClassName("main-label").item(0); mainLabel.id = PLAYER_PAGE_ID; mainLabel.textContent = track.title;
            lastTrack = track;
            // Stack rimosso: niente tracking traccia
            makeCall("GET", "Track?track_id=" + track.id, null, (req) => {
                if (req.readyState == XMLHttpRequest.DONE) {
                    if (req.status == 200) {
                        let t = JSON.parse(req.responseText); if (!t) { alert("This Track can't be played."); return; }
                        trackPlayer(document.getElementById("main"), t);
                        // Aggiorna sfondo con eventuale path normalizzato ricevuto dal server
                        try { setPlayerBackground(t?.image_path); } catch(_) {}
                    } else alert("Cannot recover data. Maybe the User has been logged out.");
                }
            });
        }
    }

    // =================== MAIN LOADER ===================
    function MainLoader() {
        this.start = function () {
            document.getElementById("logout-button").addEventListener("click", () => {
                makeCall("GET", "Logout", null, (req) => {
                    if (req.readyState == XMLHttpRequest.DONE && req.status == 200) {
                        location.href = "login.html";
                    }
                });
            });
            document.getElementById("homepage-button").addEventListener("click", () => { clearPlayerBackground(); homeView.show(); });
            const backBtn = document.getElementById("back-to-playlist-button");
            if (backBtn) backBtn.addEventListener("click", () => {
                clearPlayerBackground();
                if (lastPlaylist) { playlistView.show(lastPlaylist); }
                else { homeView.show(); }
            });
            document.getElementById("upload-track-modal-button").addEventListener("click", () => {
                loadYears();
                loadGenres();
                showModal(document.getElementById("upload-track"));
            });
            // Previous global handler removed. Per-card reorder buttons provide click handlers directly.
        };
        this.refreshPage = function () { homeView.show(); };
        function loadYears() {
            let today = new Date().getFullYear();
            let sel = document.getElementById("year-selection"); if (!sel) return; sel.innerHTML = "";
            let opt = document.createElement("option"); opt.value = ""; opt.textContent = "Year"; sel.appendChild(opt);
            for (let y = today; y >= 1901; y--) { opt = document.createElement("option"); opt.textContent = y.toString(); sel.appendChild(opt); }
        }
        function loadGenres() {
            makeCall("GET", "genres.json", null, (req) => {
                if (req.readyState == XMLHttpRequest.DONE) {
                    let genres = (req.status == 200) ? JSON.parse(req.responseText) : [];
                    let sel = document.getElementById("genre-selection"); if (!sel) return; sel.innerHTML = "";
                    let opt = document.createElement("option"); opt.value = ""; opt.textContent = "Genre"; sel.appendChild(opt);
                    genres.forEach(g => { let o = document.createElement("option"); o.textContent = g; sel.appendChild(o); });
                }
            });
        }
    }
})();

// =================== MINI PLAYER (global) ===================
function openMiniFromCard(card){
    const audioEl = document.getElementById('miniAudio');
    const mp = document.getElementById('miniPlayer');
    if(!audioEl || !mp) return;
    const cards = Array.from(document.querySelectorAll('.track-card[data-audio]'));
    const idx = cards.indexOf(card);
    const src = card.dataset.audio;
    const title = card.querySelector('.track-title')?.textContent||'';
    const artist = (card.querySelector('.track-sub')?.textContent||'').split('•')[0].trim();
    if(src){ audioEl.src = src; audioEl.play().catch(()=>{}); }
    document.getElementById('miniTitle').textContent = title;
    document.getElementById('miniArtist').textContent = artist || '';
    mp.classList.remove('hidden'); mp.setAttribute('aria-hidden','false');
    mp.dataset.index = idx.toString();
    document.body.classList.add('has-mini-player');
    updateMiniPrevNext(idx, cards.length);
    setMiniPlayIcon(true);
}
function updateMiniPrevNext(i,total){
    const prev = document.getElementById('miniPrev');
    const next = document.getElementById('miniNext');
    if(prev) prev.disabled = i<=0; if(next) next.disabled = i>=total-1;
}
function setMiniPlayIcon(isPlaying){
    const btn = document.getElementById('miniPlay');
    if(!btn) return;
    btn.innerHTML = isPlaying
        ? '<svg viewBox="0 0 24 24" width="16" height="16" fill="currentColor"><path d="M8 5h4v14H8zm6 0h4v14h-4z"/></svg>'
        : '<svg viewBox="0 0 24 24" width="16" height="16" fill="currentColor"><path d="M8 5v14l11-7z"/></svg>';
    btn.setAttribute('aria-label', isPlaying? 'Pausa' : 'Riproduci');
}
(function miniPlayerLogic(){
    const audio = document.getElementById('miniAudio');
    const play = document.getElementById('miniPlay');
    const prev = document.getElementById('miniPrev');
    const next = document.getElementById('miniNext');
    const mp = document.getElementById('miniPlayer');
    const closeBtn = document.getElementById('miniClose');
    if(!audio || !play){ console.warn('[miniPlayer] abort init: missing audio or play'); return; }
    if(!closeBtn){ console.warn('[miniPlayer] close button NOT found in DOM'); }
    play.addEventListener('click', ()=> {
        if(audio.paused){
            audio.play();
        }
        else {
            audio.pause();
        }
    });
    audio.addEventListener('play', ()=> { setMiniPlayIcon(true); });
    audio.addEventListener('pause', ()=> { setMiniPlayIcon(false);  });
    function move(dir){
        const cards = Array.from(document.querySelectorAll('.track-card[data-audio]'));
        if(!mp.dataset.index){ return; }
        let i = parseInt(mp.dataset.index,10);
        const ni = i + dir;
        if(ni <0 || ni>=cards.length){ return; }
        openMiniFromCard(cards[ni]);
    }
    prev?.addEventListener('click', ()=> move(-1));
    next?.addEventListener('click', ()=> move(1));
    audio.addEventListener('ended', ()=> move(1));
    // Close mini player: pause audio, hide container, clear src
    closeBtn?.addEventListener('click', ()=>{
        try{ audio.pause(); }catch(e){ console.warn('[miniPlayer] pause error', e); }
        audio.removeAttribute('src');
        mp.classList.add('hidden');
        mp.setAttribute('aria-hidden','true');
        mp.removeAttribute('data-index');
        document.body.classList.remove('has-mini-player');
        // Return focus to play button of last used track if possible
        const last = document.querySelector('.track-card[data-audio]');
        if(last && last.focus){ last.focus(); }
    });
})();

