package com.abiz.locker.tools;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

public class InspectDb {
    public static void main(String[] args) throws Exception {
        String url = "jdbc:postgresql://localhost:3000/abizlocker";
        String user = "abizlocker";
        String pass = "abiz_dev_secure_pass_9281";

        try (Connection c = DriverManager.getConnection(url, user, pass);
             Statement st = c.createStatement()) {
            
            System.out.println("=== Existing abiz tables ===");
            try (ResultSet rs = st.executeQuery("SELECT table_name FROM information_schema.tables WHERE table_schema='public' AND table_name LIKE 'abiz_%' ORDER BY table_name")) {
                while (rs.next()) {
                    System.out.println("  " + rs.getString(1));
                }
            }

            for (String tbl : new String[]{"abiz_operational_metrics", "abiz_slo_measurements", "abiz_configuration_versions", "abiz_fleet_incidents", "abiz_worker_heartbeats", "abiz_integrity_verifications"}) {
                System.out.println("=== Columns in " + tbl + " ===");
                try (ResultSet rs = st.executeQuery("SELECT column_name, data_type FROM information_schema.columns WHERE table_name='" + tbl + "' ORDER BY ordinal_position")) {
                    while (rs.next()) {
                        System.out.println("    " + rs.getString(1) + " (" + rs.getString(2) + ")");
                    }
                }
            }
        }
    }
}
