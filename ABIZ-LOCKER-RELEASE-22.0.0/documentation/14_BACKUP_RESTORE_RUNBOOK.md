# 14 — BACKUP & RESTORE OPERATIONAL RUNBOOK
## Point-in-Time Recovery (PITR) & Cryptographic Verification

---

### 1. Automated Backup Creation & Verification
1. Create PostgreSQL physical backup using pg_basebackup or pgBackRest.
2. Compute SHA-256 hash of backup archive:
   ```bash
   sha256sum backup-2026-08-31.tar.gz
   ```
3. Record backup verification in database:
   ```java
   drService.verifyBackupIntegrity(backupId, sha256Hex, byteSize, "backup_operator");
   ```

---

### 2. Restore Execution
1. Stop application servlet containers.
2. Restore database cluster to target point-in-time.
3. Validate schema compatibility and start application.
