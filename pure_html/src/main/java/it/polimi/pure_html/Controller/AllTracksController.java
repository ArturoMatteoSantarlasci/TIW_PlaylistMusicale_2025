package it.polimi.pure_html.controller;

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
import java.util.List;

@WebServlet("/All_Tracks")
public class AllTracksController extends HttpServlet {
    @Serial
    private static final long serialVersionUID = 1L;

    private TemplateEngine templateEngine;
    private Connection connection;
    private TrackDAO trackDAO;

    @Override
    public void init() throws ServletException {
        ServletContext context = getServletContext();
        connection = ConnectionHandler.openConnection(context);
        trackDAO = new TrackDAO(connection);
        templateEngine = TemplateThymeleaf.getTemplateEngine(context);
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res) throws IOException {
        User user = (User) req.getSession().getAttribute("user");
        if (user == null) {
            res.sendRedirect(getServletContext().getContextPath() + "/Login");
            return;
        }

        List<Track> tracks;
        try {
            tracks = trackDAO.getUserTracks(user);
        } catch (SQLException e) {
            res.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Database errore");
            e.printStackTrace();
            return;
        }

        JakartaServletWebApplication webApplication = JakartaServletWebApplication.buildApplication(getServletContext());
        WebContext context = new WebContext(webApplication.buildExchange(req, res), req.getLocale());
        context.setVariable("tracks", tracks);

        templateEngine.process("All_Tracks.html", context, res.getWriter());
    }

    @Override
    public void destroy() {
        ConnectionHandler.closeConnection(connection);
    }
}
