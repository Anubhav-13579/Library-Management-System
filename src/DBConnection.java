import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DBConnection {
    public static Connection getConnection() throws SQLException {
        String url = System.getenv().getOrDefault("LIBRARY_DB_URL", "jdbc:mysql://localhost:3306/lib_sys_db");
        String user = System.getenv("LIBRARY_DB_USER");
        String password = System.getenv("LIBRARY_DB_PASSWORD");
        if (user == null || user.isBlank()) {
            throw new SQLException("Set LIBRARY_DB_USER to your local MySQL username.");
        }
        return DriverManager.getConnection(url, user, password == null ? "" : password);
    }
}
