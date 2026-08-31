# 04 — APK SIGNING & RELEASE GOVERNANCE GUIDE
## Cryptographic Key Management & Release Integrity

---

### 1. Why Signing Matters for MDM Device Owner
Android enforces strict signature continuity for Device Owner applications. If an agent is installed with Key A, it **cannot** be upgraded with Key B without a complete factory reset. Therefore, the production private key must be safeguarded in an enterprise KMS / HSM.

---

### 2. Production Keystore Creation
Generate an enterprise RSA-4096 / EC-256 signing key:
```bash
keytool -genkeypair -v -keystore <PRODUCTION_KEYSTORE_PATH> -alias <KEY_ALIAS> -keyalg RSA -keysize 4096 -validity 10000
```

---

### 3. CI/CD Automated Signing Pipeline
In automated release pipelines, configure environment secrets without hardcoding credentials:
```bash
export ANDROID_KEYSTORE_FILE="/secure/vault/abiz-production.jks"
export ANDROID_KEYSTORE_PASS="<VAULT_RETRIEVED_KEYSTORE_PASS>"
export ANDROID_KEY_ALIAS="<VAULT_RETRIEVED_ALIAS>"
export ANDROID_KEY_PASS="<VAULT_RETRIEVED_KEY_PASS>"

apksigner sign --ks "$ANDROID_KEYSTORE_FILE" --ks-pass "env:ANDROID_KEYSTORE_PASS" --ks-key-alias "$ANDROID_KEY_ALIAS" --key-pass "env:ANDROID_KEY_PASS" --out app-release-signed.apk app-release-unsigned.apk
```

---

### 4. Disaster Recovery for Keystores
- Store encrypted offline backups of the `.jks` keystore file across two physically isolated disaster recovery vaults.
- Record the certificate SHA-256 fingerprint for identity auditing.
