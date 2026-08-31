# 08 — REST API REFERENCE SPECIFICATION
## Complete Endpoints, Authentication, and Payload Formats

---

### 1. Authentication & Security Headers
All protected API endpoints require HTTP Bearer authentication:
```http
Authorization: Bearer <ABIZ_JWT_TOKEN>
X-Abiz-Correlation-Id: <UUID>
```

---

### 2. Core API Endpoints

#### 2.1 Authentication (`AbizAuthResource`)
- `POST /api/auth/login`: Authenticates shop staff or admin and returns signed JWT token.
- `POST /api/auth/refresh`: Refreshes active session token.

#### 2.2 Device Commands (`DeviceCommandResource`)
- `POST /api/commands`: Creates a new device command (`LOCK`, `UNLOCK`, `WIPE`, `REBOOT`).
- `GET /api/commands/device/{deviceId}`: Retrieves pending commands for a specific device.
- `POST /api/commands/ack`: Cryptographically acknowledges execution of a command.

#### 2.3 Shop & Financing (`ShopResource`, `ContractResource`, `PaymentResource`)
- `GET /api/shops`: Lists tenant shops with pagination.
- `POST /api/contracts`: Creates a new device financing installment contract.
- `POST /api/payments`: Records a payment against an active contract schedule.

#### 2.4 Device & Policy (`FinancedDeviceResource`, `DevicePolicyResource`)
- `GET /api/devices/{id}`: Returns detailed hardware and lock status for a device.
- `PUT /api/devices/{id}/policy`: Updates device policy state (`ACTIVE`, `LOCKED`, `SUSPENDED`).
