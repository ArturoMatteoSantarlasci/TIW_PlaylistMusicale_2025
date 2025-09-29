package it.polimi.pure_html.controller;

import it.polimi.pure_html.DAO.TrackDAO;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@WebServlet("/player")
public class PlayerController extends HttpServlet {

    @Serial
    private static final long serialVersionUID = 1L;

    private static final String PLACEHOLDER_COVER = "https://via.placeholder.com/640x640?text=Cover";

    private TemplateEngine templateEngine;
    private Connection connection;
    private TrackDAO trackDAO;
    private JakartaServletWebApplication webApplication;

    @Override
    public void init() throws ServletException {
        ServletContext context = getServletContext();
        connection = ConnectionHandler.openConnection(context);
        trackDAO = new TrackDAO(connection);
        templateEngine = TemplateThymeleaf.getTemplateEngine(context);
        webApplication = JakartaServletWebApplication.buildApplication(context);
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res) throws IOException {
        res.setCharacterEncoding("UTF-8");
        res.setContentType("text/html;charset=UTF-8");

        User user = (User) req.getSession().getAttribute("user");
        if (user == null) {
            res.sendRedirect(req.getContextPath() + "/Login");
            return;
        }

        String trackIdParam = req.getParameter("trackId");
        if (trackIdParam == null || trackIdParam.isBlank()) {
            res.sendError(HttpServletResponse.SC_BAD_REQUEST, "trackId mancante");
            return;
        }

        int trackId;
        try {
            trackId = Integer.parseInt(trackIdParam);
        } catch (NumberFormatException e) {
            res.sendError(HttpServletResponse.SC_BAD_REQUEST, "trackId non valido");
            return;
        }

        List<Track> userTracks;
        try {
            userTracks = trackDAO.getUserTracks(user);
        } catch (SQLException e) {
            getServletContext().log("Unable to load tracks for player view", e);
            res.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "Errore durante il caricamento delle tracce");
            return;
        }

        if (userTracks.isEmpty()) {
            res.sendRedirect(req.getContextPath() + "/All_Tracks");
            return;
        }

        int currentIndex = -1;
        for (int i = 0; i < userTracks.size(); i++) {
            if (userTracks.get(i).id() == trackId) {
                currentIndex = i;
                break;
            }
        }

        if (currentIndex < 0) {
            res.sendError(HttpServletResponse.SC_NOT_FOUND, "Traccia non trovata");
            return;
        }

        Track currentTrack = userTracks.get(currentIndex);

        var currentView = buildTrackModel(currentTrack, req.getContextPath());

        var prevView = currentIndex > 0
                ? buildTrackModel(userTracks.get(currentIndex - 1), req.getContextPath())
                : null;

        var nextView = currentIndex < userTracks.size() - 1
                ? buildTrackModel(userTracks.get(currentIndex + 1), req.getContextPath())
                : null;

        WebContext context = new WebContext(webApplication.buildExchange(req, res), req.getLocale());
        context.setVariable("track", currentView);
        context.setVariable("prevTrack", prevView);
        context.setVariable("nextTrack", nextView);
        context.setVariable("tracksUrl", req.getContextPath() + "/All_Tracks");

        templateEngine.process("player_page.html", context, res.getWriter());
    }

    @Override
    public void destroy() {
        ConnectionHandler.closeConnection(connection);
        connection = null;
        trackDAO = null;
        webApplication = null;
    }

    private Map<String, Object> buildTrackModel(Track track, String contextPath) {
        Map<String, Object> model = new HashMap<>();
        model.put("trackId", track.id());
        model.put("title", track.title());
        model.put("artist", track.artist());

        String audioUrl = buildUrl(contextPath, track.song_path());
        String coverUrl = buildUrl(contextPath, track.image_path());
        if (coverUrl == null || coverUrl.isBlank()) {
            coverUrl = PLACEHOLDER_COVER;
        }

        model.put("audioUrl", audioUrl);
        model.put("coverUrl", coverUrl);

        String album = track.album_title();
        String year = track.year() == null ? null : String.valueOf(track.year());
        String genre = track.genre();

        String albumLine = Stream.of(album, year, genre)
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.joining(" · "));

        model.put("albumLine", albumLine.isEmpty() ? null : albumLine);

        return model;
    }

    private String buildUrl(String contextPath, String relativePath) {
        if (relativePath == null || relativePath.isBlank()) {
            return null;
        }
        if (relativePath.startsWith("http://") || relativePath.startsWith("https://")) {
            return relativePath;
        }
        if (contextPath == null || contextPath.isBlank()) {
            return relativePath;
        }
        if (relativePath.startsWith("/")) {
            return contextPath + relativePath;
        }
        return contextPath + '/' + relativePath;
    }
}
