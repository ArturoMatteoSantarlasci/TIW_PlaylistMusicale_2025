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
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.*;
import java.time.Year;
import java.util.*;

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

    /**
     * @param part     item received with the form
     * @param mimeType expected MIME type
     * @return record containing the file path and hash of the received item
     * @throws IOException
     * @throws SQLException
     */
    private FileDetails processPart(Part part, String mimeType) throws IOException, SQLException {
        String pathFileRelative;
        String hash;

        if (part == null || part.getSize() <= 0) {
            throw new ClientErrorException("Manca" + mimeType, Response.Status.BAD_REQUEST);
        }

        if (!part.getContentType().startsWith(mimeType)) {
            throw new ClientErrorException("File non permesso per " + mimeType + " tipo", HttpServletResponse.SC_BAD_REQUEST);
        }

        TrackDAO trackDAO = new TrackDAO(connection);

        hash = getSHA256Hash(part.getInputStream().readAllBytes());
        pathFileRelative = switch (mimeType) { // se già c'è lo ritorno con hash
            case "audio" -> trackDAO.isTrackFileAlreadyPresent(hash);
            case "image" -> trackDAO.isImageFileAlreadyPresent(hash);
            default -> null;
        };

        if (pathFileRelative != null) {
            // Fix legacy: audio file salvato in passato sotto /media/img
            if ("audio".equals(mimeType) && pathFileRelative.startsWith("/media/img/") && pathFileRelative.toLowerCase().matches(".*\\.(mp3|wav|ogg|m4a)$")) {
                String legacyName = pathFileRelative.substring(pathFileRelative.lastIndexOf('/') + 1);
                String corrected = "/media/audio/" + legacyName;
                System.out.println("[UploadTrack PURE_HTML] Legacy audio duplicate detected. Old=" + pathFileRelative + " New=" + corrected);
                try {
                    String legacyAbs = context.getRealPath(pathFileRelative);
                    String audioDirAbs = context.getRealPath("/media/audio/");
                    if (legacyAbs != null && audioDirAbs != null) {
                        var legacyFile = new File(legacyAbs);
                        var audioDir = new File(audioDirAbs);
                        if (!audioDir.exists()) audioDir.mkdirs();
                        var newFile = new File(audioDir, legacyName);
                        if (legacyFile.exists() && !newFile.exists()) {
                            java.nio.file.Files.copy(legacyFile.toPath(), newFile.toPath());
                            System.out.println("[UploadTrack PURE_HTML] Copied legacy audio to " + newFile.getAbsolutePath());
                        }
                        pathFileRelative = corrected;
                    }
                } catch (Exception moveEx) {
                    System.err.println("[UploadTrack PURE_HTML] Errore correzione legacy audio: " + moveEx.getMessage());
                }
            }
            return new FileDetails(pathFileRelative, hash);
        }

        // Cartella specifica per tipo
        final String targetFolderName = switch (mimeType) {
            case "audio" -> "audio";
            case "image" -> "img";
            default -> "misc";
        };
        System.out.println("[UploadTrack PURE_HTML] Part type=" + mimeType + " contentType=" + part.getContentType() + " -> folder=" + targetFolderName);
        String absoluteOutputFolderPath = context.getRealPath("media") + File.separator + targetFolderName + File.separator;
        String realname = Paths.get(part.getSubmittedFileName()).getFileName().toString();
        String fileName = UUID.randomUUID() + realname.substring(realname.lastIndexOf('.'));
        String filePathAbsolute = absoluteOutputFolderPath + fileName;
        File outputDir = new File(absoluteOutputFolderPath);
        if (!outputDir.exists() && !outputDir.mkdirs()) {
            throw new ServerErrorException("Errore durante salvataggio file", HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
        File outputFile = new File(filePathAbsolute);
        try {
            Files.copy(part.getInputStream(), outputFile.toPath());
            newFiles.add(outputFile);
            // Path salvato nel DB (leading slash)
            pathFileRelative = "/media/" + targetFolderName + "/" + outputFile.getName();
            System.out.println("[UploadTrack PURE_HTML] Saved file to " + pathFileRelative);

            // Copia anche nella sorgente per persistenza (src/main/webapp/media/img)
            try {
                File warRoot = new File(context.getRealPath("/"));
                File current = warRoot;
                File projectRoot = null;
                int maxLevels = 6;
                while (current != null && maxLevels-- > 0) {
                    File srcDir = new File(current, "src");
                    if (srcDir.exists() && srcDir.isDirectory()) { projectRoot = current; break; }
                    current = current.getParentFile();
                }
                if (projectRoot != null) {
                    File sourceMediaDir = new File(projectRoot, "src" + File.separator + "main" + File.separator + "webapp" + File.separator + "media" + File.separator + targetFolderName);
                    if (!sourceMediaDir.exists() && !sourceMediaDir.mkdirs()) {
                        System.err.println("[UploadTrack pure_html] Impossibile creare directory sorgente media: " + sourceMediaDir.getAbsolutePath());
                    } else {
                        File sourceCopy = new File(sourceMediaDir, outputFile.getName());
                        if (!sourceCopy.exists()) {
                            Files.copy(outputFile.toPath(), sourceCopy.toPath());
                        }
                    }
                } else {
                    System.err.println("[UploadTrack pure_html] Root progetto non trovata per copia sorgente media");
                }
            } catch (Exception copyEx) {
                System.err.println("[UploadTrack pure_html] Errore copia file in sorgente: " + copyEx.getMessage());
            }
            return new FileDetails(pathFileRelative, hash);
        } catch (Exception e) {
            e.printStackTrace();
            throw new ServerErrorException("Errore durante salvataggio file", HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * @param input Array di byte di cui calcolare l'hash
     * @return 64 caratteri stringa di hash SHA-256 del contenuto
     */
    private String getSHA256Hash(byte[] input) {
        MessageDigest digest;
        HexFormat hex = HexFormat.of();
        try {
            digest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            throw new ServerErrorException(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
        byte[] hash = digest.digest(input);
        return hex.formatHex(hash);
    }

    @Override
    public void destroy() {
        ConnectionHandler.closeConnection(connection);
    }

    record FileDetails(String path, String hash) {
    }
}