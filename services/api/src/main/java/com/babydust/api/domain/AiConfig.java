package com.babydust.api.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "ai_configs")
public class AiConfig extends BaseEntity {
    @Column(nullable = false)
    private String configType;

    @Column(nullable = false)
    private String configKey;

    @Column(nullable = false)
    private String displayName;

    @Column(nullable = false)
    private String provider;

    @Column(nullable = false)
    private String status;

    @Column(nullable = false, columnDefinition = "text")
    private String configJson;

    @Column(nullable = false)
    private String versionLabel;

    @Column(nullable = false)
    private String createdBy;

    protected AiConfig() {
    }

    public AiConfig(String configType, String configKey, String displayName, String provider, String status, String configJson, String versionLabel, String createdBy) {
        this.configType = configType;
        this.configKey = configKey;
        this.displayName = displayName;
        this.provider = provider;
        this.status = status;
        this.configJson = configJson;
        this.versionLabel = versionLabel;
        this.createdBy = createdBy;
    }

    public String getConfigType() {
        return configType;
    }

    public String getConfigKey() {
        return configKey;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getProvider() {
        return provider;
    }

    public String getStatus() {
        return status;
    }

    public String getConfigJson() {
        return configJson;
    }

    public String getVersionLabel() {
        return versionLabel;
    }

    public String getCreatedBy() {
        return createdBy;
    }
}
