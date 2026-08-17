package com.babydust.api.service;

import org.springframework.stereotype.Component;

@Component
public class AiTextSanitizer {
    public String preview(String text, int maxLength) {
        if (text == null || text.isBlank()) {
            return "";
        }
        String compact = text.replaceAll("\\s+", " ").trim();
        String redacted = compact
                .replaceAll("\\b1\\d{10}\\b", "***PHONE***")
                .replaceAll("[\\w.%+-]+@[\\w.-]+\\.[A-Za-z]{2,}", "***EMAIL***")
                .replaceAll("\\b[1-9]\\d{5}(18|19|20)\\d{2}(0[1-9]|1[0-2])(0[1-9]|[12]\\d|3[01])\\d{3}[0-9Xx]\\b", "***ID_CARD***")
                .replaceAll("(?i)bearer\\s+[A-Za-z0-9._\\-]+", "Bearer ***TOKEN***")
                .replaceAll("(?i)(api[_-]?key|authorization|token|secret)\\s*[:=]\\s*[A-Za-z0-9._\\-]+", "$1=***SECRET***")
                .replaceAll("sk-[A-Za-z0-9._\\-]{8,}", "sk-***SECRET***");
        return redacted.length() > maxLength ? redacted.substring(0, maxLength) : redacted;
    }
}
