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
import java.sql.SQLException;

@MultipartConfig
@WebServlet("/Login") // nome servlet per accesso via js
public class LoginController extends HttpServlet {
    @Serial
    private static final long serialVersionUID = 1L;
    private Connection connection = null;

    @Override
    public void init() throws ServletException {
        ServletContext context = getServletContext();//contesto webapp
        connection = ConnectionHandler.openConnection(context);
    }

    @Override
    public void doGet(HttpServletRequest req, HttpServletResponse res) throws IOException {
        String loginPage= req.getServletContext().getContextPath() + "/login.html";
        res.sendRedirect(loginPage);
    }

    @Override
    public void doPost(HttpServletRequest req, HttpServletResponse res) throws IOException {
        String nickname = req.getParameter("nickname");
        String password = req.getParameter("password");

        UserDAO userDAO = new UserDAO(connection);
        User UserToCheck = null;
        try {//chiama la dao che ritorna o l'user o null
            UserToCheck = userDAO.checkUser(nickname, password);
        } catch (SQLException e) {//mostra errore server
            res.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Database errore");
            e.printStackTrace();
            return;
        }
//logica di controllo, non va nel try catch(che controlla l'operazione)
        if (UserToCheck != null) {
            req.getSession().setAttribute("user", UserToCheck);//per non fare login ogni volta, setta user nella sessione http
            res.setStatus(HttpServletResponse.SC_OK);
        } else {
            res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            res.setContentType("text/plain");
            res.getWriter().println("Credenziali non valide");
        }
    }

    @Override
    public void destroy() {
        ConnectionHandler.closeConnection(connection);
    }
}