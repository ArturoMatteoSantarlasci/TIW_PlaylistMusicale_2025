
// Chiamata helper: io la uso per tutte le richieste asincrone verso le servlet.
// Parametri:
//  - method: 'GET', 'POST', ecc. (lo passo così com'è a XMLHttpRequest.open)
//  - url: la servlet/endpoint (senza modifiche sul context path qui)
//  - formElement: se passo un <form>, lo trasformo in FormData e lo invio; altrimenti invio vuoto
//  - callback: la funzione che voglio chiamare ogni volta che cambia readyState (riceve l'oggetto XHR)
//  - reset: se true (default) resetto il form dopo l'invio (comodo per i modali)
function makeCall(method, url, formElement, callback, reset = true) {
    // Creo l'oggetto XHR e delego la gestione degli eventi al caller (callback).
    const req = new XMLHttpRequest();
    req.onreadystatechange = () => callback(req);

    // Apro la richiesta: non aggiungo nulla al path, passo esattamente l'url ricevuto.
    req.open(method, url);

    // Se mi è stato fornito un form lo invio come FormData, altrimenti invio senza body.
    if (formElement == null) {
        req.send();
    } else {
        const formData = new FormData(formElement);
        req.send(formData);
    }

    // Se ho inviato un form e ho voluto il reset, svuoto i campi subito dopo l'invio.
    // Nota: reset qui è sincronizzato rispetto all'invio XHR (non aspetto risposta dal server).
    if (formElement != null && reset) formElement.reset();
}

// Rimuovo tutto il contenuto dentro #main. Lo chiamo quando voglio ricostruire la vista da zero.
function cleanMain() {
    const main = document.getElementById("main");
    if (main) main.innerHTML = "";
}

// Svuoto il contenitore delle modali dinamiche: utile prima di ricreare nuove modali
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
// Costruisco una modale standardizzata che riutilizzo in tutta l'app.
// Struttura DOM che creo:
// <div id="..." class="modal-window hidden">
//   <div class="modal-content">
//     <div class="nav-bar">[title][spacer][close]</div>
//     <form class="stack"> ... <div class="nav-bar">[button]</div></form>
//   </div>
// </div>
function createModal(id, titleText, buttonId, buttonText) {
    const modal = document.createElement("div");
    modal.id = id;
    // Aggiungo 'hidden' per conservare la compatibilità con la logica show/hide esistente
    modal.className = "modal-window hidden";

    const container = document.createElement("div");
    // Il pannello interno (bianco con bordo) che contiene titolo e form
    container.className = "modal-content";
    const topNavbar = document.createElement("div");
    topNavbar.className = "nav-bar";

    const title = document.createElement("div");
    title.className = "modal-title";
    title.textContent = titleText;

    const spacer = document.createElement("div");
    spacer.className = "spacer";

    // Preferisco un <button> per la chiusura: gestione tastiera e cursore corrette.
    const close = document.createElement("button");
    close.type = "button";
    close.className = "modal-close";
    close.setAttribute("aria-label", "Chiudi modale");
    // Icona X interna (decorativa)
    close.innerHTML = '<svg viewBox="0 0 24 24" fill="currentColor" width="20" height="20" aria-hidden="true"><path d="M19 6.41L17.59 5 12 10.59 6.41 5 5 6.41 10.59 12 5 17.59 6.41 19 12 13.41 17.59 19 19 17.59 13.41 12z"/></svg>';
    close.addEventListener("click", () => closeModal(modal));

    const form = document.createElement("form");
    // Uso una classe 'stack' per dare gap verticali ai campi che inserisco successivamente
    form.className = "stack";

    topNavbar.appendChild(title);
    topNavbar.appendChild(spacer);
    topNavbar.appendChild(close);
    container.appendChild(topNavbar);
    container.appendChild(form);
    modal.appendChild(container);

    // Bottom navbar con bottone primario (es. 'Crea', 'Carica'). Lo metto dentro il form
    const bottomNavbar = document.createElement("div");
    bottomNavbar.className = "nav-bar";
    const button = document.createElement("button");
    button.id = buttonId;
    // Lo imposto come type=button perché la submit la gestisco esplicitamente via JS
    button.type = "button";
    button.className = "button";
    button.textContent = buttonText;
    bottomNavbar.appendChild(button);
    form.appendChild(bottomNavbar);

    return modal;
}

// Mostro la modale: tolgo hidden e abilito pointer events.
function showModal(modal) {
    modal.classList.remove("hidden");
    modal.style.visibility = "visible";
    modal.style.pointerEvents = "auto";
}

// Nascondo la modale senza rimuoverla dal DOM (così posso riaprirla rapidamente).
function closeModal(modal) {
    modal.classList.add("hidden");
    modal.style.visibility = "hidden";
    modal.style.pointerEvents = "none";
}

/** Elimina la bottom navbar se presente (usata nella vista playlist per i pulsanti prev/next). */
// Rimuovo la bottom navbar legacy se presente (la uso raramente adesso).
function clearBottomNavbar() {
    const navbar = document.getElementById("bottom-nav-bar");
    if (navbar) navbar.remove();
}

// Recupero la lista delle tracce dell'utente o quelle non incluse in una playlist.
// Il parametro trackSelector può essere un <select> legacy oppure un container div dove creo checkbox rows.
// Se passo playlist, chiedo solo le tracce non presenti in quella playlist (usato per "Aggiungi brani").
function loadUserTracks(trackSelector, playlist = null) {
    // Pulisco il contenuto esistente
    trackSelector.innerHTML = "";
    // Scegli l'endpoint corretto in base al contesto
    const url = playlist == null ? "GetUserTracks" : "GetTracksNotInPlaylist?playlistTitle=" + encodeURIComponent(playlist.title);

    makeCall("GET", url, null, (req) => {
        if (req.readyState !== XMLHttpRequest.DONE) return;
        const message = req.responseText;
        if (req.status === 200) {
            let tracks = [];
            try { tracks = JSON.parse(message) || []; } catch (_) { tracks = []; }

            // Nessuna traccia: mostro messaggio leggibile
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

            // Supporto due modalità: legacy <select> o la nuova lista checkbox
            if (trackSelector.tagName && trackSelector.tagName.toLowerCase() === 'select') {
                // Riempio l'option per il select
                tracks.forEach(track => {
                    const option = document.createElement("option");
                    option.value = track.id.toString();
                    option.textContent = track.artist + " - " + track.title + " (" + track.year + ")";
                    trackSelector.appendChild(option);
                });
            } else {
                // Creo righe checkbox: nome input dipende dal context (create vs add)
                const inputName = (trackSelector.id && trackSelector.id.indexOf('create') >= 0) ? 'selectedTrackIds' : 'selectedTracksIds';
                tracks.forEach(track => {
                    const row = document.createElement('div');
                    row.className = 'track-checkbox-row';

                    const input = document.createElement('input');
                    input.type = 'checkbox';
                    input.name = inputName; // backend si aspetta questo nome
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
            // Sessione scaduta: reindirizzo al login
            alert("Sessione scaduta. Effettua di nuovo il login.");
            window.location.href = "login.html";
        } else {
            alert("Impossibile recuperare i dati.");
        }
    }, false); // false => non resetto eventuale form chiamante (qui non c'è)
}