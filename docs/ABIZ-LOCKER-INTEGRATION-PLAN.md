# ABIZ LOCKER — Integration Plan

> This document describes exactly how ABIZ Locker business layers integrate with the MDMesh foundation.
> Implementation follows the 24-phase order from the specification.

---

## Overview

```
ABIZ Locker = MDMesh MDM Foundation + ABIZ Business Layer

MDMesh provides:   Device Owner enrollment, commands, telemetry, offline policy
ABIZ adds:         Multi-tenancy, shops, wallets, credits, customers, contracts,
                   installments, payments, device licensing, policy state machine,
                   RBAC, audit logs, branded UI
```

---

## Repository Target Structure

```
abiz-locker/                       # This project root
├── docs/                          # Architecture & operational docs (this folder)
├── server/                        # MDMesh Java backend (git subtree or symlink from MDMesh)
│   └── src/main/java/
│       ├── com/hmdm/             # UNTOUCHED MDMesh code
│       └── com/abiz/locker/      # NEW ABIZ business modules
│           ├── auth/             # ABIZ RBAC, JWT
│           ├── shop/             # Shop management
│           ├── wallet/           # Credit/wallet ledger
│           ├── customer/         # Customer management
│           ├── device/           # ABIZ device wrapper
│           ├── contract/         # Installment contracts
│           ├── installment/      # Installment schedule
│           ├── payment/          # Payment recording
│           ├── license/          # Device licensing
│           ├── policy/           # Policy state machine
│           ├── command/          # ABIZ command authorization
│           ├── enrollment/       # Enrollment session + credit reservation
│           ├── audit/            # Audit logging
│           └── api/              # Jersey resources
├── web/                          # ABIZ Locker React frontend
│   └── src/
│       ├── design/               # ABIZ design system tokens, CSS variables
│       ├── assets/logos/         # ABIZ logo variants
│       ├── components/           # Shared UI components
│       ├── pages/
│       │   ├── super-admin/      # Super Admin pages
│       │   └── shop-owner/       # Shop Owner pages
│       └── api/                  # ABIZ API clients
├── agent-android/                # MDMesh Kotlin agent (modified for ABIZ)
│   └── (renamed package: com.abiz.locker)
├── docker-compose.yml            # Extended from MDMesh
├── docker-compose.dev.yml        # Local development
└── .env.example                  # Extended with ABIZ variables
```

---

## Phase 1 — MDMesh Inspection (COMPLETE)
Result: This document set (ARCHITECTURE, REUSE-MODIFY-REPLACE, INTEGRATION-PLAN)

## Phase 2 — Architecture Documentation (COMPLETE)
Result: docs/ folder

## Phase 3 — Local Development Environment

### Services Required Locally
```yaml
services:
  postgres:     # PostgreSQL 14
  server:       # MDMesh + ABIZ Java backend
  web:          # ABIZ Locker React frontend (Vite dev server)
```

### Local Dev Setup Steps
1. Clone MDMesh as base
2. Apply ABIZ patches/extensions
3. Copy .env.example to .env, fill credentials
4. docker compose -f docker-compose.dev.yml up -d postgres
5. Run Java server: mvn package && java -jar ... (or docker compose up server)
6. Run React: cd web && npm install && npm run dev
7. Start Cloudflare Tunnel: cloudflared tunnel run
8. Build Android APK: cd agent-android && gradlew.bat assembleDebug
9. Install APK: adb install app/build/outputs/apk/debug/app-debug.apk

---

## Phase 4 — Database Schema

### New ABIZ Tables (migrations: V200.x__)

#### shops
```sql
id, name, owner_name, phone, email, address, city,
status (ACTIVE|SUSPENDED|PENDING),
created_at, updated_at
```

#### shop_users
```sql
id, shop_id, name, email, password_hash, role (OWNER|STAFF),
status, created_at, updated_at
```

#### wallets
```sql
id, shop_id, balance, created_at, updated_at
```

#### wallet_transactions
```sql
id, shop_id, type (CREDIT|DEBIT|DEVICE_ACTIVATION|RENEWAL|REFUND|ADMIN_ADJUSTMENT),
amount, balance_before, balance_after, reference, reason,
performed_by, created_at
```

#### customers
```sql
id, shop_id, name, phone, alternate_phone, email, cnic,
address, city, notes, status, created_at, updated_at
```

#### financed_devices
```sql
id, shop_id, customer_id, contract_id,
imei1, imei2, serial_number, manufacturer, model,
android_version, agent_version,
mdm_device_id (FK to MDMesh devices table),
enrollment_status, device_owner_status,
connection_status, last_seen, battery, storage, network,
lock_status, license_status, created_at, updated_at
```

