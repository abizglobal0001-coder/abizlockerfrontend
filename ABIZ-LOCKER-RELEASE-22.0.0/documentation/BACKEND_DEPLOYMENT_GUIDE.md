# BACKEND DEPLOYMENT GUIDE
## Servlet Container Deployment (`launcher.war`)

---

### 1. Deployment Target Requirements
- **Runtime:** Java 11 or Java 17 LTS (Java 17 recommended).
- **Servlet Container:** Apache Tomcat 9.0.x / 10.1.x or Eclipse Jetty 10/11.
- **Environment Variables:**
  - `ABIZ_DB_URL`: `jdbc:postgresql://<PG_HOST>:5432/abizlocker?ssl=true`
  - `ABIZ_DB_USER`: `abizlocker_app`
  - `ABIZ_DB_PASS`: `<VAULT_RETRIEVED_DB_PASSWORD>`
  - `ABIZ_JWT_SECRET`: `<VAULT_RETRIEVED_HS512_SECRET>`

---

### 2. Rollout Procedure
1. Copy `launcher.war` to `<TOMCAT_HOME>/webapps/`.
2. Start Tomcat service:
   ```bash
   systemctl start tomcat9
   ```
3. Verify application readiness via health check:
   ```bash
   curl -i http://localhost:8080/launcher/api/health/ready
   ```
