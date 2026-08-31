import os

mapper_dir = r"C:\Users\MNA\.gemini\antigravity\scratch\abiz-locker\mdmesh\server\src\main\java\com\abiz\locker\mapper"

# 1. ApiContractMapper.java
with open(os.path.join(mapper_dir, "ApiContractMapper.java"), "w", encoding="utf-8") as f:
    f.write('''package com.abiz.locker.mapper;

import com.abiz.locker.domain.AbizApiContractVersion;
import org.apache.ibatis.annotations.*;

import java.util.List;
import java.util.UUID;

@Mapper
public interface ApiContractMapper {

    @Insert("INSERT INTO abiz_api_contract_versions (" +
            "  id, version_name, protocol_version, min_supported_agent_protocol, max_supported_agent_protocol, " +
            "  status, deprecated_at, sunset_at, created_at) " +
            "VALUES (" +
            "  #{id, typeHandler=com.abiz.locker.mapper.UuidTypeHandler}, " +
            "  #{versionName}, #{protocolVersion}, #{minSupportedAgentProtocol}, #{maxSupportedAgentProtocol}, " +
            "  #{status}, #{deprecatedAt}, #{sunsetAt}, #{createdAt})")
    void insertVersion(AbizApiContractVersion version);

    @Select("SELECT id, version_name AS versionName, protocol_version AS protocolVersion, " +
            "       min_supported_agent_protocol AS minSupportedAgentProtocol, " +
            "       max_supported_agent_protocol AS maxSupportedAgentProtocol, " +
            "       status, deprecated_at AS deprecatedAt, sunset_at AS sunsetAt, created_at AS createdAt " +
            "FROM abiz_api_contract_versions " +
            "WHERE version_name = #{versionName} " +
            "LIMIT 1")
    AbizApiContractVersion findByVersionName(@Param("versionName") String versionName);

    @Select("SELECT id, version_name AS versionName, protocol_version AS protocolVersion, " +
            "       min_supported_agent_protocol AS minSupportedAgentProtocol, " +
            "       max_supported_agent_protocol AS maxSupportedAgentProtocol, " +
            "       status, deprecated_at AS deprecatedAt, sunset_at AS sunsetAt, created_at AS createdAt " +
            "FROM abiz_api_contract_versions " +
            "WHERE status = 'ACTIVE' " +
            "ORDER BY created_at DESC")
    List<AbizApiContractVersion> findActiveVersions();

    @Select("SELECT COUNT(1) FROM abiz_api_contract_versions")
    long countAll();
}
''')

# 2. AgentUpgradeMapper.java
with open(os.path.join(mapper_dir, "AgentUpgradeMapper.java"), "w", encoding="utf-8") as f:
    f.write('''package com.abiz.locker.mapper;

import com.abiz.locker.domain.AbizAgentUpgradeEvent;
import org.apache.ibatis.annotations.*;

import java.util.List;
import java.util.UUID;

@Mapper
public interface AgentUpgradeMapper {

    @Insert("INSERT INTO abiz_agent_upgrade_events (" +
            "  id, device_id, from_version, to_version, status, failure_reason, started_at, completed_at, created_at) " +
            "VALUES (" +
            "  #{id, typeHandler=com.abiz.locker.mapper.UuidTypeHandler}, " +
            "  #{deviceId, typeHandler=com.abiz.locker.mapper.UuidTypeHandler}, " +
            "  #{fromVersion}, #{toVersion}, #{status}, #{failureReason}, #{startedAt}, #{completedAt}, #{createdAt})")
    void insertUpgradeEvent(AbizAgentUpgradeEvent event);

    @Update("UPDATE abiz_agent_upgrade_events SET " +
            "  status = #{status}, " +
            "  failure_reason = #{failureReason}, " +
            "  completed_at = #{completedAt} " +
            "WHERE id = #{id, typeHandler=com.abiz.locker.mapper.UuidTypeHandler}")
    int updateUpgradeEvent(AbizAgentUpgradeEvent event);

    @Select("SELECT id, device_id AS deviceId, from_version AS fromVersion, to_version AS toVersion, " +
            "       status, failure_reason AS failureReason, started_at AS startedAt, " +
            "       completed_at AS completedAt, created_at AS createdAt " +
            "FROM abiz_agent_upgrade_events " +
            "WHERE device_id = #{deviceId, typeHandler=com.abiz.locker.mapper.UuidTypeHandler} " +
            "ORDER BY created_at DESC " +
            "LIMIT #{limit}")
    List<AbizAgentUpgradeEvent> findRecentByDevice(
            @Param("deviceId") UUID deviceId,
            @Param("limit") int limit
    );

    @Select("SELECT COUNT(1) FROM abiz_agent_upgrade_events")
    long countAll();
}
''')

