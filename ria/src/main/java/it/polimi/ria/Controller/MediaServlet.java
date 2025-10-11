package it.polimi.ria.Controller;

import it.polimi.ria.Config.MediaConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.*;

@WebServlet(urlPatterns = "/media/*")
public class MediaServlet extends HttpServlet {

    @Override protected void doHead(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        serve(req, resp, true);
    }

    @Override protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        serve(req, resp, false);
    }

    private void serve(HttpServletRequest req, HttpServletResponse resp, boolean headOnly) throws IOException {
        Path base = MediaConfig.baseDir(getServletContext());
        String rel = req.getPathInfo();                       // es: /audio/xxx.mp3
        if (rel == null || rel.contains("..")) { resp.sendError(400); return; }

        Path file = base.resolve(rel.substring(1)).normalize();
        if (!file.startsWith(base) || !Files.exists(file) || Files.isDirectory(file)) { resp.sendError(404); return; }

        String mime = getServletContext().getMimeType(file.getFileName().toString());
        if (mime == null) mime = "application/octet-stream";
        resp.setContentType(mime);
        resp.setHeader("Cache-Control", "public, max-age=86400");
        resp.setHeader("Accept-Ranges", "bytes");

        long length = Files.size(file);
        String range = req.getHeader("Range");
        long start = 0, end = length - 1;
        boolean partial = false;

        if (range != null && range.startsWith("bytes=")) {
            partial = true;
            // support single range: bytes=start-end | start- | -suffix
            String[] p = range.substring(6).split(",", 2)[0].split("-", 2);
            try {
                if (!p[0].isEmpty()) start = Long.parseLong(p[0]);
                if (!p[1].isEmpty()) end   = Long.parseLong(p[1]);
            } catch (NumberFormatException ignore) {}
            if (!p[0].isEmpty() && p[1].isEmpty()) { end = length - 1; }             // start-
            if (p[0].isEmpty() && !p[1].isEmpty()) {                                  // -suffix
                long suffix = Long.parseLong(p[1]);
                start = Math.max(0, length - suffix);
                end   = length - 1;
            }

            if (start < 0) start = 0;
            if (end >= length) end = length - 1;
            if (start > end) { resp.setStatus(HttpServletResponse.SC_REQUESTED_RANGE_NOT_SATISFIABLE);
                resp.setHeader("Content-Range", "bytes */" + length); return; }

            long contentLen = end - start + 1;
            resp.setStatus(HttpServletResponse.SC_PARTIAL_CONTENT);
            resp.setHeader("Content-Range", "bytes " + start + "-" + end + "/" + length);
            resp.setHeader("Content-Length", String.valueOf(contentLen));
        } else {
            resp.setHeader("Content-Length", String.valueOf(length));
        }

        if (headOnly) return; // HEAD: only headers

        try (SeekableByteChannel ch = Files.newByteChannel(file, StandardOpenOption.READ)) {
            ch.position(start);
            var out = resp.getOutputStream();
            byte[] buffer = new byte[8192];
            long toWrite = partial ? (end - start + 1) : length;
            int read;
            while (toWrite > 0 && (read = ch.read(java.nio.ByteBuffer.wrap(buffer, 0, (int)Math.min(buffer.length, toWrite)))) != -1) {
                out.write(buffer, 0, read);
                toWrite -= read;
            }
        }
    }
}