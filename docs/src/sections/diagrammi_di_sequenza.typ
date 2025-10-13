#import "../lib.typ": *

#show: thymeleaf_trick.with()

= Diagrammi di sequenza

#pagebreak()

#seq_diagram(
  "Login",
  diagram({
    _par("A", display-name: "Client")
    _par("B", display-name: "Login")
    _par("G", display-name: "Request")
    _par("H", display-name: "ctx")
    _par("C", display-name: "Thymeleaf", shape: "custom", custom-image: thymeleaf)
    _par("D", display-name: "UserDAO")
    _par("E", display-name: "Session")
    _par("F", display-name: "HomePage")

    // get
    _seq("A", "B", enable-dst: true, comment: "doGet()")
    _seq("B", "G", enable-dst: true, comment: [getParameter ("error")])
    _seq("G", "B", disable-src: true, comment: [return error])
    _seq(
      "B",
      "H",
      comment: [[error != null && error == true] \ ? setVariable ("error", true)],
    )
    _seq("B", "C", enable-dst: true, comment: "process (login.html, ctx)", lifeline-style: (fill: rgb("#005F0F")))
    _seq("C", "B", disable-src: true, comment: "login.html")
    _seq("B", "A", disable-src: true, comment: "index.html")

    // post
    _seq("A", "B", enable-dst: true, comment: "doPost()")
    _seq("B", "D", enable-dst: true, comment: [getParameter ("nickname")])
    _seq("D", "B", disable-src: true, comment: [return nickname])
    _seq("B", "D", enable-dst: true, comment: [getParameter ("password")])
    _seq("D", "B", disable-src: true, comment: [return password])
    _seq("B", "D", enable-dst: true, comment: "UserChecker (nickname,password)")
    _seq("D", "B", disable-src: true, comment: "return schrödingerUser")
    _seq("B", "B", comment: [[schrödingerUser == null] \ ? redirect `/Login?error=true`])
    _seq("B", "E", comment: [[schrödingerUser != null] \ ? setAttribute("user", schrödingerUser)])
    _seq("B", "F", disable-src: true, comment: "Redirect")
  }),
  comment: [
  ],
  label_: "login-sequence",
  comment_next_page_: true,
)

#seq_diagram(
  [Registrazione],
  diagram({
    _par("A", display-name: "Client")
    _par("G", display-name: "Login")
    _par("B", display-name: "Register")
    _par("H", display-name: "Request")
    _par("C", display-name: "Thymeleaf", shape: "custom", custom-image: thymeleaf)
    _par("D", display-name: "UserDAO")
    _par("F", display-name: "Login")
    // _par("G", display-name: "ctx")
    // _par("E", display-name: "Session")

    // get
    _seq("A", "B", enable-dst: true, comment: "doGet()")
    _seq("B", "H", enable-dst: true, comment: [getParameter ("isUserAdded")])
    _seq("H", "B", disable-src: true, comment: [return isUserAdded])
    _seq("B", "C", enable-dst: true, comment: "process (register.html, ctx)", lifeline-style: (fill: rgb("#005F0F")))
    _seq("C", "B", disable-src: true, comment: "register.html")
    _seq("B", "A", disable-src: true, comment: "register.html")

    // post
    _seq("A", "B", enable-dst: true, comment: "doPost()")
    _seq("B", "H", enable-dst: true, comment: [getParameter ("nickname")])
    _seq("H", "B", disable-src: true, comment: [return nickname])
    _seq("B", "H", enable-dst: true, comment: [getParameter ("password")])
    _seq("H", "B", disable-src: true, comment: [return password])
    _seq("B", "H", enable-dst: true, comment: [getParameter ("name")])
    _seq("H", "B", disable-src: true, comment: [return name])
    _seq("B", "H", enable-dst: true, comment: [getParameter ("surname")])
    _seq("H", "B", disable-src: true, comment: [return surname])
    _seq("B", "D", enable-dst: true, comment: "addUser (user)")
    _seq("D", "B", disable-src: true, comment: "return isUserAdded")
    _seq("B", "F", comment: "[isUserAdded] ? redirect")
    _seq("B", "B", comment: [[!isUserAdded] \ ? redirect `/Registration?isUserAdded=false`])
    _seq("B", "C", enable-dst: true, comment: "process (register.html, ctx)", lifeline-style: (fill: rgb("#005F0F")))
    _seq("C", "G", disable-src: true, comment: "login.html")
    _seq("G", "A", disable-src: true, comment: "login.html")
  }),
  comment_next_page_: true,
  comment: [

  ],
  label_: "register-sequence",
)

