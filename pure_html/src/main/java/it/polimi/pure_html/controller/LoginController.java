package it.polimi.pure_html.controller;

import it.polimi.pure_html.DAO.UserDAO;
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

/**
 * Gestisce l'autenticazione degli utenti e l'erogazione della pagina di login.
 * GET: visualizza il form di login ed eventuali messaggi di errore.
 * POST: valida le credenziali e crea la sessione applicativa.
 */
@WebServlet("/Login")
public class LoginController extends HttpServlet {
    @Serial
    private static final long serialVersionUID = 1L;
    private TemplateEngine templateThymeleaf;
    private Connection connection = null;

    @Override
    public void init() throws ServletException {
        ServletContext context = getServletContext();
        connection = ConnectionHandler.openConnection(context);
        templateThymeleaf = TemplateThymeleaf.getTemplateEngine(context);
    }

    @Override
    public void doGet(HttpServletRequest req, HttpServletResponse res) throws IOException {
        JakartaServletWebApplication webApplication = JakartaServletWebApplication.buildApplication(getServletContext());
        WebContext context = new WebContext(webApplication.buildExchange(req, res), req.getLocale());
        String error = req.getParameter("error");
        if (error != null) {
            context.setVariable("error", error);
        }

        templateThymeleaf.process("login.html", context, res.getWriter());
    }

    @Override
    public void doPost(HttpServletRequest req, HttpServletResponse res) throws IOException {
        String nickname = req.getParameter("nickname");
        String password = req.getParameter("password");

        UserDAO userDAO = new UserDAO(connection);
        User userToCheck = null;
        try {
            userToCheck = userDAO.checkUser(nickname, password);
        } catch (SQLException e) {
            res.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Database errore");
            e.printStackTrace();
            return;
        }

        req.getSession().setAttribute("user", userToCheck);

        if (userToCheck != null) {
            res.sendRedirect(getServletContext().getContextPath() + "/HomePage");
        } else {
            res.sendRedirect(getServletContext().getContextPath() + "/Login?error=Credenziali%20non%20valide");
        }
    }

    @Override
    public void destroy() {
        ConnectionHandler.closeConnection(connection);
    }
}
