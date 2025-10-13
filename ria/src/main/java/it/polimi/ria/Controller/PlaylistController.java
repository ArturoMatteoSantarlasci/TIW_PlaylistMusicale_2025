package it.polimi.ria.Controller;


import com.google.gson.Gson;
import it.polimi.ria.DAO.PlaylistDAO;
import it.polimi.ria.entities.Track;
import it.polimi.ria.utils.ConnectionHandler;
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
import java.util.List;


@WebServlet("/Playlist")
@MultipartConfig
public class PlaylistController extends HttpServlet {
    @Serial
    private static final long serialVersionUID = 1L;
    private Connection connection = null;

    @Override
    public void init() throws ServletException {
        ServletContext context = getServletContext();
        connection = ConnectionHandler.openConnection(context);
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        int playlistId = Integer.parseInt(req.getParameter("playlistId"));

        PlaylistDAO playlistDAO = new PlaylistDAO(connection);
        List<Track> TracksInPlaylist;
        try {

            TracksInPlaylist = playlistDAO.getPlaylistTracksById(playlistId);
            Gson gson = new Gson();
            String playlistTracks_json = gson.toJson(TracksInPlaylist);
            resp.setContentType("application/json");
            resp.setStatus(HttpServletResponse.SC_OK);
            resp.getWriter().write(playlistTracks_json);
        } catch (SQLException e) {
            e.printStackTrace();
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
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