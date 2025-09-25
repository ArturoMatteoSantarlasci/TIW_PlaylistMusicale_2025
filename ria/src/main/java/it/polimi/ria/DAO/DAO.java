package it.polimi.ria.DAO;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public interface DAO {
    default void closeQuery(ResultSet resultObject, Statement query) {
        try {
            resultObject.close();
            query.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

default void closeQuery(Statement query) {
    try {
        query.close();
    } catch (SQLException e) {
        e.printStackTrace();
    }
}
}