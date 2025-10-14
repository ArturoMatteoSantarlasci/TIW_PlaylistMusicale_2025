package it.polimi.pure_html.controller;

import it.polimi.pure_html.DAO.PlaylistDAO;
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
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@WebServlet("/HomePage")
public class HomepageController extends HttpServlet {
    @Serial
    private static final long serialVersionUID = 1L;
    /**
     * Prepara i dati necessari a popolare la dashboard principale:
     * tracce disponibili dell'utente, playlist e messaggi di cortesia.
     */
    private TemplateEngine templateEngine;
    private Connection connection;
    private TrackDAO trackDAO;
    private PlaylistDAO playlistDAO;

    @Override
    public void init() throws ServletException {
        ServletContext context = getServletContext();
        templateEngine = TemplateThymeleaf.getTemplateEngine(context);
        connection = ConnectionHandler.openConnection(context);
        trackDAO = new TrackDAO(connection);
        playlistDAO = new PlaylistDAO(connection);
    }

    @Override
    public void doGet(HttpServletRequest req, HttpServletResponse res) throws IOException {
        User user = (User) req.getSession().getAttribute("user");

        JakartaServletWebApplication webApplication = JakartaServletWebApplication.buildApplication(getServletContext());
        WebContext context = new WebContext(webApplication.buildExchange(req, res), req.getLocale());

        // Recupero lista tracce dell'utente
        try {
            List<Track> userTracks = trackDAO.getUserTracks(user);
            context.setVariable("availableTracks", userTracks);
            context.setVariable("availableTracksCount", userTracks.size());
        } catch (SQLException e) {
            getServletContext().log("Errore nel recupero delle tracce per la homepage", e);
            context.setVariable("availableTracks", List.of());
            context.setVariable("availableTracksCount", 0);
        }

        // Recupero playlist da mostrare nella rail
        List<HomePlaylistView> playlistViews = new ArrayList<>();
        try {
            List<PlaylistDAO.PlaylistSummary> summaries = playlistDAO.getPlaylistSummaries(user);
            for (PlaylistDAO.PlaylistSummary summary : summaries) {
                LocalDate creationDate = summary.creationDate() == null ? null : summary.creationDate().toLocalDate();
                playlistViews.add(new HomePlaylistView(
                        summary.id(),
                        summary.title(),
                        summary.tracksCount(),
                        creationDate
                ));
            }
        } catch (SQLException e) {
            getServletContext().log("Errore nel recupero delle playlist per la homepage", e);
        }
        context.setVariable("playlists", playlistViews);
        context.setVariable("hasPlaylists", !playlistViews.isEmpty());

        // Messaggi successo / errore
        String created = req.getParameter("createdPlaylist");
        if("true".equalsIgnoreCase(created)){
            context.setVariable("createdPlaylist", true);
            context.setVariable("createdPlaylistTitle", req.getParameter("plTitle"));
            String added = req.getParameter("plAdded");
            if(added != null){
                try { context.setVariable("createdPlaylistAdded", Integer.parseInt(added)); } catch(NumberFormatException ignore){}
            }
        }
        if("true".equalsIgnoreCase(req.getParameter("uploadedTrack"))){
            context.setVariable("uploadedTrack", true);
            context.setVariable("uploadedTrackTitle", req.getParameter("trkTitle"));
        }
        if("true".equalsIgnoreCase(req.getParameter("duplicatePlaylist"))){
            context.setVariable("duplicatePlaylist", true);
        }
        if("true".equalsIgnoreCase(req.getParameter("duplicateTrack"))){
            context.setVariable("duplicateTrack", true);
        }

        templateEngine.process("HomePage.html", context, res.getWriter());
    }

    //eventuali form che per errore usano POST non rompono
    @Override
    public void doPost(HttpServletRequest req, HttpServletResponse res) throws IOException {
        doGet(req, res);
    }

    @Override
    public void destroy() {
        ConnectionHandler.closeConnection(connection);
        connection = null;
        trackDAO = null;
        playlistDAO = null;
    }

    private record HomePlaylistView(int id,
                                    String title,
                                    int tracksCount,
                                    LocalDate creationDate) {
    }
}
