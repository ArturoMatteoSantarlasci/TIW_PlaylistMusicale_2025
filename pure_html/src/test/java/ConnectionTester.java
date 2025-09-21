import jakarta.servlet.ServletContext;
import jakarta.servlet.http.HttpServlet;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.fail;

public class ConnectionTester extends HttpServlet {
    private static final long serialVersionUID = 1L;

    @Disabled("Disabled because it needs a servlet context, which we do not have.")
    @Test
    @Override
    public void init() {
        ServletContext context = getServletContext();
        String driver = context.getInitParameter("dbDriver");
        String url = context.getInitParameter("dbUrl");
        String user = context.getInitParameter("dbUser");
        String password = context.getInitParameter("dbPassword");
        try {
            Class.forName(driver);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
        Connection connection = null;
        try {
            connection = DriverManager.getConnection(url, user, password);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void testConnection() {
        final String DATABASE = "tiw2025";
        final String USER = "arturo";
        final String PASSWORD = "ARTUROsantarlasci";
        Connection connection = null;

        // Load the JDBC driver
        try {
            Class.forName("org.mariadb.jdbc.Driver");
            System.out.println("Driver loaded");
        } catch (ClassNotFoundException e) {
            fail("Driver not found");
            e.printStackTrace();
        }

        try {
            connection = DriverManager.getConnection
                    ("jdbc:mariadb://localhost:3307/" + DATABASE, USER, PASSWORD);
            System.out.println("Database connection successful");
            connection.close();
        } catch (Exception e) {
            fail("Connection failed");
            e.printStackTrace();
        }
    }
}