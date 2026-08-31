import java.sql.*;
import java.util.*;
public class CheckFK {
    public static void main(String[] args) throws Exception {
        try (Connection c = DriverManager.getConnection("jdbc:postgresql://localhost:3000/abizlocker", "abizlocker", "abiz_dev_secure_pass_9281");
             Statement st = c.createStatement()) {
            ResultSet rs = st.executeQuery("SELECT tc.table_name, kcu.column_name, ccu.table_name AS foreign_table_name, ccu.column_name AS foreign_column_name " +
                "FROM information_schema.table_constraints AS tc " +
                "JOIN information_schema.key_column_usage AS kcu ON tc.constraint_name = kcu.constraint_name " +
                "JOIN information_schema.constraint_column_usage AS ccu ON ccu.constraint_name = tc.constraint_name " +
                "WHERE tc.constraint_type = 'FOREIGN KEY' AND tc.table_name IN ('abiz_device_commands', 'abiz_operational_metrics', 'abiz_fleet_incidents');");
            while (rs.next()) {
                System.out.println(rs.getString(1) + "." + rs.getString(2) + " -> " + rs.getString(3) + "." + rs.getString(4));
            }
        }
    }
}