#### device_licenses
```sql
id, financed_device_id, shop_id,
activation_id, activated_at, expires_at,
status (ACTIVE|EXPIRED|REVOKED),
pricing_plan, credits_consumed, created_at
```

#### contracts
```sql
id, shop_id, customer_id, financed_device_id,
device_price, down_payment, financed_amount,
installment_amount, frequency (WEEKLY|MONTHLY|CUSTOM),
number_of_installments, first_due_date, grace_period_days,
final_due_date, outstanding_balance,
status (DRAFT|ACTIVE|COMPLETED|OVERDUE|DEFAULTED|CANCELLED),
created_at, updated_at
```

#### installments
```sql
id, contract_id, due_date, amount,
paid_amount, remaining_amount,
status (UPCOMING|DUE|PAID|OVERDUE|GRACE_PERIOD),
paid_at, created_at, updated_at
```

#### payments
```sql
id, shop_id, customer_id, contract_id, installment_id, financed_device_id,
amount, payment_method (CASH|BANK|ONLINE|OTHER),
reference, notes, recorded_by, created_at
```

#### device_policy_states
```sql
id, financed_device_id, state (ACTIVE|WARNING|GRACE_PERIOD|OVERDUE|LOCKED|SUSPENDED|RELEASED),
changed_at, changed_by, reason
```

#### device_commands_abiz
```sql
id, financed_device_id, issuer, command_type,
payload (JSONB), status (PENDING|SENT|ACKNOWLEDGED|COMPLETED|FAILED|EXPIRED|CANCELLED),
created_at, sent_at, acknowledged_at, completed_at, failure_reason
```

#### enrollment_sessions
```sql
id, shop_id, customer_id, contract_id,
qr_token, status (PENDING|WAITING|DETECTED|PROVISIONING|ENROLLED|FAILED|EXPIRED),
expires_at, created_at, completed_at
```

#### audit_logs
```sql
id, timestamp, actor_id, actor_role, shop_id,
action, target_type, target_id, result, metadata (JSONB)
```

---

## Phase 5 — Authentication + RBAC

### Roles
- SUPER_ADMIN — platform-wide access
- SHOP_OWNER — own shop only
- SHOP_STAFF — limited shop access

### JWT Structure
```json
{
  "sub": "user_id",
  "role": "SHOP_OWNER",
  "shop_id": "shop_uuid",
  "iat": 1234567890,
  "exp": 1234567890
}
```

### Tenant Isolation Rule
Every data query for SHOP_OWNER/STAFF MUST include WHERE shop_id = :shopId.
Never rely on frontend filtering. Enforce on backend.

---

## Phase 6 — Multi-Tenancy

All ABIZ business entities are shop-scoped:
- customers.shop_id
- financed_devices.shop_id
- contracts.shop_id
- installments (via contract.shop_id)
- payments.shop_id
- wallets.shop_id
- wallet_transactions.shop_id
- enrollment_sessions.shop_id
- audit_logs.shop_id

Cross-tenant access = 403 Forbidden. Never a 404 that leaks existence.

---

## Phase 7-8 — Shop & Wallet Integration

### Shop → Wallet Flow
1. Super Admin creates shop
2. System automatically creates wallet with balance=0
3. Super Admin adds credits (ledger transaction: type=CREDIT)
4. Shop can spend credits on device activation

### Credit Deduction Flow
```
Enrollment request
  -> Check shop_id = contract.shop_id (tenant isolation)
  -> SELECT wallet FOR UPDATE (pessimistic lock)
  -> Check balance >= activation_cost
  -> Reserve credit (DEBIT transaction, type=DEVICE_ACTIVATION)
  -> Create enrollment session
  -> If enrollment fails: refund credit (CREDIT transaction, type=REFUND)
  -> If enrolled: mark activation complete, consume credit permanently
```

---

## Phase 9-14 — Business Entities

### Customer -> Device -> Contract -> Installments -> Payments
Linear dependency chain. Each phase depends on previous.

### Installment Schedule Generation
On contract ACTIVATION:
1. Calculate payment dates from first_due_date + frequency
2. Generate N installment records (status=UPCOMING)
3. Scheduler sets status=DUE when date arrives
4. Grace period: OVERDUE after grace_period_days past due date

---

## Phase 15 — Enrollment Integration

