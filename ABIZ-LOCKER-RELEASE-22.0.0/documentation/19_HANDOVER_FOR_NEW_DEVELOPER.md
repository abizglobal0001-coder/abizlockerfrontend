# 19 — HANDOVER GUIDE FOR NEW DEVELOPERS
## Codebase Architecture, Conventions, and Extension Points

---

### 1. Repository Structure
- `mdmesh/common`: Shared domain models, enums, and utility classes.
- `mdmesh/jwt`: Cryptographic JWT issuance, verification, and token parsing.
- `mdmesh/server`: Core backend WAR containing REST resources, Guice modules, MyBatis mappers, and command services.
- `mdmesh/agent-android`: Native Kotlin Android Device Owner application.
- `scripts/`: Standalone deterministic test suites (Suites 1 through 43).

---

### 2. Architectural Conventions
- **UUID Handling:** Always use explicit `UuidTypeHandler` in MyBatis annotations.
- **Fail-Closed Security:** Never bypass tenant isolation checks in `AbizTenantContext`.
- **Non-Destructive Testing:** WIPE tests must always enforce `dryRun=true`.
