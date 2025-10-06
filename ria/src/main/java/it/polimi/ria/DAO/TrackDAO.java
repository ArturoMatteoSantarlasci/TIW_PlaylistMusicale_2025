package it.polimi.ria.DAO;

import it.polimi.ria.entities.Track;
import it.polimi.ria.entities.User;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
public class TrackDAO implements DAO {
    private final Connection connection;

    public TrackDAO(Connection connection) {
        this.connection = connection;
    }

    /**
     * Get Track da id (primary key)
     *
     * @param trackId
     * @return la track
     * @throws SQLException alla servlet
     */
    public Track getTrackById(int trackId) throws SQLException {
        PreparedStatement querywithparam = connection.prepareStatement(
                 """
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
                 WHERE track_id = ?
                """);

        querywithparam.setInt(1, trackId);
        ResultSet res = querywithparam.executeQuery();
 Track track = null;
        if (res.isBeforeFirst()) {
            res.next();

            track = new Track(
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

        }
            closeQuery(res, querywithparam);
            return track;
        }

    /**
     * Get Track da id (primary key)
     *
     * @param user utente che ha le tracks
     * @return lista di tracks
     * @throws SQLException alla servlet
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
        if (!res.isBeforeFirst()) {
         //gestione res vuoto
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
     * Aggiungi Track nel DB
     * @param track track da inserire
     * @param user  utente con quella track
     * @return id (auto-increment) della track inserita
     * @throws SQLException alla servlet
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

    System.out.println("[TrackDAO RIA] INSERT image_path=" + track.image_path() + " song_path=" + track.song_path());

        querywithparam.executeUpdate();
        ResultSet res = querywithparam.getGeneratedKeys();//matrice 1x1

        Integer id = null;
        if (res.isBeforeFirst()) {
            res.next();
            id = res.getInt(1);
        }
        closeQuery(querywithparam);
        return id;
    }

    /**
     * @param checksum Music file checksum
     * @return Stringa contenente la path, null se la track non c'è gia nel DB
     * @throws SQLException alla servlet
     */
    public String isTrackFileAlreadyPresent(String checksum) throws SQLException {
        PreparedStatement querywithparam = connection.prepareStatement("""
                SELECT song_path FROM track WHERE song_checksum = ?
                """);
        querywithparam.setString(1, checksum);
        ResultSet result = querywithparam.executeQuery();

        String path = null;
        if (result.isBeforeFirst()) {
            result.next();
            path = result.getString("song_path");
        }

        closeQuery(result, querywithparam);
        return path;
    }

    /**
     * @param checksum Image file checksum
     * @return Stringa contenente la path, null se l'immagine non c'è gia nel DB
     * @throws SQLException alla servlet
     */
    public String isImageFileAlreadyPresent(String checksum) throws SQLException {
        PreparedStatement querywithparam = connection.prepareStatement("""
                SELECT image_path FROM track WHERE image_checksum = ?
                """);
        querywithparam.setString(1, checksum);
        ResultSet result = querywithparam.executeQuery();

        String path = null;
        if (result.isBeforeFirst()) {
            result.next();
            path = result.getString("image_path");
        }
        closeQuery(result, querywithparam);
        return path;
    }

    /**
     * controlla se la track appartiene ad un utente
     * @param track_id track_id
     * @param user     user
     * @return true se traccia appartiene a user, false altrimenti
     * @throws SQLException alla servlet
     */
    public boolean checkTrackOwner(int track_id, User user) throws SQLException {
        PreparedStatement querywithparam = connection.prepareStatement("""
                 SELECT  user_id
                 FROM track 
                 WHERE track_id = ?
                """);

        querywithparam.setInt(1, track_id);
        ResultSet resultSet = querywithparam.executeQuery();
        int userId = user.id();

        boolean result = false;
        if (resultSet.isBeforeFirst()) {
            resultSet.next();
            if(userId == resultSet.getInt(1)) {
                result = true;
            }
        }
        closeQuery(resultSet, querywithparam);
        return result;
    }
}