# 3. DeviceQuarantineMapper.java
with open(os.path.join(mapper_dir, "DeviceQuarantineMapper.java"), "w", encoding="utf-8") as f:
    f.write('''package com.abiz.locker.mapper;

import com.abiz.locker.domain.AbizDeviceQuarantineEvent;
import org.apache.ibatis.annotations.*;

import java.util.List;
import java.util.UUID;

@Mapper
public interface DeviceQuarantineMapper {

    @Insert("INSERT INTO abiz_device_quarantine_events (" +
            "  id, device_id, shop_id, actor_id, reason, details, action, created_at) " +
            "VALUES (" +
            "  #{id, typeHandler=com.abiz.locker.mapper.UuidTypeHandler}, " +
            "  #{deviceId, typeHandler=com.abiz.locker.mapper.UuidTypeHandler}, " +
            "  #{shopId, typeHandler=com.abiz.locker.mapper.UuidTypeHandler}, " +
            "  #{actorId, typeHandler=com.abiz.locker.mapper.UuidTypeHandler}, " +
            "  #{reason}, #{details}, #{action}, #{createdAt})")
    void insertQuarantineEvent(AbizDeviceQuarantineEvent event);

    @Select("SELECT id, device_id AS deviceId, shop_id AS shopId, actor_id AS actorId, " +
            "       reason, details, action, created_at AS createdAt " +
            "FROM abiz_device_quarantine_events " +
            "WHERE device_id = #{deviceId, typeHandler=com.abiz.locker.mapper.UuidTypeHandler} " +
            "ORDER BY created_at DESC " +
            "LIMIT #{limit}")
    List<AbizDeviceQuarantineEvent> findRecentByDevice(
            @Param("deviceId") UUID deviceId,
            @Param("limit") int limit
    );

    @Select("SELECT COUNT(1) FROM abiz_device_quarantine_events")
    long countAll();
}
''')

# 4. FeatureFlagMapper.java
with open(os.path.join(mapper_dir, "FeatureFlagMapper.java"), "w", encoding="utf-8") as f:
    f.write('''package com.abiz.locker.mapper;

import com.abiz.locker.domain.AbizFeatureFlag;
import org.apache.ibatis.annotations.*;

import java.util.List;
import java.util.UUID;

@Mapper
public interface FeatureFlagMapper {

    @Insert("INSERT INTO abiz_feature_flags (" +
            "  id, flag_key, shop_id, enabled, description, updated_by, updated_at) " +
            "VALUES (" +
            "  #{id, typeHandler=com.abiz.locker.mapper.UuidTypeHandler}, " +
            "  #{flagKey}, " +
            "  #{shopId, typeHandler=com.abiz.locker.mapper.UuidTypeHandler}, " +
            "  #{enabled}, #{description}, " +
            "  #{updatedBy, typeHandler=com.abiz.locker.mapper.UuidTypeHandler}, #{updatedAt}) " +
            "ON CONFLICT (flag_key, COALESCE(shop_id, '00000000-0000-0000-0000-000000000000'::uuid)) " +
            "DO UPDATE SET enabled = EXCLUDED.enabled, description = EXCLUDED.description, " +
            "              updated_by = EXCLUDED.updated_by, updated_at = EXCLUDED.updated_at")
    void upsertFlag(AbizFeatureFlag flag);

    @Select("SELECT id, flag_key AS flagKey, shop_id AS shopId, enabled, description, " +
            "       updated_by AS updatedBy, updated_at AS updatedAt " +
            "FROM abiz_feature_flags " +
            "WHERE flag_key = #{flagKey} AND shop_id IS NULL " +
            "LIMIT 1")
    AbizFeatureFlag findGlobalFlag(@Param("flagKey") String flagKey);

    @Select("SELECT id, flag_key AS flagKey, shop_id AS shopId, enabled, description, " +
            "       updated_by AS updatedBy, updated_at AS updatedAt " +
            "FROM abiz_feature_flags " +
            "WHERE flag_key = #{flagKey} AND shop_id = #{shopId, typeHandler=com.abiz.locker.mapper.UuidTypeHandler} " +
            "LIMIT 1")
    AbizFeatureFlag findShopFlag(
            @Param("flagKey") String flagKey,
            @Param("shopId") UUID shopId
    );

    @Select("SELECT COUNT(1) FROM abiz_feature_flags")
    long countAll();
}
''')

