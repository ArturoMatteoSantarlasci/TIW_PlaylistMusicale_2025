package it.polimi.ria.Controller;
import it.polimi.ria.DAO.PlaylistDAO;
import it.polimi.ria.ConnectionHandler;
import it.polimi.ria.entities.Playlist;
import it.polimi.ria.entities.User;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
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

@WebServlet("/CreatePlaylist")
@MultipartConfig
public class CreatePlaylist extends HttpServlet {
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
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        PlaylistDAO playlistDAO = new PlaylistDAO(connection);
        user = (User) req.getSession().getAttribute("user");

        String playlistTitle = req.getParameter("playlistTitle");
        Playlist playlist_DBplaceholder = new Playlist(0, playlistTitle, null, user);

        String[] clientTrackIds_string = req.getParameterValues("selectedTrackIds");
        List<Integer> clientTracksIds = new ArrayList<>();
        if (clientTrackIds_string != null) {
            for (String id : clientTrackIds_string) {
                clientTracksIds.add(Integer.parseInt(id));
            }
        }

        if (playlistTitle == null || playlistTitle.isEmpty()) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Playlist title non valido");
            return;
        }
        try {
            Integer playlistId = null;
            playlistId = playlistDAO.createPlaylist(playlist_DBplaceholder);
            if (!clientTracksIds.isEmpty()) {
                playlistDAO.addTracksToPlaylist(clientTracksIds, playlistId);
            }
            Gson gson_converter = new GsonBuilder().setDateFormat("yyyy-MM-dd").create();
            String json = gson_converter.toJson(new Playlist(playlistId, playlist_DBplaceholder.title(), null, null));//user null in risposta
            resp.setContentType("application/json");
            resp.getWriter().write(json);
            resp.setStatus(HttpServletResponse.SC_CREATED);
        } catch (SQLIntegrityConstraintViolationException e) {//errori client
            resp.setStatus(HttpServletResponse.SC_CONFLICT);
            resp.setContentType("text/plain");
            resp.getWriter().write("Errore: duplicato playlist");
        } catch (SQLException e) {
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Errore nel creare playlist");//errori server
            e.printStackTrace();
        }
    }

    @Override
    public void destroy() {
        ConnectionHandler.closeConnection(connection);
    }
}