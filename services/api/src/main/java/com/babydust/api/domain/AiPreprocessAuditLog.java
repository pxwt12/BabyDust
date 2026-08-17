package com.babydust.api.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "ai_preprocess_audit_logs")
public class AiPreprocessAuditLog extends BaseEntity {
    @Column(nullable = false)
    private String purpose;

    @Column(nullable = false)
    private String provider;

    @Column(nullable = false)
    private String preprocessor;

    @Column(nullable = false)
    private String fileUrlPreview;

    @Column(nullable = false)
    private Integer textLength;

    @Column(nullable = false)
    private Boolean fallbackUsed;

    @Column(nullable = false)
    private String errorCode;

    @Column(nullable = false)
    private Long latencyMs;

    @Column(nullable = false)
    private String status;

    protected AiPreprocessAuditLog() {
    }

    public AiPreprocessAuditLog(String purpose, String provider, String preprocessor, String fileUrlPreview, Integer textLength, Boolean fallbackUsed, String errorCode, Long latencyMs, String status) {
        this.purpose = purpose;
        this.provider = provider;
        this.preprocessor = preprocessor;
        this.fileUrlPreview = fileUrlPreview;
        this.textLength = textLength;
        this.fallbackUsed = fallbackUsed;
        this.errorCode = errorCode;
        this.latencyMs = latencyMs;
        this.status = status;
    }

    public String getPurpose() {
        return purpose;
    }

    public String getProvider() {
        return provider;
    }

    public String getPreprocessor() {
        return preprocessor;
    }

    public String getFileUrlPreview() {
        return fileUrlPreview;
    }

    public Integer getTextLength() {
        return textLength;
    }

    public Boolean getFallbackUsed() {
        return fallbackUsed;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public Long getLatencyMs() {
        return latencyMs;
    }

    public String getStatus() {
        return status;
    }
}