# 5. BreakGlassMapper.java
with open(os.path.join(mapper_dir, "BreakGlassMapper.java"), "w", encoding="utf-8") as f:
    f.write('''package com.abiz.locker.mapper;

import com.abiz.locker.domain.AbizBreakGlassSession;
import org.apache.ibatis.annotations.*;

import java.util.Date;
import java.util.List;
import java.util.UUID;

@Mapper
public interface BreakGlassMapper {

    @Insert("INSERT INTO abiz_break_glass_sessions (" +
            "  id, actor_id, shop_id, scope, reason, granted_at, expires_at, revoked_at, status) " +
            "VALUES (" +
            "  #{id, typeHandler=com.abiz.locker.mapper.UuidTypeHandler}, " +
            "  #{actorId, typeHandler=com.abiz.locker.mapper.UuidTypeHandler}, " +
            "  #{shopId, typeHandler=com.abiz.locker.mapper.UuidTypeHandler}, " +
            "  #{scope}, #{reason}, #{grantedAt}, #{expiresAt}, #{revokedAt}, #{status})")
    void insertSession(AbizBreakGlassSession session);

    @Update("UPDATE abiz_break_glass_sessions SET " +
            "  status = #{status}, " +
            "  revoked_at = #{revokedAt} " +
            "WHERE id = #{id, typeHandler=com.abiz.locker.mapper.UuidTypeHandler}")
    int updateSessionStatus(
            @Param("id") UUID id,
            @Param("status") String status,
            @Param("revokedAt") Date revokedAt
    );

    @Select("SELECT id, actor_id AS actorId, shop_id AS shopId, scope, reason, " +
            "       granted_at AS grantedAt, expires_at AS expiresAt, revoked_at AS revokedAt, status " +
            "FROM abiz_break_glass_sessions " +
            "WHERE id = #{id, typeHandler=com.abiz.locker.mapper.UuidTypeHandler}")
    AbizBreakGlassSession findById(@Param("id") UUID id);

    @Select("SELECT id, actor_id AS actorId, shop_id AS shopId, scope, reason, " +
            "       granted_at AS grantedAt, expires_at AS expiresAt, revoked_at AS revokedAt, status " +
            "FROM abiz_break_glass_sessions " +
            "WHERE actor_id = #{actorId, typeHandler=com.abiz.locker.mapper.UuidTypeHandler} " +
            "  AND status = 'ACTIVE' AND expires_at > NOW() " +
            "ORDER BY granted_at DESC")
    List<AbizBreakGlassSession> findActiveSessionsByActor(@Param("actorId") UUID actorId);

    @Select("SELECT COUNT(1) FROM abiz_break_glass_sessions")
    long countAll();
}
''')

# 6. AuditIntegrityMapper.java
with open(os.path.join(mapper_dir, "AuditIntegrityMapper.java"), "w", encoding="utf-8") as f:
    f.write('''package com.abiz.locker.mapper;

import com.abiz.locker.domain.AbizAuditIntegrity;
import org.apache.ibatis.annotations.*;

import java.util.List;
import java.util.UUID;

@Mapper
public interface AuditIntegrityMapper {

    @Insert("INSERT INTO abiz_audit_integrity (" +
            "  id, audit_log_id, previous_hash, event_hash, sequence_num, verified_at, created_at) " +
            "VALUES (" +
            "  #{id, typeHandler=com.abiz.locker.mapper.UuidTypeHandler}, " +
            "  #{auditLogId}, #{previousHash}, #{eventHash}, #{sequenceNum}, #{verifiedAt}, #{createdAt})")
    void insertIntegrityRecord(AbizAuditIntegrity record);

    @Select("SELECT id, audit_log_id AS auditLogId, previous_hash AS previousHash, " +
            "       event_hash AS eventHash, sequence_num AS sequenceNum, " +
            "       verified_at AS verifiedAt, created_at AS createdAt " +
            "FROM abiz_audit_integrity " +
            "ORDER BY sequence_num DESC " +
            "LIMIT 1")
    AbizAuditIntegrity findLatestRecord();

    @Select("SELECT id, audit_log_id AS auditLogId, previous_hash AS previousHash, " +
            "       event_hash AS eventHash, sequence_num AS sequenceNum, " +
            "       verified_at AS verifiedAt, created_at AS createdAt " +
            "FROM abiz_audit_integrity " +
            "ORDER BY sequence_num ASC " +
            "LIMIT #{limit} OFFSET #{offset}")
    List<AbizAuditIntegrity> findChainSegment(
            @Param("limit") int limit,
            @Param("offset") int offset
    );

    @Select("SELECT COUNT(1) FROM abiz_audit_integrity")
    long countAll();
}
''')

