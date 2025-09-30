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
                    .map(track -> TrackViewModel.fromTrack(track, req.getContextPath(), getServletContext()))
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

    private static final class TrackViewModel {
        private final int trackId;
        private final String title;
        private final String artist;
        private final String albumTitle;
        private final String songUrl;
        private final String imageUrl;
        private final String durationFormatted;

        private TrackViewModel(int trackId,
                               String title,
                               String artist,
                               String albumTitle,
                               String songUrl,
                               String imageUrl,
                               String durationFormatted) {
            this.trackId = trackId;
            this.title = title;
            this.artist = artist;
            this.albumTitle = albumTitle;
            this.songUrl = songUrl;
            this.imageUrl = imageUrl;
            this.durationFormatted = durationFormatted;
        }

        private static TrackViewModel fromTrack(Track track, String contextPath, ServletContext context) {
            Objects.requireNonNull(track, "track");
            String basePath = contextPath == null ? "" : contextPath;
            String resolvedImagePath = resolveImagePath(context, track.image_path());
            return new TrackViewModel(
                    track.id(),
                    track.title(),
                    track.artist(),
                    track.album_title(),
                    buildUrl(basePath, track.song_path()),
                    buildUrl(basePath, resolvedImagePath),
                    null
            );
        }

        private static String resolveImagePath(ServletContext context, String imagePath) {
            if (imagePath == null || imagePath.isBlank()) {
                return null;
            }
            if (imagePath.startsWith("http://") || imagePath.startsWith("https://")) {
                return imagePath;
            }
            if (context == null) {
                return imagePath;
            }

            String normalized = imagePath.startsWith("/") ? imagePath : '/' + imagePath;
            if (resourceExists(context, normalized)) {
                return normalized;
            }

            int slash = normalized.lastIndexOf('/');
            int dot = normalized.lastIndexOf('.');
            if (dot > slash) {
                String base = normalized.substring(0, dot);
                String extension = normalized.substring(dot);
                String[] alternatives = {".jpeg", ".jpg", ".png", ".webp"};
                for (String candidateExt : alternatives) {
                    if (candidateExt.equalsIgnoreCase(extension)) {
                        continue;
                    }
                    String candidate = base + candidateExt;
                    if (resourceExists(context, candidate)) {
                        return candidate;
                    }
                }
            }

            return null;
        }

        private static boolean resourceExists(ServletContext context, String absolutePath) {
            try (var ignored = context.getResourceAsStream(absolutePath)) {
                return ignored != null;
            } catch (Exception ignored) {
                return false;
            }
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

        public int getTrackId() {
            return trackId;
        }

        public String getTitle() {
            return title;
        }

        public String getArtist() {
            return artist;
        }

        public String getAlbumTitle() {
            return albumTitle;
        }

        public String getSongUrl() {
            return songUrl;
        }

        public String getImageUrl() {
            return imageUrl;
        }

        public String getDurationFormatted() {
            return durationFormatted;
        }
    }
}