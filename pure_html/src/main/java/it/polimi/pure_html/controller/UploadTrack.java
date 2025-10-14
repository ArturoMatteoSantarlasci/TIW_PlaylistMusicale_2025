package it.polimi.pure_html.controller;

import it.polimi.pure_html.Config.MediaConfig;
import it.polimi.pure_html.DAO.TrackDAO;
import it.polimi.pure_html.entities.Track;
import it.polimi.pure_html.entities.User;
import it.polimi.pure_html.utils.ConnectionHandler;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.Part;
import jakarta.ws.rs.ClientErrorException;
import jakarta.ws.rs.ServerErrorException;
import jakarta.ws.rs.core.Response;

import java.io.File;
import java.io.IOException;
import java.io.Serial;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.DigestInputStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.time.Year;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Gestisce il caricamento di nuove tracce audio e delle relative copertine.
 * Verifica la validità dei metadati, deduplica i file tramite checksum
 * e salva fisicamente le risorse nelle directory configurate.
 */

@WebServlet("/UploadTrack")
@MultipartConfig(
    fileSizeThreshold = 1024 * 1024,      // 1MB buffer
    maxFileSize = 1024 * 1024 * 25,       // 25MB per singolo file (audio + immagine)
    maxRequestSize = 1024 * 1024 * 30     // 30MB totale request
)
public class UploadTrack extends HttpServlet {
    @Serial
    private static final long serialVersionUID = 1L;
    private Connection connection = null;
    private ServletContext context;
    private List<String> genres;

    /**
     * Inizializza le risorse condivise necessarie all'upload delle tracce multimediali.
     */

    public void init() throws ServletException {
        context = getServletContext();
        connection = ConnectionHandler.openConnection(context);

        genres = List.of(
                "Classical","Rock","Edm","Pop","Hip-hop","R&B","Country","Jazz","Blues",
                "Metal","Folk","Soul","Funk","Electronic","Indie","Reggae","Disco"
        );
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        doPost(req, resp);
    }

    private String getRequiredParameter(HttpServletRequest req, String paramName) throws ClientErrorException {
        String paramValue = req.getParameter(paramName);
        if (paramValue == null || paramValue.isEmpty()) {
            throw new ClientErrorException("Manca " + paramName, Response.Status.BAD_REQUEST);
        }
        return paramValue;
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        var session = req.getSession(false);
        User user = session == null ? null : (User) session.getAttribute("user");
        if (user == null) {
            resp.sendRedirect(getServletContext().getContextPath() + "/Login");
            return;
        }

        List<File> newFiles = new ArrayList<>();
        Track track;

        try {
            String title = getRequiredParameter(req, "title").trim();
            String artist = getRequiredParameter(req, "artist").trim();

            int year;
            try {
                year = Integer.parseInt(req.getParameter("year"));
            } catch (NumberFormatException e) {
                resp.sendRedirect(getServletContext().getContextPath()+"/HomePage?uploadError=Anno%20non%20valido#upload-track");
                return;
            }
            if (year < 1901 || year > Year.now().getValue()) {
                resp.sendRedirect(getServletContext().getContextPath()+"/HomePage?uploadError=Anno%20non%20valido#upload-track");
                return;
            }

            String album = getRequiredParameter(req, "album").trim();

            String genreRaw = getRequiredParameter(req, "genre").trim();
            // Normalizza genere (case-insensitive) confrontando con lista canonical
            String genre = genres.stream()
                    .filter(g -> g.equalsIgnoreCase(genreRaw))
                    .findFirst()
                    .orElse(null);
            if (genre == null) {
                resp.sendRedirect(getServletContext().getContextPath()+"/HomePage?uploadError=Genere%20non%20valido#upload-track");
                return;
            }

            FileDetails imageDetails = processPart(req.getPart("image"), "image", newFiles);
            String imagePath = imageDetails.path();
            String imageHash = imageDetails.hash();

            FileDetails audioDetails = processPart(req.getPart("musicTrack"), "audio", newFiles);
            String songPath = audioDetails.path();
            String songHash = audioDetails.hash();

            track = new Track(0, user.id(), title, artist, year, album, genre, imagePath, songPath, songHash, imageHash);
        } catch (ClientErrorException e) {
            resp.sendRedirect(getServletContext().getContextPath()+"/HomePage?uploadError=" + java.net.URLEncoder.encode(e.getMessage(), java.nio.charset.StandardCharsets.UTF_8) + "#upload-track");
            return;
        } catch (ServerErrorException e) {
            resp.sendError(e.getResponse().getStatus(), e.getMessage());
            return;
        } catch (SQLException e) {
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            return;
        } catch (IllegalStateException tooBig) {
            resp.sendRedirect(getServletContext().getContextPath()+"/HomePage?uploadError=File%20troppo%20grande#upload-track");
            return;
        }

        TrackDAO trackDAO = new TrackDAO(connection);
        try {
            trackDAO.addTrack(track, user);
            String qp = "?uploadedTrack=true&trkTitle=" + java.net.URLEncoder.encode(track.title(), java.nio.charset.StandardCharsets.UTF_8) + "#upload-track";
            resp.sendRedirect(getServletContext().getContextPath() + "/HomePage" + qp);
        } catch (SQLIntegrityConstraintViolationException e) {
            String msg = e.getMessage();
            if (msg != null && msg.toLowerCase().contains("duplicate")) {
                resp.sendRedirect(getServletContext().getContextPath() + "/HomePage?duplicateTrack=true#upload-track");
            } else {
                resp.sendRedirect(getServletContext().getContextPath() + "/HomePage?uploadError=Vincolo%20violato#upload-track");
            }
            newFiles.forEach(File::delete);
        } catch (SQLException e) {
            resp.sendRedirect(getServletContext().getContextPath() + "/HomePage?uploadError=Errore%20server#upload-track");
            newFiles.forEach(File::delete);
            e.printStackTrace();
        } finally {
            newFiles.clear();
        }
    }

