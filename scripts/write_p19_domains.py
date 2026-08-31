import os

domain_dir = r"C:\Users\MNA\.gemini\antigravity\scratch\abiz-locker\mdmesh\server\src\main\java\com\abiz\locker\domain"

# 1. AbizApiContractVersion.java
with open(os.path.join(domain_dir, "AbizApiContractVersion.java"), "w", encoding="utf-8") as f:
    f.write('''package com.abiz.locker.domain;

import java.util.Date;
import java.util.UUID;

public class AbizApiContractVersion {
    private UUID id;
    private String versionName;
    private String protocolVersion;
    private String minSupportedAgentProtocol;
    private String maxSupportedAgentProtocol;
    private String status = "ACTIVE";
    private Date deprecatedAt;
    private Date sunsetAt;
    private Date createdAt = new Date();

    public static final String STATUS_ACTIVE = "ACTIVE";
    public static final String STATUS_DEPRECATED = "DEPRECATED";
    public static final String STATUS_SUNSET = "SUNSET";

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getVersionName() { return versionName; }
    public void setVersionName(String versionName) { this.versionName = versionName; }

    public String getProtocolVersion() { return protocolVersion; }
    public void setProtocolVersion(String protocolVersion) { this.protocolVersion = protocolVersion; }

    public String getMinSupportedAgentProtocol() { return minSupportedAgentProtocol; }
    public void setMinSupportedAgentProtocol(String minSupportedAgentProtocol) { this.minSupportedAgentProtocol = minSupportedAgentProtocol; }

    public String getMaxSupportedAgentProtocol() { return maxSupportedAgentProtocol; }
    public void setMaxSupportedAgentProtocol(String maxSupportedAgentProtocol) { this.maxSupportedAgentProtocol = maxSupportedAgentProtocol; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Date getDeprecatedAt() { return deprecatedAt; }
    public void setDeprecatedAt(Date deprecatedAt) { this.deprecatedAt = deprecatedAt; }

    public Date getSunsetAt() { return sunsetAt; }
    public void setSunsetAt(Date sunsetAt) { this.sunsetAt = sunsetAt; }

    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }
}
''')

# 2. AbizAgentUpgradeEvent.java
with open(os.path.join(domain_dir, "AbizAgentUpgradeEvent.java"), "w", encoding="utf-8") as f:
    f.write('''package com.abiz.locker.domain;

import java.util.Date;
import java.util.UUID;

public class AbizAgentUpgradeEvent {
    private UUID id;
    private UUID deviceId;
    private String fromVersion;
    private String toVersion;
    private String status = "PENDING";
    private String failureReason;
    private Date startedAt = new Date();
    private Date completedAt;
    private Date createdAt = new Date();

    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_IN_PROGRESS = "IN_PROGRESS";
    public static final String STATUS_COMPLETED = "COMPLETED";
    public static final String STATUS_FAILED = "FAILED";

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getDeviceId() { return deviceId; }
    public void setDeviceId(UUID deviceId) { this.deviceId = deviceId; }

    public String getFromVersion() { return fromVersion; }
    public void setFromVersion(String fromVersion) { this.fromVersion = fromVersion; }

    public String getToVersion() { return toVersion; }
    public void setToVersion(String toVersion) { this.toVersion = toVersion; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getFailureReason() { return failureReason; }
    public void setFailureReason(String failureReason) { this.failureReason = failureReason; }

    public Date getStartedAt() { return startedAt; }
    public void setStartedAt(Date startedAt) { this.startedAt = startedAt; }

    public Date getCompletedAt() { return completedAt; }
    public void setCompletedAt(Date completedAt) { this.completedAt = completedAt; }

    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }
}
''')

# 3. AbizDeviceQuarantineEvent.java
with open(os.path.join(domain_dir, "AbizDeviceQuarantineEvent.java"), "w", encoding="utf-8") as f:
    f.write('''package com.abiz.locker.domain;

import java.util.Date;
import java.util.UUID;

public class AbizDeviceQuarantineEvent {
    private UUID id;
    private UUID deviceId;
    private UUID shopId;
    private UUID actorId;
    private String reason;
    private String details;
    private String action; // QUARANTINED, RELEASED
    private Date createdAt = new Date();

    public static final String ACTION_QUARANTINED = "QUARANTINED";
    public static final String ACTION_RELEASED = "RELEASED";

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getDeviceId() { return deviceId; }
    public void setDeviceId(UUID deviceId) { this.deviceId = deviceId; }

    public UUID getShopId() { return shopId; }
    public void setShopId(UUID shopId) { this.shopId = shopId; }

    public UUID getActorId() { return actorId; }
    public void setActorId(UUID actorId) { this.actorId = actorId; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public String getDetails() { return details; }
    public void setDetails(String details) { this.details = details; }

    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }

    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }
}
''')

