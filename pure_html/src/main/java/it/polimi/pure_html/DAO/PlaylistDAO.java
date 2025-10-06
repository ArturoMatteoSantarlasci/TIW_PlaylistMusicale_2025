package it.polimi.pure_html.DAO;

import it.polimi.pure_html.entities.Playlist;
import it.polimi.pure_html.entities.Track;
import it.polimi.pure_html.entities.User;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class PlaylistDAO implements DAO {
    private final Connection connection;

    public PlaylistDAO(Connection connection) {
        this.connection = connection;
    }

    /**
     * Ritorna playlists di un utente
     *
     * @param user User
     * @return Lista di Playlist
     * @throws SQLException
     */
    public List<Playlist> getUserPlaylists(User user) throws SQLException {
        List<Playlist> playlists = new ArrayList<>();

        PreparedStatement querywithparam = connection.prepareStatement("""
                SELECT b.playlist_id, b.playlist_title, b.creation_date
                FROM  playlist b
                WHERE b.user_id = ?
                ORDER BY creation_date DESC
                """);

        querywithparam.setInt(1, user.id());
        ResultSet res = querywithparam.executeQuery();
          if(!res.isBeforeFirst()) {
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
     * Ritorna le playlist dell'utente con metadati aggregati utili alla HomePage.
     *
     * @param user utente proprietario
     * @return lista di riassunti playlist
     * @throws SQLException in caso di errori di accesso al DB
     */
    public List<PlaylistSummary> getPlaylistSummaries(User user) throws SQLException {
        List<PlaylistSummary> playlists = new ArrayList<>();

        PreparedStatement querywithparam = connection.prepareStatement("""
                SELECT p.playlist_id,
                       p.playlist_title,
                       p.creation_date,
                       COUNT(pt.track_id) AS tracks_count
                FROM playlist p
                LEFT JOIN playlist_tracks pt ON p.playlist_id = pt.playlist_id
                WHERE p.user_id = ?
                GROUP BY p.playlist_id, p.playlist_title, p.creation_date
                ORDER BY p.creation_date DESC
                """);

        querywithparam.setInt(1, user.id());
        ResultSet res = querywithparam.executeQuery();

        while (res.next()) {
            playlists.add(new PlaylistSummary(
                    res.getInt("playlist_id"),
                    res.getString("playlist_title"),
                    res.getDate("creation_date"),
                    res.getInt("tracks_count")
            ));
        }

        closeQuery(res, querywithparam);
        return playlists;
    }
/**
     * Ritorna le tracce di una playlist dato il titolo della playlist e l'utente
     * @param playlistTitle titolo della playlist
     * @param user utente proprietario della playlist
     * @return lista di tracce
     * @throws SQLException
     */
    public List<Track> getPlaylistTracksByTitle(String playlistTitle, User user) throws SQLException {
        List<Track> tracks = new ArrayList<>();

        PreparedStatement querywithparam = connection.prepareStatement("""
                 SELECT t.track_id,t.user_id, title, album_title, artist, year, genre, song_checksum, image_checksum, song_path, image_path
                 FROM  playlist a
                     JOIN playlist_tracks b ON a.playlist_id = b.playlist_id
                     JOIN track t on b.track_id = t.track_id
            
                 WHERE a.user_id = ? AND a.playlist_title = ?
                """);

        querywithparam.setInt(1, user.id());
        querywithparam.setString(2, playlistTitle);
        ResultSet res= querywithparam.executeQuery();
        if(!res.isBeforeFirst()) {
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
     * Ritorna le tracce di una playlist dato l'id della playlist
     * @param playlistID id della playlist
     * @return lista di tracce
     * @throws SQLException
     */
    public List<Track> getPlaylistTracksById(int playlistID) throws SQLException {
        List<Track> tracks = new ArrayList<>();

        PreparedStatement querywithparam = connection.prepareStatement("""
                 SELECT track_id,user_id, title, album_title, artist, year, genre, song_checksum, image_checksum, song_path, image_path
                 FROM track a NATURAL JOIN playlist_tracks b
                 WHERE b.playlist_id = ?
                 ORDER BY a.artist, a.title, a.track_id
                """);

        querywithparam.setInt(1, playlistID);
        ResultSet res = querywithparam.executeQuery();
        if(!res.isBeforeFirst()) {
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
     * Ritorna le tracce di un utente che non sono nella playlist
     * @param playlistTitle titolo della playlist
     * @param userId id dell'utente
     * @return lista di tracce
     * @throws SQLException
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
                    SELECT t.track_id
                    FROM playlist p NATURAL JOIN playlist_tracks pt JOIN track t ON t.track_id=pt.track_id
                    WHERE p.playlist_title= ? AND p.user_id = ?
                 )
                 ORDER BY artist ASC, YEAR ASC, track_id ASC
                """);
        querywithparam.setInt(1, userId);
        querywithparam.setString(2, playlistTitle);
        querywithparam.setInt(3, userId);
        ResultSet res = querywithparam.executeQuery();

        if(!res.isBeforeFirst()) {
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
      * Ritorna le tracce di una playlist in gruppi di 5
      * @param playlistId id della playlist
      * @param groupId id del gruppo (0,1,2,...)
      * @return lista di tracce
      * @throws SQLException
      */
    public List<Track> getTrackGroup(int playlistId, int groupId) throws SQLException {
        List<Track> tracks = new ArrayList<>();

        PreparedStatement querywithparam = connection.prepareStatement("""
                 SELECT track_id, user_id,title, album_title, artist, year, genre, song_checksum, image_checksum, song_path, image_path
                 FROM track a NATURAL JOIN playlist_tracks b
                 WHERE b.playlist_id = ?
                 ORDER BY artist ASC, YEAR ASC, a.track_id ASC
                 OFFSET ? ROWS
                 FETCH NEXT 5 ROWS ONLY
                """);

        querywithparam.setInt(1, playlistId);
        querywithparam.setInt(2, groupId * 5);
        ResultSet res = querywithparam.executeQuery();
        if(!res.isBeforeFirst()) {
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
     * Conta il numero totale di tracce nella playlist (usato per calcolare i gruppi di paginazione).
     * @param playlistId id playlist
     * @return numero tracce (0 se vuota)
     * @throws SQLException propagata al chiamante
     */
    public int countPlaylistTracks(int playlistId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM playlist_tracks WHERE playlist_id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, playlistId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        }
        return 0;
    }
   /**
     * Ritorna il titolo della playlist dato l'id
     *
     * @param playlistID id della playlist
     * @return titolo della playlist
     * @throws SQLException
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
        if (res.isBeforeFirst()) {
            res.next();
            playlistTitle = res.getString("playlist_title");
        }
        closeQuery(res, querywithparam);
        return playlistTitle;
    }

    /**
     * Crea playlist nel db User.
     *
     * @param playlist Playlist da creare
     * @throws SQLException
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
     * Aggiunge tracce ad una playlist
     *
     * @param trackIds
     * @param playlistId
     * @throws SQLException
     */
    public void addTracksToPlaylist(List<Integer> trackIds, Integer playlistId) throws SQLException {
        PreparedStatement querywithparam = connection.prepareStatement("""
                INSERT INTO playlist_tracks (playlist_id, track_id) VALUES (?,?)
                """);
        querywithparam.setInt(1, playlistId);
        connection.setAutoCommit(false);
        try {
            for (int i : trackIds) {
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
     * Controlla se l'utente è il proprietario della playlist
     * @param playlist_id
     * @param user
     * @return true se l'utente è il proprietario, false altrimenti
     * @throws SQLException
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
            if( userId == res.getInt("user_id"))
                result = true;
        }

        closeQuery(res,querywithparam);
        return result;
    }

    public record PlaylistSummary(int id, String title, Date creationDate, int tracksCount) {
    }
}
