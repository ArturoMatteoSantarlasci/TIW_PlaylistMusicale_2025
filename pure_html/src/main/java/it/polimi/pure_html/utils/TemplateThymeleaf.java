package it.polimi.pure_html.utils;

import jakarta.servlet.ServletContext;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.WebApplicationTemplateResolver;
import org.thymeleaf.web.servlet.JakartaServletWebApplication;

/**
 * Thymeleaf è un template engine per Java: prende un file HTML e lo arricchisce con i dati
 *  passati dal server
 * Fa da ponte tra Servlet/Controller (Java) e View (HTML)
 *
 * FUNZIONAMENTO:
 *  1. utente fra una richiesta HTTP (es. login)
 *  2. Servlet raccoglie i dati dal database (DAO + CoonnectionHandler)
 *  3. Servlet crea un context e passa i dati a Thymeleaf (TemplateEngineHandler)
 *  4. Thymeleaf prende ad es. HomePage.html e genera il file html finale
 */

public class TemplateThymeleaf {

    public static TemplateEngine getTemplateEngine(ServletContext context) {
        TemplateEngine templateEngine = new TemplateEngine();

        //Traduce il ServletContext di Tomcat in un oggetto che Thymeleaf capisce.
        JakartaServletWebApplication webApplication = JakartaServletWebApplication.buildApplication(context);

        //Il resolver dice a Thymeleaf dove e come cercare i template.
        WebApplicationTemplateResolver templateResolver = new WebApplicationTemplateResolver(webApplication);

        templateResolver.setTemplateMode(TemplateMode.HTML);
        templateEngine.setTemplateResolver(templateResolver);
        templateResolver.setSuffix(".html");
        return templateEngine;
    }

}

