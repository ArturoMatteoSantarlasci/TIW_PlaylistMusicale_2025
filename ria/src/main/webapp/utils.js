// Helper methods

/**
 * Make an asynchronous call to the server by specifying method, URL, form to send,
 * the function to execute and whether to reset the given form.
 *
 * @param method method of the request (usually GET, POST)
 * @param url URL to call
 * @param formElement form
 * @param callback function to execute upon received data
 * @param reset whether to reset the given form
 */
function makeCall(method, url, formElement, callback, reset = true) {
    let req = new XMLHttpRequest();
    req.onreadystatechange = function () {
        callback(req);
    };
    req.open(method, url);
    if (formElement == null) {
        req.send();
    } else {
        let formData = new FormData(formElement)
        req.send(formData);
    }
    if (formElement != null && reset === true) {
        formElement.reset();
    }
}

/**
 * Delete everything from main div.
 */
function cleanMain() {
    let main_div = document.getElementById("main");
    main_div.innerHTML = "";
}

/**
 * Delete everything from modals div.
 */
function clearModals() {
    document.getElementById("modals").innerHTML = "";
}

/**
 * Create basic modal element; used as a building block for creating modals.
 *
 * @param id id of this modal
 * @param titleText title of this modal
 * @param buttonId ID of the button
 * @param buttonText text of the button
 */
function createModal(id, titleText, buttonId, buttonText) {
    // Pure_html modal markup: overlay -> modal -> header + content + actions
    const overlay = document.createElement("div");
    overlay.id = id;
    overlay.className = "modal-overlay hidden"; // hidden until showModal

    const modal = document.createElement("div");
    modal.className = "modal";
    overlay.appendChild(modal);

    const header = document.createElement("div");
    header.className = "modal-header";
    const h2 = document.createElement("h2");
    h2.className = "modal-title";
    h2.textContent = titleText;
    const closeBtn = document.createElement("button");
    closeBtn.type = "button";
    closeBtn.className = "modal-close";
    closeBtn.setAttribute("aria-label", "Chiudi");
    closeBtn.innerHTML = '<svg viewBox="0 0 24 24" fill="currentColor" width="20" height="20"><path d="M19 6.41 17.59 5 12 10.59 6.41 5 5 6.41 10.59 12 5 17.59 6.41 19 12 13.41 17.59 19 19 17.59 13.41 12z"/></svg>';
    closeBtn.addEventListener("click", () => closeModal(overlay));
    header.appendChild(h2);
    header.appendChild(closeBtn);
    modal.appendChild(header);

    const content = document.createElement("div");
    content.className = "modal-content";
    const form = document.createElement("form");
    // Le azioni devono stare DENTRO il form per consentire closest('form') sul bottone
    const actions = document.createElement("div");
    actions.className = "modal-actions";
    const primary = document.createElement("button");
    primary.id = buttonId;
    primary.type = "button"; // lasciamo button (non submit) perché gestiamo noi la validazione
    primary.className = "btn";
    primary.textContent = buttonText;
    actions.appendChild(primary);
    form.appendChild(actions);
    content.appendChild(form);
    modal.appendChild(content);

    return overlay;
}

/**
 * Make the modal visible.
 *
 * @param modal modal to make visible
 */
function showModal(modal) {
    modal.classList.remove("hidden");
    modal.classList.add("active");
    modal.style.display = "flex";
}

/**
 * Make the modal hidden.
 *
 * @param modal modal to hide
 */
function closeModal(modal) {
    modal.classList.add("hidden");
    modal.classList.remove("active");
    modal.style.display = "none";
}

/**
 * Delete the bottom navbar if present.
 */
function clearBottomNavbar() {
    let navbar = document.getElementById("bottom-nav-bar");
    if (navbar != null)
        navbar.remove();
}

/**
 * Get user tracks and add them to the track selector parameter.
 *
 * @param trackSelector HTML element to append the loaded user tracks
 * @param playlist optional parameter used for just loading tracks not present in the specified playlist
 */
function loadUserTracks(trackSelector, playlist = null) {
    trackSelector.innerHTML = "";
    let url;
    if (playlist == null) {
        url = "GetUserTracks";
    } else {
        url = "GetTracksNotInPlaylist?playlistTitle=" + encodeURIComponent(playlist.title);
    }

    makeCall("GET", url, null, function (req) {
        if (req.readyState == XMLHttpRequest.DONE) {
            let message = req.responseText;
            if (req.status == 200) {
                let tracks = JSON.parse(message);
                if (tracks.length === 0) {
                    let option = document.createElement("option");
                    option.value = "";
                    option.textContent = "There are no available tracks to be added."
                    trackSelector.setAttribute("size", "1");
                    trackSelector.appendChild(option);
                    return;
                } else if (tracks.length < 10) {
                    trackSelector.setAttribute("size", tracks.length.toString());
                } else {
                    trackSelector.setAttribute("size", "10");
                }
                tracks.forEach(function (track) {
                    let option = document.createElement("option");
                    option.value = track.id.toString();
                    option.textContent = track.artist + " - " + track.title + " (" + track.year + ")"
                    trackSelector.appendChild(option);
                });
            } else if (req.status == 401) {
                alert("Session expired. Please log in again.");
                window.location.href = "login.html";
            } else {
                alert("Cannot recover data.");
            }
        }
    });
}