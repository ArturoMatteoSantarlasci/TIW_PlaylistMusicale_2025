
/**
 * Esegue una chiamata asincrona al server usando XMLHttpRequest.
 *  - method: 'GET' | 'POST' ...
 *  - url: endpoint servlet (senza context path aggiuntivo)
 *  - formElement: se presente, i suoi campi vengono inviati (FormData)
 *  - callback: funzione chiamata ad ogni cambiamento di stato (readyState)
 *  - reset: se true (default) il form viene svuotato dopo l'invio
 */
function makeCall(method, url, formElement, callback, reset = true) {
    const req = new XMLHttpRequest();
    req.onreadystatechange = () => callback(req);
    req.open(method, url);
    if (formElement == null) {
        req.send();
    } else {
        const formData = new FormData(formElement);
        req.send(formData);
    }
    if (formElement != null && reset) formElement.reset();
}

/** Pulisce completamente il contenuto del main. */
function cleanMain() {
    const main = document.getElementById("main");
    if (main) main.innerHTML = "";
}

/** Rimuove tutte le modali create dinamicamente. */
function clearModals() {
    const m = document.getElementById("modals");
    if (m) m.innerHTML = "";
}

/**
 * Crea una modale.
 * Struttura:
 * <div id=... class="modal-window hidden">  (aggiungiamo anche 'hidden' per compatibilità con codice esistente)
 *   <div>
 *     <div class="nav-bar"> [titolo] [spacer] [close] </div>
 *     <form> ... <div class="nav-bar">[bottone]</div> </form>
 *   </div>
 * </div>
 */
function createModal(id, titleText, buttonId, buttonText) {
    const modal = document.createElement("div");
    modal.id = id;
    // Manteniamo sia la vecchia classe sia 'hidden' per riuso della logica show/hide esistente
    modal.className = "modal-window hidden";

    const container = document.createElement("div");
    // Panel vero della modale: assegniamo la classe per avere sfondo bianco e bordo rosa
    container.className = "modal-content";
    const topNavbar = document.createElement("div");
    topNavbar.className = "nav-bar";

    const title = document.createElement("div");
    title.className = "modal-title"; // now styled as pill in CSS
    title.textContent = titleText;

    const spacer = document.createElement("div");
    spacer.className = "spacer";

    // Pulsante chiusura modale (prima era <div>, ora <button> per corretto comportamento e cursore)
    const close = document.createElement("button");
    close.type = "button";
    
    close.className = "modal-close";
    close.setAttribute("aria-label", "Chiudi modale");
    // use an icon for close (keep aria-label for accessibility)
    close.innerHTML = '<svg viewBox="0 0 24 24" fill="currentColor" width="20" height="20" aria-hidden="true"><path d="M19 6.41L17.59 5 12 10.59 6.41 5 5 6.41 10.59 12 5 17.59 6.41 19 12 13.41 17.59 19 19 17.59 13.41 12z"/></svg>';
    close.addEventListener("click", () => closeModal(modal));

    const form = document.createElement("form");
    // Usa layout a griglia con gap per spaziatura verticale tra i campi
    form.className = "stack";

    topNavbar.appendChild(title);
    topNavbar.appendChild(spacer);
    topNavbar.appendChild(close);
    container.appendChild(topNavbar);
    container.appendChild(form);
    modal.appendChild(container);

    // Bottom navbar con bottone primario
    const bottomNavbar = document.createElement("div");
    bottomNavbar.className = "nav-bar";
    const button = document.createElement("button");
    button.id = buttonId;
    button.type = "button"; // non submit: la logica di validazione e invio è nel JS
    button.className = "button";
    button.textContent = buttonText;
    bottomNavbar.appendChild(button);
    form.appendChild(bottomNavbar);

    return modal;
}

function showModal(modal) {
    // Rimuoviamo eventuale classe hidden e usiamo visibility per compatibilità con vecchio stile
    modal.classList.remove("hidden");
    modal.style.visibility = "visible";
    modal.style.pointerEvents = "auto";
}

/** Nasconde la modale. */
function closeModal(modal) {
    modal.classList.add("hidden");
    modal.style.visibility = "hidden";
    modal.style.pointerEvents = "none";
}

/** Elimina la bottom navbar se presente (usata nella vista playlist per i pulsanti prev/next). */
function clearBottomNavbar() {
    const navbar = document.getElementById("bottom-nav-bar");
    if (navbar) navbar.remove();
}

/**
 * Carica le tracce dell'utente (o quelle non ancora in una playlist) dentro un <select> passato.
 *  - trackSelector: elemento <select>
 *  - playlist (opzionale): se presente carica solo tracce NON incluse in quella playlist
 */
function loadUserTracks(trackSelector, playlist = null) {
    trackSelector.innerHTML = "";
    const url = playlist == null ? "GetUserTracks" : "GetTracksNotInPlaylist?playlistTitle=" + encodeURIComponent(playlist.title);

    makeCall("GET", url, null, (req) => {
        if (req.readyState !== XMLHttpRequest.DONE) return;
        const message = req.responseText;
        if (req.status === 200) {
            let tracks = [];
            try { tracks = JSON.parse(message) || []; } catch (_) { tracks = []; }
            if (tracks.length === 0) {
                if (trackSelector.tagName && trackSelector.tagName.toLowerCase() === 'select') {
                    const option = document.createElement("option");
                    option.value = "";
                    option.textContent = "Nessuna traccia disponibile da aggiungere";
                    trackSelector.appendChild(option);
                } else {
                    const msg = document.createElement('div');
                    msg.className = 'no-tracks-msg';
                    msg.textContent = 'Nessuna traccia disponibile da aggiungere';
                    trackSelector.appendChild(msg);
                }
                return;
            }
            // Support two rendering modes: legacy <select> and the new checkbox-list container
            if (trackSelector.tagName && trackSelector.tagName.toLowerCase() === 'select') {
                // legacy select multiple
                tracks.forEach(track => {
                    const option = document.createElement("option");
                    option.value = track.id.toString();
                    option.textContent = track.artist + " - " + track.title + " (" + track.year + ")";
                    trackSelector.appendChild(option);
                });
            } else {
                // checkbox-list container: create a scrollable list of rows with an input checkbox and label
                // Determine the input name expected by the form: if id contains 'create' use selectedTrackIds, if 'add' use selectedTracksIds
                const inputName = (trackSelector.id && trackSelector.id.indexOf('create') >= 0) ? 'selectedTrackIds' : 'selectedTracksIds';
                tracks.forEach(track => {
                    const row = document.createElement('div');
                    row.className = 'track-checkbox-row';
                    const input = document.createElement('input');
                    input.type = 'checkbox';
                    input.name = inputName;
                    input.value = track.id.toString();
                    input.id = (trackSelector.id || 'tracklist') + '-chk-' + track.id;
                    input.className = 'track-checkbox-input';
                    const label = document.createElement('label');
                    label.htmlFor = input.id;
                    label.className = 'track-checkbox-label';
                    label.textContent = track.artist + ' - ' + track.title + ' (' + track.year + ')';
                    row.appendChild(input);
                    row.appendChild(label);
                    trackSelector.appendChild(row);
                });
            }
        } else if (req.status === 401) {
            alert("Sessione scaduta. Effettua di nuovo il login.");
            window.location.href = "login.html";
        } else {
            alert("Impossibile recuperare i dati.");
        }
    }, false); // false => non resettiamo il form chiamante (qui non c'è)
}