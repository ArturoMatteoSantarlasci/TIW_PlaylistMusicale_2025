package it.polimi.ria.filters;


import it.polimi.ria.DAO.PlaylistDAO;
import it.polimi.ria.entities.User;
import it.polimi.ria.utils.ConnectionHandler;
import jakarta.servlet.*;
import jakarta.servlet.http.*;

import java.io.IOException;
import java.io.Serial;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * Filter to check if the requested playlist belongs to the currently logged-in User.
 */
public class PlaylistChecker extends HttpFilter {
    @Serial
    private static final long serialVersionUID = 1L;
    private Connection connection = null;

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        ServletContext context = filterConfig.getServletContext();
        connection = ConnectionHandler.openConnection(context);
    }

    @Override
    public void doFilter(HttpServletRequest req, HttpServletResponse res, FilterChain filterChain) throws IOException, ServletException {
        HttpSession s = req.getSession();
        User user = (User) s.getAttribute("user");
        PlaylistDAO playlistDAO = new PlaylistDAO(connection);
        int playlistId;

        boolean result;
        try {
            playlistId = Integer.parseInt(req.getParameter("playlistId"));
            if (playlistId < 0)
                throw new NumberFormatException();
            result = playlistDAO.checkPlaylistOwner(playlistId, user);
            if(!result) {
                res.sendError(HttpServletResponse.SC_BAD_REQUEST, "Playlist non appartiene all'utente");
            return;
            }
            filterChain.doFilter(req, res);

        } catch (SQLException e) {
            e.printStackTrace();
            res.sendError(HttpServletResponse.SC_BAD_REQUEST);
        } catch (NumberFormatException e) {
            res.sendError(HttpServletResponse.SC_BAD_REQUEST, "PlaylistId non valido");
        }
    }

    @Override
    public void destroy() {
        ConnectionHandler.closeConnection(connection);
    }
}
