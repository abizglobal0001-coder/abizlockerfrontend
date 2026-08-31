# ABIZ LOCKER — Database Architecture

## Migration System

**Tool**: Liquibase 3.6.3 (as shipped with MDMesh/Headwind MDM)
**File**: `mdmesh/server/src/main/resources/liquibase/db.changelog.xml`
**Format**: Single XML file with date-prefixed changeset IDs
**Convention**: `YY.MM.DD-slug` (MDMesh uses this format throughout)

### ABIZ Locker Changeset ID Convention
To guarantee no collision with MDMesh changesets:

```
abiz-YY.MM.DD-table-or-column-slug
```

Examples:
- `abiz-26.08.29-shops`
- `abiz-26.08.29-wallets`
- `abiz-26.08.30-contracts`

MDMesh changesets use `22.xx.xx-*` as last IDs. ABIZ IDs starting `abiz-` are lexicographically separate.
All ABIZ changesets use `context="abiz"` (in addition to or separate from `context="common"`).

---

## MDMesh Existing Tables (DO NOT MODIFY)

| Table | Purpose |
|-------|---------|
| users | MDMesh admin users |
| devices | Enrolled Android devices |
| configurations | Device configuration profiles |
| applications | APK catalog |
| applicationVersions | APK version records |
| agentCommand | Commands sent to devices |
| agentEnrollmentToken | QR enrollment tokens |
| agentRollout | APK rollout management |
| agentRolloutCanary | Canary device set |
| device_state | Latest device telemetry snapshot |
| device_event | Device lifecycle event stream |
| device_location | GPS breadcrumb trail |
| groups | Device groups |
| userGroups | User/group assignments |
| customers | MDMesh customer tenant (NOTE: this is MDMesh "customer" = organization/tenant, NOT ABIZ customer) |

---

## ABIZ Locker Tables (New — V200+)

### abiz_shops

```sql
CREATE TABLE abiz_shops (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name        VARCHAR(200)  NOT NULL,
    owner_name  VARCHAR(200)  NOT NULL,
    phone       VARCHAR(30)   NOT NULL,
    email       VARCHAR(255),
    address     TEXT,
    city        VARCHAR(100),
    status      VARCHAR(20)   NOT NULL DEFAULT 'ACTIVE',
    created_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT abiz_shops_status_chk CHECK (status IN ('ACTIVE','SUSPENDED','PENDING'))
);
```

### abiz_shop_users

```sql
CREATE TABLE abiz_shop_users (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    shop_id        UUID NOT NULL REFERENCES abiz_shops(id),
    name           VARCHAR(200)  NOT NULL,
    email          VARCHAR(255)  NOT NULL UNIQUE,
    password_hash  VARCHAR(255)  NOT NULL,  -- bcrypt
    role           VARCHAR(20)   NOT NULL DEFAULT 'OWNER',
    status         VARCHAR(20)   NOT NULL DEFAULT 'ACTIVE',
    last_login_at  TIMESTAMP,
    created_at     TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT abiz_shop_users_role_chk   CHECK (role   IN ('OWNER','STAFF')),
    CONSTRAINT abiz_shop_users_status_chk CHECK (status IN ('ACTIVE','SUSPENDED'))
);
```

### abiz_wallets

```sql
CREATE TABLE abiz_wallets (
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    shop_id    UUID NOT NULL UNIQUE REFERENCES abiz_shops(id),
    balance    INTEGER NOT NULL DEFAULT 0,   -- credit units (never fractional)
    version    INTEGER NOT NULL DEFAULT 0,   -- optimistic lock version
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT abiz_wallets_balance_chk CHECK (balance >= 0)
);
```

### abiz_wallet_transactions (append-only ledger)

