package com.babydust.api.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "todos")
public class TodoItem extends BaseEntity {
    @Column(nullable = false)
    private UUID familyId;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String category;

    private String subjectType;

    private UUID subjectId;

    private OffsetDateTime dueAt;

    @Column(nullable = false)
    private String status = "pending";

    protected TodoItem() {
    }

    public TodoItem(UUID familyId, String title, String category, OffsetDateTime dueAt) {
        this(familyId, title, category, null, null, dueAt);
    }

    public TodoItem(UUID familyId, String title, String category, String subjectType, UUID subjectId, OffsetDateTime dueAt) {
        this.familyId = familyId;
        this.title = title;
        this.category = category;
        this.subjectType = subjectType;
        this.subjectId = subjectId;
        this.dueAt = dueAt;
    }

    public UUID getFamilyId() {
        return familyId;
    }

    public String getTitle() {
        return title;
    }

    public String getCategory() {
        return category;
    }

    public String getSubjectType() {
        return subjectType;
    }

    public UUID getSubjectId() {
        return subjectId;
    }

    public OffsetDateTime getDueAt() {
        return dueAt;
    }

    public String getStatus() {
        return status;
    }

    public void markStatus(String status) {
        this.status = status;
        touch();
    }
}
