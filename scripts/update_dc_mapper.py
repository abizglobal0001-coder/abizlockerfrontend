import re

with open(r'C:\Users\MNA\.gemini\antigravity\scratch\abiz-locker\mdmesh\server\src\main\java\com\abiz\locker\mapper\DeviceCommandMapper.java', 'r', encoding='utf-8') as f:
    dc_code = f.read()

# Replace column list in INSERT
old_insert_cols = 'dead_letter_reason, last_error_code, last_error_at, attempt_count, max_attempts, next_retry_at, completed_at)'
new_insert_cols = 'dead_letter_reason, last_error_code, last_error_at, attempt_count, max_attempts, next_retry_at, completed_at, priority, queue_partition, scheduled_at, processing_started_at, processing_completed_at, last_worker_id)'

old_insert_vals = '#{deadLetterReason}, #{lastErrorCode}, #{lastErrorAt}, #{attemptCount}, #{maxAttempts}, #{nextRetryAt}, #{completedAt})'
new_insert_vals = '#{deadLetterReason}, #{lastErrorCode}, #{lastErrorAt}, #{attemptCount}, #{maxAttempts}, #{nextRetryAt}, #{completedAt}, #{priority}, #{queuePartition}, #{scheduledAt}, #{processingStartedAt}, #{processingCompletedAt}, #{lastWorkerId})'

dc_code = dc_code.replace(old_insert_cols, new_insert_cols)
dc_code = dc_code.replace(old_insert_vals, new_insert_vals)

# Replace column list in UPDATE
old_update = '''  completed_at = #{completedAt},
  updated_at = NOW() '''
new_update = '''  completed_at = #{completedAt},
  priority = #{priority},
  queue_partition = #{queuePartition},
  scheduled_at = #{scheduledAt},
  processing_started_at = #{processingStartedAt},
  processing_completed_at = #{processingCompletedAt},
  last_worker_id = #{lastWorkerId},
  updated_at = NOW() '''

dc_code = dc_code.replace(old_update, new_update)

# Replace SELECT column projections
old_select_cols = 'max_attempts AS maxAttempts, next_retry_at AS nextRetryAt, completed_at AS completedAt'
new_select_cols = 'max_attempts AS maxAttempts, next_retry_at AS nextRetryAt, completed_at AS completedAt, priority AS priority, queue_partition AS queuePartition, scheduled_at AS scheduledAt, processing_started_at AS processingStartedAt, processing_completed_at AS processingCompletedAt, last_worker_id AS lastWorkerId'

dc_code = dc_code.replace(old_select_cols, new_select_cols)

# Add Phase 19 priority methods
p19_methods = '''
    @Select("SELECT id, shop_id AS shopId, financed_device_id AS financedDeviceId, " +
            "       command_type AS commandType, status, payload, push_token AS pushToken, " +
            "       idempotency_key AS idempotencyKey, correlation_id AS correlationId, " +
            "       retry_count AS retryCount, max_retries AS maxRetries, " +
            "       failure_reason AS failureReason, ack_status AS ackStatus, " +
            "       ack_detail AS ackDetail, agent_version AS agentVersion, " +
            "       created_by AS createdBy, created_at AS createdAt, " +
            "       dispatched_at AS dispatchedAt, acknowledged_at AS acknowledgedAt, " +
            "       executed_at AS executedAt, expires_at AS expiresAt, updated_at AS updatedAt, " +
            "       claimed_at AS claimedAt, claimed_by AS claimedBy, last_retry_at AS lastRetryAt, " +
            "       nonce, request_id AS requestId, protocol_version AS protocolVersion, " +
            "       dead_lettered_at AS deadLetteredAt, failure_code AS failureCode, " +
            "       reconciliation_state AS reconciliationState, " +
            "       dead_letter_reason AS deadLetterReason, last_error_code AS lastErrorCode, " +
            "       last_error_at AS lastErrorAt, attempt_count AS attemptCount, " +
            "       max_attempts AS maxAttempts, next_retry_at AS nextRetryAt, completed_at AS completedAt, " +
            "       priority AS priority, queue_partition AS queuePartition, scheduled_at AS scheduledAt, " +
            "       processing_started_at AS processingStartedAt, processing_completed_at AS processingCompletedAt, " +
            "       last_worker_id AS lastWorkerId " +
            "FROM abiz_device_commands " +
            "WHERE status = 'PENDING' " +
            "ORDER BY CASE priority " +
            "           WHEN 'CRITICAL' THEN 1 " +
            "           WHEN 'HIGH' THEN 2 " +
            "           WHEN 'NORMAL' THEN 3 " +
            "           WHEN 'LOW' THEN 4 " +
            "           ELSE 5 END ASC, created_at ASC " +
            "LIMIT #{limit}")
    List<AbizDeviceCommand> findPendingByPriority(@Param("limit") int limit);

    @Update("UPDATE abiz_device_commands SET " +
            "  status = 'DISPATCHING', " +
            "  claimed_at = NOW(), " +
            "  claimed_by = #{workerId}, " +
            "  last_worker_id = #{workerId}, " +
            "  processing_started_at = NOW(), " +
            "  updated_at = NOW() " +
            "WHERE id = #{commandId, typeHandler=com.abiz.locker.mapper.UuidTypeHandler} " +
            "  AND status = 'PENDING'")
    int claimCommandWithWorker(
            @Param("commandId") UUID commandId,
            @Param("workerId") String workerId
    );

    @Select("SELECT COUNT(1) FROM abiz_device_commands WHERE priority = #{priority}")
    long countCommandsByPriority(@Param("priority") String priority);
}
'''

dc_code = dc_code.rstrip().rstrip('}') + "\n" + p19_methods

with open(r'C:\Users\MNA\.gemini\antigravity\scratch\abiz-locker\mdmesh\server\src\main\java\com\abiz\locker\mapper\DeviceCommandMapper.java', 'w', encoding='utf-8') as f:
    f.write(dc_code)

print("Updated DeviceCommandMapper with Phase 19 fields and priority methods")