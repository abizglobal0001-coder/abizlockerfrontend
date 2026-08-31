# 06 — DEVICE LIFECYCLE OPERATIONS RUNBOOK
## 10-State Finite State Machine (FSM) Governance

---

### 1. Lifecycle State Machine Diagram
The device lifecycle is strictly governed by `DeviceLifecycleGovernanceService`:

```mermaid
stateDiagram-v2
    [*] --> ENROLLED
    ENROLLED --> VERIFIED: Device Owner & Attestation Valid
    VERIFIED --> ACTIVE: Contract Activated
    ACTIVE --> SUSPENDED: Overdue Default / Policy Hold
    SUSPENDED --> ACTIVE: Payment Cleared
    ACTIVE --> QUARANTINED: Outdated Agent / Tamper
    QUARANTINED --> ACTIVE: OTA Updated / Cleared
    ACTIVE --> COMPROMISED: Hardware Tamper / Integrity Breach
    COMPROMISED --> REPLACEMENT_REQUESTED: RMA Initiated
    REPLACEMENT_REQUESTED --> REPLACED: New Device Paired
    REPLACED --> ACTIVE: Re-enrolled
    ACTIVE --> RETIRED: Contract Paid in Full
    ACTIVE --> REVOKED: Fraud / Administrative Seizure
    RETIRED --> [*]
    REVOKED --> [*]
```

---

### 2. State Definitions & Transitions

| State | Operational Meaning | Allowed Next States |
|---|---|---|
| `ENROLLED` | Device registered in system, awaiting Device Owner attestation. | `VERIFIED` |
| `VERIFIED` | Device Owner active, cryptographic identity validated. | `ACTIVE` |
| `ACTIVE` | Under active financing contract, MDM policy enforced. | `SUSPENDED`, `QUARANTINED`, `COMPROMISED`, `RETIRED`, `REVOKED` |
| `SUSPENDED` | Device locked due to non-payment or administrative hold. | `ACTIVE` |
| `QUARANTINED` | Version deprecated or abnormal network behavior isolated. | `ACTIVE` |
| `COMPROMISED` | Rooting, bootloader unlock, or replay attack detected. | `REPLACEMENT_REQUESTED` |
| `REPLACEMENT_REQUESTED` | RMA workflow initiated for damaged/compromised hardware. | `REPLACED` |
| `REPLACED` | Serial swap recorded in database. | `ACTIVE` |
| `RETIRED` | Terminal state: Contract fully paid, MDM lock removed permanently. | None (Terminal) |
| `REVOKED` | Terminal state: Fraud blacklist / permanent decommissioning. | None (Terminal) |

---

### 3. Illegal Transition Defense
Any transition outside the defined matrix (e.g. `REVOKED` → `ACTIVE` or `RETIRED` → `ACTIVE`) is immediately rejected by `DeviceLifecycleGovernanceService.isValidTransition(...)` and logged as a security alert.
