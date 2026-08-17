package com.babydust.api.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "records")
public class JsonRecord extends BaseEntity {
    @Column(nullable = false)
    private UUID familyId;

    @Column(nullable = false)
    private String subjectType;

    @Column(nullable = false)
    private UUID subjectId;

    @Column(nullable = false)
    private String recordType;

    @Column(nullable = false)
    private OffsetDateTime occurredAt;

    @Column(nullable = false, columnDefinition = "text")
    private String payloadJson;

    @Column(nullable = false)
    private String privacyLevel;

    protected JsonRecord() {
    }

    public JsonRecord(UUID familyId, String subjectType, UUID subjectId, String recordType, OffsetDateTime occurredAt, String payloadJson) {
        this.familyId = familyId;
        this.subjectType = subjectType;
        this.subjectId = subjectId;
        this.recordType = recordType;
        this.occurredAt = occurredAt;
        this.payloadJson = payloadJson;
        this.privacyLevel = "family";
    }

    public UUID getFamilyId() {
        return familyId;
    }

    public String getSubjectType() {
        return subjectType;
    }

    public UUID getSubjectId() {
        return subjectId;
    }

    public String getRecordType() {
        return recordType;
    }

    public OffsetDateTime getOccurredAt() {
        return occurredAt;
    }

    public String getPayloadJson() {
        return payloadJson;
    }

    public String getPrivacyLevel() {
        return privacyLevel;
    }

    public void update(OffsetDateTime occurredAt, String payloadJson) {
        this.occurredAt = occurredAt;
        this.payloadJson = payloadJson;
        touch();
    }
}