#seq_diagram(
  [HomePage],
  diagram({
    _par("A", display-name: "Client")
    _par("B", display-name: "HomePage")
    _par("F", display-name: "Session")
    _par("C", display-name: "PlaylistDAO")
    _par("G", display-name: "Request")
    _par("D", display-name: "ctx")
    _par("E", display-name: "Thymeleaf", shape: "custom", custom-image: thymeleaf)

    _seq("A", "B", enable-dst: true, comment: "doGet()")
    _seq("B", "F", enable-dst: true, comment: [getAttribute ("user")])
    _seq("F", "B", disable-src: true, comment: [return user])
    _seq("B", "C", enable-dst: true, comment: "getUserPlaylists (user)")
    _seq("C", "B", disable-src: true, comment: "return playlists")
    _seq("B", "C", enable-dst: true, comment: "getUserTracks (user)")
    _seq("C", "B", disable-src: true, comment: "return userTracks")
    _seq("B", "G", enable-dst: true, comment: [getParameter ("duplicateTrack")])
    _seq("G", "B", disable-src: true, comment: [return duplicateTrack])
    _seq(
      "B",
      "D",
      comment: [[duplicateTrack != null && duplicateTrack == true] \ ? setVariable("duplicateTrack", true)],
    )
    _seq("B", "G", enable-dst: true, comment: [getParameter ("duplicatePlaylist")])
    _seq("G", "B", disable-src: true, comment: [return duplicatePlaylist])
    _seq(
      "B",
      "D",
      comment: [[duplicatePlaylist != null && duplicatePlaylist == true] \ ? setVariable("duplicatePlaylist", true)],
    )
    _seq("B", "D", comment: [setVariable ("userTracks", userTracks)])
    _seq("B", "D", comment: [setVariable ("playlists", Playlists)])
    _seq("B", "D", comment: [setVariable ("genres", genres)])
    _seq("B", "E", enable-dst: true, comment: "process (HomePage.html, ctx)", lifeline-style: (fill: rgb("#005F0F")))
    _seq("E", "B", disable-src: true, comment: "HomePage.html")
    _seq("B", "A", disable-src: true, comment: "HomePage.html")
  }),
  comment_next_page_: true,
  comment: [
  ],
  label_: "homepage-sequence",
)

#seq_diagram(
  [Playlist],
  diagram({
    _par("A", display-name: "HomePage")
    _par("B", display-name: "Playlist")
    _par("F", display-name: "Session")
    _par("G", display-name: "Request")
    _par("C", display-name: "PlaylistDAO")
    _par("D", display-name: "ctx")
    _par("E", display-name: "Thymeleaf", shape: "custom", custom-image: thymeleaf)

    _seq("A", "B", enable-dst: true, comment: "doGet()")
    _seq("B", "F", enable-dst: true, comment: [getAttribute ("user")])
    _seq("F", "B", disable-src: true, comment: [return user])
    _seq("B", "G", enable-dst: true, comment: [getParameter ("playlistId")])
    _seq("G", "B", disable-src: true, comment: [return playlistId])
    _seq("B", "G", enable-dst: true, comment: [getParameter ("gr")])
    _seq("G", "B", disable-src: true, comment: [return trackGroupString])
    _seq("B", "C", enable-dst: true, comment: [getPlaylistTitle (playlistId)])
    _seq("C", "B", disable-src: true, comment: "return playlistTitle")
    _seq("B", "C", enable-dst: true, comment: "getTrackGroup (playlistId, trackGroup)")
    _seq("C", "B", disable-src: true, comment: "return playlistTracks")
    _seq("B", "C", enable-dst: true, comment: "getTracksNotInPlaylist (playlistTitle, user.id())")
    _seq("C", "B", disable-src: true, comment: "return addableTracks")
    _seq("B", "D", comment: [setVariable ("trackGroup", trackGroup)])
    _seq("B", "D", comment: [setVariable ("playlistId", playlistId)])
    _seq("B", "D", comment: [setVariable ("playlistTitle", playlistTitle)])
    _seq("B", "D", comment: [setVariable ("addableTracks", addableTracks)])
    _seq("B", "D", comment: [setVariable ("playlistTracks", playlistTracks)])
    _seq("B", "E", enable-dst: true, comment: "process (playlist_page, ctx)", lifeline-style: (fill: rgb("#005F0F")))
    _seq("E", "B", disable-src: true, comment: "playlist_page.html")
    _seq("B", "A", disable-src: true, comment: "playlist_page.html")
  }),
  comment: [
  ],
  label_: "playlistpage-sequence",
  comment_next_page_: true,
)

