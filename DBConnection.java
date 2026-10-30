import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DBConnection {

    private static final String URL = "jdbc:mysql://localhost:3306/lib_sys_db";
    private static final String USER = "root";
    private static final String PASSWORD = ""; // default XAMPP password is blank

    public static Connection getConnection() throws SQLException {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            throw new SQLException("MySQL JDBC Driver not found. Make sure mysql-connector-j-x.x.x.jar is added to your project's classpath/libraries.", e);
        }
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }
}
