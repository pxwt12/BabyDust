package com.babydust.api.repository;

import com.babydust.api.domain.JsonRecord;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface JsonRecordRepository extends JpaRepository<JsonRecord, UUID>, JpaSpecificationExecutor<JsonRecord> {
    List<JsonRecord> findTop50ByFamilyIdOrderByOccurredAtDesc(UUID familyId);

    List<JsonRecord> findTop500ByFamilyIdOrderByOccurredAtAsc(UUID familyId);

    List<JsonRecord> findTop200ByFamilyIdAndRecordTypeOrderByOccurredAtAsc(UUID familyId, String recordType);

    long countByFamilyIdAndRecordType(UUID familyId, String recordType);

    List<JsonRecord> findTop100ByFamilyIdAndSubjectTypeAndSubjectIdOrderByOccurredAtDesc(UUID familyId, String subjectType, UUID subjectId);

    List<JsonRecord> findTop100ByFamilyIdAndRecordTypeAndOccurredAtBetweenOrderByOccurredAtDesc(UUID familyId, String recordType, OffsetDateTime from, OffsetDateTime to);
}
