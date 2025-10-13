package it.polimi.ria.Controller;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import it.polimi.ria.utils.ConnectionHandler;
import it.polimi.ria.DAO.PlaylistDAO;
import it.polimi.ria.entities.Track;
import it.polimi.ria.entities.User;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Restituisce gruppi (pagine) di 5 tracce di una playlist.
 * Parametri: playlistId (int), group (int, 0-based)
 */
@WebServlet("/PlaylistGroup")
public class PlaylistGroupController extends HttpServlet {
    @Serial
    private static final long serialVersionUID = 1L;
    private Connection connection;
    private static final int PAGE_SIZE = 5;

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

        String pidStr = req.getParameter("playlistId");
        String groupStr = req.getParameter("group");
        if (pidStr == null || groupStr == null) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing parameters");
            return;
        }
        int playlistId;
        int groupIndex;
        try {
            playlistId = Integer.parseInt(pidStr);
            groupIndex = Integer.parseInt(groupStr);
        } catch (NumberFormatException e) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid parameters");
            return;
        }
        if (groupIndex < 0) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Group must be >= 0");
            return;
        }

        PlaylistDAO dao = new PlaylistDAO(connection);
        try {
            // Ownership opzionale: se richiesto decommentare check
            // if (!dao.checkPlaylistOwner(playlistId, user)) { resp.sendError(HttpServletResponse.SC_FORBIDDEN); return; }
            int totalTracks = dao.countPlaylistTracks(playlistId);
            int totalGroups = (totalTracks + PAGE_SIZE - 1) / PAGE_SIZE; // 0 se empty
            if (totalGroups == 0) {
                // playlist vuota
                Map<String,Object> payload = new HashMap<>();
                payload.put("playlistId", playlistId);
                payload.put("group", 0);
                payload.put("pageSize", PAGE_SIZE);
                payload.put("tracks", List.of());
                payload.put("totalTracks", 0);
                payload.put("totalGroups", 0);
                payload.put("isFirst", true);
                payload.put("isLast", true);
                writeJson(resp, payload);
                return;
            }
            if (groupIndex >= totalGroups) {
                groupIndex = totalGroups - 1; // clamp alla ultima pagina
            }
            List<Track> tracks = dao.getTrackGroup(playlistId, groupIndex);

            Map<String,Object> payload = new HashMap<>();
            payload.put("playlistId", playlistId);
            payload.put("group", groupIndex);
            payload.put("pageSize", PAGE_SIZE);
            payload.put("tracks", tracks);
            payload.put("totalTracks", totalTracks);
            payload.put("totalGroups", totalGroups);
            payload.put("isFirst", groupIndex == 0);
            payload.put("isLast", groupIndex == totalGroups - 1);
            writeJson(resp, payload);
        } catch (SQLException e) {
            e.printStackTrace();
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "DB error");
        }
    }

    private void writeJson(HttpServletResponse resp, Object payload) throws IOException {
        Gson gson = new GsonBuilder().setDateFormat("yyyy-MM-dd").create();
        String json = gson.toJson(payload);
        resp.setContentType("application/json");
        resp.setStatus(HttpServletResponse.SC_OK);
        resp.getWriter().write(json);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException { doGet(req, resp); }

    @Override
    public void destroy() {
        ConnectionHandler.closeConnection(connection);
    }
}
