package it.polimi.ria.filters;


import it.polimi.ria.DAO.TrackDAO;
import it.polimi.ria.entities.User;
import it.polimi.ria.ConnectionHandler;
import jakarta.servlet.*;
import jakarta.servlet.http.*;

import java.io.IOException;
import java.io.Serial;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * Controllo se i track selezionati sono di un utente loggato quando  li aggiunge a una playlist
 */
public class SelectedTracksChecker extends HttpFilter {
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
        String[] selectedTracksIds = req.getParameterValues("selectedTracks");
        TrackDAO trackDAO = new TrackDAO(connection);

        if (selectedTracksIds != null) {
            for (String trackId : selectedTracksIds) {
                boolean isOwner;
                int TrackIntId=0;
                try {
                    TrackIntId=Integer.parseInt(trackId);
                    isOwner = trackDAO.checkTrackOwner(TrackIntId, user);
                } catch (SQLException e) {
                    e.printStackTrace();
                    res.sendError(HttpServletResponse.SC_BAD_REQUEST);
                    return;
                } catch (NumberFormatException e) {
                    res.sendError(HttpServletResponse.SC_BAD_REQUEST, "TrackId non valido");
                    return;
                }

                if (!isOwner) {
                    res.sendError(HttpServletResponse.SC_BAD_REQUEST, "Track non appartiene all'utente");
                    return;
                }
            }
        }
        filterChain.doFilter(req, res);
    }

    @Override
    public void destroy() {
        ConnectionHandler.closeConnection(connection);
    }
}
