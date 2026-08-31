# DATABASE DEPLOYMENT GUIDE
## PostgreSQL 18 Production Provisioning & Liquibase

---

### 1. Database Creation & Permissions
```sql
CREATE ROLE abizlocker_app WITH LOGIN PASSWORD '<STRONG_PASSWORD>';
CREATE DATABASE abizlocker OWNER abizlocker_app;
REVOKE ALL ON SCHEMA public FROM PUBLIC;
GRANT ALL ON SCHEMA public TO abizlocker_app;
```

---

### 2. Migration Execution
Apply all changesets through Phase 22:
```bash
mvn liquibase:update -Dliquibase.url="jdbc:postgresql://<PG_HOST>:5432/abizlocker" \
                     -Dliquibase.username="abizlocker_app" \
                     -Dliquibase.password="<STRONG_PASSWORD>"
```
All 9 Phase 22 tables and 22 performance indexes will be verified.
