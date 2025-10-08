package it.polimi.pure_html.controller;

import it.polimi.pure_html.Config.MediaConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.nio.file.*;

@WebServlet(urlPatterns = "/media/*")
public class MediaServlet extends HttpServlet {
    @Override protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        var base = MediaConfig.baseDir(getServletContext());
        var rel = req.getPathInfo(); // es: /img/xxx.jpg
        if (rel == null || rel.contains("..")) { resp.sendError(400); return; }
        var file = base.resolve(rel.substring(1)).normalize();
        if (!file.startsWith(base) || !Files.exists(file) || Files.isDirectory(file)) { resp.sendError(404); return; }
        var mime = getServletContext().getMimeType(file.getFileName().toString());
        if (mime == null) mime = "application/octet-stream";
        resp.setContentType(mime);
        resp.setHeader("Cache-Control", "public, max-age=86400");
        try (var in = Files.newInputStream(file); var out = resp.getOutputStream()) { in.transferTo(out); }
    }
}
