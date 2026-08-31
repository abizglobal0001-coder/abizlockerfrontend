import liquibase.Liquibase;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.resource.ClassLoaderResourceAccessor;
import java.sql.Connection;
import java.sql.DriverManager;
public class RunPhase20Migration {
    public static void main(String[] args) throws Exception {
        Class.forName("org.postgresql.Driver");
        Connection conn = DriverManager.getConnection("jdbc:postgresql://localhost:3000/abizlocker","abizlocker","abiz_dev_secure_pass_9281");
        liquibase.database.Database db = DatabaseFactory.getInstance().findCorrectDatabaseImplementation(new JdbcConnection(conn));
        Liquibase liq = new Liquibase("liquibase/abiz-changelog.xml", new ClassLoaderResourceAccessor(), db);
        liq.update("");
        System.out.println("MIGRATION_SUCCESS");
        conn.close();
    }
}