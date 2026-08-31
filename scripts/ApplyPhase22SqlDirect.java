package com.abiz.locker.test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

public class ApplyPhase22SqlDirect {
    private static final String DB_URL = "jdbc:postgresql://localhost:3000/abizlocker";
    private static final String DB_USER = "abizlocker";
    private static final String DB_PASS = "abiz_dev_secure_pass_9281";

    public static void main(String[] args) {
        System.out.println("Connecting to database: " + DB_URL);
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
             Statement stmt = conn.createStatement()) {

            System.out.println("Applying Phase 22 tables and indexes...");

            String sql = "CREATE TABLE IF NOT EXISTS abiz_deployment_releases (" +
                    "    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid()," +
                    "    release_version         VARCHAR(50) NOT NULL," +
                    "    component               VARCHAR(50) NOT NULL DEFAULT 'BACKEND_WAR'," +
                    "    artifact_name           VARCHAR(100) NOT NULL," +
                    "    artifact_sha256         VARCHAR(128) NOT NULL," +
                    "    artifact_size           BIGINT NOT NULL DEFAULT 0," +
                    "    artifact_type           VARCHAR(30) NOT NULL DEFAULT 'WAR'," +
                    "    environment             VARCHAR(50) NOT NULL DEFAULT 'PRODUCTION'," +
                    "    channel                 VARCHAR(30) NOT NULL DEFAULT 'PRODUCTION'," +
                    "    status                  VARCHAR(30) NOT NULL DEFAULT 'DRAFT'," +
                    "    initiated_by            VARCHAR(100)," +
                    "    approved_by             VARCHAR(100)," +
                    "    created_at              TIMESTAMP NOT NULL DEFAULT NOW()," +
                    "    approved_at             TIMESTAMP," +
                    "    deployed_at             TIMESTAMP," +
                    "    verified_at             TIMESTAMP," +
                    "    rollback_at             TIMESTAMP," +
                    "    rollback_reason         TEXT," +
                    "    failure_reason          TEXT," +
                    "    metadata_json           TEXT," +
                    "    correlation_id          VARCHAR(100)" +
                    ");" +
                    "CREATE INDEX IF NOT EXISTS abiz_deployment_releases_ver_env_idx ON abiz_deployment_releases (release_version, environment, status);" +
                    "CREATE INDEX IF NOT EXISTS abiz_deployment_releases_status_idx ON abiz_deployment_releases (status, created_at DESC);" +
                    "CREATE INDEX IF NOT EXISTS abiz_deployment_releases_sha_idx ON abiz_deployment_releases (artifact_sha256);" +
                    "CREATE TABLE IF NOT EXISTS abiz_deployment_checkpoints (" +
                    "    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid()," +
                    "    deployment_id       UUID NOT NULL REFERENCES abiz_deployment_releases(id) ON DELETE CASCADE," +
                    "    checkpoint_type     VARCHAR(50) NOT NULL," +
                    "    expected_value      TEXT," +
                    "    actual_value        TEXT," +
                    "    status              VARCHAR(30) NOT NULL DEFAULT 'PENDING'," +
                    "    evidence_hash       VARCHAR(128)," +
                    "    failure_reason      TEXT," +
                    "    created_at          TIMESTAMP NOT NULL DEFAULT NOW()," +
                    "    verified_at         TIMESTAMP," +
                    "    correlation_id      VARCHAR(100)" +
                    ");" +
                    "CREATE INDEX IF NOT EXISTS abiz_deployment_checkpoints_dep_idx ON abiz_deployment_checkpoints (deployment_id, checkpoint_type);" +
                    "CREATE INDEX IF NOT EXISTS abiz_deployment_checkpoints_status_idx ON abiz_deployment_checkpoints (status, checkpoint_type);" +
                    "CREATE TABLE IF NOT EXISTS abiz_recovery_plans (" +
                    "    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid()," +
                    "    plan_name           VARCHAR(100) NOT NULL," +
                    "    version             VARCHAR(50) NOT NULL DEFAULT 'v1.0'," +
                    "    recovery_scope      VARCHAR(100) NOT NULL DEFAULT 'FULL_DATABASE'," +
                    "    rto_seconds         BIGINT NOT NULL DEFAULT 300," +
                    "    rpo_seconds         BIGINT NOT NULL DEFAULT 60," +
                    "    status              VARCHAR(30) NOT NULL DEFAULT 'DRAFT'," +
                    "    configuration_hash  VARCHAR(128)," +
                    "    approved_by         VARCHAR(100)," +
                    "    created_at          TIMESTAMP NOT NULL DEFAULT NOW()," +
                    "    activated_at        TIMESTAMP" +
                    ");" +
                    "CREATE INDEX IF NOT EXISTS abiz_recovery_plans_status_idx ON abiz_recovery_plans (status, version);" +
                    "CREATE TABLE IF NOT EXISTS abiz_recovery_executions (" +
                    "    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid()," +
                    "    recovery_plan_id    UUID REFERENCES abiz_recovery_plans(id)," +
                    "    execution_id        VARCHAR(100) NOT NULL UNIQUE," +
                    "    trigger_type        VARCHAR(50) NOT NULL DEFAULT 'SCHEDULED_REHEARSAL'," +
                    "    started_at          TIMESTAMP NOT NULL DEFAULT NOW()," +
                    "    completed_at        TIMESTAMP," +
                    "    status              VARCHAR(30) NOT NULL DEFAULT 'STARTING'," +
                    "    expected_rto        BIGINT NOT NULL DEFAULT 300," +
                    "    actual_rto          BIGINT," +
                    "    expected_rpo        BIGINT NOT NULL DEFAULT 60," +
                    "    actual_rpo          BIGINT," +
                    "    records_verified    BIGINT NOT NULL DEFAULT 0," +
                    "    corruption_detected BOOLEAN NOT NULL DEFAULT FALSE," +
                    "    evidence_hash       VARCHAR(128)," +
                    "    failure_reason      TEXT," +
                    "    correlation_id      VARCHAR(100)" +
                    ");" +
                    "CREATE INDEX IF NOT EXISTS abiz_recovery_executions_status_idx ON abiz_recovery_executions (status, started_at DESC);" +
                    "CREATE INDEX IF NOT EXISTS abiz_recovery_executions_exec_id_idx ON abiz_recovery_executions (execution_id);" +
                    "CREATE TABLE IF NOT EXISTS abiz_backup_verifications (" +
                    "    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid()," +
                    "    backup_identifier   VARCHAR(100) NOT NULL," +
                    "    backup_type         VARCHAR(30) NOT NULL DEFAULT 'FULL'," +
                    "    backup_timestamp    TIMESTAMP NOT NULL DEFAULT NOW()," +
                    "    size_bytes          BIGINT NOT NULL DEFAULT 0," +
                    "    sha256              VARCHAR(128) NOT NULL," +
                    "    database_version    VARCHAR(50) NOT NULL DEFAULT 'PostgreSQL 18.0'," +
                    "    schema_version      VARCHAR(50) NOT NULL DEFAULT '22.0'," +
                    "    verification_status VARCHAR(30) NOT NULL DEFAULT 'DISCOVERED'," +
                    "    restore_tested      BOOLEAN NOT NULL DEFAULT FALSE," +
                    "    restore_schema      VARCHAR(100)," +
                    "    restore_started_at  TIMESTAMP," +
                    "    restore_completed_at TIMESTAMP," +
                    "    created_at          TIMESTAMP NOT NULL DEFAULT NOW()" +
                    ");" +
                    "CREATE INDEX IF NOT EXISTS abiz_backup_verifications_status_idx ON abiz_backup_verifications (verification_status, backup_timestamp DESC);" +
                    "CREATE INDEX IF NOT EXISTS abiz_backup_verifications_bkid_idx ON abiz_backup_verifications (backup_identifier);" +
                    "CREATE TABLE IF NOT EXISTS abiz_configuration_drift_events (" +
                    "    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid()," +
                    "    shop_id                 UUID REFERENCES abiz_shops(id)," +
                    "    configuration_version   INT NOT NULL DEFAULT 1," +
                    "    expected_hash           VARCHAR(128) NOT NULL," +
                    "    observed_hash           VARCHAR(128) NOT NULL," +
                    "    drift_type              VARCHAR(50) NOT NULL DEFAULT 'VALUE_MISMATCH'," +
                    "    severity                VARCHAR(30) NOT NULL DEFAULT 'HIGH'," +
                    "    status                  VARCHAR(30) NOT NULL DEFAULT 'DETECTED'," +
                    "    detected_at             TIMESTAMP NOT NULL DEFAULT NOW()," +
                    "    resolved_at             TIMESTAMP," +
                    "    correlation_id          VARCHAR(100)," +
                    "    details                 TEXT" +
                    ");" +
                    "CREATE INDEX IF NOT EXISTS abiz_config_drift_status_idx ON abiz_configuration_drift_events (status, severity, detected_at DESC);" +
                    "CREATE INDEX IF NOT EXISTS abiz_config_drift_shop_idx ON abiz_configuration_drift_events (shop_id, status);" +
                    "CREATE TABLE IF NOT EXISTS abiz_device_lifecycle_events (" +
                    "    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid()," +
                    "    device_id           UUID NOT NULL REFERENCES abiz_financed_devices(id)," +
                    "    shop_id             UUID NOT NULL REFERENCES abiz_shops(id)," +
                    "    previous_state      VARCHAR(30) NOT NULL," +
                    "    new_state           VARCHAR(30) NOT NULL," +
                    "    reason              TEXT NOT NULL," +
                    "    actor               VARCHAR(100) NOT NULL," +
                    "    correlation_id      VARCHAR(100)," +
                    "    evidence_hash       VARCHAR(128)," +
                    "    created_at          TIMESTAMP NOT NULL DEFAULT NOW()" +
                    ");" +
                    "CREATE INDEX IF NOT EXISTS abiz_device_lifecycle_dev_idx ON abiz_device_lifecycle_events (device_id, created_at DESC);" +
                    "CREATE INDEX IF NOT EXISTS abiz_device_lifecycle_shop_idx ON abiz_device_lifecycle_events (shop_id, created_at DESC);" +
                    "CREATE INDEX IF NOT EXISTS abiz_device_lifecycle_states_idx ON abiz_device_lifecycle_events (previous_state, new_state);" +
                    "CREATE TABLE IF NOT EXISTS abiz_security_incidents (" +
                    "    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid()," +
                    "    shop_id             UUID REFERENCES abiz_shops(id)," +
                    "    device_id           UUID REFERENCES abiz_financed_devices(id)," +
                    "    worker_id           VARCHAR(100)," +
                    "    event_type          VARCHAR(100) NOT NULL," +
                    "    severity            VARCHAR(30) NOT NULL DEFAULT 'MEDIUM'," +
                    "    fingerprint         VARCHAR(128) NOT NULL," +
                    "    source              VARCHAR(100) NOT NULL DEFAULT 'CORRELATION_ENGINE'," +
                    "    correlation_id      VARCHAR(100)," +
                    "    payload_hash        VARCHAR(128)," +
                    "    occurrence_count    INT NOT NULL DEFAULT 1," +
                    "    first_seen          TIMESTAMP NOT NULL DEFAULT NOW()," +
                    "    last_seen           TIMESTAMP NOT NULL DEFAULT NOW()," +
                    "    status              VARCHAR(30) NOT NULL DEFAULT 'DETECTED'," +
                    "    mitigation          TEXT," +
                    "    resolved_at         TIMESTAMP," +
                    "    evidence_hash       VARCHAR(128)" +
                    ");" +
                    "CREATE INDEX IF NOT EXISTS abiz_sec_incidents_fp_idx ON abiz_security_incidents (fingerprint, status);" +
                    "CREATE INDEX IF NOT EXISTS abiz_sec_incidents_sev_status_idx ON abiz_security_incidents (status, severity, last_seen DESC);" +
                    "CREATE INDEX IF NOT EXISTS abiz_sec_incidents_shop_idx ON abiz_security_incidents (shop_id, status);" +
                    "CREATE TABLE IF NOT EXISTS abiz_production_freezes (" +
                    "    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid()," +
                    "    freeze_reason       VARCHAR(255) NOT NULL," +
                    "    freeze_type         VARCHAR(50) NOT NULL DEFAULT 'DEPLOYMENT_FREEZE'," +
                    "    is_active           BOOLEAN NOT NULL DEFAULT TRUE," +
                    "    initiated_by        VARCHAR(100) NOT NULL," +
                    "    approved_by         VARCHAR(100)," +
                    "    started_at          TIMESTAMP NOT NULL DEFAULT NOW()," +
                    "    expires_at          TIMESTAMP," +
                    "    ended_at            TIMESTAMP," +
                    "    correlation_id      VARCHAR(100)" +
                    ");" +
                    "CREATE INDEX IF NOT EXISTS abiz_prod_freezes_active_idx ON abiz_production_freezes (is_active, freeze_type);";

            stmt.execute(sql);
            System.out.println("PHASE 22 DATABASE SCHEMA APPLIED SUCCESSFULLY!");

        } catch (Exception e) {
            System.err.println("SQL EXECUTION FAILED: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}
