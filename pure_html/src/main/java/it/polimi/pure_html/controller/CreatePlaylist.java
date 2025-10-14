package it.polimi.pure_html.controller;

import it.polimi.pure_html.DAO.PlaylistDAO;
import it.polimi.pure_html.entities.Playlist;
import it.polimi.pure_html.entities.User;
import it.polimi.pure_html.utils.ConnectionHandler;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.Serial;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.util.ArrayList;
import java.util.List;

/**
 * Gestisce la creazione di una nuova playlist e l'eventuale associazione
 * di tracce selezionate durante la fase di creazione.
 */
@WebServlet("/CreatePlaylist")
public class CreatePlaylist extends HttpServlet {
    @Serial
    private static final long serialVersionUID = 1L;
    private Connection connection = null;

    @Override
    public void init() throws ServletException {
        ServletContext context = getServletContext();
        connection = ConnectionHandler.openConnection(context);
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        doPost(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        PlaylistDAO playlistDAO = new PlaylistDAO(connection);
        User user = (User) req.getSession().getAttribute("user");

        String playlistTitle = req.getParameter("playlistTitle");
        Playlist playlist = new Playlist(0, playlistTitle, null, user);

        String[] selectedCreationTracksStringIds = req.getParameterValues("selectedTracks");
        List<Integer> selectedCreationTracksIds = new ArrayList<>();
        if (selectedCreationTracksStringIds != null) {
            for (String id : selectedCreationTracksStringIds) {
                try {
                    selectedCreationTracksIds.add(Integer.parseInt(id));
                } catch (NumberFormatException ex) {
                    resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "TrackId non valido");
                    return;
                }
            }
        }

        if (playlistTitle == null || playlistTitle.isEmpty()) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Titolo playlist non valido");
            return;
        }
        try {
            Integer playlistId = playlistDAO.createPlaylist(playlist);
            int added = 0;
            if (!selectedCreationTracksIds.isEmpty()) {
                playlistDAO.addTracksToPlaylist(selectedCreationTracksIds, playlistId);
                added = selectedCreationTracksIds.size();
            }
            String qp = "?createdPlaylist=true&plTitle=" + java.net.URLEncoder.encode(playlistTitle, java.nio.charset.StandardCharsets.UTF_8) + (added>0?"&plAdded="+added:"");
            resp.sendRedirect(getServletContext().getContextPath() + "/HomePage" + qp + "#create-playlist");

        } catch (SQLIntegrityConstraintViolationException e) {
            resp.sendRedirect(getServletContext().getContextPath() + "/HomePage?duplicatePlaylist=true#create-playlist");

        } catch (SQLException e) {
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error while creating playlist");
            e.printStackTrace();

        }
    }

    @Override
    public void destroy() {
        ConnectionHandler.closeConnection(connection);
    }
}