#seq_diagram(
  [Track],
  diagram({
    _par("A", display-name: "Client")
    _par("B", display-name: "Track")
    _par("G", display-name: "Request")
    _par("C", display-name: "TrackDAO")
    _par("D", display-name: "ctx")
    _par("E", display-name: "Thymeleaf", shape: "custom", custom-image: thymeleaf)

    _seq("A", "B", enable-dst: true, comment: "doGet()")
    _seq("B", "G", enable-dst: true, comment: [getParameter ("track_id")])
    _seq("G", "B", disable-src: true, comment: [return trackId])
    _seq("B", "C", enable-dst: true, comment: "getTrackById (trackId)")
    _seq("C", "B", disable-src: true, comment: "return track")
    _seq("B", "D", comment: [setVariable("track", track)])
    _seq("B", "E", enable-dst: true, comment: "process (player_page, ctx)", lifeline-style: (fill: rgb("#005F0F")))
    _seq("E", "B", disable-src: true, comment: "player_page.html")
    _seq("B", "A", disable-src: true, comment: "player_page.html")
  }),
  comment: [
  ],
  label_: "track-sequence",
  comment_next_page_: false,
)

#seq_diagram(
  [Creazione Playlist],
  diagram({
    _par("A", display-name: "Client")
    _par("B", display-name: "CreatePlaylist")
    _par("C", display-name: "AddTracksToPlaylist")
    _par("G", display-name: "Request")
    _par("E", display-name: "PlaylistDAO")
    _par("D", display-name: "HomePage")

    _seq("A", "B", enable-dst: true, comment: "doPost()")
    _seq("B", "G", enable-dst: true, comment: [getParameter ("playlistTitle")])
    _seq("G", "B", disable-src: true, comment: [return playlistTitle])
    _seq("B", "G", enable-dst: true, comment: [getParameterValues ("selectedTracks")])
    _seq("G", "B", disable-src: true, comment: [return selectedTracks])
    _seq("B", "E", enable-dst: true, comment: [createPlaylist (playlistTitle)])
    _seq("E", "B", disable-src: true, comment: [return playlistId])
    _seq(
      "B",
      "E",
      comment: [
        !selectedCreationTracksIds.isEmpty() \
        ? addTracksToPlaylist (selectedCreationTracksIds, playlistId)
      ],
    )
    _seq("B", "D", disable-src: true, comment: [Redirect])
  }),
  comment: [
  ],
  label_: "createplaylist-sequence",
  comment_next_page_: false,
)

#seq_diagram(
  [Logout],
  diagram({
    _par("A", display-name: "Client")
    _par("B", display-name: "Logout")
    _par("C", display-name: "Request")
    _par("D", display-name: "Login")

    _seq("A", "B", enable-dst: true, comment: "doGet()")
    _seq("B", "C", enable-dst: true, comment: [getSession (false)])
    _seq("C", "B", disable-src: true, comment: [return session $=>$ [session != null] ? session.invalidate()])
    _seq("B", "D", disable-src: true, comment: "Redirect")
  }),
  comment: [
  ],
  label_: "logout-sequence",
  comment_next_page_: false,
  next_page: false,
)

#v(42cm)

