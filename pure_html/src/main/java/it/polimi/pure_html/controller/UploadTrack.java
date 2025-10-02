package it.polimi.pure_html.controller;


import com.fasterxml.jackson.databind.ObjectMapper;
import it.polimi.pure_html.DAO.TrackDAO;
import it.polimi.pure_html.entities.Track;
import it.polimi.pure_html.entities.User;
import it.polimi.pure_html.utils.ConnectionHandler;
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
import java.sql.*;
import java.time.Year;
import java.util.*;

@WebServlet("/UploadTrack")
@MultipartConfig
public class UploadTrack extends HttpServlet {
    @Serial
    private static final long serialVersionUID = 1L;
    private Connection connection = null;
    private String relativeOutputFolder;
    private User user;
    private Track track;
    private List<File> newFiles;
    private ServletContext context;
    private List<String> genres;

    public void init() throws ServletException {
        context = getServletContext();
        connection = ConnectionHandler.openConnection(context);
        //NOME CARTELLA DOVE SALVARE I FILE NELLA WEBAPP ES. ''uploads''
        relativeOutputFolder = getServletContext().getInitParameter("outputPath");
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
            trackDAO.addTrack(track, user);
            resp.sendRedirect(getServletContext().getContextPath() + "/HomePage");


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
        pathFileRelative = switch (mimeType) {// se già c'è lo ritorno con hash
            case "audio" -> trackDAO.isTrackFileAlreadyPresent(hash);
            case "image" -> trackDAO.isImageFileAlreadyPresent(hash);
            default -> null;
        };

        if (pathFileRelative != null) {
            return new FileDetails(pathFileRelative, hash);
        }

        String absoluteOutputFolderPath = context.getRealPath(relativeOutputFolder) + File.separator + mimeType + File.separator;
        String realname = Paths.get(part.getSubmittedFileName()).getFileName().toString();
        String FilePathAbsolute = absoluteOutputFolderPath + absoluteOutputFolderPath + UUID.randomUUID() + realname.substring(realname.lastIndexOf('.'));
        File outputfolder = new File(FilePathAbsolute);

        if (!outputfolder.exists()) {
            boolean created = outputfolder.mkdirs();
            if (!created) {
                throw new ServerErrorException("Errore durante salvataggio file", HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            }
        }

        File outputFile = new File(FilePathAbsolute);

        try {
            Files.copy(part.getInputStream(), outputFile.toPath());
            newFiles.add(outputFile);
            pathFileRelative = relativeOutputFolder + File.separator + mimeType + File.separator + outputFile.getName();
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