# ABIZ LOCKER — OPERATIONS RUNBOOK

## Production Operations, Deployment Governance & Incident Management

---

### 1. Deployment & Release Governance Workflow

#### 1.1 Four-Eyes Release Initiation & Peer Approval
1. An authorized release operator initiates a deployment release draft using `DeploymentGovernanceService.createRelease(...)`.
2. A separate, authenticated peer approver reviews the release candidate and submits cryptographic approval:
   ```java
   govService.approveRelease(releaseId, "approver_username", correlationId);
   ```
   *Single-operator self-approval is rejected by system policy.*

#### 1.2 12-Point Preflight Checklist Execution
Prior to transitioning from `APPROVED` to `DEPLOYING`, the automated preflight engine must be triggered:
```java
Map<String, Boolean> checklist = preflightService.evaluatePreflightChecklist(warSha256, apkSha256);
boolean passed = preflightService.isPreflightPassed(checklist);
```
Rollout is permitted **only** if all 12 items return `true`:
- `DATABASE_REACHABLE`
- `SCHEMA_VERSION_VALID`
- `WAR_HASH_VERIFIED`
- `APK_HASH_VERIFIED`
- `CONFIGURATION_APPROVED`
- `WORKER_POOL_HEALTHY`
- `SLO_HEALTH_ACCEPTABLE`
- `NO_BLOCKING_CRITICAL_INCIDENTS`
- `RECENT_BACKUP_VERIFIED`
- `SECURITY_POSTURE_CLEAN`
- `FLEET_HEALTH_ACCEPTABLE`
- `ROLLBACK_ARTIFACT_AVAILABLE`

#### 1.3 Stage Checkpoints & Atomic Rollback
During rollout across canary and staging rings, `DeploymentVerificationService` records stage checkpoints. If any metric violates latency or error thresholds, invoke atomic rollback:
```java
govService.rollbackRelease(releaseId, "SLO threshold breached on Canary Ring 1", correlationId);
```
The system transitions the release to `ROLLED_BACK`, notifies operators, and logs the event to `abiz_audit_logs`.

---

### 2. Configuration Drift & Production Freezes

#### 2.1 Configuration Drift Monitoring
`ProductionObservabilityService.detectDrift(...)` calculates the SHA-256 hash of active runtime configurations and compares it to the approved baseline:
```java
AbizConfigurationDriftEvent drift = obsService.detectDrift(configKey, actualValue, approvedBaselineValue, "worker_id");
```
- If a drift is detected (`DRIFT_DETECTED`), the system generates an audit record and automatically triggers a security incident for operator remediation.

#### 2.2 Production Freeze Window Enforcement
During designated freeze windows (e.g. end-of-month financial reconciliation, holiday cutoffs), changes are prohibited:
- To schedule a freeze:
  ```java
  obsService.enactFreezeWindow("Q4 Peak Freeze", freezeStart, freezeEnd, "Change Management", "Freeze active");
  ```
- Any deployment or critical configuration change will be blocked unless an **Audited Emergency Override** is submitted with explicit justification and multi-party sign-off.

---

### 3. Disaster Recovery & Backup Verification

#### 3.1 DR Execution & SLA Measurement
- In the event of a cluster failure or database outage, activate the DR plan:
  ```java
  drService.executeRecovery(planId, "FULL_DATABASE", "dr_officer");
  ```
- The service measures:
  - **RTO (Recovery Time Objective):** Must be ≤ 300 seconds.
  - **RPO (Recovery Point Objective):** Must be ≤ 60 seconds.
- Compliance is stored in `abiz_recovery_executions` and audited.

#### 3.2 Point-in-Time Backup Verification
- Every automated backup must be cryptographically verified:
  ```java
  drService.verifyBackupIntegrity(backupId, backupSha256, byteSize, "system_worker");
  ```

---

### 4. Fleet Lifecycle Governance & Quarantine

#### 4.1 Strict Lifecycle State Machine
Device lifecycle state transitions must strictly adhere to valid paths:
```
ENROLLED → VERIFIED → ACTIVE ⇄ SUSPENDED
                             ⇄ QUARANTINED
                             → COMPROMISED → REPLACEMENT_REQUESTED → REPLACED → ACTIVE
                                                                   → RETIRED
                                                                   → REVOKED
```
- Attempting illegal transitions (e.g., `REVOKED` → `ACTIVE` or `RETIRED` → `ACTIVE`) will immediately be blocked and logged as a security alert.

#### 4.2 Outdated Agent Protocol Quarantine
`FleetVersionComplianceService` evaluates agent heartbeats against the required baseline (`v0.1.22` / Version Code `22`):
- Any device presenting an outdated protocol or deprecated binary is automatically placed into `QUARANTINED` status and locked until an OTA update is applied.

---

### 5. Security Incident Operations

#### 5.1 Real-Time Ingestion & Deduplication
Security events from workers, API gateways, and agent telemetry are ingested via `SecurityEventCorrelationService`:
- Each event is assigned a deterministic SHA-256 fingerprint (`shopId:deviceId:eventType:source`).
- Duplicate events within the active window increment `occurrence_count` rather than flooding the database.

#### 5.2 Automated Severity Escalation & Mitigation
Incidents automatically escalate severity based on repeat occurrences:
- Count ≥ 5: `LOW` → `MEDIUM`
- Count ≥ 10: `MEDIUM` → `HIGH`
- Count ≥ 20: `HIGH` → `CRITICAL`

When mitigated, mark resolved with an audit trail:
```java
secService.resolveIncident(incidentId, "Mitigation applied: credential rotated and rogue worker isolated");
```