#seq_diagram(
  [UploadTrack sequence diagram],
  diagram({
    _par("A", display-name: "Client")
    _par("B", display-name: "UploadTrack")
    _par("E", display-name: "Track")
    _par("C", display-name: "TrackDAO")
    _par("F", display-name: "FileSys")
    _par("D", display-name: "HomePage")

    _seq("A", "B", enable-dst: true, comment: "doPost()")
    _note("right", [POST /UploadTrack \ title, artist, album, year, \ genre, musicTrack, image])
    _seq("B", "B", enable-dst: true, comment: [fileDetails = processPart(image, "image")])
    _seq("B", "B", comment: [hash = java.util.HexFormat.of().formatHex(digest.digest())])
    _seq("B", "C", enable-dst: true, comment: [relativeFilePath = isImageFileAlreadyPresent(hash)])
    _seq("C", "B", disable-src: true, comment: [return path || null])
    _seq("B", "F", comment: [outputFile = absolutePath = targetDir.resolve(filename)])
    

    _seq("B", "B", disable-src: true, comment: [return new FileDetails(relativeFilePath, hash)])
    _seq("B", "B", comment: [fileDetails = processPart(musicTrack, "audio")])
    _seq("B", "E", comment: [track = new Track(...)])
    _alt(
      "try",
      {
        _seq("B", "C", comment: [addTrack(track)])
        _seq("B", "D", comment: [Redirect])
      },
      "catch SQLException",
      { _seq("B", "B", comment: [newFiles.forEach(file -> file.delete())]) },
      [finally],
      {
        _seq("B", "B", disable-src: true, comment: [newFiles.clear()])
      },
    )
  })
)

#seq_diagram(
  [Aggiungi traccia],
  diagram({
    _par("A", display-name: "Client")
    _par("B", display-name: "addTracksToPlaylist")
    _par("C", display-name: "Session")
    _par("D", display-name: "Request")
    _par("E", display-name: "PlaylistDAO")

    _seq("A", "B", enable-dst: true, comment: "doPost()")
    _seq("B", "C", enable-dst: true, comment: [getAttribute ("user")])
    _seq("C", "B", disable-src: true, comment: [return user])
    _seq("B", "D", enable-dst: true, comment: [getParameterValues ("selectedTracksIds")])
    _seq("D", "B", disable-src: true, comment: [return selectedTracksIds])
    _seq("B", "D", enable-dst: true, comment: [getParameter ("playlistId")])
    _seq("D", "B", disable-src: true, comment: [return playlistId])
    _seq("B", "E", disable-src: true, comment: [addTracksToPlaylist (clientTracksIds, playlistId)])
  }),
  comment: [],
  label_: "add-tracks-sequence",
  comment_next_page_: false,
  next_page: true,
)

#seq_diagram(
  [#ria() Riordine tracce],
  diagram({
    _par("A", display-name: "Client")
    _par("B", display-name: "TrackReorder")
    _par("C", display-name: "Request")
    _par("E", display-name: "Gson")
    _par("D", display-name: "PlaylistDAO")

    _seq("A", "B", enable-dst: true, comment: "doGet()")
    _seq("B", "C", enable-dst: true, comment: [getReader])
    _seq("C", "B", disable-src: true, comment: [return reader])
    _seq("B", "E", enable-dst: true, comment: [fromJson(reader)])
    _seq("E", "B", disable-src: true, comment: [return requestDoubleData])
    _seq(
      "B",
      "D",
      disable-src: true,
    )
  }),
  label_: "track-reordering-sequence",
  comment_next_page_: false,
)

#seq_diagram(
  [#ria() GetUserTracks],
  diagram({
    _par("A", display-name: "Client")
    _par("B", display-name: "GetUserTracks")
    _par("C", display-name: "TrackDAO")
    _par("E", display-name: "Gson")

    _seq("A", "B", enable-dst: true, comment: [doGet()])
    _seq("B", "C", comment: [getAttribute ("user")])
    _seq("C", "B", comment: [return user])
    _seq("B", "C", comment: [getUserTracks (user)])
    _seq("C", "B", comment: [return userTracks])
    _seq("B", "E", enable-dst: true, comment: [toJson (userTracks)])
    _seq("E", "B", disable-src: true, comment: [return userTracks[JSON]])
    _seq("B", "A", disable-src: true, comment: [userTracks])
  }),
  comment: [
  ],
  label_: "get-user-tracks-sequence",
  comment_next_page_: false,
)

