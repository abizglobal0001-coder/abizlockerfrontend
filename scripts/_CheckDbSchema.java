import java.sql.*;
public class CheckDbSchema {
    public static void main(String[] args) throws Exception {
        Class.forName("org.postgresql.Driver");
        Connection c = DriverManager.getConnection("jdbc:postgresql://localhost:3000/abizlocker", "abizlocker", "abiz_dev_secure_pass_9281");
        Statement s = c.createStatement();
        ResultSet rs = s.executeQuery("SELECT id, author, id FROM databasechangelog ORDER BY orderexecuted DESC LIMIT 5");
        System.out.println("Latest changeSet in databasechangelog:");
        while(rs.next()) {
            System.out.println("  " + rs.getString(1) + " by " + rs.getString(2));
        }
        ResultSet cols = s.executeQuery("SELECT column_name FROM information_schema.columns WHERE table_name='abiz_financed_devices'");
        System.out.println("Columns in abiz_financed_devices:");
        while(cols.next()) {
            System.out.println("  " + cols.getString(1));
        }
        c.close();
    }
}