# 7. SloMeasurementMapper.java
with open(os.path.join(mapper_dir, "SloMeasurementMapper.java"), "w", encoding="utf-8") as f:
    f.write('''package com.abiz.locker.mapper;

import com.abiz.locker.domain.AbizSloMeasurement;
import org.apache.ibatis.annotations.*;

import java.util.List;
import java.util.UUID;

@Mapper
public interface SloMeasurementMapper {

    @Insert("INSERT INTO abiz_slo_measurements (" +
            "  id, metric_name, target_value, actual_value, status, measured_at) " +
            "VALUES (" +
            "  #{id, typeHandler=com.abiz.locker.mapper.UuidTypeHandler}, " +
            "  #{metricName}, #{targetValue}, #{actualValue}, #{status}, #{measuredAt})")
    void insertMeasurement(AbizSloMeasurement measurement);

    @Select("SELECT id, metric_name AS metricName, target_value AS targetValue, " +
            "       actual_value AS actualValue, status, measured_at AS measuredAt " +
            "FROM abiz_slo_measurements " +
            "WHERE metric_name = #{metricName} " +
            "ORDER BY measured_at DESC " +
            "LIMIT #{limit}")
    List<AbizSloMeasurement> findRecentByMetric(
            @Param("metricName") String metricName,
            @Param("limit") int limit
    );

    @Select("SELECT COUNT(1) FROM abiz_slo_measurements")
    long countAll();
}
''')

# 8. AlertEventMapper.java
with open(os.path.join(mapper_dir, "AlertEventMapper.java"), "w", encoding="utf-8") as f:
    f.write('''package com.abiz.locker.mapper;

import com.abiz.locker.domain.AbizAlertEvent;
import org.apache.ibatis.annotations.*;

import java.util.Date;
import java.util.List;
import java.util.UUID;

@Mapper
public interface AlertEventMapper {

    @Insert("INSERT INTO abiz_alert_events (" +
            "  id, alert_key, severity, message, correlation_id, occurrence_count, first_seen_at, last_seen_at, status) " +
            "VALUES (" +
            "  #{id, typeHandler=com.abiz.locker.mapper.UuidTypeHandler}, " +
            "  #{alertKey}, #{severity}, #{message}, #{correlationId}, #{occurrenceCount}, #{firstSeenAt}, #{lastSeenAt}, #{status})")
    void insertAlert(AbizAlertEvent alert);

    @Update("UPDATE abiz_alert_events SET " +
            "  occurrence_count = #{occurrenceCount}, " +
            "  last_seen_at = #{lastSeenAt}, " +
            "  message = #{message} " +
            "WHERE id = #{id, typeHandler=com.abiz.locker.mapper.UuidTypeHandler}")
    int updateAlertOccurrence(AbizAlertEvent alert);

    @Select("SELECT id, alert_key AS alertKey, severity, message, correlation_id AS correlationId, " +
            "       occurrence_count AS occurrenceCount, first_seen_at AS firstSeenAt, " +
            "       last_seen_at AS lastSeenAt, status " +
            "FROM abiz_alert_events " +
            "WHERE alert_key = #{alertKey} AND status = 'ACTIVE' " +
            "ORDER BY last_seen_at DESC " +
            "LIMIT 1")
    AbizAlertEvent findActiveByKey(@Param("alertKey") String alertKey);

    @Select("SELECT id, alert_key AS alertKey, severity, message, correlation_id AS correlationId, " +
            "       occurrence_count AS occurrenceCount, first_seen_at AS firstSeenAt, " +
            "       last_seen_at AS lastSeenAt, status " +
            "FROM abiz_alert_events " +
            "WHERE status = 'ACTIVE' " +
            "ORDER BY last_seen_at DESC " +
            "LIMIT #{limit}")
    List<AbizAlertEvent> findActiveAlerts(@Param("limit") int limit);

    @Select("SELECT COUNT(1) FROM abiz_alert_events WHERE status = 'ACTIVE'")
    long countActiveAlerts();

    @Select("SELECT COUNT(1) FROM abiz_alert_events")
    long countAll();
}
''')

