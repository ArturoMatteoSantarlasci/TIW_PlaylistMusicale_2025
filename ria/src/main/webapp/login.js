function setupLogin() {
    // add listener on Login button
    document.getElementById("login-button").addEventListener("click", (e) => login(e));

    /**
     * Checks for the Login and redirects to Homepage.
     *
     * @param e mouse event from the User
     */
    function login(e) {
        e.preventDefault();
        let form = e.target.closest("form");
        if (form.checkValidity()) {
            makeCall("POST", "Login", form, (req) => {
                if (req.readyState === XMLHttpRequest.DONE) {
                    let message = req.responseText;
                    switch (req.status) {
                        case 200:
                            window.location.href = "homepage.html";
                            break;
                        case 400:
                        case 401:
                        case 500:
                            document.getElementById("error").textContent = message;
                            break;
                    }
                }
            }, false);
        } else {
            form.reportValidity();
        }
    }

    /**
     * Redirects to Register web page.
     */
    document.getElementById("register-button").addEventListener("click", () => {
        window.location.href = "register.html"
    })
}

setupLogin();