# 4. AbizFeatureFlag.java
with open(os.path.join(domain_dir, "AbizFeatureFlag.java"), "w", encoding="utf-8") as f:
    f.write('''package com.abiz.locker.domain;

import java.util.Date;
import java.util.UUID;

public class AbizFeatureFlag {
    private UUID id;
    private String flagKey;
    private UUID shopId;
    private boolean enabled = true;
    private String description;
    private UUID updatedBy;
    private Date updatedAt = new Date();

    public static final String FLAG_REMOTE_COMMANDS = "REMOTE_COMMANDS";
    public static final String FLAG_REMOTE_LOCK = "REMOTE_LOCK";
    public static final String FLAG_REMOTE_UNLOCK = "REMOTE_UNLOCK";
    public static final String FLAG_WIPE = "WIPE";
    public static final String FLAG_ANDROID_UPDATES = "ANDROID_UPDATES";
    public static final String FLAG_ADMIN_REPLAY = "ADMIN_REPLAY";
    public static final String FLAG_AUTOMATED_RETRY = "AUTOMATED_RETRY";

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getFlagKey() { return flagKey; }
    public void setFlagKey(String flagKey) { this.flagKey = flagKey; }

    public UUID getShopId() { return shopId; }
    public void setShopId(UUID shopId) { this.shopId = shopId; }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public UUID getUpdatedBy() { return updatedBy; }
    public void setUpdatedBy(UUID updatedBy) { this.updatedBy = updatedBy; }

    public Date getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Date updatedAt) { this.updatedAt = updatedAt; }
}
''')

# 5. AbizBreakGlassSession.java
with open(os.path.join(domain_dir, "AbizBreakGlassSession.java"), "w", encoding="utf-8") as f:
    f.write('''package com.abiz.locker.domain;

import java.util.Date;
import java.util.UUID;

public class AbizBreakGlassSession {
    private UUID id;
    private UUID actorId;
    private UUID shopId;
    private String scope;
    private String reason;
    private Date grantedAt = new Date();
    private Date expiresAt;
    private Date revokedAt;
    private String status = "ACTIVE";

    public static final String STATUS_ACTIVE = "ACTIVE";
    public static final String STATUS_EXPIRED = "EXPIRED";
    public static final String STATUS_REVOKED = "REVOKED";

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getActorId() { return actorId; }
    public void setActorId(UUID actorId) { this.actorId = actorId; }

    public UUID getShopId() { return shopId; }
    public void setShopId(UUID shopId) { this.shopId = shopId; }

    public String getScope() { return scope; }
    public void setScope(String scope) { this.scope = scope; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public Date getGrantedAt() { return grantedAt; }
    public void setGrantedAt(Date grantedAt) { this.grantedAt = grantedAt; }

    public Date getExpiresAt() { return expiresAt; }
    public void setExpiresAt(Date expiresAt) { this.expiresAt = expiresAt; }

    public Date getRevokedAt() { return revokedAt; }
    public void setRevokedAt(Date revokedAt) { this.revokedAt = revokedAt; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public boolean isSessionValid() {
        return STATUS_ACTIVE.equalsIgnoreCase(status) &&
               expiresAt != null &&
               new Date().before(expiresAt) &&
               revokedAt == null;
    }
}
''')

# 6. AbizAuditIntegrity.java
with open(os.path.join(domain_dir, "AbizAuditIntegrity.java"), "w", encoding="utf-8") as f:
    f.write('''package com.abiz.locker.domain;

import java.util.Date;
import java.util.UUID;

public class AbizAuditIntegrity {
    private UUID id;
    private Long auditLogId;
    private String previousHash;
    private String eventHash;
    private long sequenceNum;
    private Date verifiedAt;
    private Date createdAt = new Date();

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public Long getAuditLogId() { return auditLogId; }
    public void setAuditLogId(Long auditLogId) { this.auditLogId = auditLogId; }

    public String getPreviousHash() { return previousHash; }
    public void setPreviousHash(String previousHash) { this.previousHash = previousHash; }

    public String getEventHash() { return eventHash; }
    public void setEventHash(String eventHash) { this.eventHash = eventHash; }

    public long getSequenceNum() { return sequenceNum; }
    public void setSequenceNum(long sequenceNum) { this.sequenceNum = sequenceNum; }

    public Date getVerifiedAt() { return verifiedAt; }
    public void setVerifiedAt(Date verifiedAt) { this.verifiedAt = verifiedAt; }

    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }
}
''')

