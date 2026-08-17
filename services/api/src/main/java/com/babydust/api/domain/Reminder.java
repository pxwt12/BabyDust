package com.babydust.api.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "reminders")
public class Reminder extends BaseEntity {
    @Column(nullable = false)
    private UUID familyId;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String scene;

    private String subjectType;

    private UUID subjectId;

    @Column(nullable = false)
    private OffsetDateTime triggerAt;

    @Column(nullable = false)
    private String status = "scheduled";

    protected Reminder() {
    }

    public Reminder(UUID familyId, String title, String scene, OffsetDateTime triggerAt) {
        this(familyId, title, scene, null, null, triggerAt);
    }

    public Reminder(UUID familyId, String title, String scene, String subjectType, UUID subjectId, OffsetDateTime triggerAt) {
        this.familyId = familyId;
        this.title = title;
        this.scene = scene;
        this.subjectType = subjectType;
        this.subjectId = subjectId;
        this.triggerAt = triggerAt;
    }

    public UUID getFamilyId() {
        return familyId;
    }

    public String getTitle() {
        return title;
    }

    public String getScene() {
        return scene;
    }

    public String getSubjectType() {
        return subjectType;
    }

    public UUID getSubjectId() {
        return subjectId;
    }

    public OffsetDateTime getTriggerAt() {
        return triggerAt;
    }

    public String getStatus() {
        return status;
    }

    public void markStatus(String status) {
        this.status = status;
        touch();
    }
}
