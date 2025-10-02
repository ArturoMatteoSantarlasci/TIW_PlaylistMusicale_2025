(function() {
    // Add event listeners for buttons
    document.getElementById("homepage-button").addEventListener("click", () => {
        // Load homepage content
        console.log("Homepage clicked");
    });

    document.getElementById("playlist-button").addEventListener("click", () => {
        // Load playlist content
        console.log("Playlist clicked");
    });

    document.getElementById("track-button").addEventListener("click", () => {
        // Load track content
        console.log("Track clicked");
    });

    document.getElementById("upload-track-modal-button").addEventListener("click", () => {
        // Open upload track modal
        console.log("Upload track clicked");
    });

    document.getElementById("track-selector-modal-button").addEventListener("click", () => {
        // Open add playlist modal
        console.log("Add playlist clicked");
    });

    document.getElementById("logout-button").addEventListener("click", () => {
        // Logout
        window.location.href = "login.html";
    });
})();