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
import java.util.List;


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
        try {
            playlistTracks = playlistDAO.getTrackGroup(playlistId, trackGroup);
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

        context.setVariable("trackGroup", trackGroup);
        context.setVariable("playlistId", playlistId);
        context.setVariable("playlistTitle", playlistTitle);
        context.setVariable("addableTracks", addableTracks);
        context.setVariable("playlistTracks", playlistTracks);
        String path = "playlist_page";
        templateEngine.process(path, context, resp.getWriter());
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        doGet(req, resp);
    }

    @Override
    public void destroy() {
        ConnectionHandler.closeConnection(connection);
    }
}