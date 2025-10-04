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
    let modal = document.createElement("div");
    modal.id = id;
    modal.className = "modal-window";

    let container = document.createElement("div"),
        topNavbar = document.createElement("div");
    topNavbar.className = "nav-bar";

    let title = document.createElement("div");
    title.className = "modal-title";
    title.textContent = titleText;

    let spacer = document.createElement("div");
    spacer.className = "spacer";

    let close = document.createElement("div");
    close.className = "modal-close";
    close.textContent = "Close";
    close.addEventListener("click", () => {
        closeModal(modal);
    });

    let form = document.createElement("form");
    topNavbar.appendChild(title);
    topNavbar.appendChild(spacer);
    topNavbar.appendChild(close);
    container.appendChild(topNavbar);
    container.appendChild(form);
    modal.appendChild(container);

    let bottomNavbar = document.createElement("div"),
        button = document.createElement("button");

    bottomNavbar.className = "nav-bar";
    button.id = buttonId;
    button.type = "button";
    button.className = "button";
    button.textContent = buttonText;
    bottomNavbar.appendChild(button);
    form.appendChild(bottomNavbar);

    return modal;
}

/**
 * Make the modal visible.
 *
 * @param modal modal to make visible
 */
function showModal(modal) {
    console.log("Showing modal:", modal);
    modal.classList.remove("hidden");
    modal.style.visibility = "";
    modal.style.pointerEvents = "";
}

/**
 * Make the modal hidden.
 *
 * @param modal modal to hide
 */
function closeModal(modal) {
    modal.classList.add("hidden");
    modal.style.visibility = "hidden";
    modal.style.pointerEvents = "none";
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