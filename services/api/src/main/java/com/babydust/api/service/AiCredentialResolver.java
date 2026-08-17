package com.babydust.api.service;

import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
public class AiCredentialResolver {
    private final Environment environment;

    public AiCredentialResolver(Environment environment) {
        this.environment = environment;
    }

    public String resolve(String credentialRef, String directValue) {
        if (directValue != null && !directValue.isBlank()) {
            return directValue;
        }
        if (credentialRef == null || credentialRef.isBlank()) {
            return "";
        }
        if (credentialRef.startsWith("env:")) {
            String property = credentialRef.substring("env:".length());
            return environment.getProperty(property, "");
        }
        return "";
    }
}
