# ABIZ LOCKER — Reuse / Modify / Replace Decision Matrix

> **Guiding principle**: Never destroy working MDM functionality to make code easier.
> Build ABIZ Locker as a commercial product **on top of** the existing MDM foundation.

---

## Decision Categories

| Symbol | Meaning |
|--------|---------|
| REUSE | Use as-is without modification |
| EXTEND | Add functionality without changing core |
| MODIFY | Change existing code |
| WRAP | Keep underlying, add ABIZ authorization layer on top |
| NEW | Build from scratch (no MDMesh equivalent) |
| DO NOT TOUCH | Leave completely unchanged |

---

## PLANE 1 — Java Control Plane (server/)

### Database / PostgreSQL
| Component | Decision | Rationale |
|-----------|----------|-----------|
| PostgreSQL 14 setup | REUSE | Same DB, add ABIZ schema in new tables |
| Existing MDMesh tables (devices, configs, users, apps) | DO NOT TOUCH | MDM functionality depends on it |
| New ABIZ tables (shops, wallets, customers, contracts, etc.) | NEW | Separate V2xx.x Flyway/Liquibase migrations |
| MyBatis mappers for existing tables | DO NOT TOUCH | Leave MDM queries intact |
| New MyBatis mappers for ABIZ tables | NEW | Add alongside existing |

