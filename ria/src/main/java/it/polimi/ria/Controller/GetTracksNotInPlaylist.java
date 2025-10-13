package it.polimi.ria.Controller;


import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import it.polimi.ria.DAO.PlaylistDAO;
import it.polimi.ria.entities.Track;
import it.polimi.ria.entities.User;
import it.polimi.ria.utils.ConnectionHandler;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
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

@WebServlet("/GetTracksNotInPlaylist")
public class GetTracksNotInPlaylist extends HttpServlet {
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
        HttpSession s = req.getSession();
        User user = (User) s.getAttribute("user");

        if (user == null) {
            resp.sendError(HttpServletResponse.SC_UNAUTHORIZED, "User not logged in");
            return;
        }

        String playlistTitle = req.getParameter("playlistTitle");
//logica controllo
        if (playlistTitle == null || playlistTitle.isEmpty()) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
        }

        PlaylistDAO playlistDAO = new PlaylistDAO(connection);
        List<Track> TracksNotInPlaylist = new ArrayList<>();
        try {
            TracksNotInPlaylist = playlistDAO.getTracksNotInPlaylist(playlistTitle, user.id());

            Gson gson = new GsonBuilder().setDateFormat("yyyy-MM-dd").create();

            String json = gson.toJson(TracksNotInPlaylist);
            resp.setContentType("application/json");
            resp.setStatus(HttpServletResponse.SC_OK);
            resp.getWriter().write(json);

        } catch (SQLException e) {
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            e.printStackTrace();
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