### ABIZ Enrollment Flow (extends MDMesh QR enrollment)
```
Shop Owner UI: Add Customer -> Add Device -> Create Contract -> Check Credits
  -> Reserve activation credit (wallet_transactions: DEBIT/pending)
  -> Create enrollment_session (status=PENDING)
  -> Generate QR via MDMesh /agent/v1/enroll endpoint (embed session_id in QR payload)
  -> Show QR + live status (polling: WAITING -> DETECTED -> PROVISIONING -> ENROLLED)
  -> Android Agent: scans QR, enrolls as Device Owner, registers with server
  -> Server: links MDMesh device to financed_device + contract
  -> Verify device identity (compare agent-reported data with shop-submitted data)
  -> Confirm credit consumption (wallet_transactions: status=CONFIRMED)
  -> Activate device_license
  -> Set policy state = ACTIVE
  -> Notify shop owner: enrollment complete
```

### Failure Handling
- If enrollment session expires: refund reserved credit
- If device identity mismatch: reject enrollment, refund credit
- If DO binding fails: rollback session, refund credit

---

## Phase 16 — Device Policy State Machine

```
States: ACTIVE -> WARNING -> GRACE_PERIOD -> OVERDUE -> LOCKED -> RELEASED

Transitions triggered by:
- Scheduler: installment due date reached -> WARNING
- Scheduler: grace period expired -> OVERDUE
- Scheduler: overdue threshold exceeded -> LOCKED (command issued)
- Payment recorded: LOCKED -> PAYMENT_VERIFIED -> UNLOCK_AUTHORIZED -> ACTIVE

On LOCKED:
  -> Issue ABIZ_LOCK command to device
  -> Show AbizDeviceLockedActivity on device
  -> Record audit log

On UNLOCKED (after payment):
  -> Issue UNLOCK command to device
  -> Update policy state = ACTIVE
  -> Record audit log
```

### Policy Timing (configurable by Super Admin)
- days_before_due_warning: default 3
- grace_period_days: default 3
- overdue_lock_delay_hours: default 24

---

## Phase 17 — Lock / Unlock Authorization Flow

### Lock (server-side)
```
Admin/Shop Owner triggers lock
  -> Validate actor has permission for this device's shop
  -> Validate contract state = OVERDUE or DEFAULTED
  -> Validate policy state eligibility
  -> Transition policy state = LOCKED
  -> Create device_command (type=ABIZ_LOCK, status=PENDING)
  -> Dispatch via MDMesh command infrastructure
  -> Android agent receives command, executes DevicePolicyManager.lockNow()
  -> Device shows AbizDeviceLockedActivity
  -> Agent acknowledges: command status=COMPLETED
  -> Record audit log
```

### Unlock (server-side)
```
Payment recorded and verified
  -> Validate payment covers outstanding installment(s)
  -> Validate current policy state = LOCKED
  -> Transition policy state = UNLOCK_AUTHORIZED
  -> Create device_command (type=UNLOCK, status=PENDING)
  -> Dispatch via MDMesh command infrastructure
  -> Agent executes: dismiss lock screen / restore normal policy
  -> Agent acknowledges: command status=COMPLETED
  -> Policy state = ACTIVE
  -> Record audit log
```

---

## Phase 18 — Offline Policy

Android agent behavior when offline:
1. Last valid policy version stored in Room database
2. Last lock state persisted in DataStore
3. If locked offline: device STAYS locked
4. Local clock: do NOT use for financial decisions (no unlock based on local time)
5. When reconnected: sync policy, upload telemetry, acknowledge pending commands

---

## Phase 19-21 — UI Implementation

### Design System
- Palette: Deep charcoal (#1a1d23), Near-black (#111214), White (#ffffff), Soft gray (#f4f5f7)
- Accent: ABIZ Blue (#1565c0) + ABIZ Cyan (#00bcd4)
- NO gradients, NO glassmorphism
- Logo: Use uploaded ABIZ Locker variants (white on dark, dark on light)
- Typography: Inter or system-ui

### Layout: Sidebar + Content
- Fixed left sidebar (240px) with ABIZ logo + nav items
- Top header with search, notifications, user menu
- Content area with page header + body

---

## ABIZ-Specific API Namespace

All new ABIZ endpoints under:
- /rest/abiz/admin/* — Super Admin (requires SUPER_ADMIN JWT)
- /rest/abiz/shop/* — Shop Owner (requires SHOP_OWNER/STAFF JWT)
- /agent/v2/abiz/* — Extended agent protocol (additive, follows MDMesh v1 rules)

Existing MDMesh endpoints are NEVER modified.

---

## Open Source Compliance

All MDMesh (Apache-2.0) attribution preserved:
- LICENSE file: Apache-2.0
- NOTICE file: Headwind MDM attribution retained
- New ABIZ code: separate package namespaces (com.abiz.locker.*)
- See /docs/OPEN-SOURCE-NOTICES.md for full compliance details
