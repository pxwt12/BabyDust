package com.babydust.api.repository;

import com.babydust.api.domain.AiAuditLog;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface AiAuditLogRepository extends JpaRepository<AiAuditLog, UUID>, JpaSpecificationExecutor<AiAuditLog> {
    List<AiAuditLog> findTop20ByOrderByCreatedAtDesc();
}
