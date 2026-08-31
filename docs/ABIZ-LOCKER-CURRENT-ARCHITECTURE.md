# ABIZ LOCKER — Current MDMesh Architecture Analysis

> **Purpose**: Comprehensive inspection of the MDMesh open-source codebase as the foundation for ABIZ Locker.
> **Date**: 2026-08-29
> **Repository**: https://github.com/MDMesh-app/MDMesh (Apache-2.0)

---

## 1. Repository Overview

MDMesh is a modern, self-hosted Android MDM platform forked from **Headwind MDM** (Apache-2.0).
Organized as a **monorepo** with four cooperating planes.

### Repository Root Structure

```
MDMesh/
├── server/               # PLANE 1 — Java control plane (Jersey/Guice/MyBatis/PostgreSQL)
├── common/               # Shared Java models, DAOs, utilities
├── jwt/                  # JWT token module
├── notification/         # Push notification module
├── plugins/              # Plugin extension points
├── web/                  # PLANE 2 — React + Vite + TypeScript admin frontend (early stage)
├── agent-android/        # PLANE 3 — Kotlin Device Owner Android agent
│   ├── app/              # AdminReceiver, MainActivity, CheckInService
│   ├── core/             # API, commands, check-in, device identity
│   ├── policy/           # DevicePolicyManager abstraction layer
│   ├── kiosk/            # COSU / kiosk lock-task
│   ├── oem/              # OEM adapters (GenericOemAdapter, KnoxAdapter)
│   ├── proto/            # Shared protocol types (@Serializable)
│   ├── remote/           # WebRTC remote control (future)
│   ├── gradlew           # Gradle Wrapper (no Android Studio required)
│   └── gradlew.bat
├── proto/                # Shared protocol: capability matrix + command model (source of truth)
├── docs/                 # Architecture docs, ADRs, screenshots
├── docker/               # Dockerfiles
├── docker-compose.yml    # Production: PostgreSQL + MDMesh server + Caddy
├── docker-compose.dev.yml
├── .env.example
├── pom.xml               # Root Maven multi-module POM
├── LICENSE               # Apache-2.0
└── NOTICE                # Attribution: Headwind MDM (Apache-2.0)
```

---

## 2. PLANE 1 — Control Plane (Java Backend)

### Technology Stack
| Layer | Technology |
|-------|-----------|
| Runtime | JDK 17 |
| Web Framework | Jersey (JAX-RS) |
| DI | Guice |
| ORM/SQL | MyBatis |
| Database | PostgreSQL 14 |
| Build | Maven multi-module (server/ common/ jwt/ notification/ plugins/) |
| Packaging | WAR (launcher.war) in Tomcat |
| Auth | Session-cookie (JSESSIONID) + optional RSA password mode |
| API | REST at /rest/ prefix; response envelope {status, message, data} |

