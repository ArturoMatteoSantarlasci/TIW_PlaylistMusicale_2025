(function () {
    // =================== VARIABILI GLOBALI ===================
    let lastPlaylist = null, lastTrack = null;
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
    let tracklist, trackGroup = 0; // tracklist corrente e indice gruppo (pagine da 5)
    let homeView = new HomeView(), playlistView = new PlaylistView(), trackView = new TrackView();

    // Gestione bottone indietro solo per vista traccia -> playlist
    function showBackToPlaylist(show) {
        const btn = document.getElementById("back-to-playlist-button");
        if (!btn) return;
        if (show) btn.classList.remove("hidden"); else btn.classList.add("hidden");
    }

    // Avvio pagina
    window.addEventListener("load", () => {
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
            cleanMain();
            let mainLabel = document.getElementsByClassName("main-label").item(0);
            mainLabel.id = HOMEPAGE_ID;
            mainLabel.textContent = HOMEPAGE_LABEL;

            makeCall("GET", "HomePage", null, (req) => {
                if (req.readyState == XMLHttpRequest.DONE) {
                    let message = req.responseText;
                    if (req.status == 200) {
                        let playlists = JSON.parse(message);
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
            newButton.textContent = "Add Playlist";
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
                                    let itemsContainer = document.querySelector(".items-container");
                                    if (itemsContainer) itemsContainer.insertBefore(createPlaylistButton(newPlaylist), itemsContainer.firstChild);
                                    self.parentElement.previousElementSibling.setAttribute("class", "success");
                                    self.parentElement.previousElementSibling.textContent = "Playlist creata con successo";
                                    form.reset();
                                    break; }
                                case 409:
                                    self.parentElement.previousElementSibling.setAttribute("class", "error");
                                    self.parentElement.previousElementSibling.textContent = message || "Playlist già esistente";
                                    break;
                                case 400:
                                    self.parentElement.previousElementSibling.setAttribute("class", "error");
                                    self.parentElement.previousElementSibling.textContent = message || "Dati non validi";
                                    break;
                                case 500:
                                    self.parentElement.previousElementSibling.setAttribute("class", "error");
                                    self.parentElement.previousElementSibling.textContent = message || "Errore interno";
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
                                    self.parentElement.previousElementSibling.setAttribute("class", "success");
                                    self.parentElement.previousElementSibling.textContent = "Brano caricato con successo";
                                    form.getElementsByTagName("input").item(0).value = "";
                                    document.getElementById("musicTrack").value = "";
                                    break;
                                case 409:
                                    self.parentElement.previousElementSibling.setAttribute("class", "error");
                                    self.parentElement.previousElementSibling.textContent = message || "File già presente";
                                    break;
                                case 400:
                                    self.parentElement.previousElementSibling.setAttribute("class", "error");
                                    self.parentElement.previousElementSibling.textContent = message || "Dati mancanti o non validi";
                                    break;
                                case 500:
                                    self.parentElement.previousElementSibling.setAttribute("class", "error");
                                    self.parentElement.previousElementSibling.textContent = message || "Errore interno";
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
            const main = document.getElementById("main");
            const container = document.createElement("div");
            container.className = "playlists-vertical"; // nuova lista verticale
            main.appendChild(container);
            playlists.forEach(p => container.appendChild(createPlaylistCard(p)));
        }

        function createPlaylistCard(playlist) {
            // Wrapper card stile pure_html adattato e reso a colonna singola
            const card = document.createElement("div");
            card.className = "pl-card playlist-card-row";
            card.addEventListener("click", () => { playlistView.show(playlist); trackGroup = 0; });

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
            reorderBtn.addEventListener("click", (e) => {
                e.stopPropagation();
                loadReorderModal(playlist);
                showModal(document.getElementById("reorder-tracks-modal"));
            });
            card.appendChild(reorderBtn);
            return card;
        }

        // ===== Modali =====
        function loadCreatePlaylistModal() {
            let modalContainer = document.getElementById("modals"),
                modal = createModal("create-playlist", "Create new playlist", "create-playlist-btn", "Create playlist"),
                form = modal.getElementsByTagName("form").item(0), navbar = form.firstChild;
            let titleInput = document.createElement("input");
            titleInput.type = "text"; titleInput.className = "text-field"; titleInput.name = "playlistTitle"; titleInput.placeholder = "Title"; titleInput.required = true;
            form.insertBefore(titleInput, navbar);
            form.insertBefore(document.createElement("br"), navbar);
            form.insertBefore(document.createElement("br"), navbar);
            // Backend avanzato: id = track-selector-create, name = selectedTrackIds
            let label = document.createElement("label"); label.className = "label"; label.htmlFor = "track-selector-create"; label.textContent = "Select songs to add:"; form.insertBefore(label, navbar); form.insertBefore(document.createElement("br"), navbar);
            let selector = document.createElement("select");
            selector.className = "text-field";
            selector.id = "track-selector-create";
            selector.name = "selectedTrackIds"; // backend avanzato si aspetta selectedTrackIds per la creazione
            selector.multiple = true;
            form.insertBefore(selector, navbar);
            form.insertBefore(document.createElement("div"), navbar); // area messaggi
            modalContainer.appendChild(modal);
        }

        function loadUploadTrackModal() {
            let modalContainer = document.getElementById("modals"),
                modal = createModal("upload-track", "Upload Track", "upload-track-btn", "Add track"),
                form = modal.getElementsByTagName("form").item(0), navbar = form.firstChild;
            // Campi base
            const addInput = (name, placeholder) => { let i = document.createElement("input"); i.type = "text"; i.className = "text-field"; i.name = name; i.placeholder = placeholder; i.required = true; form.insertBefore(i, navbar); form.insertBefore(document.createElement("br"), navbar); };
            addInput("title", "Title");
            addInput("artist", "Artist");
            addInput("album", "Album title");
            // Select Year
            let yearSel = document.createElement("select"); yearSel.className = "text-field"; yearSel.id = "year-selection"; yearSel.name = "year"; yearSel.required = true; form.insertBefore(yearSel, navbar); form.insertBefore(document.createElement("br"), navbar);
            // Select Genre
            let genreSel = document.createElement("select"); genreSel.className = "text-field"; genreSel.id = "genre-selection"; genreSel.name = "genre"; genreSel.required = true; form.insertBefore(genreSel, navbar); form.insertBefore(document.createElement("br"), navbar);
            // File track
            let trackLabel = document.createElement("label"); trackLabel.className = "label"; trackLabel.htmlFor = "musicTrack"; trackLabel.textContent = "Track:"; form.insertBefore(trackLabel, navbar);
            let trackInput = document.createElement("input"); trackInput.type = "file"; trackInput.name = "musicTrack"; trackInput.id = "musicTrack"; trackInput.required = true; form.insertBefore(trackInput, navbar); form.insertBefore(document.createElement("br"), navbar);
            // File image
            let imageLabel = document.createElement("label"); imageLabel.className = "label"; imageLabel.htmlFor = "image"; imageLabel.textContent = "Album image:"; form.insertBefore(imageLabel, navbar);
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
            let top_nav_bar = document.createElement("div"); top_nav_bar.className = "nav-bar";
            let title_div = document.createElement("div"); title_div.className = "modal-title"; title_div.textContent = "Reorder Tracks in " + playlist.title.toString();
            let spacer = document.createElement("div"); spacer.className = "spacer";
            let modal_close = document.createElement("button"); modal_close.type = "button"; modal_close.title = "Close"; modal_close.className = "modal-close"; modal_close.textContent = "Close"; modal_close.addEventListener("click", () => closeReorderModal());
            top_nav_bar.appendChild(title_div); top_nav_bar.appendChild(spacer); top_nav_bar.appendChild(modal_close);
            let main_form = document.createElement("form");
            let label = document.createElement("label"); label.className = "label"; label.htmlFor = "track-reorder"; label.textContent = "Drag the track to reorder:";
            let ordered_list = document.createElement("ol"); ordered_list.name = "reorderingTracks"; ordered_list.id = "track-reorder"; ordered_list.className = "text-field";
            loadUserTracksOl(ordered_list, playlist);
            let bottom_div = document.createElement("div"); bottom_div.className = "nav-bar";
            let reorder_track_btn = document.createElement("button"); reorder_track_btn.id = "track-reorder-btn"; reorder_track_btn.className = "button"; reorder_track_btn.type = "button"; reorder_track_btn.textContent = "Reorder Tracks"; reorder_track_btn.addEventListener("click", (e) => saveOrder(e, playlist.id.toString()));
            bottom_div.appendChild(reorder_track_btn);
            main_form.appendChild(label); main_form.appendChild(ordered_list); main_form.appendChild(document.createElement("div")); main_form.appendChild(bottom_div);
            inner_div.appendChild(top_nav_bar); inner_div.appendChild(main_form); modal_div.appendChild(inner_div); modalContainer.appendChild(modal_div);
        }

        function closeReorderModal() {
            let modal_div = document.getElementById("reorder-tracks-modal");
            if (modal_div) modal_div.remove();
        }

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
                            li.title = "ID DB: " + track.id; // tooltip debug opzionale
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
            req.onreadystatechange = function () {
                if (req.readyState == XMLHttpRequest.DONE) {
                    let message = req.responseText;
                    switch (req.status) {
                        case 200:
                            target.parentElement.previousElementSibling.setAttribute("class", "success");
                            target.parentElement.previousElementSibling.textContent = message; break;
                        case 500:
                            target.parentElement.previousElementSibling.setAttribute("class", "error");
                            target.parentElement.previousElementSibling.textContent = message; break;
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

        function trackGrid(tracks) {
            const main = document.getElementById("main");
            cleanMain();
            const container = document.createElement("div");
            container.className = "tracks-vertical";
            main.appendChild(container);
            const MAX_PER_PAGE = 5;
            for (let i = 0; i < MAX_PER_PAGE; i++) {
                const track = tracks[i + trackGroup * MAX_PER_PAGE];
                if (!track) break;
                container.appendChild(buildTrackCard(track));
            }
        }

        function buildTrackCard(track) {
            const card = document.createElement("div");
            card.className = "track-card";
            card.tabIndex = 0;
            card.addEventListener("click", () => trackView.show(track));
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

        function loadPlaylistTracks(playlist) {
            cleanMain();
            let mainLabel = document.getElementsByClassName("main-label").item(0);
            mainLabel.id = PLAYLIST_PAGE_ID; mainLabel.textContent = playlist.title;
            lastPlaylist = playlist;
            makeCall("GET", "Playlist?playlistId=" + playlist.id, null, (req) => {
                if (req.readyState == XMLHttpRequest.DONE) {
                    if (req.status == 200) {
                        let tracks = JSON.parse(req.responseText);
                        if (tracks.length === 0) return; // nessun brano
                        tracklist = tracks;
                        trackGrid(tracks);
                        loadPrevNextButton();
                    } else alert("Cannot recover data. Maybe the User has been logged out.");
                }
            });
        }

        function loadPlaylistView(playlist) {
            loadPlaylistTracks(playlist);
            // Rimpiazzo pulsante per usare add-tracks
            let modalButton = document.getElementById("track-selector-modal-button");
            let newButton = modalButton.cloneNode(true);
            modalButton.parentNode.replaceChild(newButton, modalButton);
            newButton.textContent = "Add Tracks";
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
                                loadPlaylistTracks(playlist);
                                loadUserTracks(document.getElementById("track-selector-add"), playlist);
                                self.parentElement.previousElementSibling.className = "success";
                                self.parentElement.previousElementSibling.textContent = "Tracce aggiunte con successo";
                                form.reset();
                            } else {
                                self.parentElement.previousElementSibling.className = "error";
                                if (req.status === 409) {
                                    self.parentElement.previousElementSibling.textContent = req.responseText || "Alcune tracce sono già presenti";
                                } else if (req.status === 400) {
                                    self.parentElement.previousElementSibling.textContent = req.responseText || "Richiesta non valida";
                                } else {
                                    self.parentElement.previousElementSibling.textContent = req.responseText || "Errore";
                                }
                            }
                        }
                    }, false);
                } else form.reportValidity();
            });

            let bottomNavbar = document.createElement("div");
            bottomNavbar.id = "bottom-nav-bar"; bottomNavbar.className = "nav-bar";
            document.getElementById("main").after(bottomNavbar);
        }

        function loadAddTracksModal() {
            let modalContainer = document.getElementById("modals"),
                modal = createModal("add-tracks", "Add tracks to playlist", "add-tracks-btn", "Add tracks"),
                form = modal.getElementsByTagName("form").item(0), navbar = form.firstChild;
            let label = document.createElement("label"); label.className = "label"; label.htmlFor = "track-selector-add"; label.textContent = "Select songs to add:"; form.insertBefore(label, navbar); form.insertBefore(document.createElement("br"), navbar);
            let selector = document.createElement("select");
            selector.className = "text-field";
            selector.id = "track-selector-add";
            selector.name = "selectedTracksIds"; // backend avanzato per add tracks
            selector.multiple = true;
            form.insertBefore(selector, navbar);
            form.insertBefore(document.createElement("div"), navbar);
            modalContainer.appendChild(modal);
        }

        function loadPrevNextButton() {
            let navbar = document.getElementById("bottom-nav-bar");
            navbar.innerHTML = "";
            if (trackGroup > 0) {
                let bPrev = document.createElement("button"); bPrev.className = "button"; bPrev.type = "button"; bPrev.textContent = "Previous Tracks"; bPrev.addEventListener("click", () => { trackGroup--; trackGrid(tracklist); loadPrevNextButton(); }); navbar.appendChild(bPrev);
            }
            let spacer = document.createElement("div"); spacer.className = "spacer"; navbar.appendChild(spacer);
            if (tracklist.length > 5 * (1 + trackGroup)) {
                let bNext = document.createElement("button"); bNext.className = "button"; bNext.type = "button"; bNext.textContent = "Next Tracks"; bNext.addEventListener("click", () => { trackGroup++; trackGrid(tracklist); loadPrevNextButton(); }); navbar.appendChild(bNext);
            }
        }
    }

    // =================== TRACK VIEW ===================
    function TrackView() {
        const PLAYER_PAGE_ID = "player";
        this.show = function (track) {
            clearModals();
            clearBottomNavbar();
            loadSingleTrack(track);
            showBackToPlaylist(true); // in player mostra il bottone indietro
        };
        function trackPlayer(container, track) {
            container.innerHTML = "";
            // Wrapper principale del player
            const playerWrapper = document.createElement("div");
            playerWrapper.className = "player-track-info"; // nuovo layout flessibile

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
                d.title = txt; // hover mostra completo
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
            document.getElementById("homepage-button").addEventListener("click", () => homeView.show());
            const backBtn = document.getElementById("back-to-playlist-button");
            if (backBtn) backBtn.addEventListener("click", () => {
                if (lastPlaylist) {
                    playlistView.show(lastPlaylist);
                } else {
                    homeView.show(); // fallback di sicurezza
                }
            });
            document.getElementById("upload-track-modal-button").addEventListener("click", () => {
                loadYears();
                loadGenres();
                showModal(document.getElementById("upload-track"));
            });
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