```sql
CREATE TABLE abiz_wallet_transactions (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    shop_id         UUID NOT NULL REFERENCES abiz_shops(id),
    type            VARCHAR(30)   NOT NULL,
    amount          INTEGER NOT NULL,        -- positive = credit, negative = debit
    balance_before  INTEGER NOT NULL,
    balance_after   INTEGER NOT NULL,
    reference       VARCHAR(255),            -- external reference
    reason          TEXT,
    performed_by    UUID,                    -- NULL = system (scheduler/auto)
    related_id      UUID,                    -- enrollment_session_id or payment_id
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT abiz_wallet_tx_type_chk CHECK (type IN (
        'CREDIT_ADD','DEVICE_ACTIVATION_RESERVE','DEVICE_ACTIVATION_CONFIRM',
        'DEVICE_ACTIVATION_REFUND','ADMIN_ADJUSTMENT','RENEWAL'
    ))
);
CREATE INDEX abiz_wallet_tx_shop_idx ON abiz_wallet_transactions(shop_id, created_at DESC);
```

### abiz_customers

```sql
CREATE TABLE abiz_customers (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    shop_id        UUID NOT NULL REFERENCES abiz_shops(id),
    name           VARCHAR(200)  NOT NULL,
    phone          VARCHAR(30)   NOT NULL,
    alternate_phone VARCHAR(30),
    cnic           VARCHAR(20),             -- National ID
    email          VARCHAR(255),
    address        TEXT,
    city           VARCHAR(100),
    notes          TEXT,
    status         VARCHAR(20)   NOT NULL DEFAULT 'ACTIVE',
    created_at     TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT abiz_customers_status_chk CHECK (status IN ('ACTIVE','INACTIVE'))
);
CREATE INDEX abiz_customers_shop_idx ON abiz_customers(shop_id);
```

### abiz_financed_devices

```sql
CREATE TABLE abiz_financed_devices (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    shop_id             UUID NOT NULL REFERENCES abiz_shops(id),
    customer_id         UUID REFERENCES abiz_customers(id),
    contract_id         UUID,               -- FK added after contract table
    imei1               VARCHAR(20),
    imei2               VARCHAR(20),
    serial_number       VARCHAR(100),
    manufacturer        VARCHAR(100),
    model               VARCHAR(200),
    android_version     VARCHAR(20),
    agent_version       VARCHAR(30),
    mdm_device_number   VARCHAR(255),       -- FK to MDMesh devices.number
    enrollment_status   VARCHAR(30)  NOT NULL DEFAULT 'NOT_ENROLLED',
    device_owner_active BOOLEAN NOT NULL DEFAULT FALSE,
    connection_status   VARCHAR(20)  NOT NULL DEFAULT 'OFFLINE',
    last_seen_at        TIMESTAMP,
    battery_pct         INTEGER,
    storage_used_mb     BIGINT,
    network_type        VARCHAR(20),
    lock_status         VARCHAR(20)  NOT NULL DEFAULT 'UNLOCKED',
    license_status      VARCHAR(20)  NOT NULL DEFAULT 'INACTIVE',
    created_at          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT abiz_financed_devices_enrollment_chk    CHECK (enrollment_status  IN ('NOT_ENROLLED','PENDING','ENROLLED','FAILED','REVOKED')),
    CONSTRAINT abiz_financed_devices_connection_chk    CHECK (connection_status   IN ('ONLINE','OFFLINE')),
    CONSTRAINT abiz_financed_devices_lock_chk          CHECK (lock_status         IN ('UNLOCKED','WARNING','LOCKED')),
    CONSTRAINT abiz_financed_devices_license_chk       CHECK (license_status      IN ('INACTIVE','ACTIVE','EXPIRED'))
);
CREATE INDEX abiz_financed_devices_shop_idx ON abiz_financed_devices(shop_id);
CREATE INDEX abiz_financed_devices_mdm_idx  ON abiz_financed_devices(mdm_device_number);
```

### abiz_device_licenses

