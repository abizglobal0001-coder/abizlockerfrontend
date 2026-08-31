package com.abiz.locker.tools;

import liquibase.Liquibase;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.resource.FileSystemResourceAccessor;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;

public class RunLiquibase {
    public static void main(String[] args) {
        String url = args.length > 0 ? args[0] : "jdbc:postgresql://localhost:3000/abizlocker";
        String user = args.length > 1 ? args[1] : "abizlocker";
        String pass = args.length > 2 ? args[2] : "abiz_dev_secure_pass_9281";
        String changelogPath = "C:/Users/MNA/.gemini/antigravity/scratch/abiz-locker/mdmesh/server/src/main/resources/liquibase/db.changelog.xml";

        System.out.println("Connecting to " + url + " as " + user + "...");
        try (Connection conn = DriverManager.getConnection(url, user, pass)) {
            Database db = DatabaseFactory.getInstance().findCorrectDatabaseImplementation(new JdbcConnection(conn));
            File changelogFile = new File(changelogPath);
            FileSystemResourceAccessor accessor = new FileSystemResourceAccessor(changelogFile.getParentFile().getAbsolutePath());
            Liquibase liquibase = new Liquibase(changelogFile.getName(), accessor, db);

            System.out.println("Validating Liquibase changelogs...");
            liquibase.validate();
            System.out.println("Validation passed. Applying migrations with contexts 'common,private'...");
            liquibase.update("common,private");
            System.out.println("MIGRATIONS_SUCCESS");
        } catch (Exception e) {
            System.err.println("MIGRATIONS_FAILED: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}
