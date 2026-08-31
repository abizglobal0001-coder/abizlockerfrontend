# ABIZ LOCKER — API Architecture & Contracts

## 1. Boundary Principle: MDMesh vs ABIZ

ABIZ Locker builds strictly on top of MDMesh while maintaining a clean architectural separation.

```
+-------------------------------------------------------------+
|                     ABIZ LOCKER CLIENT                      |
| (Web Console SPA: React/TS)   (Android Device: Kotlin DO)   |
+------------------------------+------------------------------+
               |                               |
        REST / JSON                     HTTP / JSON
               v                               v
+------------------------------+------------------------------+
|     ABIZ REST LAYER          |   MDMESH AGENT CONTRACT      |
|     /rest/abiz/**            |   /agent/v1/** (FROZEN/GOLD) |
| (Auth, Shops, Wallet,        | (Check-in, Command ACK,      |
|  Installments, Policy)       |  Config push, Telemetry)     |
+------------------------------+------------------------------+
               |                               |
               v                               v
+-------------------------------------------------------------+
|                     JAVA BACKEND CORE                       |
|   com.abiz.locker.*                  com.hmdm.*             |
|   (Business logic, Quartz,           (Base MDM controller,  |
|    Tenant context, Ledger)            Jersey, Guice, MyBatis)|
+-------------------------------------------------------------+
                               |
                               v
+-------------------------------------------------------------+
|                   POSTGRESQL 18 DATABASE                    |
|   abiz_* tables (15 new)             MDMesh base tables     |
+-------------------------------------------------------------+
```

### HARD COMPATIBILITY BOUNDARIES:
1. `/agent/v1/*` is a **frozen golden contract**. NEVER alter query parameters, response structures, or endpoint paths. MDMesh agents rely on strict parsing.
2. `/rest/abiz/*` is the **ABIZ business API surface**. All new ABIZ endpoints reside here.
3. Authentications are isolated:
   - MDMesh web/console uses session cookie `JSESSIONID`.
   - MDMesh agent check-in uses SHA-256 device secret authentication.
   - ABIZ web console and API consumers use stateless JWT (`Authorization: Bearer <token>`).

---

## 2. Authentication & Authorization (/rest/abiz/auth)

### Endpoints
- `POST /rest/abiz/auth/login`
  - Body: `{"email": "...", "password": "..."}`
  - Response:
    ```json
    {
      "accessToken": "ey...",
      "refreshToken": "ey...",
      "expiresInSeconds": 900,
      "role": "SUPER_ADMIN|SHOP_OWNER|SHOP_STAFF",
      "shopId": "uuid-or-null",
      "email": "user@abizlocker.com",
      "name": "Shop Admin"
    }
    ```
- `POST /rest/abiz/auth/refresh`
  - Body: `{"refreshToken": "..."}`
  - Response: `{"accessToken": "...", "expiresInSeconds": 900}`
- `POST /rest/abiz/auth/logout`
  - Body: `{"refreshToken": "..."}`
  - Response: `{"message": "Logged out"}`
- `GET /rest/abiz/auth/me`
  - Headers: `Authorization: Bearer <accessToken>`
  - Response: `{"userId": "...", "role": "...", "shopId": "...", "email": "..."}`

---

## 3. Super Admin API Surface (/rest/abiz/admin/*)

Requires role `SUPER_ADMIN`. Cross-tenant platform administration.

### Shops Management
- `GET /rest/abiz/admin/shops` — List all shops with summary stats (devices, balance, overdue count).
- `POST /rest/abiz/admin/shops` — Create a new shop + initialize its wallet with 0 credits.
- `GET /rest/abiz/admin/shops/{shopId}` — Get detailed shop profile.
- `PUT /rest/abiz/admin/shops/{shopId}` — Update shop profile (name, phone, address, status).
- `POST /rest/abiz/admin/shops/{shopId}/suspend` — Suspend shop access.

### Global Wallet & Credits
- `POST /rest/abiz/admin/shops/{shopId}/wallet/add-credits`
  - Body: `{"amount": 100, "reason": "Bank transfer payment ref #12345"}`
  - Response: `{"shopId": "...", "balance": 100}`
- `GET /rest/abiz/admin/shops/{shopId}/wallet/ledger` — View shop's transaction ledger.
- `GET /rest/abiz/admin/audit-logs` — Global audit log query.

---

## 4. Shop Owner & Staff API Surface (/rest/abiz/shop/*)

Requires role `SHOP_OWNER` or `SHOP_STAFF`. `AbizTenantContext` automatically enforces `shop_id` scoping on every query.

### Wallet & Balance
- `GET /rest/abiz/shop/wallet` — Get current shop credit balance.
- `GET /rest/abiz/shop/wallet/ledger` — Paginated transaction ledger.

### Customers
- `GET /rest/abiz/shop/customers` — List customers for this shop.
- `POST /rest/abiz/shop/customers` — Create customer profile (CNIC, phone, name, address).
- `GET /rest/abiz/shop/customers/{id}` — Customer details with active contracts.
- `PUT /rest/abiz/shop/customers/{id}` — Update customer details.

### Contracts & Installment Plans
- `POST /rest/abiz/shop/contracts`
  - Body:
    ```json
    {
      "customerId": "uuid",
      "devicePrice": 120000.00,
      "downPayment": 30000.00,
      "financedAmount": 90000.00,
      "numberOfInstallments": 6,
      "frequency": "MONTHLY",
      "firstDueDate": "2026-09-15",
      "gracePeriodDays": 3
    }
    ```
  - Behavior: Generates 6 `abiz_installments` rows atomically.

### Payments
- `POST /rest/abiz/shop/payments`
  - Body:
    ```json
    {
      "contractId": "uuid",
      "installmentId": "uuid",
      "amount": 15000.00,
      "paymentMethod": "CASH",
      "reference": "Receipt #9872"
    }
    ```
  - Behavior: Updates installment `paid_amount`, contract `outstanding_balance`, records payment in ledger, and automatically evaluates if device can be unlocked.

### Financed Devices & Enforcement
- `GET /rest/abiz/shop/devices` — List devices with lock status, connection state, contract balance.
- `GET /rest/abiz/shop/devices/{id}` — Device telemetry, hardware ID, IMEI, lock logs.
- `POST /rest/abiz/shop/devices/{id}/lock` — Manual emergency lock command.
- `POST /rest/abiz/shop/devices/{id}/unlock` — Manual unlock command (with audit justification).

### Enrollment Wizard
- `POST /rest/abiz/shop/enrollment/create-session`
  - Body: `{"customerId": "uuid", "contractId": "uuid", "model": "Samsung A15"}`
  - Behavior:
    1. Checks wallet balance >= 1.
    2. Atomically reserves 1 credit (`DEVICE_ACTIVATION_RESERVE`).
    3. Generates enrollment session with signed QR payload.
  - Response: `{"sessionId": "uuid", "qrToken": "...", "qrPayload": {...}}`
- `GET /rest/abiz/shop/enrollment/{sessionId}/status` — Poll enrollment progress (DETECTED -> PROVISIONING -> ENROLLED).
