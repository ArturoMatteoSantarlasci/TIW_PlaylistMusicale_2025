(function() {

    /**
     * Centralized path resolver with context path support
     * Now using /media/ directory instead of /uploads/
     */
    // Auto-detected application context path (works for root, /ria, /ria_war_exploded, etc.)
    const __APP_CTX = (function () {
        // 1) Explicit override via data-context if present
        if (document.body && document.body.dataset && document.body.dataset.context) {
            return document.body.dataset.context;
        }
        // 2) Derive from current pathname: take first segment if it is not an .html file
        const parts = window.location.pathname.split("/").filter(Boolean);
        if (parts.length === 0) return ""; // running at root
        const first = parts[0];
        // If first segment looks like a file (contains a dot) => no context
        if (first.includes(".")) return "";
        return "/" + first; // e.g. /ria_war_exploded
    })();

    function withCtx(p) {
        if (!p) return "";
        const rel = p.startsWith("/") ? p : "/" + p;
        return __APP_CTX + rel;
    }

    // showModal, closeModal e loadUserTracks definiti in utils.js

    // Initialize all the global variables and objects
    let lastPlaylist = null, lastTrack = null;
    // Simple navigation history stack for undo (stores functions to execute)
    const viewHistory = [];
    function pushView(showFn) {
        if (typeof showFn === 'function') {
            viewHistory.push(showFn);
            const backBtn = document.getElementById("back-button");
            if (backBtn) backBtn.disabled = (viewHistory.length <= 1);
        }
    }
    function undoLastView() {
        // Pop current view
        viewHistory.pop();
        const prev = viewHistory[viewHistory.length - 1];
        if (prev) prev(); else homeView.show();
    }
    let tracklist, trackGroup = 0;
    let homeView = new HomeView(),
        playlistView = new PlaylistView(),
        trackView = new TrackView();

    // Load the HomePage via the MainLoader
    window.addEventListener("load", () => {
        let mainLoader = new MainLoader();
        mainLoader.start();
        mainLoader.refreshPage();
    })

    /**
     * Classe per gestire la homepage
     *
     * @constructor
     */
    function HomeView() {
    const HOMEPAGE_LABEL = "Le mie Playlist";
        const HOMEPAGE_ID = "homepage";

        /**
         * Mostra homepage tramite fun ausiliarie
         */
        this.show = function () {
            clearModals();
            clearBottomNavbar();
            loadCreatePlaylistModal();
            loadUploadTrackModal();
            loadButtons();
            loadPlaylists();
            pushView(() => homeView.show());
        }

        /**
         * Loads all the User Playlists.
         */
        function loadPlaylists() {
            // Ricostruisci il contenitore evitando di cancellare la struttura con cleanMain()
            const main = document.getElementById("main");
            main.innerHTML = '<div class="board"><section class="playlists-grid" id="playlistsGrid"></section></div>';

            const mainLabel = document.getElementsByClassName("main-label").item(0);
            if (mainLabel) {
                mainLabel.id = HOMEPAGE_ID;
                mainLabel.textContent = HOMEPAGE_LABEL;
            }

            makeCall("GET", "HomePage", null, (req) => {
                if (req.readyState === XMLHttpRequest.DONE) {
                    const message = req.responseText;
                    if (req.status === 200) {
                        let playlists = [];
                        try { playlists = JSON.parse(message) || []; } catch (_) { playlists = []; }
                        playlistGrid(playlists);
                    } else {
                        alert("Cannot recover data. Maybe the User has been logged out.");
                    }
                }
            });
        }

        /**
         * Load the Playlists.
         *
         * @param playlists array of Playlists
         */
        function playlistGrid(playlists) {
            let container = document.getElementById("playlistsGrid");
            if (!container) {
                // Fallback: ricrea struttura se mancata
                const main = document.getElementById("main");
                const board = document.createElement("div");
                board.className = "board";
                container = document.createElement("section");
                container.className = "playlists-grid";
                container.id = "playlistsGrid";
                board.appendChild(container);
                main.appendChild(board);
            }
            container.innerHTML = "";
            if (!Array.isArray(playlists) || playlists.length === 0) {
                const empty = document.createElement("div");
                empty.className = "pl-empty";
                empty.textContent = "Nessuna playlist";
                container.appendChild(empty);
                return;
            }
            playlists.forEach(p => container.appendChild(createPlaylistButton(p)));
        }
        /**
         * Load buttons in the top nav bar and button functionality in the sidebar.
         */
        function loadButtons() {
            // Replace the track selector button and add "create playlist" functionality; needed for removing already
            // present event listeners, as this button is also used for adding tracks to a playlist
            let modalButton = document.getElementById("track-selector-modal-button");
            let newButton = modalButton.cloneNode(true);
            modalButton.parentNode.replaceChild(newButton, modalButton);
            newButton.textContent = "Add Playlist"
            newButton.addEventListener("click", () => {
                console.debug("[CreatePlaylist] Open modal: loading user tracks into #track-selector-create");
                loadUserTracks(document.getElementById("track-selector-create"));
                console.debug("[CreatePlaylist] Showing create-playlist modal");
                showModal(document.getElementById("create-playlist"));
            });

            // Even if no submit button is present, forms with a single implicit submission blocking field still
            // submit when the Enter key is pressed
            // https://html.spec.whatwg.org/multipage/form-control-infrastructure.html#implicit-submission
            document.getElementById("create-playlist").getElementsByTagName("form")
                .item(0).addEventListener("submit", (e) => {
                e.preventDefault();
            });

            // Create new playlist
            document.getElementById("create-playlist-btn").addEventListener("click", function () {
                console.debug("[CreatePlaylist] Click submit button");
                let self = this;
                let form = this.closest("form");

                if (!form) {
                    console.error("[CreatePlaylist] Form not found via closest('form')");
                }
                console.debug("[CreatePlaylist] Form validity?", form.checkValidity());
                const titleVal = form.querySelector('input[name="playlistTitle"]').value;
                console.debug("[CreatePlaylist] playlistTitle=", titleVal);
                const selectedOptions = Array.from(form.querySelectorAll('#track-selector-create option:checked')).map(o=>o.value);
                console.debug("[CreatePlaylist] selectedTrackIds count=", selectedOptions.length, selectedOptions);

                if (form.checkValidity()) {
                    console.debug("[CreatePlaylist] Sending POST CreatePlaylist ...");
                    makeCall("POST", "CreatePlaylist", form, function (req) {
                        if (req.readyState === XMLHttpRequest.OPENED) {
                            console.debug("[CreatePlaylist][XHR] OPENED");
                        }
                        if (req.readyState === XMLHttpRequest.HEADERS_RECEIVED) {
                            console.debug("[CreatePlaylist][XHR] HEADERS_RECEIVED");
                        }
                        if (req.readyState === XMLHttpRequest.LOADING) {
                            console.debug("[CreatePlaylist][XHR] LOADING");
                        }
                        if (req.readyState == XMLHttpRequest.DONE) {
                            let message = req.responseText;
                            console.debug("[CreatePlaylist][XHR] DONE status=", req.status, "response=", message);
                            switch (req.status) {
                                case 201:
                                    let newPlaylist = JSON.parse(message);
                                    console.debug("[CreatePlaylist] Success newPlaylist=", newPlaylist);

                                    // Update the home view with newly created playlist
                                    let itemsContainer = document.getElementById("playlistsGrid");
                                    if (itemsContainer) {
                                        console.debug("[CreatePlaylist] Injecting new playlist card at top");
                                        itemsContainer.insertBefore(createPlaylistButton(newPlaylist), itemsContainer.firstChild);
                                    }

                                    self.parentElement.previousElementSibling.setAttribute("class", "successo");
                                    self.parentElement.previousElementSibling.textContent = "Playlist creata";
                                    form.reset();
                                    break;
                                case 409:
                                    console.warn("[CreatePlaylist] Conflict 409 response=", message);
                                    self.parentElement.previousElementSibling.setAttribute("class", "errore");
                                    self.parentElement.previousElementSibling.textContent = message;
                                    break;
                                case 400:
                                    console.warn("[CreatePlaylist] Bad Request 400 response=", message);
                                    self.parentElement.previousElementSibling.setAttribute("class", "errore");
                                    self.parentElement.previousElementSibling.textContent = message || "Dati non validi";
                                    break;
                                case 500:
                                    console.error("[CreatePlaylist] Server Error 500 response=", message);
                                    self.parentElement.previousElementSibling.setAttribute("class", "errore");
                                    self.parentElement.previousElementSibling.textContent = "Errore server";
                                    break;
                                default:
                                    console.error("[CreatePlaylist] Unexpected status=", req.status, "response=", message);
                                    self.parentElement.previousElementSibling.setAttribute("class", "errore");
                                    self.parentElement.previousElementSibling.textContent = "Errore (" + req.status + ")";
                            }
                        }
                    }, false);
                } else {
                    console.debug("[CreatePlaylist] form.reportValidity() triggered");
                    form.reportValidity();
                }
            });

            // Upload track
            document.getElementById("upload-track-btn").addEventListener("click", function () {
                let self = this;
                let form = this.closest("form");
                if (form.checkValidity()) {
                    makeCall("POST", "UploadTrack", this.closest("form"), function (req) {
                        let message = req.responseText;

                        if (req.readyState == XMLHttpRequest.DONE) {
                            switch (req.status) {
                                case 201:
                                    self.parentElement.previousElementSibling.setAttribute("class", "successo");
                                    self.parentElement.previousElementSibling.textContent = "Track caricato correttamente ";
                                    form.getElementsByTagName("input").item(0).value = "";
                                    (document.getElementById("musicTrack")).value = "";//chiedi
                                    break;
                                case 409:
                                    self.parentElement.previousElementSibling.setAttribute("class", "error");
                                    self.parentElement.previousElementSibling.textContent = message;
                                    break;
                                case 400:
                                    self.parentElement.previousElementSibling.setAttribute("class", "error");
                                    self.parentElement.previousElementSibling.textContent = "Dati invalidi: " + message;
                                    break;
                                case 500:
                                    self.parentElement.previousElementSibling.setAttribute("class", "error");
                                    self.parentElement.previousElementSibling.textContent = "Errore server interno";
                                    break;
                                default:
                                    self.parentElement.previousElementSibling.setAttribute("class", "error");
                                    self.parentElement.previousElementSibling.textContent = "Errore sconosciuto: " + req.status;
                            }

                        }
                    }, false);
                } else {
                    form.reportValidity();
                }
            });

            // show the upload and add track modal
            const uploadBtn = document.getElementById("upload-track-modal-button");
            if (uploadBtn) {
                uploadBtn.className = "button";
                uploadBtn.style.display = ""; // ensure visible when in homepage
            }
            const trackSelBtn = document.getElementById("track-selector-modal-button");
            if (trackSelBtn) trackSelBtn.className = "button";
        }

        /**
         * Creates and returns a wrapper div with playlist button and reorder button
         *
         * @param playlist
         */
        function createPlaylistButton(playlist) {
            // Pure_html style card
            const card = document.createElement("div");
            card.className = "pl-card";
            card.tabIndex = 0;

            card.addEventListener("click", () => {
                playlistView.show(playlist);
                pushView(() => playlistView.show(playlist));
                trackGroup = 0;
            });

            const meta = document.createElement("div");
            meta.className = "pl-meta";

            const name = document.createElement("h3");
            name.className = "pl-name";
            name.textContent = playlist.title;
            meta.appendChild(name);

            // Rimosso sottotitolo con numero brani

            // Reorder icon button (overlay small action) 
            const reorderBtn = document.createElement("button");
            reorderBtn.type = "button";
            reorderBtn.title = "Riordina tracce";
            reorderBtn.className = "reorder-btn";
            const icon = document.createElement("span");
            icon.innerHTML = '<svg viewBox="0 0 24 24" width="20" height="20" fill="currentColor"><path d="M3 10h14l-4.293-4.293 1.414-1.414L21.828 12l-7.707 7.707-1.414-1.414L17 14H3v-2zm0 8h8v-2H3v2zm0-12h8V4H3v2z"/></svg>';
            reorderBtn.appendChild(icon);
            reorderBtn.addEventListener("click", (e) => {
                e.stopPropagation();
                loadReorderModal(playlist);
            });

            card.appendChild(meta);
            card.appendChild(reorderBtn);
            return card;
        }

        /**
         * Generates the modal to reorder the Tracks from homepage.
         *
         * @param playlist Playlist from which recover the tracks
         */
        function loadReorderModal(playlist) {
            playlistView.loadReorderModal(playlist);
        }
        /**
         * Loads the create-playlist modal DOM elements to the modal container.
         */
        function loadCreatePlaylistModal() {
            let modalContainer = document.getElementById("modals"),
                modal = createModal("create-playlist", "Create new playlist", "create-playlist-btn", "Create playlist"),
                form = modal.getElementsByTagName("form").item(0), navbar = form.firstChild;

            let titleInput = document.createElement("input");
            titleInput.type = "text";
            titleInput.className = "text-field";
            titleInput.name = "playlistTitle";
            titleInput.placeholder = "Title";
            titleInput.required = true;
            form.insertBefore(titleInput, navbar);
            form.insertBefore(document.createElement("br"), navbar);
            form.insertBefore(document.createElement("br"), navbar);

            let label = document.createElement("label");
            label.className = "label";
            label.htmlFor = "track-selector-create";
            label.textContent = "Select songs to add:"
            form.insertBefore(label, navbar);
            form.insertBefore(document.createElement("br"), navbar);

            let selector = document.createElement("select");
            selector.className = "text-field";
            selector.id = "track-selector-create";
            selector.name = "selectedTrackIds";
            selector.multiple = true;
            form.insertBefore(selector, navbar);

            // This will hold errors and success messages
            form.insertBefore(document.createElement("div"), navbar);

            modalContainer.appendChild(modal);
            modal.classList.add("hidden");
        }
        /**
         * Loads the modal for uploading tracks to the modal container.
         */
        function loadUploadTrackModal() {
            let modalContainer = document.getElementById("modals"),
                modal = createModal("upload-track", "Upload Track", "upload-track-btn", "Add track"),
                form = modal.getElementsByTagName("form").item(0), navbar = form.firstChild;

            let titleInput = document.createElement("input");
            titleInput.type = "text";
            titleInput.className = "text-field";
            titleInput.name = "title";
            titleInput.placeholder = "Title";
            titleInput.required = true;
            form.insertBefore(titleInput, navbar);
            form.insertBefore(document.createElement("br"), navbar);

            let artistInput = document.createElement("input");
            artistInput.type = "text";
            artistInput.className = "text-field";
            artistInput.name = "artist";
            artistInput.placeholder = "Artist";
            artistInput.required = true;
            form.insertBefore(artistInput, navbar);
            form.insertBefore(document.createElement("br"), navbar);

            let albumInput = document.createElement("input");
            albumInput.type = "text";
            albumInput.className = "text-field";
            albumInput.name = "album";
            albumInput.placeholder = "Album title";
            albumInput.required = true;
            form.insertBefore(albumInput, navbar);
            form.insertBefore(document.createElement("br"), navbar);

            // Year selection (dropdown) so we only allow valid years and match server expectations
            let yearSelect = document.createElement("select");
            yearSelect.id = "year-selection"; // used by loadYears()
            yearSelect.name = "year"; // server expects parameter 'year'
            yearSelect.className = "text-field";
            yearSelect.required = true;
            form.insertBefore(yearSelect, navbar);
            form.insertBefore(document.createElement("br"), navbar);

            // Genre selection (dropdown populated from genres.json)
            let genreSelect = document.createElement("select");
            genreSelect.id = "genre-selection"; // used by loadGenres()
            genreSelect.name = "genre"; // server expects parameter 'genre'
            genreSelect.className = "text-field";
            genreSelect.required = true;
            form.insertBefore(genreSelect, navbar);
            form.insertBefore(document.createElement("br"), navbar);

            let trackLabel = document.createElement("label");
            trackLabel.className = "label";
            trackLabel.htmlFor = "musicTrack";
            trackLabel.textContent = "Track:"
            form.insertBefore(trackLabel, navbar);

            let trackInput = document.createElement("input");
            trackInput.type = "file";
            trackInput.name = "musicTrack";
            trackInput.id = "musicTrack";
            trackInput.required = true;
            form.insertBefore(trackInput, navbar);
            form.insertBefore(document.createElement("br"), navbar);

            let imageLabel = document.createElement("label");
            imageLabel.className = "label";
            imageLabel.htmlFor = "image";
            imageLabel.textContent = "Album image:"
            form.insertBefore(imageLabel, navbar);

            let imageInput = document.createElement("input");
            imageInput.type = "file";
            imageInput.name = "image";
            imageInput.id = "image";
            imageInput.required = true;
            form.insertBefore(imageInput, navbar);

            form.insertBefore(document.createElement("div"), navbar);

            modalContainer.appendChild(modal);
            modal.classList.add("hidden");
        }



    }


    function PlaylistView() {
        
        const PLAYLIST_PAGE_ID = "playlist";



        /**
         * Loads user tracks to ordered list with drag&drop functionality.
         *
         * @param trackSelector unordered list in which to load all the draggable items
         * @param playlist playlist from where to fetch the Tracks
         */
        function loadUserTracksOl(trackSelector, playlist, modal) {
            trackSelector.innerHTML = "";
            let url = "Playlist?playlistId=" + playlist.id;

            makeCall("GET", url, null, function (req) {
                if (req.readyState == XMLHttpRequest.DONE) {
                    let message = req.responseText;
                    if (req.status == 200) {
                        let tracks = JSON.parse(message);
                        tracks.forEach(function (track, index) {
                            let list_item = document.createElement("li");
                            list_item.draggable = true;
                            list_item.addEventListener("dragstart", dragStart);
                            list_item.addEventListener("dragover", dragOver);
                            list_item.addEventListener("dragleave", dragLeave);
                            list_item.addEventListener("drop", drop);
                            list_item.value = track.id;
                            list_item.textContent = track.artist + " - " + track.title + " (" + track.year + ")"
                            trackSelector.appendChild(list_item);
                        });
                        showModal(modal);
                    } else {
                        alert("Cannot recover data. Maybe the User has been logged out.");
                    }
                }
            });
        }

        // Drag events
        let startElement;

        /**
         * As soon as the User drags an Element.
         */
        function dragStart(event) {
            let list_item = event.target;
            startElement = list_item;
        }

        /**
         * When the User drags an Element over another one.
         */
        function dragOver(event) {
            event.preventDefault();
        }

        /**
         * When the User drags away from another Element.
         */
        function dragLeave(event) {
            event.preventDefault();
        }

        /**
         * When the User drops an Element over another one.
         */
        function drop(event) {
            let finalDest = event.target;
            let completeList = finalDest.closest("ol");
            let songsArray = Array.from(completeList.querySelectorAll("li"));
            let indexDest = songsArray.indexOf(finalDest);

            if (songsArray.indexOf(startElement) < indexDest) {
                startElement.parentElement.insertBefore(startElement, finalDest.nextSibling);
            } else {
                startElement.parentElement.insertBefore(startElement, finalDest);
            }
        }

        /**
         * Save new Tracks custom order.
         */
        function saveOrder(e, _playlistId) {
            let songsContainer = document.getElementById("track-reorder");
            let _trackIds = Array.from(songsContainer.querySelectorAll("li"))
                .map(e => e.value);

            let req = new XMLHttpRequest();
            let target = e.target;

            req.onreadystatechange = function () {
                if (req.readyState == XMLHttpRequest.DONE) {
                    let message = req.responseText;
                    switch (req.status) {
                        case 200:
                            target.parentElement.previousElementSibling.setAttribute("class", "success");
                            target.parentElement.previousElementSibling.textContent = message;
                            break;
                        case 500:
                            target.parentElement.previousElementSibling.setAttribute("class", "error");
                            target.parentElement.previousElementSibling.textContent = message;
                            break;
                    }
                }
            }

            let requestData = {
                trackIds: _trackIds,
                playlistId: _playlistId
            }

            req.open("POST", "TrackReorder");
            req.setRequestHeader("Content-Type", "application/json");
            req.send(JSON.stringify(requestData));
        }

        /**
         * Removes the reorder tracks modal.
         */
        // closeReorderModal non più necessario: si usa closeModal(overlay) del nuovo sistema

        /**
         * Generates the modal to reorder the Tracks.
         *
         * @param playlist Playlist from which recover the tracks
         */
        function loadReorderModal(playlist) {
            const modalContainer = document.getElementById("modals");
            // Overlay
            const overlay = document.createElement("div");
            overlay.id = "reorder-tracks-modal";
            overlay.className = "modal-overlay";

            // Modal
            const modal = document.createElement("div");
            modal.className = "modal";
            overlay.appendChild(modal);

            // Header
            const header = document.createElement("div");
            header.className = "modal-header";
            const h2 = document.createElement("h2");
            h2.className = "modal-title";
            h2.textContent = "Riordina tracce - " + playlist.title;
            const closeBtn = document.createElement("button");
            closeBtn.type = "button";
            closeBtn.className = "modal-close";
            closeBtn.setAttribute("aria-label", "Chiudi");
            closeBtn.innerHTML = '<svg viewBox="0 0 24 24" fill="currentColor" width="20" height="20"><path d="M19 6.41 17.59 5 12 10.59 6.41 5 5 6.41 10.59 12 5 17.59 6.41 19 12 13.41 17.59 19 19 17.59 13.41 12z"/></svg>';
            closeBtn.addEventListener("click", () => closeModal(overlay));
            header.appendChild(h2);
            header.appendChild(closeBtn);
            modal.appendChild(header);

            // Content
            const content = document.createElement("div");
            content.className = "modal-content";
            const form = document.createElement("form");
            content.appendChild(form);
            modal.appendChild(content);

            const label = document.createElement("label");
            label.className = "label";
            label.htmlFor = "track-reorder";
            label.textContent = "Trascina per riordinare:";
            form.appendChild(label);

            const ordered_list = document.createElement("ol");
            ordered_list.id = "track-reorder";
            ordered_list.name = "reorderingTracks";
            ordered_list.className = "text-field";
            form.appendChild(ordered_list);

            // Actions
            const actions = document.createElement("div");
            actions.className = "modal-actions";
            const reorderBtn = document.createElement("button");
            reorderBtn.id = "track-reorder-btn";
            reorderBtn.type = "button";
            reorderBtn.className = "btn";
            reorderBtn.textContent = "Salva ordine";
            reorderBtn.addEventListener("click", (e) => saveOrder(e, playlist.id.toString()));
            actions.appendChild(reorderBtn);
            form.appendChild(actions);

            modalContainer.appendChild(overlay);
            overlay.classList.add("hidden");

            // Carica le tracce e mostra il modal alla fine
            loadUserTracksOl(ordered_list, playlist, overlay);
        }

        /**
         * Show the Playlist page.
         *
         * @param playlist
         */
        this.show = function (playlist) {
            clearBottomNavbar();
            clearModals();
            loadAddTracksModal();
            loadPlaylistView(playlist);

            // Nascondi il pulsante Upload Track dentro la vista playlist (restaurato tornando a home)
            const uploadBtn = document.getElementById("upload-track-modal-button");
            if (uploadBtn) {
                uploadBtn.style.display = "none";
            }

            // show the add track modal
            document.getElementById("track-selector-modal-button").className = "button";
        }

        /**
         * Expose loadReorderModal to be called from other views
         */
        this.loadReorderModal = loadReorderModal;

        /**
         * Create the DOM elements for the Tracks of a Playlist.
         *
         * @param tracks array of Tracks
         */
        function trackGrid(tracks) {
            tracklist = tracks;
            let button, text, span_1,
                span_2, image;
            let main = document.getElementById("main");
            let container = document.createElement("div");
            cleanMain()
            container.setAttribute("class", "items-container");
            main.appendChild(container);

            for (let i = 0; i < 5; i++) {
                let track = tracks[i + trackGroup * 5];
                if (track == null)
                    break;

                button = document.createElement("button");
                button.setAttribute("class", "single-item song-button");
                button.setAttribute("name", "playlistId");

                text = document.createElement("span");
                text.setAttribute("class", "text-container")

                span_1 = document.createElement("span");
                span_1.setAttribute("class", "first-line");
                span_1.textContent = track.title;

                span_2 = document.createElement("span");
                span_2.setAttribute("class", "second-line");
                span_2.textContent = track.album_title;

                if (track.image_path && track.image_path.trim() !== "") {
                    image = document.createElement("img");
                    image.setAttribute("class", "album-image");
                    image.setAttribute("src", withCtx(track.image_path));
                    image.setAttribute("width", "100");
                    image.setAttribute("height", "100");
                }

                button.addEventListener("click", () => {
                    trackView.show(track);
                });

                button.appendChild(text);
                text.appendChild(span_1);
                text.appendChild(document.createElement("br"));
                text.appendChild(span_2);
                if (track.image_path && track.image_path.trim() !== "") {
                    button.appendChild(image);
                }
                container.appendChild(button);
            }
        }

        /**
         * Load all the Tracks associated to a Playlist.
         *
         * @param playlist Playlist of which to load the Tracks
         */
        function loadPlaylistTracks(playlist) {
            cleanMain();
            const main = document.getElementById("main");
            let mainLabel = document.getElementsByClassName("main-label").item(0);
            mainLabel.id = PLAYLIST_PAGE_ID;
            mainLabel.textContent = playlist.title;

            lastPlaylist = playlist;

            makeCall("GET", "Playlist?playlistId=" + playlist.id.toString(),
                null,
                function (req) {
                    if (req.readyState == XMLHttpRequest.DONE) {
                        let message = req.responseText;
                        if (req.status == 200) {
                            let tracks = JSON.parse(message);

                            if (tracks.length === 0) {
                                let message = document.createElement("p");
                                message.textContent = "Nessun track in questa playlist. Aggiungi alcuni track.";
                                main.appendChild(message);
                            } else {
                                trackGrid(tracks);
                                loadPrevNextButton();
                            }

                            // Add reorder button only if more than one track
                            if (tracks.length > 1) {
                                let reorderBtn = document.createElement("button");
                                reorderBtn.textContent = "Reorder Tracks";
                                reorderBtn.className = "button";
                                reorderBtn.addEventListener("click", () => {
                                    loadReorderModal(playlist);
                                });
                                main.appendChild(reorderBtn);
                            } else {
                            }
                        } else {
                            alert("Cannot recover data. Maybe the User has been logged out.");
                        }
                    }
                });
        }

        /**
         * Load everything needed for viewing and interacting with the Playlist and its contents.
         *
         * @param playlist Playlist to load
         */
        function loadPlaylistView(playlist) {
            loadPlaylistTracks(playlist)

            // Replace the track selector button and add "add tracks to playlist" functionality;
            // needed for removing already present event listeners, as this button is also used for creating a playlist.
            let modalButton = document.getElementById("track-selector-modal-button");
            let newButton = modalButton.cloneNode(true);

            modalButton.parentNode.replaceChild(newButton, modalButton);
            newButton.textContent = "Add Tracks"
            newButton.addEventListener("click", () => {
                loadUserTracks(document.getElementById("track-selector-add"), playlist);
                showModal(document.getElementById("add-tracks"));
            });

            document.getElementById("add-tracks-btn").addEventListener("click", function () {
                let self = this;
                let form = this.closest("form");

                if (form.checkValidity()) {
                    makeCall("POST", "AddTracks?playlistId=" + playlist.id, form, function (req) {
                        if (req.readyState == XMLHttpRequest.DONE) {
                            switch (req.status) {
                                case 201:
                                    loadPlaylistTracks(playlist);
                                    loadUserTracks(document.getElementById("track-selector-add"), playlist);
                                    self.parentElement.previousElementSibling.setAttribute("class", "success");
                                    self.parentElement.previousElementSibling.textContent = "Tracks added successfully";
                                    form.reset();
                                    break;
                                default:
                                    self.parentElement.previousElementSibling.setAttribute("class", "error");
                                    self.parentElement.previousElementSibling.textContent = "Error";
                            }
                        }
                    }, false);
                } else {
                    form.reportValidity();
                }
            });

            let bottomNavbar = document.createElement("div");
            bottomNavbar.id = "bottom-nav-bar";
            bottomNavbar.className = "nav-bar";
            document.getElementById("main").after(bottomNavbar);
        }

        /**
         * Loads the modal for adding tracks to a playlist to the modal container.
         */
        function loadAddTracksModal() {
            let modalContainer = document.getElementById("modals"),
                modal = createModal("add-tracks", "Add tracks to playlist", "add-tracks-btn", "Add tracks"),
                form = modal.getElementsByTagName("form").item(0), navbar = form.firstChild;

            let label = document.createElement("label");
            label.className = "label";
            label.htmlFor = "track-selector-add";
            label.textContent = "Select songs to add:"
            form.insertBefore(label, navbar);
            form.insertBefore(document.createElement("br"), navbar);

            let selector = document.createElement("select");
            selector.className = "text-field";
            selector.id = "track-selector-add";
            selector.name = "selectedTracksIds";
            selector.multiple = true;
            form.insertBefore(selector, navbar);

            // This will hold errors and success messages
            form.insertBefore(document.createElement("div"), navbar);

            modalContainer.appendChild(modal);
            modal.classList.add("hidden");
        }

        /**
         * Load the buttons for changing the viewed track group in the playlist view.
         */
        function loadPrevNextButton() {
            let navbar = document.getElementById("bottom-nav-bar");
            navbar.innerHTML = "";

            if (trackGroup > 0) {
                let button = document.createElement("button");
                button.className = "button";
                button.type = "button";
                button.textContent = "Previous Tracks";
                button.addEventListener("click", () => {
                    trackGroup--;
                    trackGrid(tracklist);
                    loadPrevNextButton();
                });
                navbar.appendChild(button);
            }

            let spacer = document.createElement("div");
            spacer.className = "spacer";
            navbar.appendChild(spacer);

            if (tracklist.length > 5 * (1 + trackGroup)) {
                let button = document.createElement("button");
                button.className = "button";
                button.type = "button";
                button.textContent = "Next Tracks";
                button.addEventListener("click", () => {
                    trackGroup++;
                    trackGrid(tracklist);
                    loadPrevNextButton();
                });
                navbar.appendChild(button);
            }
        }
    }

    /**
     * Class to manage the Player page, which contains only the single track player.
     *
     * @constructor
     */
    function TrackView() {
        const PLAYER_PAGE_ID = "player";

        /**
         * Show the Track to play.
         *
         * @param track Track to play
         */
        this.show = function (track) {
            clearModals();
            clearBottomNavbar();
            loadSingleTrack(track);
        }

        /**
         * Load the Track player DOM elements. Unlike the other loaders, it's only a center panel.
         *
         * @param container container in which load the Track player
         * @param track Track to load
         */
        function trackPlayer(container, track) {
            container.innerHTML = "";

            let centerPanelContainer = document.createElement("div");
            centerPanelContainer.setAttribute("class", "center-panel-container");

            let center_panel = document.createElement("div");
            center_panel.setAttribute("class", "center-panel")

            let track_metadata;

            /**
             * Returns a Track metadata div.
             *
             * @param track_property attribute to set as text, textContent
             * @return div of class "track-metadata"
             */
            function createTrack(track_property) {
                track_metadata = document.createElement("div");
                track_metadata.setAttribute("class", "track-metadata");

                track_metadata.setAttribute("text", track_property);
                track_metadata.textContent = track_property;

                return track_metadata;
            }

            let image;
            if (track.image_path && track.image_path.trim() !== "") {
                image = document.createElement("img");
                image.setAttribute("src", withCtx(track.image_path));
                image.setAttribute("width", "200");
                image.setAttribute("height", "200");
            }

            let audio_ctrl = document.createElement("audio");
            audio_ctrl.controls = true;

            let audio_src = document.createElement("source");
            audio_src.setAttribute("src", withCtx(track.song_path));
            audio_src.setAttribute("type", "audio/mpeg");

            // track panel creation
            center_panel.appendChild(createTrack(track.artist));
            center_panel.appendChild(createTrack(track.album_title));
            center_panel.appendChild(createTrack(track.year.toString()));
            center_panel.appendChild(createTrack(track.genre));
            center_panel.appendChild(document.createElement("hr"));
            if (track.image_path && track.image_path.trim() !== "") {
                center_panel.appendChild(image);
                center_panel.appendChild(document.createElement("hr"));
            }
            audio_ctrl.appendChild(audio_src);
            center_panel.appendChild(audio_ctrl);

            centerPanelContainer.appendChild(center_panel);
            container.appendChild(centerPanelContainer);
        }

        /**
         * Load a single Track from a Playlist.
         *
         * @param track Track to load
         */
        function loadSingleTrack(track) {
            // clean main div
            cleanMain();

            // Hide the modals
            document.getElementById("upload-track-modal-button").className = "button hidden";
            document.getElementById("track-selector-modal-button").className = "button hidden";

            let mainLabel = document.getElementsByClassName("main-label").item(0);
            mainLabel.id = PLAYER_PAGE_ID;
            mainLabel.textContent = track.title;

            lastTrack = track;

            makeCall("GET", "Track?track_id=" + track.id.toString(), null,
                function (req) {
                    if (req.readyState == XMLHttpRequest.DONE) {
                        let message = req.responseText;
                        if (req.status == 200) {
                            let track = JSON.parse(message);

                            if (track === null) {
                                alert("This Track can't be played.");
                                return;
                            }

                            trackPlayer(document.getElementById("main"), track);
                        } else {
                            alert("Cannot recover data. Maybe the User has been logged out.");
                        }
                    }
                });
        }
    }

    /**
     * Centralized management of the HomePage.
     *
     * @constructor
     */
    function MainLoader() {
        /**
         * Initializes the HomePage: adds listeners on buttons, refreshes the page.
         */
        this.start = function () {
            document.getElementById("logout-button").addEventListener("click", () => {
                makeCall("GET", "Logout", null, (req) => {
                    if (req.readyState === XMLHttpRequest.DONE) {
                        if (req.status === 200) {
                            window.location.href = withCtx("/login.html");
                        } else {
                            console.error("Logout failed, status:", req.status);
                        }
                    }
                });
            });

            // add listener for remaining homepage sidebar button only
            const homepageBtn = document.getElementById("homepage-button");
            if (homepageBtn) {
                homepageBtn.addEventListener("click", function () {
                    homeView.show();
                    pushView(() => homeView.show());
                });
            }

            const backBtn = document.getElementById("back-button");
            function refreshBackState() {
                if (backBtn) backBtn.disabled = (viewHistory.length <= 1);
            }
            if (backBtn) {
                backBtn.addEventListener("click", () => {
                    if (viewHistory.length > 1) {
                        undoLastView();
                        refreshBackState();
                    }
                });
            }
            // Initial state
            refreshBackState();

            // load modal data when clicking on the modal
            document.getElementById("upload-track-modal-button").addEventListener("click", function () {
                // Populate dynamic selects each time before showing (in case user navigated away)
                try { loadYears(); } catch(e) { console.warn("loadYears non disponibile", e); }
                try { loadGenres(); } catch(e) { console.warn("loadGenres non disponibile", e); }
                showModal(document.getElementById("upload-track"));
            });
        }

        /**
         * Refresh the HomePage: clear all modals and reload them.
         */
        this.refreshPage = function () {
            homeView.show();
        }

        /**
         * Load year from 1900 to the current one for upload track modal.
         */
        function loadYears() {
            let today = new Date().getFullYear();
            let year_selection = document.getElementById("year-selection");
            year_selection.innerHTML = "";

            let option = document.createElement("option");
            option.setAttribute("value", "");
            option.textContent = "Year";
            year_selection.appendChild(option);

            for (let i = today; i >= 1901; i--) {
                option = document.createElement("option");
                option.textContent = i.toString();
                year_selection.appendChild(option);
            }
        }

        /**
         * Load the musical genres for upload track modal.
         */
        function loadGenres() {
            makeCall("GET", "genres.json", null, (req) => {
                let genres;

                if (req.readyState == XMLHttpRequest.DONE) {
                    if (req.status == 200) {
                        genres = JSON.parse(req.responseText);
                    } else {
                        genres = [];
                    }

                    let genre_selection = document.getElementById("genre-selection");
                    genre_selection.innerHTML = "";
                    let option = document.createElement("option");
                    option.setAttribute("value", "");
                    option.textContent = "Genre";
                    genre_selection.appendChild(option);

                    genres.forEach(function (genre) {
                        option = document.createElement("option");
                        option.textContent = genre;
                        genre_selection.appendChild(option);
                    });
                }
            });
        }
        
    }
})();

