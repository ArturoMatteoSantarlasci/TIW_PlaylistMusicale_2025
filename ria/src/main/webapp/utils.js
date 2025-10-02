function makeCall(method, url, formElement, callback, reset = true) {
    let req = new XMLHttpRequest();
    req.onreadystatechange = function() {
        callback(this);
    };
    req.open(method, url);
    if (formElement == null) {
        req.send();
    } else {
        let data = new URLSearchParams(new FormData(formElement));
        req.setRequestHeader('Content-Type', 'application/x-www-form-urlencoded');
        req.send(data);
    }
    if (formElement != null && reset === true) {
        formElement.reset();
    }
}