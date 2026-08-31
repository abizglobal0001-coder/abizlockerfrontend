# ABIZ LOCKER — Production & Local Deployment Guide

## 1. Local Development Topology

```
+-------------------------------------------------------------+
|                     LOCAL HOST (WINDOWS)                    |
|                                                             |
|   React SPA (Vite)          Java Backend (Tomcat/WAR)       |
|   http://localhost:5173     http://localhost:8080           |
|            |                         |                      |
|            +---- /rest/** Proxy -----+                      |
|                                      |                      |
|                                      v                      |
|                            PostgreSQL 18 Service            |
|                            localhost:3000                   |
+--------------------------------------+----------------------+
                                       |
                                       v
                             Cloudflare Tunnel (HTTPS)
                             https://<subdomain>.trycloudflare.com
                                       ^
                                       |
                     +-----------------+-----------------+
                     |                                   |
                     v                                   v
             Physical Android Test Device        Admin Browser
```

---

## 2. PostgreSQL Configuration

On this host, PostgreSQL 18 is installed locally and runs as a Windows service (`postgresql-x64-18`) on **Port 3000**.

### Database Setup:
```powershell
psql -p 3000 -U postgres -c "CREATE USER abizlocker WITH PASSWORD 'abiz_dev_pass';"
psql -p 3000 -U postgres -c "CREATE DATABASE abizlocker_dev OWNER abizlocker;"
psql -p 3000 -U postgres -c "GRANT ALL PRIVILEGES ON DATABASE abizlocker_dev TO abizlocker;"
```

### Liquibase Migrations:
The migrations execute automatically when the Java application boots (`LiquibaseJARResourceAccessor` during Guice startup). The ABIZ changelog is included via `abiz-changelog.xml`.

---

## 3. Cloudflare Tunnel (HTTPS Boundary)

Android Enterprise strictly enforces HTTPS with valid trusted CA certificates for all Device Owner enrollment downloads and API traffic. Self-signed certificates will cause enrollment failure (`PROVISIONING_FAILED_DOWNLOAD`).

### Setting up a Quick Tunnel:
```powershell
cloudflared tunnel --url http://localhost:8080
```
This yields a public HTTPS endpoint (e.g. `https://quiet-galaxy-123.trycloudflare.com`). Set this URL as `BASE_URL` in `server/build.properties`.

---

## 4. Production Deployment Topology (Staging / Production)

```
                       Internet Traffic
                              |
                              v
                +----------------------------+
                |   Cloudflare Edge (WAF)    |
                | SSL Termination / Anti-DDoS|
                +--------------+-------------+
                               |
                               v
                +----------------------------+
                |   Reverse Proxy (Caddy)    |
                +--------------+-------------+
                               |
                +--------------+-------------+
                |                            |
                v                            v
        +---------------+            +---------------+
        |  Java Backend |            |  Web Static   |
        |  Tomcat / WAR |            |  (Vite Build) |
        +-------+-------+            +---------------+
                |
                v
        +---------------+
        | PostgreSQL 18 |
        | Managed DB    |
        +---------------+
```

### Critical Production Environment Variables:
- `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER`, `DB_PASSWORD`
- `BASE_URL` (production domain with HTTPS)
- `HASH_SECRET` (OpenSSL random 24 bytes)
- `ABIZ_JWT_SECRET` (OpenSSL random 32 bytes)
- `SECURE_ENROLLMENT=1`
