package it.polimi.pure_html.controller;

import it.polimi.pure_html.DAO.PlaylistDAO;
import it.polimi.pure_html.entities.User;
import it.polimi.pure_html.utils.ConnectionHandler;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
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
//aggiunge tracce a playlist esistente
@WebServlet("/AddTracks")
@MultipartConfig
public class AddTracksToPlaylist extends HttpServlet {
    @Serial
    private static final long serialVersionUID = 1L;
    private Connection connection = null;
    User user;

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
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        PlaylistDAO playlistDAO = new PlaylistDAO(connection);
        user = (User) req.getSession().getAttribute("user");//ritorna object,serve cast
        String[] clientTrackIds_string = req.getParameterValues("selectedTracksIds");
        List<Integer> clientTracksIds = new ArrayList<>();
        if (clientTrackIds_string != null) {
            for (String id : clientTrackIds_string) {
                clientTracksIds.add(Integer.parseInt(id));
            }
        }

        int playlistId = Integer.parseInt(req.getParameter("playlistId"));

        try {
            playlistDAO.addTracksToPlaylist(clientTracksIds, playlistId);
            int added = clientTracksIds.size();
        resp.sendRedirect(
            getServletContext().getContextPath() + 
                "/Playlist?playlistId=" + playlistId + "&gr=0" + (added>0?"&added="+added:"")
        );

        } catch (SQLIntegrityConstraintViolationException e) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "una o più tracce già presenti nella playlist");

        } catch (SQLException e) {
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            e.printStackTrace();

        }

    }

    @Override
    public void destroy() {
        ConnectionHandler.closeConnection(connection);
    }
}
