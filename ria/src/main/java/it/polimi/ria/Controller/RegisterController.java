package it.polimi.ria.Controller;

import it.polimi.ria.DAO.UserDAO;
import it.polimi.ria.entities.User;
import it.polimi.ria.ConnectionHandler;
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

@MultipartConfig
@WebServlet("/Register")//no redirect, chiamata ajax
public class RegisterController extends HttpServlet {
    @Serial
    private static final long serialVersionUID = 1L;
    private Connection connection = null;

    @Override
    public void init() throws ServletException {
        ServletContext context = getServletContext();
        connection = ConnectionHandler.openConnection(context);
    }

    @Override
    public void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        doPost(req, res);
    }

    @Override
    public void doPost(HttpServletRequest req, HttpServletResponse res) throws IOException {
        String nickname = req.getParameter("nickname");
        String password = req.getParameter("password");
        String name = req.getParameter("name");
        String surname = req.getParameter("surname");
//logica controllo
        if (nickname == null || nickname.isEmpty() || password == null || password.isEmpty() || name == null || name.isEmpty() || surname == null || surname.isEmpty()) {
            res.sendError(HttpServletResponse.SC_BAD_REQUEST, "parametri invalidi");
            return;
        }

        UserDAO userDAO = new UserDAO(connection);
        User user = new User(
                0,//placeholder, poi db lo settaa auto-increment
                nickname,
                password,
                name,
                surname
        );
//dao ritorna true o false
        boolean isUserAdded = userDAO.addUser(user);

        if (isUserAdded) {
            res.setStatus(HttpServletResponse.SC_CREATED);
        } else {
            res.setStatus(HttpServletResponse.SC_CONFLICT);
            res.setContentType("text/plain");
            res.getWriter().println("Nickname già in uso");
        }

    }

    @Override
    public void destroy() {
        ConnectionHandler.closeConnection(connection);
    }
}
