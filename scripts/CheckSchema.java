import java.sql.*;
public class CheckSchema {
    public static void main(String[] args) throws Exception {
        try (Connection c = DriverManager.getConnection("jdbc:postgresql://localhost:3000/abizlocker", "abizlocker", "abiz_dev_secure_pass_9281");
             ResultSet rs = c.getMetaData().getColumns(null, null, "abiz_slo_measurements", null)) {
            while (rs.next()) {
                System.out.println("col: " + rs.getString("COLUMN_NAME") + " type: " + rs.getString("TYPE_NAME"));
            }
        }
    }
}
