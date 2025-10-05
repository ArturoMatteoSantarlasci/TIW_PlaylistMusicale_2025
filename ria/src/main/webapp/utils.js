
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
    const topNavbar = document.createElement("div");
    topNavbar.className = "nav-bar";

    const title = document.createElement("div");
    title.className = "modal-title";
    title.textContent = titleText;

    const spacer = document.createElement("div");
    spacer.className = "spacer";

    // Pulsante chiusura modale (prima era <div>, ora <button> per corretto comportamento e cursore)
    const close = document.createElement("button");
    close.type = "button";
    close.className = "modal-close";
    close.setAttribute("aria-label", "Chiudi modale");
    close.textContent = "Close"; // testo legacy mantenuto
    close.addEventListener("click", () => closeModal(modal));

    const form = document.createElement("form");

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
                const option = document.createElement("option");
                option.value = "";
                option.textContent = "Nessuna traccia disponibile da aggiungere";
                trackSelector.setAttribute("size", "1");
                trackSelector.appendChild(option);
                return;
            }
            trackSelector.setAttribute("size", tracks.length < 10 ? tracks.length.toString() : "10");
            tracks.forEach(track => {
                const option = document.createElement("option");
                option.value = track.id.toString();
                option.textContent = track.artist + " - " + track.title + " (" + track.year + ")";
                trackSelector.appendChild(option);
            });
        } else if (req.status === 401) {
            alert("Sessione scaduta. Effettua di nuovo il login.");
            window.location.href = "login.html";
        } else {
            alert("Impossibile recuperare i dati.");
        }
    }, false); // false => non resettiamo il form chiamante (qui non c'è)
}