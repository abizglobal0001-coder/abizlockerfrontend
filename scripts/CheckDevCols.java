import java.sql.*;
public class CheckDevCols {
    public static void main(String[] args) throws Exception {
        try (Connection c = DriverManager.getConnection("jdbc:postgresql://localhost:3000/abizlocker", "abizlocker", "abiz_dev_secure_pass_9281");
             Statement st = c.createStatement();
             ResultSet rs = st.executeQuery("SELECT column_name, is_nullable, data_type FROM information_schema.columns WHERE table_name='abiz_financed_devices' AND is_nullable='NO'")) {
            while (rs.next()) {
                System.out.println("NOT NULL: " + rs.getString(1) + " (" + rs.getString(3) + ")");
            }
        }
    }
}
