package it.polimi.pure_html.controller;


import it.polimi.pure_html.DAO.PlaylistDAO;
import it.polimi.pure_html.entities.Track;
import it.polimi.pure_html.entities.User;
import it.polimi.pure_html.utils.ConnectionHandler;
import it.polimi.pure_html.utils.TemplateThymeleaf;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.WebContext;
import org.thymeleaf.web.servlet.JakartaServletWebApplication;

import java.io.IOException;
import java.io.Serial;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;


@WebServlet("/Playlist")
public class PlaylistController extends HttpServlet {
    @Serial
    private static final long serialVersionUID = 1L;
    private TemplateEngine templateEngine;
    private Connection connection = null;

    @Override
    public void init() throws ServletException {
        ServletContext context = getServletContext();
        connection = ConnectionHandler.openConnection(context);
        templateEngine = TemplateThymeleaf.getTemplateEngine(context);
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        JakartaServletWebApplication webApplication = JakartaServletWebApplication.buildApplication(getServletContext());
        WebContext context = new WebContext(webApplication.buildExchange(req, resp), req.getLocale());

        User user = (User) req.getSession().getAttribute("user");
        int playlistId = Integer.parseInt(req.getParameter("playlistId"));

        PlaylistDAO playlistDAO = new PlaylistDAO(connection);

        String trackGroupStringParam = req.getParameter("gr");
        int trackGroup = 0;
        if (trackGroupStringParam != null) {
            try {
                int Parsed = Integer.parseInt(trackGroupStringParam);
                if (Parsed > 0) {
                    trackGroup = Parsed;
                }
            } catch (NumberFormatException e) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid group");
                return;
            }

        }

        String playlistTitle;

        try {
            playlistTitle = playlistDAO.getPlaylistTitle(playlistId);
        } catch (SQLException e) {
            e.printStackTrace();
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            return;
        }

    List<Track> playlistTracks;
        int totalGroups = 1;
        try {
            playlistTracks = playlistDAO.getTrackGroup(playlistId, trackGroup);
            int totalTracks = playlistDAO.countPlaylistTracks(playlistId);
            int groupSize = 5;
            totalGroups = (int) Math.ceil(totalTracks / (double) groupSize);
            if (totalGroups < 1) totalGroups = 1;
            if (trackGroup >= totalGroups) {
                // gruppo richiesto fuori range -> vai all'ultimo valido
                resp.sendRedirect(req.getContextPath() + "/Playlist?playlistId=" + playlistId + "&gr=" + Math.max(0, totalGroups - 1));
                return;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            return;
        }

        List<Track> addableTracks;
        try {
            addableTracks = playlistDAO.getTracksNotInPlaylist(playlistTitle, user.id());
        } catch (SQLException e) {
            e.printStackTrace();
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            return;
        }

        // Costruisce modelli tracce normalizzate (come PlayerController) per evitare problemi di path legacy
        String ctxPath = req.getContextPath();
        List<Map<String,Object>> normalizedTracks = playlistTracks.stream()
                .map(t -> buildTrackModel(t, ctxPath))
                .collect(Collectors.toList());

        context.setVariable("trackGroup", trackGroup);
        context.setVariable("playlistId", playlistId);
        context.setVariable("playlistTitle", playlistTitle);
        context.setVariable("addableTracks", addableTracks);
        context.setVariable("playlistTracks", normalizedTracks);
        context.setVariable("totalGroups", totalGroups);
        // Messaggio feedback aggiunta tracce
        try {
            String addedParam = req.getParameter("added");
            if(addedParam != null){
                int addedCount = Integer.parseInt(addedParam);
                context.setVariable("addedCount", addedCount);
            }
        } catch (NumberFormatException ignore){ }
        String path = "playlist_page";
        templateEngine.process(path, context, resp.getWriter());
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        doGet(req, resp);
    }

    private Map<String,Object> buildTrackModel(Track track, String contextPath){
        Map<String,Object> m = new HashMap<>();
        m.put("trackId", track.id());
        m.put("title", track.title());
        m.put("artist", track.artist());
        m.put("album_title", track.album_title());
        m.put("year", track.year());
        m.put("genre", track.genre());

        String rawSong = normalizeLegacyPath(track.song_path(), true);
        String rawImg = normalizeLegacyPath(track.image_path(), false);
        String audioUrl = buildUrl(contextPath, rawSong);
        String imageUrl = buildUrl(contextPath, rawImg);
        if (imageUrl == null || imageUrl.isBlank()) {
            imageUrl = ""; // lasciamo vuoto per placeholder front-end
        }
        m.put("audioUrl", audioUrl);
        m.put("imageUrl", imageUrl);

        String album = track.album_title();
        String year = track.year() == null ? null : String.valueOf(track.year());
        String genre = track.genre();
        String albumLine = Stream.of(album, year, genre)
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.joining(" · "));
        m.put("albumLine", albumLine.isEmpty()? null : albumLine);
        return m;
    }

    private String buildUrl(String contextPath, String relative){
        if (relative == null || relative.isBlank()) return null;
        if (relative.startsWith("http://") || relative.startsWith("https://")) return relative;
        if (contextPath == null || contextPath.isBlank()) return relative.startsWith("/")? relative : "/"+relative;
        if (relative.startsWith("/")) return contextPath + relative;
        return contextPath + '/' + relative;
    }

    private String normalizeLegacyPath(String original, boolean audio){
        if (original == null || original.isBlank()) return original;
        String o = original.trim();
        if (o.startsWith("media/")) o = '/' + o;
        String lower = o.toLowerCase();
        boolean looksAudio = lower.endsWith(".mp3")||lower.endsWith(".wav")||lower.endsWith(".ogg")||lower.endsWith(".m4a");
        boolean looksImage = lower.endsWith(".jpg")||lower.endsWith(".jpeg")||lower.endsWith(".png")||lower.endsWith(".gif")||lower.endsWith(".webp");
        if (o.startsWith("/media/img/") && looksAudio){
            o = o.replaceFirst("/media/img/","/media/audio/");
        }
        if (!o.startsWith("/media/")){
            if (audio || looksAudio) return "/media/audio/" + o.replaceAll("^/+", "");
            if (!audio && looksImage) return "/media/img/" + o.replaceAll("^/+", "");
            return "/media/" + (audio?"audio":"img") + '/' + o.replaceAll("^/+", "");
        }
        return o;
    }

    @Override
    public void destroy() {
        ConnectionHandler.closeConnection(connection);
    }
}