```sql
CREATE TABLE abiz_device_licenses (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    financed_device_id UUID NOT NULL REFERENCES abiz_financed_devices(id),
    shop_id          UUID NOT NULL REFERENCES abiz_shops(id),
    activated_at     TIMESTAMP,
    expires_at       TIMESTAMP,
    status           VARCHAR(20) NOT NULL DEFAULT 'INACTIVE',
    credits_consumed INTEGER NOT NULL DEFAULT 0,
    created_at       TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT abiz_device_licenses_status_chk CHECK (status IN ('INACTIVE','ACTIVE','EXPIRED','REVOKED'))
);
```

### abiz_contracts

```sql
CREATE TABLE abiz_contracts (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    shop_id                 UUID NOT NULL REFERENCES abiz_shops(id),
    customer_id             UUID NOT NULL REFERENCES abiz_customers(id),
    financed_device_id      UUID REFERENCES abiz_financed_devices(id),
    device_price            NUMERIC(12,2) NOT NULL,
    down_payment            NUMERIC(12,2) NOT NULL DEFAULT 0,
    financed_amount         NUMERIC(12,2) NOT NULL,
    installment_amount      NUMERIC(12,2) NOT NULL,
    frequency               VARCHAR(20)   NOT NULL DEFAULT 'MONTHLY',
    number_of_installments  INTEGER NOT NULL,
    first_due_date          DATE NOT NULL,
    grace_period_days       INTEGER NOT NULL DEFAULT 3,
    outstanding_balance     NUMERIC(12,2) NOT NULL,
    status                  VARCHAR(20)   NOT NULL DEFAULT 'DRAFT',
    created_at              TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at              TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT abiz_contracts_frequency_chk CHECK (frequency IN ('WEEKLY','MONTHLY','CUSTOM')),
    CONSTRAINT abiz_contracts_status_chk    CHECK (status    IN ('DRAFT','ACTIVE','COMPLETED','OVERDUE','DEFAULTED','CANCELLED'))
);
CREATE INDEX abiz_contracts_shop_idx     ON abiz_contracts(shop_id);
CREATE INDEX abiz_contracts_customer_idx ON abiz_contracts(customer_id);
```

### abiz_installments

```sql
CREATE TABLE abiz_installments (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    contract_id      UUID NOT NULL REFERENCES abiz_contracts(id),
    installment_num  INTEGER NOT NULL,
    due_date         DATE NOT NULL,
    amount           NUMERIC(12,2) NOT NULL,
    paid_amount      NUMERIC(12,2) NOT NULL DEFAULT 0,
    remaining_amount NUMERIC(12,2) NOT NULL,
    status           VARCHAR(20) NOT NULL DEFAULT 'UPCOMING',
    paid_at          TIMESTAMP,
    created_at       TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT abiz_installments_status_chk CHECK (status IN (
        'UPCOMING','DUE','GRACE_PERIOD','OVERDUE','PAID','PARTIALLY_PAID','WAIVED'
    )),
    UNIQUE(contract_id, installment_num)
);
CREATE INDEX abiz_installments_contract_idx  ON abiz_installments(contract_id);
CREATE INDEX abiz_installments_due_date_idx  ON abiz_installments(due_date, status);
```

### abiz_payments

```sql
CREATE TABLE abiz_payments (
    id                 UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    shop_id            UUID NOT NULL REFERENCES abiz_shops(id),
    customer_id        UUID NOT NULL REFERENCES abiz_customers(id),
    contract_id        UUID NOT NULL REFERENCES abiz_contracts(id),
    installment_id     UUID REFERENCES abiz_installments(id),
    financed_device_id UUID REFERENCES abiz_financed_devices(id),
    amount             NUMERIC(12,2) NOT NULL,
    payment_method     VARCHAR(20)   NOT NULL DEFAULT 'CASH',
    reference          VARCHAR(255),
    notes              TEXT,
    recorded_by        UUID NOT NULL REFERENCES abiz_shop_users(id),
    created_at         TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT abiz_payments_method_chk CHECK (payment_method IN ('CASH','BANK','ONLINE','OTHER'))
);
CREATE INDEX abiz_payments_shop_idx     ON abiz_payments(shop_id);
CREATE INDEX abiz_payments_contract_idx ON abiz_payments(contract_id);
```

