package it.polimi.pure_html.DAO;


import it.polimi.pure_html.entities.Track;
import it.polimi.pure_html.entities.User;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class TrackDAO implements DAO {
    private Connection connection;

    public TrackDAO(Connection connection) {
        this.connection = connection;
    }

    /**
     * Restituisce tutte le tracce di un utente
     *
     * @param user Utente di cui si vogliono le tracce
     * @return Lista di tracce dell'utente
     * @throws SQLException
     */
    public List<Track> getUserTracks(User user) throws SQLException {
        List<Track> userTracks = new ArrayList<>();
        PreparedStatement querywithparam = connection.prepareStatement("""
                 SELECT track_id,
                        user_id,
                        title,
                        artist,
                        year,
                        album_title,
                        genre,
                        image_path,
                        song_path,
                        song_checksum,
                        image_checksum
                 FROM track
                 WHERE user_id = ?
                 ORDER BY artist ASC, YEAR ASC, track_id ASC
                """);
        querywithparam.setInt(1, user.id());
        ResultSet res = querywithparam.executeQuery();
        if(!res.isBeforeFirst()){
            closeQuery(res, querywithparam);
            return userTracks;
        }

        while (res.next()) {
            Track track = new Track(
                    res.getInt("track_id"),
                    res.getInt("user_id"),
                    res.getString("title"),
                    res.getString("artist"),
                    res.getInt("year"),
                    res.getString("album_title"),
                    res.getString("genre"),
                    res.getString("image_path"),
                    res.getString("song_path"),
                    res.getString("song_checksum"),
                    res.getString("image_checksum")
            );
            userTracks.add(track);
        }

        closeQuery(res, querywithparam);
        return userTracks;
    }

    /**
     * Aggiunge una traccia al DB
     *
     * @param track Traccia da aggiungere
     * @param user  Utente a cui appartiene la traccia
     * @return Integer contenente l'id della traccia appena inserita; null altrimenti
     * @throws SQLException
     */
    public Integer addTrack(Track track, User user) throws SQLException {
        PreparedStatement querywithparam = connection.prepareStatement("""
                INSERT INTO track (user_id, title, artist, year, album_title, genre, image_path, song_path, song_checksum, image_checksum)
                VALUES (?,?,?,?,?,?,?,?,?,?)
                """, Statement.RETURN_GENERATED_KEYS);

        querywithparam.setInt(1, user.id());
        querywithparam.setString(2, track.title());
        querywithparam.setString(3, track.artist());
        querywithparam.setInt(4, track.year());
        querywithparam.setString(5, track.album_title());
        querywithparam.setString(6, track.genre());
        querywithparam.setString(7, track.image_path());
        querywithparam.setString(8, track.song_path());
        querywithparam.setString(9, track.song_checksum());
        querywithparam.setString(10, track.image_checksum());

        querywithparam.executeUpdate();
        ResultSet res = querywithparam.getGeneratedKeys();

        Integer id = null;
        if (res.isBeforeFirst()) {
            res.next();
            id = res.getInt(1);
        }
        closeQuery(querywithparam);
        return id;
    }

    /**
     * @param checksum Song file checksum
     * @return String contenente il file song_path se presente; altrimenti null
     * @throws SQLException
     */
    public String isTrackFileAlreadyPresent(String checksum) throws SQLException {
        PreparedStatement querywithparam = connection.prepareStatement("""
                SELECT song_path FROM track WHERE song_checksum = ?
                """);
        querywithparam.setString(1, checksum);
        ResultSet res = querywithparam.executeQuery();

        String path = null;
        if (res.isBeforeFirst()) {
            res.next();
            path = res.getString("song_path");
        }

        closeQuery(res, querywithparam);
        return path;
    }

    /**
     * @param checksum Image file checksum
     * @return String contenente il file image_path se presente; altrimenti null
     * @throws SQLException
     */
    public String isImageFileAlreadyPresent(String checksum) throws SQLException {
        PreparedStatement querywithparam = connection.prepareStatement("""
                SELECT image_path FROM track WHERE image_checksum = ?
                """);
        querywithparam.setString(1, checksum);
        ResultSet res = querywithparam.executeQuery();

        String path = null;
        if (res.isBeforeFirst()) {
            res.next();
            path = res.getString("image_path");
        }
        closeQuery(res, querywithparam);
        return path;
    }

    /**
     * Controlla se la traccia appartiene ad un utente specifico
     * @param track_id track_id
     * @param user     user
     * @return true se traccia appartiene , altrimenti false
     * @throws SQLException
     */
    public boolean checkTrackOwner(int track_id, User user) throws SQLException {
        PreparedStatement querywithparam = connection.prepareStatement("""
                 SELECT track_id,
                        user_id
                 FROM track 
                 WHERE track_id = ?
                """);

        querywithparam.setInt(1, track_id);
        ResultSet res = querywithparam.executeQuery();
        int userId = user.id();

        boolean result = false;
        if (res.isBeforeFirst()) {
            res.next();
            if(userId == res.getInt("user_id")) {
                result = true;
            }
        }
        closeQuery(res, querywithparam);
        return result;
    }
}
