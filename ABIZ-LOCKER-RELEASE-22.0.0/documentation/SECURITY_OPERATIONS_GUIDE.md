# SECURITY OPERATIONS GUIDE
## Threat Detection, Deduplication & Severity Escalation

---

### 1. Incident Aggregation & Fingerprints
The correlation engine hashes incoming events:
```text
Fingerprint = SHA256(shopId:deviceId:eventType:source)
```
Duplicate events increment `occurrence_count` to eliminate alert storms.

---

### 2. Escalation Thresholds
- **Count 1–4:** `LOW`
- **Count 5–9:** `MEDIUM`
- **Count 10–19:** `HIGH`
- **Count ≥ 20:** `CRITICAL` (Triggers automatic device quarantine and operator alert).
