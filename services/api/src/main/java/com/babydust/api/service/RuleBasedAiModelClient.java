package com.babydust.api.service;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class RuleBasedAiModelClient implements AiModelClient {
    @Override
    public AiModelResult extract(AiModelRequest request) {
        if ("report_extraction".equals(request.purpose())) {
            return new AiModelResult(
                    true,
                    "MODEL_CLIENT_NOT_CONFIGURED",
                    "Rule-based report fallback",
                    List.of(),
                    List.of(),
                    List.of(Map.of(
                            "reportType", guessReportType(request.text()),
                            "title", "AI report draft",
                    "rawText", request.text(),
                    "indicators", List.of()
                    )),
                    0,
                    0,
                    0,
                    "CNY",
                    java.math.BigDecimal.ZERO
            );
        }
        return new AiModelResult(
                true,
                "MODEL_CLIENT_NOT_CONFIGURED",
                "Rule-based record fallback",
                recordDrafts(request.text()),
                todoDrafts(request.text()),
                List.of(),
                0,
                0,
                0,
                "CNY",
                java.math.BigDecimal.ZERO
        );
    }

    private List<Map<String, Object>> recordDrafts(String text) {
        String normalized = text.toLowerCase(Locale.ROOT);
        if (normalized.contains("血压") || normalized.contains("blood pressure")) {
            return List.of(Map.of(
                    "recordType", "blood_pressure",
                    "payload", Map.of("systolic", 120, "diastolic", 80, "sourceText", text)
            ));
        }
        if (normalized.contains("体重") || normalized.contains("weight")) {
            return List.of(Map.of(
                    "recordType", "weight",
                    "payload", Map.of("weightKg", 56, "sourceText", text)
            ));
        }
        if (normalized.contains("胎动") || normalized.contains("kick")) {
            return List.of(Map.of(
                    "recordType", "fetal_movement",
                    "payload", Map.of("count", 10, "durationMinutes", 30, "sourceText", text)
            ));
        }
        return List.of(Map.of("recordType", "note", "payload", Map.of("text", text)));
    }

    private List<Map<String, Object>> todoDrafts(String text) {
        String normalized = text.toLowerCase(Locale.ROOT);
        if (normalized.contains("提醒") || normalized.contains("todo") || normalized.contains("复查")) {
            return List.of(Map.of(
                    "title", text.length() > 40 ? text.substring(0, 40) : text,
                    "category", "custom",
                    "source", "ai_draft"
            ));
        }
        return List.of();
    }

    private String guessReportType(String text) {
        String normalized = text.toLowerCase(Locale.ROOT);
        if (normalized.contains("hcg") || normalized.contains("孕酮") || normalized.contains("血")) {
            return "blood";
        }
        if (normalized.contains("b超") || normalized.contains("超声") || normalized.contains("ultrasound")) {
            return "ultrasound";
        }
        return "unknown";
    }
}
