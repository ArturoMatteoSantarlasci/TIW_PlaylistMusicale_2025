package it.polimi.ria.DAO;

import it.polimi.ria.entities.User;

import java.sql.*;

public class UserDAO implements DAO {
    private final Connection connection;

    //faccio costruttore
    public UserDAO(Connection connection) {
        this.connection = connection;
    }

    /**
     * controlla se esiste un utente in DB con nickname e password passati
     *
     * @param nickname nickname da controllare
     * @param password password da controllare
     * @return un user se esiste, null altrimenti
     * @throws SQLException al controller-servlet
     * @see User
     */
    public User checkUser(String nickname, String password) throws SQLException {
        PreparedStatement querywithparam = connection.prepareStatement(
                "SELECT * FROM user WHERE nickname = ? AND password = ?");

        querywithparam.setString(1, nickname);
        querywithparam.setString(2, password);

        ResultSet res = querywithparam.executeQuery();

        User user = null;
        if (res.isBeforeFirst()) {
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
     * Aggiunge user al DB
     *
     * @param user User che deve essere aggiunto al DB
     * @return false se non è stato aggiunto, true altrimenti
     */
    public boolean addUser(User user) {
        PreparedStatement query = null;
        try {
            query = connection.prepareStatement(
                    "INSERT INTO user(nickname, password, name, surname)VALUES( ?, ?, ?, ?)"
            );

            query.setString(1, user.nickname());
            query.setString(2, user.password());
            query.setString(3, user.name());
            query.setString(4, user.surname());

            query.executeUpdate();
        } catch (SQLException e) {
            System.out.println(e.getMessage());
            return false;
        } finally {
            if (query != null) {
                closeQuery(query);
            }
        }
        return true;
    }
}
