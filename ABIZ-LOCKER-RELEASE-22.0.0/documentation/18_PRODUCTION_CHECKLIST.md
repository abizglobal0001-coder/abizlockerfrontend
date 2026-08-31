# 18 — PRODUCTION DEPLOYMENT READINESS CHECKLIST
## Formal Sign-Off Matrix for Operations Teams

---

### 1. Infrastructure Readiness
- [ ] PostgreSQL 18 HA Cluster provisioned with SSL enforcement.
- [ ] Application server (Tomcat 9 / Jetty 10) configured with Java 17 runtime.
- [ ] Reverse proxy configured with TLS 1.3 certificates and strict security headers.
- [ ] Production FCM push notification credentials configured.
- [ ] Offsite backup storage (S3/GCS) provisioned and access keys mounted.

---

### 2. Application & Mobile Readiness
- [ ] `launcher.war` SHA-256 verified against `ADC5EE780CAB0D53C003D1606D2D18FA4A8FA286AA6CF08B18A3CB4EA8CB41EC`.
- [ ] Android APK signed with production release keystore and verified via `apksigner`.
- [ ] Initial SuperAdmin account initialized with complex credentials.
- [ ] Disaster recovery drill rehearsed and verified within 300s RTO SLA.
