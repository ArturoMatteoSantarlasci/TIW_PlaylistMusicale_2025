package it.polimi.ria.Controller;
import it.polimi.ria.DAO.PlaylistDAO;
import it.polimi.ria.utils.ConnectionHandler;
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
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.io.Serial;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

@WebServlet("/HomePage")
@MultipartConfig
public class HomepageController extends HttpServlet {
    @Serial
    private static final long serialVersionUID = 1L;
    private Connection connection = null;

    @Override
    public void init() throws ServletException {
        ServletContext context = getServletContext();
        connection = ConnectionHandler.openConnection(context);
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res) throws IOException {
        HttpSession s = req.getSession();
        User user = (User) s.getAttribute("user");

        if (user == null) {
            res.sendError(HttpServletResponse.SC_UNAUTHORIZED, "User not logged in");
            return;
        }

        PlaylistDAO playlistDAO = new PlaylistDAO(connection);

        List<Playlist> playlists= new ArrayList<Playlist>();
        try {
            playlists = playlistDAO.getUserPlaylists(user);
        } catch (SQLException e) {
            res.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            e.printStackTrace();
            return;
        }

        Gson gson = new GsonBuilder().setDateFormat("yyyy-MM-dd").create();

        String playlists_json = gson.toJson(playlists);

        res.setContentType("application/json");
        res.setStatus(HttpServletResponse.SC_OK);
        res.getWriter().write(playlists_json);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        doGet(req, res);
    }

    @Override
    public void destroy() {
        ConnectionHandler.closeConnection(connection);
    }
}