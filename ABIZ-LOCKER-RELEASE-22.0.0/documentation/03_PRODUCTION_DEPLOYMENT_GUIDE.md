# 03 — PRODUCTION DEPLOYMENT GUIDE
## Production Infrastructure Provisioning & Handover Requirements

---

### 1. Production Architecture Boundary
`Production Ready` denotes software engineering and regression completeness. It requires physical production infrastructure provisioning prior to live commercial enrollment.

```text
[Release Package 22.0.0]
        │
        ├── Backend: launcher.war ───► [Production Servlet Container: Tomcat 9 / Jetty 10]
        │                                        │
        │                                        ▼
        │                              [PostgreSQL 18 HA Cluster]
        │
        └── Android: app-release ────► [Production Release Keystore Signing]
                                                 │
                                                 ▼
                                       [Physical Device Fleet (QR / Zero-Touch)]
```

---

### 2. Infrastructure Requirements
- **Compute:** 4 vCPU, 16 GB RAM minimum for API cluster nodes.
- **Database:** PostgreSQL 18 HA cluster with streaming replication and WAL-E / pgBackRest offsite backups.
- **Reverse Proxy:** NGINX or Cloud Load Balancer with TLS 1.3 termination and strict HSTS headers.
- **Push Notification Service:** Google Firebase Cloud Messaging (FCM) production service account JSON configured for cellular device wakeup.

---

### 3. Android Production Release Keystore Signing
The development build produces `app-release-unsigned.apk`. For commercial rollout:
1. Sign APK using production HSM / Keystore:
   ```bash
   apksigner sign --ks <PRODUCTION_KEYSTORE_PATH> --ks-key-alias <KEY_ALIAS> --out app-release-signed.apk app-release-unsigned.apk
   ```
2. Verify signature and version:
   ```bash
   apksigner verify --verbose app-release-signed.apk
   ```