# 9. RateLimitCounterMapper.java
with open(os.path.join(mapper_dir, "RateLimitCounterMapper.java"), "w", encoding="utf-8") as f:
    f.write('''package com.abiz.locker.mapper;

import com.abiz.locker.domain.AbizRateLimitCounter;
import org.apache.ibatis.annotations.*;

import java.util.UUID;

@Mapper
public interface RateLimitCounterMapper {

    @Insert("INSERT INTO abiz_rate_limit_counters (" +
            "  id, counter_key, hit_count, window_start_ms, expires_at_ms, created_at) " +
            "VALUES (" +
            "  #{id, typeHandler=com.abiz.locker.mapper.UuidTypeHandler}, " +
            "  #{counterKey}, #{hitCount}, #{windowStartMs}, #{expiresAtMs}, #{createdAt}) " +
            "ON CONFLICT (counter_key) " +
            "DO UPDATE SET hit_count = abiz_rate_limit_counters.hit_count + 1")
    void incrementCounter(AbizRateLimitCounter counter);

    @Select("SELECT id, counter_key AS counterKey, hit_count AS hitCount, " +
            "       window_start_ms AS windowStartMs, expires_at_ms AS expiresAtMs, created_at AS createdAt " +
            "FROM abiz_rate_limit_counters " +
            "WHERE counter_key = #{counterKey} " +
            "LIMIT 1")
    AbizRateLimitCounter findByKey(@Param("counterKey") String counterKey);

    @Delete("DELETE FROM abiz_rate_limit_counters WHERE expires_at_ms < #{nowMs}")
    int cleanupExpiredCounters(@Param("nowMs") long nowMs);

    @Select("SELECT COUNT(1) FROM abiz_rate_limit_counters")
    long countAll();
}
''')

# 10. SecurityTimelineMapper.java
with open(os.path.join(mapper_dir, "SecurityTimelineMapper.java"), "w", encoding="utf-8") as f:
    f.write('''package com.abiz.locker.mapper;

import com.abiz.locker.domain.AbizSecurityTimeline;
import org.apache.ibatis.annotations.*;

import java.util.List;
import java.util.UUID;

@Mapper
public interface SecurityTimelineMapper {

    @Insert("INSERT INTO abiz_security_timelines (" +
            "  id, correlation_id, event_id, shop_id, device_id, actor_id, event_type, stage, payload_redacted, timestamp) " +
            "VALUES (" +
            "  #{id, typeHandler=com.abiz.locker.mapper.UuidTypeHandler}, " +
            "  #{correlationId}, " +
            "  #{eventId, typeHandler=com.abiz.locker.mapper.UuidTypeHandler}, " +
            "  #{shopId, typeHandler=com.abiz.locker.mapper.UuidTypeHandler}, " +
            "  #{deviceId, typeHandler=com.abiz.locker.mapper.UuidTypeHandler}, " +
            "  #{actorId, typeHandler=com.abiz.locker.mapper.UuidTypeHandler}, " +
            "  #{eventType}, #{stage}, #{payloadRedacted}, #{timestamp})")
    void insertTimelineEvent(AbizSecurityTimeline timeline);

    @Select("SELECT id, correlation_id AS correlationId, event_id AS eventId, " +
            "       shop_id AS shopId, device_id AS deviceId, actor_id AS actorId, " +
            "       event_type AS eventType, stage, payload_redacted AS payloadRedacted, timestamp " +
            "FROM abiz_security_timelines " +
            "WHERE correlation_id = #{correlationId} " +
            "ORDER BY timestamp ASC")
    List<AbizSecurityTimeline> findTimelineByCorrelationId(@Param("correlationId") String correlationId);

    @Select("SELECT COUNT(1) FROM abiz_security_timelines")
    long countAll();
}
''')

print("Created all Phase 19 Mappers successfully")