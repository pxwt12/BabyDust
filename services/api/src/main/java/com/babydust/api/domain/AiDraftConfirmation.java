package com.babydust.api.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "ai_draft_confirmations")
public class AiDraftConfirmation extends BaseEntity {
    @Column(nullable = false)
    private UUID familyId;

    @Column(nullable = false)
    private UUID userId;

    @Column(nullable = false)
    private String subjectType;

    @Column(nullable = false)
    private UUID subjectId;

    @Column(nullable = false)
    private String provider;

    @Column(nullable = false)
    private String model;

    @Column(nullable = false)
    private String purpose;

    @Column(nullable = false)
    private String draftPreview;

    @Column(nullable = false, columnDefinition = "text")
    private String recordIdsJson;

    @Column(nullable = false, columnDefinition = "text")
    private String reportIdsJson;

    @Column(nullable = false, columnDefinition = "text")
    private String todoIdsJson;

    @Column(nullable = false)
    private OffsetDateTime confirmedAt;

    protected AiDraftConfirmation() {
    }

    public AiDraftConfirmation(UUID familyId, UUID userId, String subjectType, UUID subjectId, String provider, String model, String purpose, String draftPreview, String recordIdsJson, String reportIdsJson, String todoIdsJson, OffsetDateTime confirmedAt) {
        this.familyId = familyId;
        this.userId = userId;
        this.subjectType = subjectType;
        this.subjectId = subjectId;
        this.provider = provider;
        this.model = model;
        this.purpose = purpose;
        this.draftPreview = draftPreview;
        this.recordIdsJson = recordIdsJson;
        this.reportIdsJson = reportIdsJson;
        this.todoIdsJson = todoIdsJson;
        this.confirmedAt = confirmedAt;
    }

    public UUID getFamilyId() {
        return familyId;
    }

    public UUID getUserId() {
        return userId;
    }

    public String getSubjectType() {
        return subjectType;
    }

    public UUID getSubjectId() {
        return subjectId;
    }

    public String getProvider() {
        return provider;
    }

    public String getModel() {
        return model;
    }

    public String getPurpose() {
        return purpose;
    }

    public String getDraftPreview() {
        return draftPreview;
    }

    public String getRecordIdsJson() {
        return recordIdsJson;
    }

    public String getReportIdsJson() {
        return reportIdsJson;
    }

    public String getTodoIdsJson() {
        return todoIdsJson;
    }

    public OffsetDateTime getConfirmedAt() {
        return confirmedAt;
    }
}