    private FileDetails processPart(Part part, String mimeType, List<File> newFiles) throws IOException, SQLException {
        if (part == null || part.getSize() <= 0) {
            throw new ClientErrorException("Manca " + mimeType, Response.Status.BAD_REQUEST);
        }
        if (!part.getContentType().startsWith(mimeType)) {
            throw new ClientErrorException("File non permesso per " + mimeType + " tipo", HttpServletResponse.SC_BAD_REQUEST);
        }

        Path tempFile = Files.createTempFile("upload_", ".bin");
        java.security.MessageDigest digest;
        try { digest = java.security.MessageDigest.getInstance("SHA-256"); }
        catch (java.security.NoSuchAlgorithmException e) { throw new ServerErrorException(HttpServletResponse.SC_INTERNAL_SERVER_ERROR); }

        try (var in = part.getInputStream();
             var dis = new java.security.DigestInputStream(in, digest);
             var out = Files.newOutputStream(tempFile)) {
            dis.transferTo(out);
        }
        String hash = java.util.HexFormat.of().formatHex(digest.digest());

        TrackDAO trackDAO = new TrackDAO(connection);
        String pathFileRelative = switch (mimeType) {
            case "audio" -> trackDAO.isTrackFileAlreadyPresent(hash);
            case "image" -> trackDAO.isImageFileAlreadyPresent(hash);
            default -> null;
        };
        if (pathFileRelative != null) {
            Files.deleteIfExists(tempFile);
            return new FileDetails(pathFileRelative, hash);
        }

        var mediaBase = MediaConfig.baseDir(context);
        final String folder = switch (mimeType) {
            case "audio" -> "audio";
            case "image" -> "img";
            default -> "misc";
        };
        var targetDir = mediaBase.resolve(folder);
        Files.createDirectories(targetDir);

        String realname = Paths.get(part.getSubmittedFileName()).getFileName().toString();
        String ext = "";
        int dot = realname.lastIndexOf('.');
        if (dot > -1 && dot < realname.length() - 1) ext = realname.substring(dot);
        String fileName = UUID.randomUUID() + ext;

        Path absolutePath = targetDir.resolve(fileName);
        Files.move(tempFile, absolutePath, StandardCopyOption.REPLACE_EXISTING);
        newFiles.add(absolutePath.toFile());

        pathFileRelative = "/media/" + folder + "/" + fileName;
        System.out.println("[UploadTrack] Saved " + absolutePath + " -> " + pathFileRelative);

        return new FileDetails(pathFileRelative, hash);
    }

    @Override
    public void destroy() {
        ConnectionHandler.closeConnection(connection);
    }

    record FileDetails(String path, String hash) {
    }
}