#seq_diagram(
  [#ria() Evento: Login],
  diagram({
    _par("A", display-name: "login.html")
    _par("B", display-name: [utils.js + login.js])
    _par("E", display-name: [Login])
    _par("C", display-name: [UserDAO])
    _par("D", display-name: [HomePage.html])

    _seq("A", "B", comment: [GET])
    _seq("A", "B", enable-dst: true, comment: [Login], lifeline-style: (fill: rgb("#3178C6")))
    _seq("B", "E", comment: [POST: Login])
    _seq("E", "C", comment: [checkUser()])
    _seq("C", "B", comment: [Response])
    _seq("B", "B", comment: [[Response.status != 200] ? error])
    _seq("B", "D", disable-src: true, comment: [Redirect])
  }),
  comment: [
  ],
  label_: "ria-event-login-sequence",
  comment_next_page_: false,
)

#seq_diagram(
  [#ria() Evento: Register],
  diagram({
    _par("A", display-name: "login.html")
    _par("B", display-name: [utils.js])
    _par("D", display-name: [register.html + register.js])
    _par("E", display-name: [Register])
    _par("C", display-name: [UserDAO])

    _seq("A", "B", comment: [GET])
    _seq("A", "B", enable-dst: true, comment: [register()], lifeline-style: (fill: rgb("#3178C6")))
    _seq("B", "D", comment: [Redirect])
    _seq("D", "E", comment: [Register])
    _seq("E", "C", comment: [addUser()])
    _seq("C", "B", comment: [Response])
    _seq("B", "D", comment: [[response.status != 200] ? error])
    _seq("B", "A", disable-src: true, comment: [Redirect])
  }),
  comment: [
  ],
  label_: "ria-event-register-sequence",
  comment_next_page_: false,
)

#seq_diagram(
  [#ria() Evento: Logout],
  diagram({
    _par("A", display-name: "homepage.html")
    _par("B", display-name: [homepage.js])
    _par("C", display-name: [Logout])
    _par("D", display-name: [login.html])

    _seq("A", "B", comment: [GET])
    _seq("A", "B", enable-dst: true, comment: [Logout], lifeline-style: (fill: rgb("#3178C6")))
    _seq("B", "C", comment: [GET])
    _seq("C", "B", comment: [response])
    _seq("B", "D", disable-src: true, comment: [[response.status == 200] ? Redirect])
  }),
  comment: [
  ],
  label_: "ria-event-logout-sequence",
  comment_next_page_: false,
)

