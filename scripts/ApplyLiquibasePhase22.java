package com.abiz.locker.test;

import liquibase.Liquibase;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.resource.ClassLoaderResourceAccessor;

import java.sql.Connection;
import java.sql.DriverManager;

public class ApplyLiquibasePhase22 {
    private static final String DB_URL = "jdbc:postgresql://localhost:3000/abizlocker";
    private static final String DB_USER = "abizlocker";
    private static final String DB_PASS = "abiz_dev_secure_pass_9281";

    public static void main(String[] args) {
        System.out.println("Connecting to database: " + DB_URL);
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS)) {
            Database database = DatabaseFactory.getInstance().findCorrectDatabaseImplementation(new JdbcConnection(conn));
            Liquibase liquibase = new Liquibase("liquibase/abiz-changelog.xml", new ClassLoaderResourceAccessor(ApplyLiquibasePhase22.class.getClassLoader()), database);
            System.out.println("Running liquibase.update()...");
            liquibase.update("");
            System.out.println("LIQUIBASE UPDATE SUCCESSFUL!");
        } catch (Exception e) {
            System.err.println("LIQUIBASE UPDATE FAILED: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}
