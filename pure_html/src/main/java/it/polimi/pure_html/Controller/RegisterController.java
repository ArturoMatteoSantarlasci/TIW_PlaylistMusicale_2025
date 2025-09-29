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
//prima get poi register.html che chiama post , o login se ok o redirect a get e mostra errore con thymeleaf
@WebServlet("/Register")
public class RegisterController extends HttpServlet {
    @Serial
    private static final long serialVersionUID = 1L;
    private TemplateEngine templateEngine;
    private Connection connection = null;

    @Override
    public void init() throws ServletException {
        ServletContext context = getServletContext();
        connection = ConnectionHandler.openConnection(context);
        templateEngine = TemplateThymeleaf.getTemplateEngine(context);
    }

    @Override
    public void doGet(HttpServletRequest req, HttpServletResponse res) throws IOException {
        JakartaServletWebApplication webApplication = JakartaServletWebApplication.buildApplication(getServletContext());
        WebContext context = new WebContext(webApplication.buildExchange(req, res), req.getLocale());

        String param = req.getParameter("isUserAdded");
        boolean isUserAdded;
        if (param == null) {
            isUserAdded = true;    // Prima volta
        } else {
            isUserAdded = false;   // Deve essere "false" (errore)
        }
        context.setVariable("isUserAdded", isUserAdded);
        templateEngine.process("register.html", context, res.getWriter());
    }

    @Override
    public void doPost(HttpServletRequest req, HttpServletResponse res) throws IOException {
        String nickname = req.getParameter("nickname");
        String password = req.getParameter("password");
        String name = req.getParameter("name");
        String surname = req.getParameter("surname");

        if (nickname == null || nickname.isEmpty() || password == null || password.isEmpty() || name == null || name.isEmpty() || surname == null || surname.isEmpty()) {
            res.sendError(HttpServletResponse.SC_BAD_REQUEST, "parametri invalidi");
            return;
        }

        UserDAO userDAO = new UserDAO(connection);
        User user = new User(
                0,
                nickname,
                password,
                name,
                surname
        );

        boolean isUserAdded = userDAO.addUser(user);

        if (isUserAdded) {
            res.sendRedirect(getServletContext().getContextPath() + "/Login");
        } else {
            res.sendRedirect(
                    getServletContext().getContextPath() + "/Register?isUserAdded=" + false
            );
        }

    }

    @Override
    public void destroy() {
        ConnectionHandler.closeConnection(connection);
    }
}