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
import java.util.List;
import java.util.Objects;

@WebServlet("/All_Tracks")
public class AllTracksController extends HttpServlet {
    @Serial
    private static final long serialVersionUID = 1L;

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
            res.sendRedirect(getServletContext().getContextPath() + "/Login");
            return;
        }

        List<TrackViewModel> tracks;
        try {
            tracks = trackDAO.getUserTracks(user)
                    .stream()
                    .map(track -> TrackViewModel.fromTrack(track, req.getContextPath()))
                    .toList();
        } catch (SQLException e) {
            getServletContext().log("Unable to retrieve tracks for user " + user.id(), e);
            res.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "Errore durante il recupero delle tracce");
            return;
        }

        WebContext context = new WebContext(webApplication.buildExchange(req, res), req.getLocale());
        context.setVariable("tracks", tracks);
        context.setVariable("hasTracks", !tracks.isEmpty());

        templateEngine.process("All_Tracks.html", context, res.getWriter());
    }

    @Override
    public void destroy() {
        ConnectionHandler.closeConnection(connection);
        connection = null;
        trackDAO = null;
        webApplication = null;
    }

    private static record TrackViewModel(int trackId,
                                  String title,
                                  String artist,
                                  String albumTitle,
                                  String songUrl,
                                  String imageUrl,
                                  String durationFormatted) {

        private static TrackViewModel fromTrack(Track track, String contextPath) {
            Objects.requireNonNull(track, "track");
            String basePath = contextPath == null ? "" : contextPath;
            return new TrackViewModel(
                    track.id(),
                    track.title(),
                    track.artist(),
                    track.album_title(),
                    buildUrl(basePath, track.song_path()),
                    buildUrl(basePath, track.image_path()),
                    null
            );
        }

        private static String buildUrl(String contextPath, String relativePath) {
            if (relativePath == null || relativePath.isBlank()) {
                return relativePath;
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
}
