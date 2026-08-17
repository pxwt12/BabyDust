package com.babydust.api.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "medical_reports")
public class MedicalReport extends BaseEntity {
    @Column(nullable = false)
    private UUID familyId;

    @Column(nullable = false)
    private String subjectType;

    @Column(nullable = false)
    private UUID subjectId;

    @Column(nullable = false)
    private String reportType;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private LocalDate examinedAt;

    @Column(nullable = false, columnDefinition = "text")
    private String indicatorsJson;

    protected MedicalReport() {
    }

    public MedicalReport(UUID familyId, String subjectType, UUID subjectId, String reportType, String title, LocalDate examinedAt, String indicatorsJson) {
        this.familyId = familyId;
        this.subjectType = subjectType;
        this.subjectId = subjectId;
        this.reportType = reportType;
        this.title = title;
        this.examinedAt = examinedAt;
        this.indicatorsJson = indicatorsJson;
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

    public String getReportType() {
        return reportType;
    }

    public String getTitle() {
        return title;
    }

    public LocalDate getExaminedAt() {
        return examinedAt;
    }

    public String getIndicatorsJson() {
        return indicatorsJson;
    }

    public void update(String title, LocalDate examinedAt, String indicatorsJson) {
        this.title = title;
        this.examinedAt = examinedAt;
        this.indicatorsJson = indicatorsJson;
        touch();
    }
}
