package it.polimi.pure_html.Controller;

import it.polimi.pure_html.entities.User;
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

@WebServlet("/HomePage")
public class HomepageController extends HttpServlet {
    @Serial
    private static final long serialVersionUID = 1L;
    private TemplateEngine templateEngine;

    @Override
    public void init() throws ServletException {
        ServletContext context = getServletContext();
        templateEngine = TemplateThymeleaf.getTemplateEngine(context);
    }

    @Override
    public void doGet(HttpServletRequest req, HttpServletResponse res) throws IOException {
        // Verifica se l'utente è loggato
        User user = (User) req.getSession().getAttribute("user");
        if (user == null) {
            // Se non è loggato, redirect al login
            res.sendRedirect(getServletContext().getContextPath() + "/Login");
            return;
        }

        // Se è loggato, mostra la homepage
        JakartaServletWebApplication webApplication = JakartaServletWebApplication.buildApplication(getServletContext());
        WebContext context = new WebContext(webApplication.buildExchange(req, res), req.getLocale());

        // Aggiungi l'utente al contesto Thymeleaf se necessario
        context.setVariable("user", user);

        templateEngine.process("HomePage.html", context, res.getWriter());
    }

    @Override
    public void doPost(HttpServletRequest req, HttpServletResponse res) throws IOException {
        doGet(req, res);
    }
}
