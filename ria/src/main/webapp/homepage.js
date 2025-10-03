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

    /**
     * Show modal by removing hidden class and making it visible
     */
    function showModal(el) {
        el.classList.remove("hidden");
        el.style.visibility = "visible";
        el.style.pointerEvents = "auto";
    }

    /**
     * Close modal by adding hidden class and hiding it
     */
    function closeModal(el) {
        el.classList.add("hidden");
        el.style.visibility = "hidden";
        el.style.pointerEvents = "none";
    }

    // loadUserTracks function is already defined in utils.js

    // Initialize all the global variables and objects
    let lastPlaylist = null, lastTrack = null;
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
        const HOMEPAGE_LABEL = "All Playlists";
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
        }

        /**
         * Loads all the User Playlists.
         */
        function loadPlaylists() {
            cleanMain();
            let mainLabel = document.getElementsByClassName("main-label").item(0);
            mainLabel.id = HOMEPAGE_ID;
            mainLabel.textContent = HOMEPAGE_LABEL;

            makeCall("GET", "HomePage", null,
                (req) => {
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

        /**
         * Load the Playlists.
         *
         * @param playlists array of Playlists
         */
        function playlistGrid(playlists) {
            let main = document.getElementById("main");
            let container = document.createElement("div");
            container.setAttribute("class", "items-container");
            main.appendChild(container);

            playlists.forEach(function (playlist) {
                container.appendChild(createPlaylistButton(playlist));
            })
        }
        /**
         * Load buttons in the top nav bar and button functionality in the sidebar.
         */
        function loadButtons() {
            console.log("loadButtons called");
            // Replace the track selector button and add "create playlist" functionality; needed for removing already
            // present event listeners, as this button is also used for adding tracks to a playlist
            let modalButton = document.getElementById("track-selector-modal-button");
            let newButton = modalButton.cloneNode(true);
            modalButton.parentNode.replaceChild(newButton, modalButton);
            newButton.textContent = "Add Playlist"
            newButton.addEventListener("click", () => {
                console.log("Add Playlist button clicked");
                console.log("Modal element:", document.getElementById("create-playlist"));
                loadUserTracks(document.getElementById("track-selector-create"));
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
                let self = this;
                let form = this.closest("form");

                if (form.checkValidity()) {
                    makeCall("POST", "CreatePlaylist", form, function (req) {
                        if (req.readyState == XMLHttpRequest.DONE) {
                            let message = req.responseText;
                            switch (req.status) {
                                case 201:
                                    let newPlaylist = JSON.parse(message);

                                    // Update the home view with newly created playlist
                                    let itemsContainer = document.querySelector(".items-container");//itemsContainer è una classe
                                    itemsContainer.insertBefore(createPlaylistButton(newPlaylist), itemsContainer.firstChild);

                                    self.parentElement.previousElementSibling.setAttribute("class", "successo");
                                    self.parentElement.previousElementSibling.textContent = "Playlist creata";
                                    form.reset();
                                    break;
                                case 409:
                                    self.parentElement.previousElementSibling.setAttribute("class", "errore");
                                    self.parentElement.previousElementSibling.textContent = message;
                                    break;
                            }
                        }
                    }, false);
                } else {
                    form.reportValidity();
                }
            });

            // Upload track
            document.getElementById("upload-track-btn").addEventListener("click", function () {
                let self = this;
                let form = this.closest("form");

                console.log("Upload button clicked, form valid:", form.checkValidity());
                if (form.checkValidity()) {
                    console.log("Sending upload request...");
                    makeCall("POST", "UploadTrack", this.closest("form"), function (req) {
                        let message = req.responseText;
                        console.log("Upload response status:", req.status, "message:", message);

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
            document.getElementById("upload-track-modal-button").className = "button";
            document.getElementById("track-selector-modal-button").className = "button";
        }

        /**
         * Creates and returns a wrapper div with playlist button and reorder button
         *
         * @param playlist
         */
        function createPlaylistButton(playlist) {
            const wrapper = document.createElement("div");
            wrapper.className = "single-item playlist-title";

            const openBtn = document.createElement("button");
            openBtn.className = "playlist-open";
            openBtn.addEventListener("click", () => {
                playlistView.show(playlist);
                trackGroup = 0;
            });

            const title = document.createElement("span");
            title.className = "first-line";
            title.textContent = playlist.title;
            openBtn.appendChild(title);

            const reorderBtn = document.createElement("button");
            const icon = document.createElement("img");
            icon.className = "reorder-button";
            icon.src = withCtx("img/reorder/reorder.svg");
            icon.width = 40; 
            icon.height = 40;
            reorderBtn.appendChild(icon);
            reorderBtn.addEventListener("click", (e) => {
                e.stopPropagation();
                loadReorderModal(playlist);
            });

            wrapper.append(openBtn, reorderBtn);
            return wrapper;
        }

        /**
         * Generates the modal to reorder the Tracks from homepage.
         *
         * @param playlist Playlist from which recover the tracks
         */
        function loadReorderModal(playlist) {
            console.log("loadReorderModal called from HomeView for playlist:", playlist.title);
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

            let yearInput = document.createElement("input");
            yearInput.type = "text";
            yearInput.className = "text-field";
            yearInput.name = "year";
            yearInput.placeholder = "Year";
            yearInput.required = true;
            form.insertBefore(yearInput, navbar);
            form.insertBefore(document.createElement("br"), navbar);

            let genreInput = document.createElement("input");
            genreInput.type = "text";
            genreInput.className = "text-field";
            genreInput.name = "genre";
            genreInput.placeholder = "Genre";
            genreInput.required = true;
            form.insertBefore(genreInput, navbar);
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
        console.log("PlaylistView function started");
        const PLAYLIST_PAGE_ID = "playlist";



        /**
         * Loads user tracks to ordered list with drag&drop functionality.
         *
         * @param trackSelector unordered list in which to load all the draggable items
         * @param playlist playlist from where to fetch the Tracks
         */
        function loadUserTracksOl(trackSelector, playlist, modal) {
            console.log("loadUserTracksOl called with playlist:", playlist.title, "modal:", modal);
            trackSelector.innerHTML = "";
            let url = "Playlist?playlistId=" + playlist.id;
            console.log("Making AJAX call to:", url);

            makeCall("GET", url, null, function (req) {
                console.log("AJAX response received. ReadyState:", req.readyState, "Status:", req.status);
                if (req.readyState == XMLHttpRequest.DONE) {
                    let message = req.responseText;
                    console.log("Response message:", message);
                    if (req.status == 200) {
                        let tracks = JSON.parse(message);
                        console.log("Parsed tracks:", tracks.length, "tracks found");
                        tracks.forEach(function (track, index) {
                            console.log("Adding track", index + 1, ":", track.title, "by", track.artist);
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
                        console.log("All tracks added to list. Calling showModal.");
                        showModal(modal);
                    } else {
                        console.error("AJAX error - Status:", req.status, "Message:", message);
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
            console.log("Drag start from:", list_item.textContent);
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
        function closeReorderModal() {
            let modal_div = document.getElementById("reorder-tracks-modal");
            modal_div.remove();
        }

        /**
         * Generates the modal to reorder the Tracks.
         *
         * @param playlist Playlist from which recover the tracks
         */
        function loadReorderModal(playlist) {
            console.log("loadReorderModal function defined successfully");
            console.log("Playlist details - id:", playlist.id, "title:", playlist.title);
            let modalContainer = document.getElementById("modals");
            console.log("Modal container found:", modalContainer);
            let modal_div = document.createElement("div");
            modal_div.setAttribute("id", "reorder-tracks-modal");
            modal_div.setAttribute("class", "modal-window");

            let inner_div = document.createElement("div");
            inner_div.setAttribute("class", "modal-content");

            // Top nav bar
            let top_nav_bar = document.createElement("div");
            top_nav_bar.setAttribute("class", "nav-bar");

            let title_div = document.createElement("div");
            title_div.setAttribute("class", "modal-title");
            title_div.textContent = "Reorder Tracks in " + playlist.title.toString();

            let spacer = document.createElement("div");
            spacer.setAttribute("class", "spacer");

            let modal_close = document.createElement("a");
            modal_close.setAttribute("title", "Close");
            modal_close.setAttribute("class", "modal-close");
            modal_close.textContent = "Close";

            modal_close.addEventListener("click", () => {
                closeReorderModal();
            })

            top_nav_bar.appendChild(title_div);
            top_nav_bar.appendChild(spacer);
            top_nav_bar.appendChild(modal_close);

            // Main form
            let main_form = document.createElement("form");

            let label = document.createElement("label");
            label.setAttribute("class", "label");
            label.setAttribute("for", "track-reorder");
            label.textContent = "Drag the track to reorder:";

            let ordered_list = document.createElement("ol");
            ordered_list.setAttribute("name", "reorderingTracks");
            ordered_list.setAttribute("id", "track-reorder");
            ordered_list.setAttribute("class", "text-field");

            loadUserTracksOl(ordered_list, playlist, modal_div);

            let bottom_div = document.createElement("div");
            bottom_div.setAttribute("class", "nav-bar");

            let reorder_track_btn = document.createElement("button");
            reorder_track_btn.setAttribute("id", "track-reorder-btn");
            reorder_track_btn.setAttribute("class", "button");
            reorder_track_btn.type = "button";
            reorder_track_btn.textContent = "Reorder Tracks";

            reorder_track_btn.addEventListener("click", (e) => {
                saveOrder(e, playlist.id.toString());
            });

            bottom_div.appendChild(reorder_track_btn);

            main_form.appendChild(label);
            main_form.appendChild(ordered_list);
            main_form.appendChild(bottom_div);

            inner_div.appendChild(top_nav_bar);
            inner_div.appendChild(main_form);
            modal_div.appendChild(inner_div);

            modalContainer.appendChild(modal_div);
            console.log("loadReorderModal completed - modal created and hidden, waiting for tracks to load");
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

            // hide the upload track modal
            // document.getElementById("upload-track-modal-button").className = "button hidden";

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
                                console.log("Adding reorder button for playlist:", playlist.title, "with", tracks.length, "tracks");
                                let reorderBtn = document.createElement("button");
                                reorderBtn.textContent = "Reorder Tracks";
                                reorderBtn.className = "button";
                                reorderBtn.addEventListener("click", () => {
                                    console.log("Reorder button clicked for playlist:", playlist.title);
                                    console.log("Calling loadReorderModal with playlist:", playlist);
                                    loadReorderModal(playlist);
                                });
                                main.appendChild(reorderBtn);
                                console.log("Reorder button added to DOM");
                            } else {
                                console.log("Not adding reorder button - only", tracks.length, "track(s) in playlist");
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
                    if (req.readyState == XMLHttpRequest.DONE) {
                        if (req.status == 200) {
                            location.href = "index.html";
                        }
                    }
                });
            });

            // add listeners on sidebar buttons
            document.getElementById("homepage-button").addEventListener("click", function () {
                homeView.show();
            });

            document.getElementById("playlist-button").addEventListener("click", function () {
                if (lastPlaylist != null) {
                    playlistView.show(lastPlaylist);
                }
            });

            document.getElementById("track-button").addEventListener("click", function () {
                if (lastTrack != null) {
                    trackView.show(lastTrack);
                }
            });

            // load modal data when clicking on the modal
            document.getElementById("upload-track-modal-button").addEventListener("click", function () {
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
        console.log("PlaylistView function completed");
    }
})();

