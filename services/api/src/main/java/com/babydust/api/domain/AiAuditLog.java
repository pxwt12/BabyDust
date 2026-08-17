package com.babydust.api.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "ai_audit_logs")
public class AiAuditLog extends BaseEntity {
    private UUID familyId;

    private UUID userId;

    @Column(nullable = false)
    private String purpose;

    @Column(nullable = false)
    private String provider;

    @Column(nullable = false)
    private String model;

    @Column(nullable = false)
    private String inputType;

    @Column(nullable = false)
    private Integer inputLength;

    @Column(nullable = false)
    private String inputPreview;

    @Column(nullable = false)
    private String riskLevel;

    @Column(nullable = false)
    private Boolean blocked;

    @Column(nullable = false)
    private Boolean fallbackUsed;

    @Column(nullable = false)
    private String errorCode;

    @Column(nullable = false)
    private Integer promptTokens;

    @Column(nullable = false)
    private Integer completionTokens;

    @Column(nullable = false)
    private Integer totalTokens;

    @Column(nullable = false)
    private Long latencyMs;

    @Column(nullable = false)
    private String costCurrency;

    @Column(nullable = false)
    private BigDecimal estimatedCost;

    @Column(nullable = false)
    private String status;

    @Column(nullable = false)
    private String policyVersion;

    @Column(nullable = false)
    private Boolean policyConfigured;

    @Column(nullable = false)
    private String safetyPolicy;

    @Column(nullable = false)
    private String riskReasons;

    protected AiAuditLog() {
    }

    public AiAuditLog(UUID familyId, UUID userId, String purpose, String provider, String model, String inputType, Integer inputLength, String inputPreview, String riskLevel, Boolean blocked, Boolean fallbackUsed, String errorCode, Integer promptTokens, Integer completionTokens, Integer totalTokens, Long latencyMs, String costCurrency, BigDecimal estimatedCost, String status, String policyVersion, Boolean policyConfigured, String safetyPolicy, String riskReasons) {
        this.familyId = familyId;
        this.userId = userId;
        this.purpose = purpose;
        this.provider = provider;
        this.model = model;
        this.inputType = inputType;
        this.inputLength = inputLength;
        this.inputPreview = inputPreview;
        this.riskLevel = riskLevel;
        this.blocked = blocked;
        this.fallbackUsed = fallbackUsed;
        this.errorCode = errorCode;
        this.promptTokens = promptTokens;
        this.completionTokens = completionTokens;
        this.totalTokens = totalTokens;
        this.latencyMs = latencyMs;
        this.costCurrency = costCurrency;
        this.estimatedCost = estimatedCost;
        this.status = status;
        this.policyVersion = policyVersion;
        this.policyConfigured = policyConfigured;
        this.safetyPolicy = safetyPolicy;
        this.riskReasons = riskReasons;
    }

    public UUID getFamilyId() {
        return familyId;
    }

    public UUID getUserId() {
        return userId;
    }

    public String getPurpose() {
        return purpose;
    }

    public String getProvider() {
        return provider;
    }

    public String getModel() {
        return model;
    }

    public String getInputType() {
        return inputType;
    }

    public Integer getInputLength() {
        return inputLength;
    }

    public String getInputPreview() {
        return inputPreview;
    }

    public String getRiskLevel() {
        return riskLevel;
    }

    public Boolean getBlocked() {
        return blocked;
    }

    public Boolean getFallbackUsed() {
        return fallbackUsed;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public Integer getPromptTokens() {
        return promptTokens;
    }

    public Integer getCompletionTokens() {
        return completionTokens;
    }

    public Integer getTotalTokens() {
        return totalTokens;
    }

    public Long getLatencyMs() {
        return latencyMs;
    }

    public String getCostCurrency() {
        return costCurrency;
    }

    public BigDecimal getEstimatedCost() {
        return estimatedCost;
    }

    public String getStatus() {
        return status;
    }

    public String getPolicyVersion() {
        return policyVersion;
    }

    public Boolean getPolicyConfigured() {
        return policyConfigured;
    }

    public String getSafetyPolicy() {
        return safetyPolicy;
    }

    public String getRiskReasons() {
        return riskReasons;
    }
}
