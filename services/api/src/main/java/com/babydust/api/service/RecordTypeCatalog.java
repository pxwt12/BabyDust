package com.babydust.api.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class RecordTypeCatalog {
    private static final List<RecordTypeDefinition> DEFINITIONS = List.of(
            new RecordTypeDefinition("weight", "pregnancy", "number", List.of("weightKg")),
            new RecordTypeDefinition("blood_pressure", "pregnancy", "object", List.of("systolic", "diastolic")),
            new RecordTypeDefinition("symptom", "pregnancy", "object", List.of("name", "severity")),
            new RecordTypeDefinition("medication", "pregnancy", "object", List.of("name", "dose")),
            new RecordTypeDefinition("supplement", "pregnancy", "object", List.of("name", "dose")),
            new RecordTypeDefinition("fetal_movement", "pregnancy", "object", List.of("count", "durationMinutes")),
            new RecordTypeDefinition("mood", "pregnancy", "object", List.of("mood")),
            new RecordTypeDefinition("note", "pregnancy", "object", List.of("text")),
            new RecordTypeDefinition("fertility_cycle", "family", "object", List.of("cycleDay")),
            new RecordTypeDefinition("ovulation_test", "family", "object", List.of("result")),
            new RecordTypeDefinition("intercourse", "family", "object", List.of("note")),
            new RecordTypeDefinition("basal_temperature", "family", "object", List.of("temperatureC")),
            new RecordTypeDefinition("fertility_supplement", "family", "object", List.of("name", "dose")),
            new RecordTypeDefinition("delivery_event", "family", "object", List.of("event", "note")),
            new RecordTypeDefinition("contraction", "family", "object", List.of("durationSeconds", "intervalMinutes")),
            new RecordTypeDefinition("delivery_note", "family", "object", List.of("text")),
            new RecordTypeDefinition("postpartum_lochia", "family", "object", List.of("level")),
            new RecordTypeDefinition("postpartum_mood", "family", "object", List.of("mood")),
            new RecordTypeDefinition("postpartum_medication", "family", "object", List.of("name", "dose")),
            new RecordTypeDefinition("postpartum_note", "family", "object", List.of("text")),
            new RecordTypeDefinition("baby_feeding", "baby", "object", List.of("amountMl")),
            new RecordTypeDefinition("baby_sleep", "baby", "object", List.of("durationMinutes")),
            new RecordTypeDefinition("baby_diaper", "baby", "object", List.of("type")),
            new RecordTypeDefinition("baby_growth", "baby", "object", List.of("babyWeightKg", "heightCm")),
            new RecordTypeDefinition("baby_note", "baby", "object", List.of("text"))
    );

    private static final Map<String, RecordTypeDefinition> BY_TYPE = DEFINITIONS.stream()
            .collect(java.util.stream.Collectors.toUnmodifiableMap(RecordTypeDefinition::type, definition -> definition));

    private final ObjectMapper objectMapper;

    public RecordTypeCatalog(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public List<RecordTypeDefinition> definitions() {
        return DEFINITIONS;
    }

    public RecordTypeDefinition requireDefinition(String recordType) {
        RecordTypeDefinition definition = BY_TYPE.get(recordType);
        if (definition == null) {
            throw new IllegalArgumentException("Unsupported record type: " + recordType);
        }
        return definition;
    }

    public void validatePayload(String recordType, String payloadJson) {
        RecordTypeDefinition definition = requireDefinition(recordType);
        JsonNode payload = parse(payloadJson);
        if (!payload.isObject()) {
            throw new IllegalArgumentException("Record payload must be a JSON object");
        }
        for (String field : definition.requiredFields()) {
            JsonNode value = payload.get(field);
            if (value == null || value.isNull() || isBlankString(value)) {
                throw new IllegalArgumentException("Missing required payload field: " + field);
            }
        }
        validateByType(recordType, payload);
    }

    private JsonNode parse(String payloadJson) {
        try {
            return objectMapper.readTree(payloadJson);
        } catch (Exception ex) {
            throw new IllegalArgumentException("Record payload must be valid JSON");
        }
    }

    private boolean isBlankString(JsonNode value) {
        return value.isTextual() && value.asText().isBlank();
    }

    private void validateByType(String recordType, JsonNode payload) {
        switch (recordType) {
            case "weight" -> requireRange(payload, "weightKg", 20, 250);
            case "blood_pressure" -> {
                requireRange(payload, "systolic", 60, 240);
                requireRange(payload, "diastolic", 30, 160);
            }
            case "symptom" -> requireRange(payload, "severity", 1, 5);
            case "fetal_movement" -> {
                requireRange(payload, "count", 0, 500);
                requireRange(payload, "durationMinutes", 1, 240);
            }
            case "fertility_cycle" -> requireRange(payload, "cycleDay", 1, 120);
            case "basal_temperature" -> requireRange(payload, "temperatureC", 34, 43);
            case "contraction" -> {
                requireRange(payload, "durationSeconds", 1, 600);
                requireRange(payload, "intervalMinutes", 0, 120);
            }
            case "baby_feeding" -> requireRange(payload, "amountMl", 0, 300);
            case "baby_sleep" -> requireRange(payload, "durationMinutes", 1, 1440);
            case "baby_growth" -> {
                requireRange(payload, "babyWeightKg", 0.2, 30);
                requireRange(payload, "heightCm", 20, 120);
            }
            default -> {
            }
        }
    }

    private void requireRange(JsonNode payload, String field, double min, double max) {
        JsonNode value = payload.get(field);
        if (!value.isNumber()) {
            throw new IllegalArgumentException("Payload field must be numeric: " + field);
        }
        double number = value.asDouble();
        if (number < min || number > max) {
            throw new IllegalArgumentException("Payload field out of range: " + field);
        }
    }

    public record RecordTypeDefinition(String type, String subjectType, String valueKind, List<String> requiredFields) {
    }
}
