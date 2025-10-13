#import "../lib.typ": *

= Struttura del codice

== Componenti

#columns(2, [

  
  / Backend : 

  + DAOs
    - DAO interface
    - PlaylistDAO
    - TrackDAO
    - UserDAO

  + Entities
    - Playlist
    - Track
    - User

  + Servlets/Controller 
    - LoginController
    - HomePageController
    - AddTracksToPlaylist
    - PlaylistController
    - RegisterController
    - MediaServlet
    - PlayerController
    - Logout
    - CreatePlaylist
    - UploadTrack
    - #ria() GetTracksNotInPlaylist #footnote[#ria() Presenti solo nella versione RIA ]
    - #ria() GetUserTracks
    - #ria() TrackReorder
    - #ria() PlaylistGroupController 
  
  + Filters
    - InvalidUserChecker
    - PlaylistChecker
    - SelectedTracksChecker
    - TrackChecker
    - UserChecker

  + Utils
    - ConnectionHandler
    - TemplateThymeleaf #footnote[Solo per pure_html]
  
  + Config
    - MediaConfig

#v(1cm)

/ Frontend:

Nel frontend, a differenza del backend in cui troviamo solo poche differenze, pure_html e RIA sono molto diverse. 

- Pure_HTML
  - HomePage.html
  - Login.html
  - player_page.html
  - playlist_page.html
  - register.html

- #ria() RIA version
  - HomePage.html 
    - HomePage.js
  - login.html
    - login.js
  - register.html
    - register.js
  - utils.js



])

