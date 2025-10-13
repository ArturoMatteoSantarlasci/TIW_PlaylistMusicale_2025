package it.polimi.pure_html.controller;


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
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.*;
import java.time.Year;
import java.util.*;

import it.polimi.pure_html.Config.MediaConfig;
import java.nio.file.Path;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.security.NoSuchAlgorithmException;
import java.security.DigestInputStream;

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
    // Rimosso outputPath: i file vengono sempre salvati sotto /media/img
    private User user;
    private Track track;
    private List<File> newFiles;
    private ServletContext context;
    private List<String> genres; // lista fissa hardcoded

    public void init() throws ServletException {
        context = getServletContext();
        connection = ConnectionHandler.openConnection(context);
        //NOME CARTELLA DOVE SALVARE I FILE NELLA WEBAPP ES. ''uploads''
        newFiles = new ArrayList<>();

        // Lista hardcoded (niente più json)
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
        // Non creare nuova sessione se scaduta
        var session = req.getSession(false);
        if (session == null || (user = (User) session.getAttribute("user")) == null) {
            // Sessione scaduta: redirect login o 401
            resp.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Sessione scaduta");
            return;
        }

        try {
            String title = getRequiredParameter(req, "title");
            String artist = getRequiredParameter(req, "artist");

            int year;
            try {
                year = Integer.parseInt(req.getParameter("year"));
            } catch (NumberFormatException e) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Anno non valido");

                return;
            }
            if (year < 1901 || year > Year.now().getValue()) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Anno non valido");
                return;
            }

            String album = getRequiredParameter(req, "album");

            String genre = getRequiredParameter(req, "genre");
            if (!genres.contains(genre)) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Genere non valido");
                return;
            }

            FileDetails fileDetails;

            // Immagini in /media/img
            fileDetails = processPart(req.getPart("image"), "image");
            String imagePath = fileDetails.path();
            String imageHash = fileDetails.hash();

            // Audio in /media/audio
            fileDetails = processPart(req.getPart("musicTrack"), "audio");
            String songPath = fileDetails.path();
            String songHash = fileDetails.hash();

            track = new Track(0, user.id(), title, artist, year, album, genre, imagePath, songPath, songHash, imageHash);
        } catch (ClientErrorException | ServerErrorException e) {
            resp.sendError(e.getResponse().getStatus(), e.getMessage());
            return;
        } catch (SQLException e) {
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            return;
        } catch (IllegalStateException tooBig) {
            // File oltre i limiti del @MultipartConfig
            resp.sendError(HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE, "File troppo grande");
            return;
        }

        // Add track
        TrackDAO trackDAO = new TrackDAO(connection);
        try {
            trackDAO.addTrack(track, user);
            String qp = "?uploadedTrack=true&trkTitle=" + java.net.URLEncoder.encode(track.title(), java.nio.charset.StandardCharsets.UTF_8) + "#upload-track";
            resp.sendRedirect(getServletContext().getContextPath() + "/HomePage" + qp);


        } catch (SQLIntegrityConstraintViolationException e) {
            if (e.getMessage().contains("Duplicate")) {
                resp.sendRedirect(getServletContext().getContextPath() + "/HomePage?duplicateTrack=true#upload-track");

            }
            // Delete newly created files if addTrack fails
            newFiles.forEach(File::delete);
        } catch (SQLException e) {
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            newFiles.forEach(File::delete);
            e.printStackTrace();
        } finally {
            newFiles.clear();
        }
    }

    private FileDetails processPart(Part part, String mimeType) throws IOException, SQLException {
        if (part == null || part.getSize() <= 0) {
            throw new ClientErrorException("Manca " + mimeType, Response.Status.BAD_REQUEST);
        }
        if (!part.getContentType().startsWith(mimeType)) {
            throw new ClientErrorException("File non permesso per " + mimeType + " tipo", HttpServletResponse.SC_BAD_REQUEST);
        }

        // 1) Leggi in temp + calcola SHA-256 in streaming
        Path tempFile = Files.createTempFile("upload_", ".bin");
        java.security.MessageDigest digest;
        try { digest = java.security.MessageDigest.getInstance("SHA-256"); }
        catch (NoSuchAlgorithmException e) { throw new ServerErrorException(HttpServletResponse.SC_INTERNAL_SERVER_ERROR); }

        try (var in = part.getInputStream();
             var dis = new java.security.DigestInputStream(in, digest);
             var out = Files.newOutputStream(tempFile)) {
            dis.transferTo(out);
        }
        String hash = java.util.HexFormat.of().formatHex(digest.digest());

        // 2) Dedup su DB
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

        // 3) Sposta nella cartella ESTERNA configurata
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
        String fileName = java.util.UUID.randomUUID() + ext;

        Path absolutePath = targetDir.resolve(fileName);
        Files.move(tempFile, absolutePath, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
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