### Key API Endpoints
| Endpoint | Purpose |
|----------|---------|
| GET /rest/public/auth/options | Auth options (RSA public key) |
| POST /rest/public/auth/login | Login {login, password} |
| POST /rest/public/auth/logout | Logout |
| POST /rest/private/devices/search | Device list query |
| /agent/v1/* | Versioned device agent API (additive-only contract) |

### Agent Protocol
- Versioned at /agent/v1/ — additive-only, golden contract test in CI
- Older agents continue working across server updates
- Commands are capability-gated via CapabilityMatrix
- Server cannot send commands requiring capabilities not advertised by agent

---

## 3. PLANE 2 — Web Console (React Frontend)

### Technology Stack
| Layer | Technology |
|-------|-----------|
| Framework | React 18 + TypeScript |
| Build | Vite 5 |
| Routing | react-router v6 |
| Styling | Hand-written CSS (no component library) |

### Current Status — Early Scaffold Only
Only Login + Device List pages. Proxies to Java server at :8080.

```
web/src/
├── api/         client.ts, auth.ts (MD5 hashing), devices.ts
├── auth/        AuthContext.tsx, ProtectedRoute.tsx
└── pages/       LoginPage.tsx, DevicesPage.tsx
```

---

## 4. PLANE 3 — Android Agent (Kotlin)

### Technology Stack
| Layer | Technology |
|-------|-----------|
| Language | Kotlin |
| Min SDK | 24 (Android 7.0) |
| Target/Compile SDK | 35 |
| DI | Hilt |
| Async | Coroutines + Flow |
| Local Storage | Room + DataStore |
| Background | WorkManager |
| Network | Retrofit + kotlinx.serialization |
| Build | Gradle Wrapper (gradlew/gradlew.bat) — no Android Studio needed |

### Module Structure
| Module | Key Classes | Purpose |
|--------|-------------|---------|
| :app | Application, AdminReceiver, MainActivity, CheckInService | App entry, Device Owner receiver |
| :core | MdmApi, CapabilityCollector, CommandDispatcher, DeviceIdStore, CheckInCoordinator, CheckInWorker | Server comms, check-in, commands |
| :policy | DeviceControl, PolicyStrategy, WifiPolicy, CapabilityRegistry | DevicePolicyManager abstraction |
| :kiosk | KioskController, CrashLoopGuard | COSU/lock-task |
| :oem | OemAdapter, GenericOemAdapter, KnoxAdapter | OEM behavior |
| :remote | RemoteControlSession, InputInjectionService | WebRTC (future) |
| :proto | CapabilityMatrix, CommandEnvelope, CommandResult, ProtocolJson | Protocol types |

### Capability Architecture
- Each policy area: interface + multiple PolicyStrategy implementations
- Factory selects supported strategy at runtime
- CapabilityRegistry reports only working capabilities
- Unknown commands degrade to status=unsupported (never crash)

### Device Identity
- Server-issued device ID (DataStore) — no IMEI/serial (restricted Android 10+)
- DO binding tied to signing certificate

### Build Commands
```batch
rem One-time wrapper setup:
gradle wrapper --gradle-version 8.10.2

rem Debug APK:
gradlew.bat assembleDebug
rem Output: agent-android/app/build/outputs/apk/debug/app-debug.apk

rem Release APK:
gradlew.bat assembleRelease

rem Tests:
gradlew.bat test
```

---

## 5. Infrastructure

### docker-compose.yml
```yaml
services:
  postgres:      # PostgreSQL 14 (no host ports)
  server:        # MDMesh Java server (WAR)
  caddy:         # TLS termination
  cloudflared:   # Cloudflare Tunnel (profile: cloudflare)
```

### Key Environment Variables
| Variable | Purpose |
|----------|---------|
| DB_NAME, DB_USER, DB_PASSWORD | PostgreSQL credentials |
| BASE_URL | Public base URL (embedded in enrollment QR) |
| HASH_SECRET | Signing secret |
| SECURE_ENROLLMENT | Enforce signed enrollment (0 or 1) |
| CF_TUNNEL_TOKEN | Cloudflare Tunnel token |

---

## 6. License & Attribution

| Item | Value |
|------|-------|
| License | Apache-2.0 |
| NOTICE | Derivative work of Headwind MDM (Apache-2.0) |
| Obligation | Preserve Apache-2.0 NOTICE per section 4d |

---

## 7. MDMesh Capabilities vs ABIZ Requirements

### MDMesh Provides ✅
- QR-code Device Owner enrollment
- Modern Kotlin Device Owner agent
- Capability-negotiated command protocol
- Device telemetry (battery, storage, network, Android version)
- Remote device lock command
- Kiosk / COSU lock-task (setLockTaskPackages)
- Silent app install/upgrade via Device Owner
- Location tracking
- Reliable connectivity / wake channel (WorkManager)
- PostgreSQL-backed server (Jersey/Guice/MyBatis)
- React + Vite + TypeScript frontend (seed)
- Gradle Wrapper (CLI-only builds, no Android Studio needed)
- Docker Compose deployment
- Cloudflare Tunnel support

### MDMesh Does NOT Provide (ABIZ Must Add) ❌
- Multi-tenancy / shop isolation
- Shop management (create/suspend/activate)
- Credit / wallet system (ledger-based accounting)
- Device licensing (activation credits)
- Customer management
- Installment contracts and payment schedules
- Payment recording
- Device policy state machine (ACTIVE→WARNING→GRACE→LOCKED→RELEASED)
- Financing-specific lock/unlock flow (payment-gated authorization)
- RBAC (Super Admin / Shop Owner / Shop Staff)
- Business audit logging
- ABIZ Locker branded UI/UX
- Android payment warning and lock screens (ABIZ branded)
- Multi-step enrollment wizard
- Reports and analytics
- Notification system (payment due, overdue, low credits)
