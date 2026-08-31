import re

# Update FinancedDeviceMapper.java
with open(r'C:\Users\MNA\.gemini\antigravity\scratch\abiz-locker\mdmesh\server\src\main\java\com\abiz\locker\mapper\FinancedDeviceMapper.java', 'r', encoding='utf-8') as f:
    fd_code = f.read()

# Add columns to SELECT queries
old_cols = 'last_successful_command_at AS lastSuccessfulCommandAt,       agent_protocol_version AS agentProtocolVersion,       integrity_status AS integrityStatus,'
new_cols = '''last_successful_command_at AS lastSuccessfulCommandAt,       agent_protocol_version AS agentProtocolVersion,       integrity_status AS integrityStatus,       agent_version_code AS agentVersionCode,       upgrade_status AS upgradeStatus,       quarantine_status AS quarantineStatus,       quarantine_reason AS quarantineReason,       quarantined_at AS quarantinedAt,       last_integrity_check_at AS lastIntegrityCheckAt,'''

fd_code = fd_code.replace(old_cols, new_cols)

# Add Phase 19 mapper methods before closing brace
p19_fd_methods = '''
    @Update("UPDATE abiz_financed_devices SET " +
            "  quarantine_status = #{quarantineStatus}, " +
            "  quarantine_reason = #{quarantineReason}, " +
            "  quarantined_at = #{quarantinedAt}, " +
            "  updated_at = NOW() " +
            "WHERE id = #{id, typeHandler=com.abiz.locker.mapper.UuidTypeHandler}")
    int updateQuarantineStatus(
            @Param("id") UUID id,
            @Param("quarantineStatus") String quarantineStatus,
            @Param("quarantineReason") String quarantineReason,
            @Param("quarantinedAt") Date quarantinedAt
    );

    @Update("UPDATE abiz_financed_devices SET " +
            "  upgrade_status = #{upgradeStatus}, " +
            "  agent_version_code = #{agentVersionCode}, " +
            "  agent_version = #{agentVersion}, " +
            "  updated_at = NOW() " +
            "WHERE id = #{id, typeHandler=com.abiz.locker.mapper.UuidTypeHandler}")
    int updateUpgradeStatus(
            @Param("id") UUID id,
            @Param("upgradeStatus") String upgradeStatus,
            @Param("agentVersionCode") int agentVersionCode,
            @Param("agentVersion") String agentVersion
    );

    @Update("UPDATE abiz_financed_devices SET " +
            "  last_integrity_check_at = #{lastIntegrityCheckAt}, " +
            "  updated_at = NOW() " +
            "WHERE id = #{id, typeHandler=com.abiz.locker.mapper.UuidTypeHandler}")
    int updateLastIntegrityCheck(
            @Param("id") UUID id,
            @Param("lastIntegrityCheckAt") Date lastIntegrityCheckAt
    );

    @Select("SELECT COUNT(1) FROM abiz_financed_devices WHERE quarantine_status = #{quarantineStatus}")
    long countDevicesByQuarantine(@Param("quarantineStatus") String quarantineStatus);
}
'''

fd_code = fd_code.rstrip().rstrip('}') + "\n" + p19_fd_methods

with open(r'C:\Users\MNA\.gemini\antigravity\scratch\abiz-locker\mdmesh\server\src\main\java\com\abiz\locker\mapper\FinancedDeviceMapper.java', 'w', encoding='utf-8') as f:
    f.write(fd_code)

print("Updated FinancedDeviceMapper with Phase 19 fields and methods")