### abiz_device_policy_states

```sql
CREATE TABLE abiz_device_policy_states (
    id                 UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    financed_device_id UUID NOT NULL REFERENCES abiz_financed_devices(id),
    state              VARCHAR(30) NOT NULL,
    changed_at         TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    changed_by         UUID,          -- shop_user id or NULL for system
    reason             TEXT,
    CONSTRAINT abiz_device_policy_states_state_chk CHECK (state IN (
        'ACTIVE','WARNING','GRACE_PERIOD','OVERDUE','LOCKED','SUSPENDED','RELEASED'
    ))
);
CREATE INDEX abiz_device_policy_states_device_idx ON abiz_device_policy_states(financed_device_id, changed_at DESC);
```

### abiz_enrollment_sessions

```sql
CREATE TABLE abiz_enrollment_sessions (
    id                 UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    shop_id            UUID NOT NULL REFERENCES abiz_shops(id),
    customer_id        UUID NOT NULL REFERENCES abiz_customers(id),
    contract_id        UUID NOT NULL REFERENCES abiz_contracts(id),
    financed_device_id UUID NOT NULL REFERENCES abiz_financed_devices(id),
    qr_token           VARCHAR(255) NOT NULL UNIQUE,
    status             VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    credit_reservation_tx_id UUID,    -- wallet_transaction id for reserved credit
    expires_at         TIMESTAMP NOT NULL,
    created_at         TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at       TIMESTAMP,
    CONSTRAINT abiz_enrollment_sessions_status_chk CHECK (status IN (
        'PENDING','WAITING','DETECTED','PROVISIONING','ENROLLED','FAILED','EXPIRED','CANCELLED'
    ))
);
```

### abiz_audit_logs (append-only)

```sql
CREATE TABLE abiz_audit_logs (
    id          BIGSERIAL PRIMARY KEY,
    timestamp   TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    actor_id    UUID,
    actor_role  VARCHAR(30),
    actor_email VARCHAR(255),
    shop_id     UUID,
    action      VARCHAR(100) NOT NULL,
    target_type VARCHAR(50),
    target_id   UUID,
    result      VARCHAR(20)  NOT NULL DEFAULT 'SUCCESS',
    detail      TEXT,
    ip_address  VARCHAR(45),
    CONSTRAINT abiz_audit_logs_result_chk CHECK (result IN ('SUCCESS','FAILURE','PARTIAL'))
);
CREATE INDEX abiz_audit_logs_shop_ts_idx ON abiz_audit_logs(shop_id, timestamp DESC);
CREATE INDEX abiz_audit_logs_action_idx  ON abiz_audit_logs(action, timestamp DESC);
```

---

## Tenant Isolation Rules

Every query against an ABIZ table for a SHOP_OWNER context MUST include `AND shop_id = :shopId`.

The backend `AbizTenantContext` injects the authenticated shop_id into every request. The MyBatis mappers use `<if test="shopId != null">AND shop_id = #{shopId}</if>` patterns.

A SUPER_ADMIN may omit the shop_id filter to access all data.

---

## Transaction Safety Rules

1. Wallet credit changes: ALWAYS use a transaction + SELECT FOR UPDATE on wallet row
2. Enrollment credit reservation: transactional (reserve → create session); rollback = refund
3. Payment recording: transactional (payment + installment update + contract balance update)
4. Audit log: written AFTER the main operation succeeds; failure to write audit must not roll back the main transaction (best-effort audit)
5. Policy state transitions: logged to abiz_device_policy_states (append-only history)