#seq_diagram(
  [#ria() Evento: accesso Homepage],
  diagram({
    _par("A", display-name: "homepage.html +
      homepage.js")
    _par("B", display-name: "HomeView")
    _par("C", display-name: "HomePage")
    _par("D", display-name: "PlaylistDAO")

    _seq("A", "B", enable-dst: true, comment: [homeView.show()])
    _seq("B", "B", comment: [clearModals()])
    _seq("B", "B", comment: [clearBottomNavbar()])
    _seq("B", "B", comment: [loadCreatePlaylistModal()])
    _seq("B", "B", comment: [loadUploadTrackModal()])
    _seq("B", "B", comment: [loadButtons()])
    _seq("B", "B", enable-dst: true, comment: [loadPlaylists()])
    _seq("B", "B", comment: [cleanMain()])
    _seq(
      "B",
      "C",
      enable-dst: true,
      comment: [AJAX GET],
      lifeline-style: (fill: rgb("#3178C6")),
    )
    _seq("C", "D", enable-dst: true, comment: [getUserPlaylists(user)])
    _seq("D", "C", disable-src: true, comment: [playlists])
    _seq("C", "B", disable-src: true, comment: [playlists])
    _seq("B", "B", comment: [[req.status == 200]? \ playlistGrid(playlists)])
    _seq("B", "B", comment: [[else] alert(...)])
    _seq("B", "B", disable-src: true)
    _seq("B", "A", disable-src: true)
  }),
  comment: [
  ],
  label_: "ria-event-logout-sequence",
  comment_next_page_: false,
)

#seq_diagram(
  [#ria() Evento: Accesso playlist],
  diagram({
    _par("A", display-name: "homepage.html +
      homepage.js")
    _par("B", display-name: "PlaylistView")
    _par("C", display-name: "Playlist")
    _par("D", display-name: "PlaylistDAO")

    _seq("A", "B", enable-dst: true, comment: [playlistView.show(playlist)])
    _seq("B", "B", comment: [clearBottomNavbar()])
    _seq("B", "B", comment: [clearModals()])
    _seq("B", "B", comment: [loadAddTracksModal()])
    _seq("B", "B", comment: [loadPlaylistView(playlist)])
    _seq("B", "B", enable-dst: true, comment: [loadPlaylistGroup(playlist, currentGroup)])
    _seq("B", "B", comment: [cleanMain()])
    _seq("B", "C", enable-dst: true, comment: [AJAX GET \ /Playlist?\ playlistId=playlist.id], lifeline-style: (fill: rgb("#3178C6")))
    _seq("C", "D", enable-dst: true, comment: [getPlaylistTracksId(playlistId)])
    _seq("D", "C", disable-src: true, comment: [playlistTracks])
    _seq("C", "B", disable-src: true, comment: [playlistTracks])
    _seq("B", "B", comment: [[req.status == 200]? \ trackGrid(playlistTracks) \ loadPrevNextButtons()])
    _seq("B", "B", comment: [[else] alert(...)])
    _seq("B", "B", disable-src: true)
    _seq("B", "A", disable-src: true)
  }),
  comment: [
 
  ],
  label_: "ria-event-logout-sequence",
  comment_next_page_: false,
)

#seq_diagram(
  [#ria() Evento: Upload Track modal],
  diagram({
    _par("A", display-name: "HomeView")
    _par("B", display-name: "MainLoader")
    _par("D", display-name: "utils.js")
    _par("C", display-name: "UploadTrack modal")
    _par("F", display-name: "UploadTrack")

    _seq("A", "B", enable-dst: true, comment: [click()])
    _seq("B", "B", comment: [loadYears()])
    _seq(
      "B",
      "D",
      enable-dst: true,
      comment: [loadGenres()],
      lifeline-style: (fill: rgb("#3178C6")),
    )
    _seq(
      "D",
      "D",
      comment: [
        AJAX GET \
        genres.json
      ],
    )
    _seq(
      "D",
      "D",
      comment: [
        [req.status == 200] ? \
        append genres to \
        genre-selection
      ],
    )
    _seq("D", "B", disable-src: true)
    _seq("B", "C", disable-src: true, enable-dst: true, comment: [showModal (upload-track)])
    _seq("C", "C", comment: [upload-track-btn.click()])
    _seq(
      "C",
      "F",
      enable-dst: true,
      comment: [
        AJAX POST \
        form
      ],
      lifeline-style: (fill: rgb("#3178C6")),
    )
    _seq(
      "F",
      "C",
      disable-src: true,
      comment: [response],
    )
    _seq(
      "C",
      "C",
      comment: [
        [req.status == 201] \
        ? success : error
      ],
    )
    _seq("C", "A", disable-src: true, comment: [modal-close.click()])
  }),
  comment: [
  
  ],
  label_: "upload-track-modal-sequence",
  comment_next_page_: false,
)

#seq_diagram(
  [#ria() Evento: modale reorder],
  diagram({
    _par("B", display-name: "HomeView")
    _par("A", display-name: "MainLoader")
    _par("C", display-name: "ReorderTrack modal")
    _par("D", display-name: "homepage.js")
    _par("E", display-name: "Playlist")
    _par("F", display-name: "TrackReorder")

    _seq("B", "A", comment: [click()])
    _seq("A", "C", enable-dst: true, comment: [loadReorderModal()])
    _seq(
      "C",
      "D",
      enable-dst: true,
      comment: [
        loadUserTracksOl (\
        trackSelector,\
        playlist)
      ],
      lifeline-style: (fill: rgb("#3178C6")),
    )
    _seq("D", "E", enable-dst: true, comment: [AJAX GET \ Playlist? \ playlistId=playlist.id])
    _seq("E", "D", disable-src: true, comment: [tracks])
    _seq("D", "D", comment: [[req.status == 200] ? \ add tracks to selector])
    _seq("D", "D", comment: [[else] alert(...)])
    _seq("D", "C", disable-src: true)
    _seq(
      "C",
      "D",
      enable-dst: true,
      comment: [saveOrder (e, \
        playlistId)
      ],
      lifeline-style: (fill: rgb("#3178C6")),
    )
    _seq(
      "D",
      "F",
      enable-dst: true,
      comment: [
        AJAX POST \
        requestData:\
        {trackIds,playlistId}
      ],
    )
    _seq(
      "F",
      "D",
      disable-src: true,
      comment: [
        [req.status == 201] \
        ? success : error
      ],
    )
    _seq("D", "C", disable-src: true)
    _seq("C", "B", disable-src: true, comment: [closeReorderModal()])
  }),
  comment: [
  ],
  label_: "reorder-modal-sequence",
  comment_next_page_: false,
)
#seq_diagram(
  [#ria() Evento: modale creazione Playlist],
  diagram({
    _par("B", display-name: "HomeView")
    _par("C", display-name: "create-playlist modal")
    _par("A", display-name: "GetUserTracks")
    _par("D", display-name: "homepage.js")
    _par("E", display-name: "CreatePlaylist")

    _seq("B", "C", enable-dst: true, comment: [click()])
    _seq("C", "C", comment: [loadUserTracks()])
    _seq("C", "A", enable-dst: true, comment: [
        AJAX GET
      ],
      lifeline-style: (fill: rgb("#3178C6")),
    )
    _seq("A", "C", disable-src: true, comment: [tracks])
    _seq(
      "C",
      "D",
      enable-dst: true,
      comment: [create-playlist-btn.click()],
      lifeline-style: (fill: rgb("#3178C6")),
    )
    _seq(
      "D",
      "E",
      enable-dst: true,
      comment: [
        AJAX POST \
        form
      ],
    )
    _seq(
      "E",
      "D",
      disable-src: true,
      comment: [
        [req.status == 201] \
        ? success : error
      ],
    )
    _seq("D", "C", disable-src: true)
    _seq("C", "B", disable-src: true, comment: [modal-close.click()])
  }),
  label_: "create-playlist-modal-sequence",
  comment_next_page_: false,
)

#seq_diagram(
  [#ria() Evento: modale aggiungi traccia],
  diagram({
    _par("A", display-name: "Browser")
    _par("B", display-name: "PlaylistView")
    _par("E", display-name: "Add Track modal")
    _par("C", display-name: "utils.js")
    _par("D", display-name: "GetTracksNotInPlaylist")
    _par("F", display-name: "PlaylistDAO")

    _seq("A", "B", enable-dst: true, comment: [Add Playlist \ button click])
    _seq(
      "B",
      "C",
      enable-dst: true,
      comment: [loadUserTracks \ (trackSelector, playlist)],
      lifeline-style: (fill: rgb("#3178C6")),
    )
    _seq("C", "D", enable-dst: true, comment: [AJAX GET \ /GetTracksNotInPlaylist? \ playlistTitle=playlist.title])
    _seq("D", "C", disable-src: true, comment: [tracks])
    _seq("C", "C", comment: [[req.status == 200]? \ add tracks to selector])
    _seq("C", "C", comment: [[else] alert(...)])
    _seq("C", "B", disable-src: true)
    _seq("B", "C", comment: [showModal(modal)])
    _seq("B", "B", disable-src: true)
    _seq("A", "E", enable-dst: true, comment: [Create playlist button click])
    _seq("E", "F", comment: [AJAX POST /AddTracks?playlistId=playlist.id])
    _alt(
      [req.status==201],
      {
        _seq("E", "C", comment: [loadUserTracks \ (trackSelector, \ playlist)])
        _seq("E", "B", comment: [loadPlaylist \ Tracks(playlist)])
        _seq("E", "E", comment: [Show success \ message \ form.reset()])
      },
      [else],
      {
        _seq("E", "E", comment: [Show error \ message])
      },
    )
    _seq("E", "E", disable-src: true)
  }),
  label_: "add-track-modal-sequence",
)
