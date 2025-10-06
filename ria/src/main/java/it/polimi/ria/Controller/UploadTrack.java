package it.polimi.ria.Controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import it.polimi.ria.DAO.TrackDAO;
import it.polimi.ria.entities.Track;
import it.polimi.ria.entities.User;
import it.polimi.ria.ConnectionHandler;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.UnavailableException;
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
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.time.Year;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

@WebServlet("/UploadTrack")
@MultipartConfig
public class UploadTrack extends HttpServlet {
    @Serial
    private static final long serialVersionUID = 1L;
    private Connection connection = null;
    private User user;
    private Track track;
    private List<File> newFiles;
    private ServletContext context;
    private List<String> genres;

    public void init() throws ServletException {
        context = getServletContext();
        connection = ConnectionHandler.openConnection(context);
        // I file ora vengono salvati direttamente in webapp/media/
        newFiles = new ArrayList<>();

        try {
            ObjectMapper objectMapper = new ObjectMapper();
            genres = List.of(objectMapper.readValue(new File(context.getRealPath("genres.json")), String[].class));
        } catch (IOException e) {
            e.printStackTrace();
            throw new UnavailableException("Impossibile caricare i generi");
        }
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
        user = (User) req.getSession().getAttribute("user");


        try {
            String title = getRequiredParameter(req,"title");
            String artist = getRequiredParameter(req,"artist");

            int year;
            try {
                year = Integer.parseInt(req.getParameter("year"));
            } catch (NumberFormatException e) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                resp.setContentType("text/plain");
                resp.getWriter().println("Anno non valido");
                return;
            }
            if (year < 1901 || year > Year.now().getValue()) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Anno non valido");
                return;
            }

            String album = getRequiredParameter(req,"album");

            String genre = getRequiredParameter(req,"genre");
            if (!genres.contains(genre)) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                resp.setContentType("text/plain");
                resp.getWriter().println("Genere non valido");
                return;
            }

            FileDetails fileDetails;

            // Salva file: immagini in /media/img, audio in /media/audio
            fileDetails = processPart(req.getPart("image"), "image");
            String imagePath = fileDetails.path();
            String imageHash = fileDetails.hash();

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
        }

        // Add track
        TrackDAO trackDAO = new TrackDAO(connection);
        try {
            Integer trackId = trackDAO.addTrack(track, user);

            Gson gson = new Gson();
            resp.setStatus(HttpServletResponse.SC_CREATED);
            resp.setContentType("application/json");
            resp.getWriter().println(gson.toJson(
                    new Track(trackId, user.id(), track.title(), track.artist(), track.year(), track.album_title(), track.genre(), track.image_path(),
                            track.song_path(), track.song_checksum(), track.image_checksum())));
        } catch (SQLIntegrityConstraintViolationException e) {
            if (e.getMessage().contains("Duplicate")) {
                resp.setStatus(HttpServletResponse.SC_CONFLICT);
                resp.setContentType("text/plain");
                resp.getWriter().println("Duplicato track");
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
            // Correzione legacy: audio duplicato salvato in passato in /media/img
            if ("audio".equals(mimeType) && pathFileRelative.startsWith("/media/img/") && pathFileRelative.toLowerCase().matches(".*\\.(mp3|wav|ogg|m4a)$")) {
                String legacyName = pathFileRelative.substring(pathFileRelative.lastIndexOf('/') + 1);
                String corrected = "/media/audio/" + legacyName;
                System.out.println("[UploadTrack RIA] Legacy audio duplicate detected. Old=" + pathFileRelative + " New=" + corrected);
                try {
                    String legacyAbs = context.getRealPath(pathFileRelative);
                    String audioDirAbs = context.getRealPath("/media/audio/");
                    if (legacyAbs != null && audioDirAbs != null) {
                        File legacyFile = new File(legacyAbs);
                        File audioDir = new File(audioDirAbs);
                        if (!audioDir.exists()) audioDir.mkdirs();
                        File newFile = new File(audioDir, legacyName);
                        if (legacyFile.exists() && !newFile.exists()) {
                            Files.copy(legacyFile.toPath(), newFile.toPath());
                            System.out.println("[UploadTrack RIA] Copied legacy audio to " + newFile.getAbsolutePath());
                        }
                        pathFileRelative = corrected;
                    }
                } catch (Exception ex) {
                    System.err.println("[UploadTrack RIA] Errore correzione legacy audio: " + ex.getMessage());
                }
            }
            return new FileDetails(pathFileRelative, hash);
        }

        // Cartella target distinta per tipo: image -> img, audio -> audio
        final String targetFolderName = switch (mimeType) {
            case "audio" -> "audio";
            case "image" -> "img";
            default -> "misc"; // fallback improbabile
        };
        String baseReal = context.getRealPath("media");
        System.out.println("[UploadTrack RIA] Part type=" + mimeType + " contentType=" + part.getContentType() + " baseRealPath=" + baseReal + " -> folder=" + targetFolderName);

        if (baseReal == null) {
            // Caso raro: il container non fornisce real path (deploy non exploded). Usiamo temp dir applicativa.
            File tempRoot = (File) context.getAttribute("javax.servlet.context.tempdir");
            baseReal = new File(tempRoot, "media").getAbsolutePath();
            System.out.println("[UploadTrack RIA] baseReal era null. Fallback tempdir=" + baseReal);
        }

        String absoluteOutputFolderPath = baseReal + File.separator + targetFolderName + File.separator;
        String realname = Paths.get(part.getSubmittedFileName()).getFileName().toString();
        String fileName = UUID.randomUUID() + realname.substring(realname.lastIndexOf('.'));
        String filePathAbsolute = absoluteOutputFolderPath + fileName;
        File outputFile = new File(filePathAbsolute);

        if (!outputFile.getParentFile().exists() && !outputFile.getParentFile().mkdirs()) {
            throw new ServerErrorException("Errore durante creazione cartelle", HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }

        try {
            Files.copy(part.getInputStream(), outputFile.toPath());
            System.out.println("[UploadTrack RIA] Wrote file absolute=" + outputFile.getAbsolutePath() + " exists=" + outputFile.exists() + " size=" + (outputFile.exists()? outputFile.length(): -1));
            newFiles.add(outputFile);
            pathFileRelative = "/media/" + targetFolderName + "/" + fileName;
            System.out.println("[UploadTrack RIA] Saved file to " + pathFileRelative);

            // Copia anche nella cartella sorgente per persistenza tra rebuild (src/main/webapp/media/img)
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
                        System.err.println("[UploadTrack] Impossibile creare directory sorgente media: " + sourceMediaDir.getAbsolutePath());
                    } else {
                        File sourceCopy = new File(sourceMediaDir, fileName);
                        if (!sourceCopy.exists()) {
                            Files.copy(outputFile.toPath(), sourceCopy.toPath());
                            System.out.println("[UploadTrack RIA] Copied to source dir: " + sourceCopy.getAbsolutePath());
                        } else {
                            System.out.println("[UploadTrack RIA] Source copy already exists: " + sourceCopy.getAbsolutePath());
                        }
                    }
                } else {
                    System.err.println("[UploadTrack] Root progetto non trovata per copia sorgente media");
                }
            } catch (Exception copyEx) {
                System.err.println("[UploadTrack] Errore copia file in sorgente: " + copyEx.getMessage());
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