### Authentication & Authorization
| Component | Decision | Rationale |
|-----------|----------|-----------|
| Existing session-cookie auth (/rest/public/auth/*) | DO NOT TOUCH | Agent enrollment depends on it |
| ABIZ JWT auth (new API endpoints) | NEW | Stateless auth for ABIZ API |
| ABIZ RBAC (Super Admin / Shop Owner / Staff) | NEW | No equivalent in MDMesh |
| Existing admin user model | EXTEND | Add ABIZ roles alongside MDMesh admin |

### Jersey REST Resources
| Component | Decision | Rationale |
|-----------|----------|-----------|
| /rest/public/auth/* | DO NOT TOUCH | Agent enrollment depends on it |
| /rest/private/devices/* | DO NOT TOUCH | MDM device management |
| /agent/v1/* | DO NOT TOUCH | CRITICAL: additive-only contract |
| New /rest/abiz/admin/* | NEW | Super Admin API |
| New /rest/abiz/shop/* | NEW | Shop Owner API |
| New /rest/abiz/enroll/* | NEW | Enrollment with credit reservation |

### Device Commands
| Component | Decision | Rationale |
|-----------|----------|-----------|
| Existing command dispatch | WRAP | ABIZ adds payment validation before issuing |
| LOCK command | WRAP | Gate behind: contract + payment + policy state machine |
| UNLOCK command | WRAP | Gate behind: payment verification + authorization |
| SYNC_POLICY, REFRESH_POLICY | REUSE | Use directly |
| SHOW_WARNING | REUSE | Trigger when installment overdue |
| New ABIZ_PAYMENT_WARNING command | NEW | Custom command for branded screens |

### Java Modules
| Module | Decision | Notes |
|--------|----------|-------|
| common/ | DO NOT TOUCH | Shared MDM models |
| jwt/ | WRAP | Reuse JWT infrastructure, add ABIZ claims |
| notification/ | EXTEND | Add ABIZ notification types |
| plugins/ | DO NOT TOUCH | Plugin system |
| server/ Jersey resources | EXTEND | Add ABIZ Guice modules alongside |

---

## PLANE 2 — React Web Console (web/)

### Overall: REPLACE UI (keeping technology stack)

The current web console is a seed (Login + Device List only). ABIZ Locker requires a complete multi-role multi-page application.

| Component | Decision | Rationale |
|-----------|----------|-----------|
| Vite + React + TypeScript setup | REUSE | Exact match with ABIZ requirement |
| API client pattern (client.ts fetch wrapper) | EXTEND | Add ABIZ API modules |
| Auth context + ProtectedRoute pattern | MODIFY | Support RBAC roles |
| LoginPage.tsx | MODIFY | Re-brand to ABIZ Locker design |
| DevicesPage.tsx | REUSE as base | Reference for MDM device data |
| Hand-written CSS approach | MODIFY | Replace with ABIZ design system |

### New Pages (all NEW — no MDMesh equivalent)
Super Admin: Dashboard, Shops, ShopDetail, ShopOwners, Wallet/Credits, CreditLedger,
             Customers, Devices, DeviceDetail, Contracts, Installments, Payments,
             DeviceCommands, Reports, AuditLogs, Settings, Licenses

Shop Owner: Dashboard, Customers, CustomerDetail, Devices, DeviceDetail,
            Contracts, ContractDetail, Installments, Payments, EnrollDevice,
            Wallet, Reports, Settings

Enrollment Wizard: 5-step (Customer → Device → Installment → License → Enrollment + QR)

---

## PLANE 3 — Android Agent (agent-android/)

### Overall: EXTEND with ABIZ branding and financial policy

| Component | Decision | Rationale |
|-----------|----------|-----------|
| Application ID | MODIFY | com.mdmesh.agent -> com.abiz.locker |
| AdminReceiver | EXTEND | Add ABIZ policy enforcement callbacks |
| CheckInService / CheckInWorker | WRAP | Add ABIZ policy sync to check-in payload |
| CommandDispatcher | EXTEND | Add ABIZ_PAYMENT_WARNING, ABIZ_LOCK, ABIZ_UNLOCK |
| :policy module | EXTEND | Add AbizLockerPolicy strategy |
| :kiosk module | REUSE | Lock-task is exactly what ABIZ needs |
| :core (MdmApi, DeviceIdStore) | EXTEND | Add AbizApi alongside MdmApi |
| :oem module | REUSE | OEM adapter pattern preserved |
| :proto module | EXTEND | Add ABIZ capability flags |
| Gradle Wrapper (gradlew.bat) | REUSE | Already supports CLI builds |
| Offline policy (Room + DataStore) | REUSE | Critical for ABIZ offline enforcement |
| Reboot recovery (WorkManager) | REUSE | Policy survives reboot |
| CrashLoopGuard | REUSE | Prevents bricking devices |

### New Android UI (all NEW)
| Screen | Notes |
|--------|-------|
| AbizPaymentWarningActivity | ABIZ branded payment warning |
| AbizDeviceLockedActivity | ABIZ branded locked screen |
| Contact shop action | Show shop phone from policy |

### Package Rename
```
com.mdmesh.agent  ->  com.abiz.locker
```
Safe because DO binding is to the signing certificate, not package name.
CRITICAL: Use dedicated ABIZ Locker release keystore. Document key custody.

---

## PLANE 4 — Remote Control

| Component | Decision |
|-----------|----------|
| :remote module | DO NOT TOUCH — parked, not needed for ABIZ MVP |

---

## Infrastructure

| Component | Decision | Notes |
|-----------|----------|-------|
| docker-compose.yml | EXTEND | Add ABIZ env vars, keep existing services |
| docker-compose.dev.yml | EXTEND | Add local dev config |
| .env.example | EXTEND | Add ABIZ-specific variables |
| Caddy TLS | REUSE | No change |
| Cloudflare Tunnel | REUSE | Use for local dev -> phone connectivity |
| setup.sh / quickstart.sh | REUSE | Keep for base server setup |

---

## What MUST NOT Be Faked (from spec)

1. Lock button — must trigger actual DevicePolicyManager via authenticated command flow
2. QR enrollment — must participate in real Device Owner provisioning
3. Device Owner status — must reflect actual Android DPC state
4. Wallet balance — must have full ledger transaction history
5. Device command acknowledgement — must be real device acknowledgement from Android agent
6. Offline lock — must persist when network is unavailable
