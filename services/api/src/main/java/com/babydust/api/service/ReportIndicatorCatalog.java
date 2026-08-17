package com.babydust.api.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class ReportIndicatorCatalog {
    private static final List<IndicatorDefinition> DEFINITIONS = List.of(
            new IndicatorDefinition("hcg", "HCG", "mIU/mL", "blood", "number"),
            new IndicatorDefinition("progesterone", "Progesterone", "ng/mL", "blood", "number"),
            new IndicatorDefinition("hemoglobin", "Hemoglobin", "g/L", "blood", "number"),
            new IndicatorDefinition("platelet", "Platelet", "10^9/L", "blood", "number"),
            new IndicatorDefinition("fasting_glucose", "Fasting glucose", "mmol/L", "glucose", "number"),
            new IndicatorDefinition("systolic", "Systolic blood pressure", "mmHg", "blood_pressure", "number"),
            new IndicatorDefinition("diastolic", "Diastolic blood pressure", "mmHg", "blood_pressure", "number")
    );

    private static final Map<String, IndicatorDefinition> BY_CODE = DEFINITIONS.stream()
            .collect(java.util.stream.Collectors.toUnmodifiableMap(IndicatorDefinition::code, definition -> definition));

    private final ObjectMapper objectMapper;

    public ReportIndicatorCatalog(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public List<IndicatorDefinition> definitions() {
        return DEFINITIONS;
    }

    public void validateIndicators(String indicatorsJson) {
        JsonNode root = parse(indicatorsJson);
        if (!root.isObject()) {
            throw new IllegalArgumentException("Report indicators must be a JSON object");
        }
        JsonNode indicators = root.get("indicators");
        if (indicators == null || !indicators.isArray()) {
            throw new IllegalArgumentException("Report indicators must contain an indicators array");
        }
        for (JsonNode indicator : indicators) {
            validateIndicator(indicator);
        }
    }

    private JsonNode parse(String indicatorsJson) {
        try {
            return objectMapper.readTree(indicatorsJson);
        } catch (Exception ex) {
            throw new IllegalArgumentException("Report indicators must be valid JSON");
        }
    }

    private void validateIndicator(JsonNode indicator) {
        JsonNode code = indicator.get("code");
        JsonNode value = indicator.get("value");
        if (code == null || !code.isTextual() || code.asText().isBlank()) {
            throw new IllegalArgumentException("Report indicator code is required");
        }
        if (!BY_CODE.containsKey(code.asText())) {
            throw new IllegalArgumentException("Unsupported report indicator: " + code.asText());
        }
        if (value == null || !value.isNumber()) {
            throw new IllegalArgumentException("Report indicator value must be numeric: " + code.asText());
        }
    }

    public record IndicatorDefinition(String code, String name, String unit, String reportType, String valueKind) {
    }
}
