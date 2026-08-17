package com.babydust.api.service;

import com.babydust.api.domain.JsonRecord;
import com.babydust.api.repository.JsonRecordRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class AnalyticsService {
    private final JsonRecordRepository records;
    private final ObjectMapper objectMapper;

    public AnalyticsService(JsonRecordRepository records, ObjectMapper objectMapper) {
        this.records = records;
        this.objectMapper = objectMapper;
    }

    public SeriesResponse series(UUID familyId, String metric) {
        MetricDefinition definition = MetricDefinition.fromMetric(metric);
        List<SeriesPoint> points = records.findTop200ByFamilyIdAndRecordTypeOrderByOccurredAtAsc(familyId, definition.recordType()).stream()
                .map(record -> toPoint(record, definition))
                .flatMap(java.util.Optional::stream)
                .toList();
        return new SeriesResponse(metric, definition.unit(), points);
    }

    private java.util.Optional<SeriesPoint> toPoint(JsonRecord record, MetricDefinition definition) {
        try {
            JsonNode payload = objectMapper.readTree(record.getPayloadJson());
            JsonNode value = payload.get(definition.payloadField());
            if (value == null || !value.isNumber()) {
                return java.util.Optional.empty();
            }
            return java.util.Optional.of(new SeriesPoint(record.getOccurredAt(), BigDecimal.valueOf(value.asDouble())));
        } catch (Exception ex) {
            return java.util.Optional.empty();
        }
    }

    public record SeriesResponse(String metric, String unit, List<SeriesPoint> points) {
    }

    public record SeriesPoint(OffsetDateTime occurredAt, BigDecimal value) {
    }

    private record MetricDefinition(String metric, String recordType, String payloadField, String unit) {
        static MetricDefinition fromMetric(String metric) {
            return switch (metric) {
                case "weight" -> new MetricDefinition(metric, "weight", "weightKg", "kg");
                case "blood_pressure_systolic" -> new MetricDefinition(metric, "blood_pressure", "systolic", "mmHg");
                case "blood_pressure_diastolic" -> new MetricDefinition(metric, "blood_pressure", "diastolic", "mmHg");
                case "fetal_movement" -> new MetricDefinition(metric, "fetal_movement", "count", "count");
                case "baby_weight" -> new MetricDefinition(metric, "baby_growth", "babyWeightKg", "kg");
                case "baby_height" -> new MetricDefinition(metric, "baby_growth", "heightCm", "cm");
                default -> throw new IllegalArgumentException("Unsupported analytics metric: " + metric);
            };
        }
    }
}