# 7. AbizSloMeasurement.java
with open(os.path.join(domain_dir, "AbizSloMeasurement.java"), "w", encoding="utf-8") as f:
    f.write('''package com.abiz.locker.domain;

import java.util.Date;
import java.util.UUID;

public class AbizSloMeasurement {
    private UUID id;
    private String metricName;
    private double targetValue;
    private double actualValue;
    private String status = "HEALTHY";
    private Date measuredAt = new Date();

    public static final String STATUS_HEALTHY = "HEALTHY";
    public static final String STATUS_WARNING = "WARNING";
    public static final String STATUS_CRITICAL = "CRITICAL";

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getMetricName() { return metricName; }
    public void setMetricName(String metricName) { this.metricName = metricName; }

    public double getTargetValue() { return targetValue; }
    public void setTargetValue(double targetValue) { this.targetValue = targetValue; }

    public double getActualValue() { return actualValue; }
    public void setActualValue(double actualValue) { this.actualValue = actualValue; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Date getMeasuredAt() { return measuredAt; }
    public void setMeasuredAt(Date measuredAt) { this.measuredAt = measuredAt; }
}
''')

# 8. AbizAlertEvent.java
with open(os.path.join(domain_dir, "AbizAlertEvent.java"), "w", encoding="utf-8") as f:
    f.write('''package com.abiz.locker.domain;

import java.util.Date;
import java.util.UUID;

public class AbizAlertEvent {
    private UUID id;
    private String alertKey;
    private String severity = "INFO";
    private String message;
    private String correlationId;
    private int occurrenceCount = 1;
    private Date firstSeenAt = new Date();
    private Date lastSeenAt = new Date();
    private String status = "ACTIVE";

    public static final String SEVERITY_INFO = "INFO";
    public static final String SEVERITY_WARNING = "WARNING";
    public static final String SEVERITY_HIGH = "HIGH";
    public static final String SEVERITY_CRITICAL = "CRITICAL";

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getAlertKey() { return alertKey; }
    public void setAlertKey(String alertKey) { this.alertKey = alertKey; }

    public String getSeverity() { return severity; }
    public void setSeverity(String severity) { this.severity = severity; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getCorrelationId() { return correlationId; }
    public void setCorrelationId(String correlationId) { this.correlationId = correlationId; }

    public int getOccurrenceCount() { return occurrenceCount; }
    public void setOccurrenceCount(int occurrenceCount) { this.occurrenceCount = occurrenceCount; }

    public Date getFirstSeenAt() { return firstSeenAt; }
    public void setFirstSeenAt(Date firstSeenAt) { this.firstSeenAt = firstSeenAt; }

    public Date getLastSeenAt() { return lastSeenAt; }
    public void setLastSeenAt(Date lastSeenAt) { this.lastSeenAt = lastSeenAt; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
''')

# 9. AbizRateLimitCounter.java
with open(os.path.join(domain_dir, "AbizRateLimitCounter.java"), "w", encoding="utf-8") as f:
    f.write('''package com.abiz.locker.domain;

import java.util.Date;
import java.util.UUID;

public class AbizRateLimitCounter {
    private UUID id;
    private String counterKey;
    private int hitCount = 1;
    private long windowStartMs;
    private long expiresAtMs;
    private Date createdAt = new Date();

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getCounterKey() { return counterKey; }
    public void setCounterKey(String counterKey) { this.counterKey = counterKey; }

    public int getHitCount() { return hitCount; }
    public void setHitCount(int hitCount) { this.hitCount = hitCount; }

    public long getWindowStartMs() { return windowStartMs; }
    public void setWindowStartMs(long windowStartMs) { this.windowStartMs = windowStartMs; }

    public long getExpiresAtMs() { return expiresAtMs; }
    public void setExpiresAtMs(long expiresAtMs) { this.expiresAtMs = expiresAtMs; }

    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }
}
''')

# 10. AbizSecurityTimeline.java
with open(os.path.join(domain_dir, "AbizSecurityTimeline.java"), "w", encoding="utf-8") as f:
    f.write('''package com.abiz.locker.domain;

import java.util.Date;
import java.util.UUID;

public class AbizSecurityTimeline {
    private UUID id;
    private String correlationId;
    private UUID eventId;
    private UUID shopId;
    private UUID deviceId;
    private UUID actorId;
    private String eventType;
    private String stage;
    private String payloadRedacted;
    private Date timestamp = new Date();

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getCorrelationId() { return correlationId; }
    public void setCorrelationId(String correlationId) { this.correlationId = correlationId; }

    public UUID getEventId() { return eventId; }
    public void setEventId(UUID eventId) { this.eventId = eventId; }

    public UUID getShopId() { return shopId; }
    public void setShopId(UUID shopId) { this.shopId = shopId; }

    public UUID getDeviceId() { return deviceId; }
    public void setDeviceId(UUID deviceId) { this.deviceId = deviceId; }

    public UUID getActorId() { return actorId; }
    public void setActorId(UUID actorId) { this.actorId = actorId; }

    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }

    public String getStage() { return stage; }
    public void setStage(String stage) { this.stage = stage; }

    public String getPayloadRedacted() { return payloadRedacted; }
    public void setPayloadRedacted(String payloadRedacted) { this.payloadRedacted = payloadRedacted; }

    public Date getTimestamp() { return timestamp; }
    public void setTimestamp(Date timestamp) { this.timestamp = timestamp; }
}
''')

print("Created all Phase 19 Domain models successfully")