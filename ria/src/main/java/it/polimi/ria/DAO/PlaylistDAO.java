package it.polimi.ria.DAO;

import it.polimi.ria.entities.Playlist;
import it.polimi.ria.entities.Track;
import it.polimi.ria.entities.User;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;

public class PlaylistDAO implements DAO {
    private final Connection connection;

    public PlaylistDAO(Connection connection) {
        this.connection = connection;
    }

    /**
     * ritorna tutte le playlist di un utente
     *
     * @param user User
     * @return Lista di Playlist
     * @throws SQLException alla servlet
     */
    public List<Playlist> getUserPlaylists(User user) throws SQLException {
        List<Playlist> playlists = new ArrayList<>();

        PreparedStatement querywithparam = connection.prepareStatement("""
                SELECT playlist_id, playlist_title, creation_date
                FROM playlist 
                WHERE user_id = ?
                ORDER BY creation_date DESC
                """);

        querywithparam.setInt(1, user.id());
        ResultSet res = querywithparam.executeQuery();
        if (!res.isBeforeFirst()) {
            closeQuery(res, querywithparam);
            return playlists;
        }
        while (res.next()) {
            Playlist playlist = new Playlist(
                    res.getInt("playlist_id"),
                    res.getString("playlist_title"),
                    res.getDate("creation_date"),
                    user
            );
            playlists.add(playlist);
        }
        closeQuery(res, querywithparam);
        return playlists;
    }

    /**
     * ritorna tutte le tracce di una playlist di un utente dato titolo playlist
     *
     * @param playlistTitle titolo della playlist
     * @param user          User
     * @return Lista di tracce di una Playlist
     * @throws SQLException alla servlet
     */
    public List<Track> getPlaylistTracksByTitle(String playlistTitle, User user) throws SQLException {
        List<Track> tracks = new ArrayList<>();

        PreparedStatement querywithparam = connection.prepareStatement("""
                 SELECT t.track_id, t.user_id, title, album_title, artist, year, genre, song_checksum, image_checksum, song_path, image_path
                 FROM  playlist a
                     JOIN playlist_tracks b ON a.playlist_id = b.playlist_id
                     JOIN track t on b.track_id = t.track_id
                 WHERE a.user_id = ? AND a.playlist_title = ?
                """);

        querywithparam.setInt(1, user.id());
        querywithparam.setString(2, playlistTitle);
        ResultSet res = querywithparam.executeQuery();

        if (!res.isBeforeFirst()) {
            closeQuery(res, querywithparam);
            return tracks;
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
            tracks.add(track);
        }
        closeQuery(res, querywithparam);
        return tracks;
    }

    /**
     * ritorna tutte le tracce di una playlist dato id playlist
     *
     * @param playlistID id della playlist
     * @return Lista di tracce di una Playlist
     * @throws SQLException alla servlet
     */

    public List<Track> getPlaylistTracksById(int playlistID) throws SQLException {
        List<Track> tracks = new ArrayList<>();
//NO GROUPBY NEL CONTEGGIO, HAI 1 SOLA PLAYLIST
        PreparedStatement querywithparam = connection.prepareStatement("""
                SELECT custom_order, track_id, user_id, title, album_title, artist, year, genre, song_checksum, image_checksum, song_path, image_path
                FROM playlist_tracks b NATURAL JOIN track a , (SELECT COUNT(*) AS count
                                                              FROM track a NATURAL JOIN playlist_tracks b
                                                              WHERE playlist_id = ? AND custom_order IS NOT NULL) c 
                
                WHERE b.playlist_id = ?
                ORDER BY
                    CASE WHEN c.count = 0 THEN artist END,
                    CASE WHEN c.count = 0 THEN year END,
                    CASE WHEN c.count = 0 THEN a.track_id END,
                    CASE WHEN c.count != 0 THEN -custom_order END DESC,
                    CASE WHEN c.count != 0 THEN a.track_id END
                """);//- DESC per avere ordine crescente in caso di negativi
        querywithparam.setInt(1, playlistID);
        querywithparam.setInt(2, playlistID);
        ResultSet res = querywithparam.executeQuery();
        if (!res.isBeforeFirst()) {
            closeQuery(res, querywithparam);
            return tracks;
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
                    res.getString("image_checksum"),
                    res.getInt("custom_order")
            );
            tracks.add(track);
        }

