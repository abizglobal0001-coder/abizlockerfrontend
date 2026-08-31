# TROUBLESHOOTING GUIDE
## Quick Diagnostic & Remediation Matrix

---

### 1. Common Operational Issues

| Issue | Root Cause | Resolution Command / Step |
|---|---|---|
| **Device Owner Provisioning Failed** | Accounts exist on device | Factory reset device; provision before configuring Google account. |
| **API Returns 401 Unauthorized** | JWT expired or invalid key | Re-authenticate via `/api/auth/login`; verify clock sync. |
| **Database Connection Refused** | PostgreSQL offline / port mismatch | Verify service status: `systemctl status postgresql-18`. |
| **Device Quarantined** | Outdated agent version code `< 22` | Distribute signed `app-release-signed.apk` (v0.1.22). |
