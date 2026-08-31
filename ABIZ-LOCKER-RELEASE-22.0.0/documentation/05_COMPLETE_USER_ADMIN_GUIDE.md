# 05 — COMPLETE USER & ADMINISTRATOR GUIDE
## Operator Manual for Web Dashboard & Shop Management

---

### 1. User Roles & Capabilities
- **SuperAdmin:** Manages platform configuration, approves deployment releases, oversees system health and disaster recovery.
- **Shop Owner:** Manages inventory, creates installment financing contracts, triggers manual lock/unlock, views revenue ledger.
- **Shop Staff:** Onboards customers, performs initial device enrollment, records in-person cash installments.

---

### 2. Core Workflows

#### 2.1 Customer & Financing Contract Creation
1. Navigate to **Customers** → **New Customer**. Enter national identity, phone, and name.
2. Navigate to **Financing Contracts** → **Create Contract**.
3. Select customer, device IMEI, device price, down payment, installment frequency (`WEEKLY`, `MONTHLY`), and number of terms.
4. System automatically generates the deterministic installment schedule and assigns grace periods.

#### 2.2 Device Lock / Unlock Operations
- **Automatic:** When an installment becomes overdue beyond the grace period (default 3 days), the backend scheduler transitions policy to `LOCKED` and dispatches a persistent lock payload.
- **Manual Unlock:** Upon payment recording, the system recalculates outstanding balances and automatically dispatches an `UNLOCK` command.