        closeQuery(res, querywithparam);
        return tracks;
    }

    /**
     * ritorna tutte le tracce non in una playlist dato id playlist
     *
     * @param userId
     * @param playlistTitle titolo della playlist
     * @return Lista di tracce non in una Playlist
     * @throws SQLException alla servlet
     */
    public List<Track> getTracksNotInPlaylist(String playlistTitle, Integer userId) throws SQLException {
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
                 WHERE user_id = ? AND track_id not in (
                    SELECT pt.track_id
                    FROM playlist p NATURAL JOIN playlist_tracks pt
                    WHERE p.playlist_title= ? AND p.user_id = ?
                 )
                 ORDER BY artist ASC, year ASC, track_id ASC
                """);
        querywithparam.setInt(1, userId);
        querywithparam.setString(2, playlistTitle);
        querywithparam.setInt(3, userId);
        ResultSet res = querywithparam.executeQuery();

        if (!res.isBeforeFirst()) {
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
     * Get 6 Tracks data una Playlist.
     *
     * @param playlistId ID playlist
     * @param groupId    group (inizia da 1
     * @return lista di tracks a gruppi
     * @throws SQLException alla servlet
     */
    public List<Track> getTrackGroup(int playlistId, int groupId) throws SQLException {
        List<Track> tracks = new ArrayList<>();

        PreparedStatement querywithparam = connection.prepareStatement("""
                 SELECT track_id, user_id, title, album_title, artist, year, genre, song_checksum, image_checksum, song_path, image_path
                 FROM track a NATURAL JOIN playlist_tracks b
                 WHERE b.playlist_id = ?
                 ORDER BY custom_order, artist ASC, year ASC, track_id ASC
                 OFFSET ? ROWS
                 FETCH NEXT 5 ROWS ONLY
                """);

        querywithparam.setInt(1, playlistId);
        querywithparam.setInt(2, groupId * 5);
        ResultSet res = querywithparam.executeQuery();

        if (!res.isBeforeFirst()) {
            closeQuery(res, querywithparam);
            return tracks;
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
            tracks.add(track);
        }

        closeQuery(res, querywithparam);
        return tracks;
    }

    /**
     * Ritorna il titolo di una playlist dato l'id
     *
     * @param playlistID id della playlist
     * @return titolo della playlist
     * @throws SQLException alla servlet
     */
    public String getPlaylistTitle(int playlistID) throws SQLException {
        PreparedStatement querywithparam = connection.prepareStatement("""
                        SELECT playlist_title
                        FROM playlist
                        WHERE playlist_id = ?
                """);

        querywithparam.setInt(1, playlistID);
        ResultSet res = querywithparam.executeQuery();

        String playlistTitle = null;
        if (!res.isBeforeFirst()) {
            closeQuery(res, querywithparam);
            return playlistTitle;
        } else {
            res.next();
            playlistTitle = res.getString("playlist_title");

            closeQuery(res, querywithparam);
            return playlistTitle;
        }
    }

    /**
     * Crea playlist per un user.
     *
     * @param playlist Playlist da creare
     * @throws SQLException alla servlet
     */
    public Integer createPlaylist(Playlist playlist) throws SQLException {
        PreparedStatement querywithparam = connection.prepareStatement("""
                INSERT INTO playlist (playlist_title, user_id) VALUES (?,?)
                """, Statement.RETURN_GENERATED_KEYS);
        querywithparam.setString(1, playlist.title());
        querywithparam.setInt(2, playlist.user().id());
        querywithparam.executeUpdate();
        ResultSet res = querywithparam.getGeneratedKeys();

        Integer playlistID = null;
        if (res.isBeforeFirst()) {
            res.next();

            playlistID = res.getInt(1);
        }
        closeQuery(res, querywithparam);
        return playlistID;
    }

    /**
     * Aggiunge tracce a playlist data.
     *
     * @param trackIds
     * @param playlistId
     * @throws SQLException alla servlet
     */
    public void addTracksToPlaylist(List<Integer> trackIds, Integer playlistId) throws SQLException {
        //void e non boolean per gestione errori, lancia eccezioni e non dice solo true o false
        PreparedStatement querywithparam = connection.prepareStatement("""
                INSERT INTO playlist_tracks (playlist_id, track_id) VALUES (?,?)
                """);
        querywithparam.setInt(1, playlistId);
        connection.setAutoCommit(false);
        try {
            for (Integer i : trackIds) {
                querywithparam.setInt(2, i);
                querywithparam.executeUpdate();
            }
            connection.commit();

        } catch (SQLIntegrityConstraintViolationException e) {
            connection.rollback();
            throw new SQLIntegrityConstraintViolationException();
        } catch (SQLException e) {
            connection.rollback();
            throw new SQLException();
        } finally {
            connection.setAutoCommit(true);
            closeQuery(querywithparam);
        }
    }


    /**
     * controlla ownership playlist
     * @param playlist_id playlist_id
     * @param user        user
     * @return true se appartiene, false altrimenti
     * @throws SQLException alla servlet
     */
    public boolean checkPlaylistOwner(int playlist_id, User user) throws SQLException {
        PreparedStatement querywithparam = connection.prepareStatement("""
                 SELECT playlist_id,
                        user_id
                 FROM playlist
                 WHERE playlist_id = ?
                """);

        querywithparam.setInt(1, playlist_id);
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


    /**
     * ritorna tracce ordinate di una playlist nel db
     * @param playlist_id playlist_id
     * @return lista di tracce ordinate
     * @throws SQLException
     */
    public List<Track> getOrderderedTracks(User user, int playlist_id) throws SQLException {
        List<Track> tracks = new ArrayList<>();

        PreparedStatement querywithparam = connection.prepareStatement("""
                        SELECT track_id, user_id, title, album_title, artist, year, genre, song_checksum, image_checksum, song_path, image_path, custom_order
                        FROM track NATURAL JOIN playlist_tracks
                        WHERE user_id = ? AND playlist_id = ?
                """);

        querywithparam.setInt(1, user.id());
        querywithparam.setInt(2, playlist_id);
        ResultSet res = querywithparam.executeQuery();

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
                    res.getString("image_checksum"),
                    res.getInt("custom_order")
            );
            tracks.add(track);
        }

        closeQuery(res, querywithparam);
        return tracks;
    }



    /**
     * Update Track custom order data una playlist.
     *
     * @param trackIds    lista di trackId in ordine desiderato
     * @param playlist_id playlist_id della playlist da modificare
     * @param user        session user
     * @throws SQLException alla servlet
     */
    public void updateTrackOrder(List<Integer> trackIds, int playlist_id, User user) throws SQLException {
        PreparedStatement querywithparam = connection.prepareStatement("""
                UPDATE playlist_tracks pt JOIN playlist p ON p.playlist_id = pt.playlist_id
                SET pt.custom_order = ?
                WHERE pt.playlist_id = ? AND pt.track_id = ? AND p.user_id = ?
                """);

        connection.setAutoCommit(false);
        querywithparam.setInt(2, playlist_id);
        querywithparam.setInt(4, user.id());

        try {
            for (int i = 0; i < trackIds.size(); i++) {
                querywithparam.setInt(1, i + 1);
                querywithparam.setInt(3, trackIds.get(i));
                querywithparam.executeUpdate();
            }
            connection.commit();
        } catch (SQLException e) {
            connection.rollback();
            throw new SQLException(e);
        }

        connection.setAutoCommit(true);
        closeQuery(querywithparam);
    }

}