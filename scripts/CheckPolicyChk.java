import java.sql.*;
public class CheckPolicyChk {
    public static void main(String[] args) throws Exception {
        try (Connection c = DriverManager.getConnection("jdbc:postgresql://localhost:3000/abizlocker", "abizlocker", "abiz_dev_secure_pass_9281");
             Statement st = c.createStatement();
             ResultSet rs = st.executeQuery("SELECT conname, pg_get_constraintdef(oid) FROM pg_constraint WHERE conrelid = 'abiz_financed_devices'::regclass")) {
            while (rs.next()) {
                System.out.println(rs.getString(1) + ": " + rs.getString(2));
            }
        }
    }
}
