(function () {
    document.getElementById("create-account-button").addEventListener("click", (e) => register(e));

    /**
     * Registra un nuovo utente e lo reindirizza alla pagina di login.
     *
     * @param e evento mouse dall'utente
     */
    function register(e) {
        e.preventDefault();
        let form = e.target.closest("form");
        if (form.checkValidity()) {
            makeCall("POST", "Register", form, (req) => {
                if (req.readyState === XMLHttpRequest.DONE) {
                    let message = req.responseText;
                    switch (req.status) {
                        case 201:
                            window.location.href = "login.html?reg=1";
                            break;
                        case 400:
                        case 409:
                        case 500:
                            document.getElementById("error").firstChild.textContent = message;
                            document.getElementById("error").appendChild(document.createElement("hr"));
                            break;
                    }
                }
            }, false);
        } else {
            form.reportValidity();
        }
    }
}());