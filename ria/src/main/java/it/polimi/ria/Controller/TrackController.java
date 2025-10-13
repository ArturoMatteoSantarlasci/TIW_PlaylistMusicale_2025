package it.polimi.ria.Controller;

import com.google.gson.Gson;
import it.polimi.ria.DAO.TrackDAO;
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

@WebServlet("/Track")
@MultipartConfig
public class TrackController extends HttpServlet {
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
        int trackId = Integer.parseInt(req.getParameter("track_id"));

        TrackDAO trackDAO = new TrackDAO(connection);
        Track track = null;
        try {
            track = trackDAO.getTrackById(trackId);
        } catch (SQLException e) {
            e.printStackTrace();
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            return;
        }

        Gson gson = new Gson();

        String track_json = gson.toJson(track);

        resp.setContentType("application/json");
        resp.setStatus(HttpServletResponse.SC_OK);
        resp.getWriter().write(track_json);
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