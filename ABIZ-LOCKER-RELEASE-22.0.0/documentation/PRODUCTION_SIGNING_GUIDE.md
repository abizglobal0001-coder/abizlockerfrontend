# PRODUCTION SIGNING GUIDE
## Android Enterprise Keystore Management & Release Signing

---

### 1. Keystore Security Rules
- **Keystore Path:** `keystores/abiz-release.jks` (Protected, excluded from public distribution).
- **Algorithm:** RSA 4096-bit (validity: 10,000 days).
- **Certificate Fingerprint:** `EF:FB:3C:95:80:C4:80:E7:40:A1:94:65:01:0A:7D:F2:86:7F:7B:46:FD:B9:5D:CE:74:F1:72:BE:7F:9A:0A:25`.

---

### 2. Signing Command Procedure
```bash
export JAVA_HOME="<JDK_17_PATH>"
export PATH="$JAVA_HOME/bin:$PATH"

apksigner sign --ks <PRODUCTION_KEYSTORE_PATH> \
               --ks-pass "env:KEYSTORE_PASS" \
               --ks-key-alias abiz-release \
               --out app-release-signed.apk \
               app-release-unsigned.apk
```

---

### 3. Verification Command
```bash
apksigner verify --verbose --print-certs app-release-signed.apk
```
*Expected Output:* `Verifies (Verified using v2 scheme: true, Verified using v3 scheme: true)`.
