package it.polimi.pure_html.DAO;

import it.polimi.pure_html.entities.User;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Fornisce le operazioni CRUD principali sugli utenti dell'applicazione.
 */
public class UserDAO implements DAO {
    private final Connection connection;

    public UserDAO(Connection connection) {
        this.connection = connection;
    }

    /**
     * controlla se esiste un utente nel database con nickname e password passati come parametri.
     * @param nickname nickname
     * @param password password
     * @return user se esiste, null altrimenti
     * @throws SQLException
     * @see User
     */
    public User checkUser(String nickname, String password) throws SQLException {
        PreparedStatement querywithparam = connection.prepareStatement("SELECT * FROM user WHERE nickname = ? AND password = ?");

        querywithparam.setString(1, nickname);
        querywithparam.setString(2, password);

        ResultSet res = querywithparam.executeQuery();

        User user = null;
        if (res.isBeforeFirst()) {
            // the user exists
            res.next();
            user = new User(
                    res.getInt("user_id"),
                    res.getString("nickname"),
                    res.getString("password"),
                    res.getString("name"),
                    res.getString("surname")
            );
        }

        closeQuery(res, querywithparam);
        return user;
    }

    /**
     * Aggiunge user a db
     *
     * @param user User
     * @return false se non è stato aggiunto, true altrimenti
     */
    public boolean addUser(User user) {
        PreparedStatement querywithparam = null;
        try {
            querywithparam = connection.prepareStatement(
                    "INSERT INTO user(nickname, password, name, surname)VALUES( ?, ?, ?, ?)"
            );

            querywithparam.setString(1, user.nickname());
            querywithparam.setString(2, user.password());
            querywithparam.setString(3, user.name());
            querywithparam.setString(4, user.surname());

            querywithparam.executeUpdate();
        } catch (SQLException e) {
            // the user could not be added
            System.out.println(e.getMessage());
            return false;
        } finally {
            if (querywithparam != null) {
                closeQuery(querywithparam);
            }
        }

        // the operation succeded
        return true;
    }
}
