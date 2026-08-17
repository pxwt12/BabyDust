package com.babydust.api.web;

import com.babydust.api.common.ApiResponse;
import com.babydust.api.domain.AiAuditLog;
import com.babydust.api.domain.AiConfig;
import com.babydust.api.domain.AiDraftConfirmation;
import com.babydust.api.domain.AiPreprocessAuditLog;
import com.babydust.api.repository.AiAuditLogRepository;
import com.babydust.api.repository.AiConfigRepository;
import com.babydust.api.repository.AiDraftConfirmationRepository;
import com.babydust.api.repository.AiPreprocessAuditLogRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin")
public class AdminController {
    private final AiConfigRepository aiConfigs;
    private final AiAuditLogRepository aiAuditLogs;
    private final AiPreprocessAuditLogRepository aiPreprocessAuditLogs;
    private final AiDraftConfirmationRepository aiDraftConfirmations;
    private final ObjectMapper objectMapper;

    public AdminController(AiConfigRepository aiConfigs, AiAuditLogRepository aiAuditLogs, AiPreprocessAuditLogRepository aiPreprocessAuditLogs, AiDraftConfirmationRepository aiDraftConfirmations, ObjectMapper objectMapper) {
        this.aiConfigs = aiConfigs;
        this.aiAuditLogs = aiAuditLogs;
        this.aiPreprocessAuditLogs = aiPreprocessAuditLogs;
        this.aiDraftConfirmations = aiDraftConfirmations;
        this.objectMapper = objectMapper;
    }

    @PostMapping("/content")
    public ApiResponse<Map<String, Object>> content(@RequestBody Map<String, Object> body) {
        return ApiResponse.ok(Map.of("status", "draft", "content", body));
    }

    @PostMapping("/templates")
    public ApiResponse<Map<String, Object>> templates(@RequestBody Map<String, Object> body) {
        return ApiResponse.ok(Map.of("status", "draft", "template", body));
    }

    @PostMapping("/i18n")
    public ApiResponse<Map<String, Object>> i18n(@RequestBody Map<String, Object> body) {
        return ApiResponse.ok(Map.of("status", "draft", "message", body));
    }

    @GetMapping("/ai-configs")
    public ApiResponse<List<AiConfig>> aiConfigs(@RequestParam(required = false) String configType) {
        if (configType == null || configType.isBlank()) {
            return ApiResponse.ok(aiConfigs.findTop50ByOrderByCreatedAtDesc());
        }
        validateConfigType(configType);
        return ApiResponse.ok(aiConfigs.findTop50ByConfigTypeOrderByCreatedAtDesc(configType));
    }

    @GetMapping("/ai-audit-logs")
    public ApiResponse<List<AiAuditLog>> aiAuditLogs(
            @RequestParam(required = false) String purpose,
            @RequestParam(required = false) String provider,
            @RequestParam(required = false) String model,
            @RequestParam(required = false) String riskLevel,
            @RequestParam(required = false) Boolean blocked,
            @RequestParam(required = false) Boolean fallbackUsed,
            @RequestParam(required = false) Boolean policyConfigured,
            @RequestParam(required = false) String safetyPolicy,
            @RequestParam(required = false) String errorCode,
            @RequestParam(defaultValue = "50") int limit
    ) {
        int pageSize = Math.max(1, Math.min(limit, 100));
        Specification<AiAuditLog> spec = this.<AiAuditLog, String>optionalEquals("purpose", purpose)
                .and(this.<AiAuditLog, String>optionalEquals("provider", provider))
                .and(this.<AiAuditLog, String>optionalEquals("model", model))
                .and(this.<AiAuditLog, String>optionalEquals("riskLevel", riskLevel))
                .and(this.<AiAuditLog, Boolean>optionalEquals("blocked", blocked))
                .and(this.<AiAuditLog, Boolean>optionalEquals("fallbackUsed", fallbackUsed))
                .and(this.<AiAuditLog, Boolean>optionalEquals("policyConfigured", policyConfigured))
                .and(this.<AiAuditLog, String>optionalEquals("safetyPolicy", safetyPolicy))
                .and(this.<AiAuditLog, String>optionalEquals("errorCode", errorCode));
        return ApiResponse.ok(aiAuditLogs.findAll(
                spec,
                PageRequest.of(0, pageSize, Sort.by(Sort.Direction.DESC, "createdAt"))
        ).getContent());
    }

    @GetMapping("/ai-preprocess-audit-logs")
    public ApiResponse<List<AiPreprocessAuditLog>> aiPreprocessAuditLogs(
            @RequestParam(required = false) String purpose,
            @RequestParam(required = false) String provider,
            @RequestParam(required = false) String preprocessor,
            @RequestParam(required = false) Boolean fallbackUsed,
            @RequestParam(required = false) String errorCode,
            @RequestParam(defaultValue = "50") int limit
    ) {
        int pageSize = Math.max(1, Math.min(limit, 100));
        Specification<AiPreprocessAuditLog> spec = this.<AiPreprocessAuditLog, String>optionalEquals("purpose", purpose)
                .and(this.<AiPreprocessAuditLog, String>optionalEquals("provider", provider))
                .and(this.<AiPreprocessAuditLog, String>optionalEquals("preprocessor", preprocessor))
                .and(this.<AiPreprocessAuditLog, Boolean>optionalEquals("fallbackUsed", fallbackUsed))
                .and(this.<AiPreprocessAuditLog, String>optionalEquals("errorCode", errorCode));
        return ApiResponse.ok(aiPreprocessAuditLogs.findAll(
                spec,
                PageRequest.of(0, pageSize, Sort.by(Sort.Direction.DESC, "createdAt"))
        ).getContent());
    }

    @GetMapping("/ai-draft-confirmations")
    public ApiResponse<List<AdminAiDraftConfirmationResponse>> aiDraftConfirmations(
            @RequestParam(required = false) UUID familyId,
            @RequestParam(required = false) UUID userId,
            @RequestParam(required = false) String provider,
            @RequestParam(required = false) String model,
            @RequestParam(required = false) String purpose,
            @RequestParam(required = false) String subjectType,
            @RequestParam(defaultValue = "50") int limit
    ) {
        int pageSize = Math.max(1, Math.min(limit, 100));
        Specification<AiDraftConfirmation> spec = this.<AiDraftConfirmation, UUID>optionalEquals("familyId", familyId)
                .and(this.<AiDraftConfirmation, UUID>optionalEquals("userId", userId))
                .and(this.<AiDraftConfirmation, String>optionalEquals("provider", provider))
                .and(this.<AiDraftConfirmation, String>optionalEquals("model", model))
                .and(this.<AiDraftConfirmation, String>optionalEquals("purpose", purpose))
                .and(this.<AiDraftConfirmation, String>optionalEquals("subjectType", subjectType));
        List<AdminAiDraftConfirmationResponse> data = aiDraftConfirmations.findAll(
                spec,
                PageRequest.of(0, pageSize, Sort.by(Sort.Direction.DESC, "confirmedAt"))
        ).getContent().stream().map(AdminAiDraftConfirmationResponse::from).toList();
        return ApiResponse.ok(data);
    }

    @PostMapping("/ai-configs")
    public ApiResponse<AiConfig> createAiConfig(@Valid @RequestBody CreateAiConfigRequest body) {
        validateConfigType(body.configType());
        validateStatus(body.status());
        validateConfigJson(body.configType(), body.configJson());
        AiConfig config = new AiConfig(
                body.configType(),
                body.configKey(),
                body.displayName(),
                body.provider(),
                body.status(),
                body.configJson(),
                body.versionLabel(),
                body.createdBy()
        );
        return ApiResponse.ok(aiConfigs.save(config));
    }

    private void validateConfigType(String configType) {
        if (!List.of("provider", "prompt", "schema", "preprocessor", "qa_policy").contains(configType)) {
            throw new IllegalArgumentException("Unsupported AI config type");
        }
    }

    private void validateStatus(String status) {
        if (!List.of("draft", "active", "archived").contains(status)) {
            throw new IllegalArgumentException("Unsupported AI config status");
        }
    }

    private void validateConfigJson(String configType, String configJson) {
        try {
            JsonNode json = objectMapper.readTree(configJson);
            if (!json.isObject() && !json.isArray()) {
                throw new IllegalArgumentException("AI config JSON must be an object or array");
            }
            if ("provider".equals(configType)) {
                validateProviderConfigJson(json);
            } else if ("prompt".equals(configType)) {
                validatePromptConfigJson(json);
            } else if ("schema".equals(configType)) {
                validateSchemaConfigJson(json);
            } else if ("preprocessor".equals(configType)) {
                validatePreprocessorConfigJson(json);
            } else if ("qa_policy".equals(configType)) {
                validateQaPolicyConfigJson(json);
            }
        } catch (JsonProcessingException ex) {
            throw new IllegalArgumentException("AI config JSON is invalid");
        }
    }

    private void validateProviderConfigJson(JsonNode json) {
        if (!json.isObject()) {
            throw new IllegalArgumentException("Provider config JSON must be an object");
        }
        requireText(json, "model");
        String credentialRef = requireText(json, "credentialRef");
        if (!isSecretReference(credentialRef)) {
            throw new IllegalArgumentException("Provider credentialRef must reference env, KMS or secret storage");
        }
        if (json.hasNonNull("baseUrl")) {
            String baseUrl = requireText(json, "baseUrl");
            if (!baseUrl.startsWith("https://") && !baseUrl.startsWith("http://")) {
                throw new IllegalArgumentException("Provider baseUrl must be an HTTP(S) URL");
            }
        }
        validateNoInlineSecrets(json, "");
        validateProviderPricing(json.path("pricing"));
    }

    private String requireText(JsonNode json, String field) {
        JsonNode value = json.path(field);
        if (!value.isTextual() || value.asText().isBlank()) {
            throw new IllegalArgumentException("Provider config field is required: " + field);
        }
        return value.asText().trim();
    }

    private boolean isSecretReference(String value) {
        return value.startsWith("env:") || value.startsWith("kms:") || value.startsWith("secret:");
    }

    private void validateNoInlineSecrets(JsonNode node, String fieldName) {
        if (node.isObject()) {
            node.fields().forEachRemaining(entry -> validateNoInlineSecrets(entry.getValue(), entry.getKey()));
            return;
        }
        if (node.isArray()) {
            node.forEach(item -> validateNoInlineSecrets(item, fieldName));
            return;
        }
        if (!node.isTextual() || fieldName == null || fieldName.isBlank()) {
            return;
        }
        String normalized = fieldName.toLowerCase();
        boolean secretField = normalized.contains("apikey")
                || normalized.contains("api_key")
                || normalized.contains("accesskey")
                || normalized.contains("secret")
                || normalized.contains("token")
                || normalized.contains("authorization")
                || normalized.contains("password");
        if (secretField && !"credentialref".equals(normalized) && !isSecretReference(node.asText())) {
            throw new IllegalArgumentException("Provider config must not contain inline secrets: " + fieldName);
        }
    }

    private void validateProviderPricing(JsonNode pricing) {
        if (pricing.isMissingNode() || pricing.isNull()) {
            return;
        }
        if (!pricing.isObject()) {
            throw new IllegalArgumentException("Provider pricing must be a JSON object");
        }
        if (pricing.hasNonNull("currency") && (!pricing.path("currency").isTextual() || pricing.path("currency").asText().isBlank())) {
            throw new IllegalArgumentException("Provider pricing currency must be text");
        }
        validateNonNegativeDecimal(pricing.path("promptPer1K"), "promptPer1K");
        validateNonNegativeDecimal(pricing.path("completionPer1K"), "completionPer1K");
    }

    private void validateNonNegativeDecimal(JsonNode value, String field) {
        if (value.isMissingNode() || value.isNull()) {
            return;
        }
        try {
            BigDecimal decimal = new BigDecimal(value.asText());
            if (decimal.compareTo(BigDecimal.ZERO) < 0) {
                throw new IllegalArgumentException("Provider pricing must be non-negative: " + field);
            }
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Provider pricing must be numeric: " + field);
        }
    }

    private void validatePromptConfigJson(JsonNode json) {
        if (!json.isObject()) {
            throw new IllegalArgumentException("Prompt config JSON must be an object");
        }
        String purpose = requireConfigText(json, "purpose", "Prompt config field is required: ");
        if (!List.of("record_extraction", "report_extraction", "ocr_report", "asr_record").contains(purpose)) {
            throw new IllegalArgumentException("Unsupported prompt purpose");
        }
        if (json.hasNonNull("safetyPolicy")) {
            String safetyPolicy = requireConfigText(json, "safetyPolicy", "Prompt config field is required: ");
            if (!"draft_only".equals(safetyPolicy)) {
                throw new IllegalArgumentException("Prompt safetyPolicy must be draft_only");
            }
        }
        if (json.hasNonNull("systemPrompt") && json.path("systemPrompt").asText().length() > 4000) {
            throw new IllegalArgumentException("Prompt systemPrompt is too long");
        }
        validateNoInlineSecrets(json, "");
    }

    private void validateQaPolicyConfigJson(JsonNode json) {
        if (!json.isObject()) {
            throw new IllegalArgumentException("Q&A policy config JSON must be an object");
        }
        String safetyPolicy = requireConfigText(json, "safetyPolicy", "Q&A policy field is required: ");
        if (!"no_medical_decision".equals(safetyPolicy)) {
            throw new IllegalArgumentException("Q&A policy safetyPolicy must be no_medical_decision");
        }
        if (json.hasNonNull("locales")) {
            JsonNode locales = json.path("locales");
            if (!locales.isObject() || locales.isEmpty()) {
                throw new IllegalArgumentException("Q&A policy locales must be a non-empty object");
            }
            locales.fields().forEachRemaining(entry -> validateQaPolicyLocale(entry.getValue()));
        } else {
            validateQaPolicyLocale(json);
        }
        validateNoInlineSecrets(json, "");
    }

    private void validateQaPolicyLocale(JsonNode json) {
        if (!json.isObject()) {
            throw new IllegalArgumentException("Q&A policy locale entry must be an object");
        }
        validateOptionalText(json, "educationAnswer", 2000, "Q&A policy educationAnswer is too long");
        validateOptionalText(json, "safetyAnswer", 2000, "Q&A policy safetyAnswer is too long");
        validateOptionalStringArray(json, "suggestedQuestions", 6);
        validateOptionalStringArray(json, "safetyQuestions", 6);
        validateOptionalStringArray(json, "warnings", 6);
    }

    private void validateOptionalText(JsonNode json, String field, int maxLength, String tooLongMessage) {
        if (!json.hasNonNull(field)) {
            return;
        }
        JsonNode value = json.path(field);
        if (!value.isTextual()) {
            throw new IllegalArgumentException("Q&A policy field must be text: " + field);
        }
        if (value.asText().length() > maxLength) {
            throw new IllegalArgumentException(tooLongMessage);
        }
    }

    private void validateOptionalStringArray(JsonNode json, String field, int maxItems) {
        if (!json.hasNonNull(field)) {
            return;
        }
        JsonNode value = json.path(field);
        if (!value.isArray()) {
            throw new IllegalArgumentException("Q&A policy field must be an array: " + field);
        }
        if (value.size() > maxItems) {
            throw new IllegalArgumentException("Q&A policy field has too many items: " + field);
        }
        for (JsonNode item : value) {
            if (!item.isTextual() || item.asText().isBlank() || item.asText().length() > 300) {
                throw new IllegalArgumentException("Q&A policy array items must be non-empty short text: " + field);
            }
        }
    }

    private void validateSchemaConfigJson(JsonNode json) {
        if (!json.isObject()) {
            throw new IllegalArgumentException("Schema config JSON must be an object");
        }
        String type = requireConfigText(json, "type", "Schema config field is required: ");
        if (!"object".equals(type)) {
            throw new IllegalArgumentException("Schema type must be object");
        }
        JsonNode required = json.path("required");
        if (!required.isArray() || required.isEmpty()) {
            throw new IllegalArgumentException("Schema required must be a non-empty array");
        }
        boolean hasDraftArray = false;
        for (JsonNode item : required) {
            if (!item.isTextual()) {
                throw new IllegalArgumentException("Schema required items must be text");
            }
            String value = item.asText();
            if (!List.of("records", "todos", "reports").contains(value)) {
                throw new IllegalArgumentException("Schema required contains unsupported draft array");
            }
            hasDraftArray = true;
        }
        if (!hasDraftArray) {
            throw new IllegalArgumentException("Schema must require at least one draft array");
        }
        JsonNode properties = json.path("properties");
        if (properties.isMissingNode()) {
            return;
        }
        if (!properties.isObject()) {
            throw new IllegalArgumentException("Schema properties must be an object");
        }
        for (String draftArray : List.of("records", "todos", "reports")) {
            if (properties.has(draftArray)) {
                JsonNode property = properties.path(draftArray);
                if (!"array".equals(property.path("type").asText(""))) {
                    throw new IllegalArgumentException("Schema draft property must be an array: " + draftArray);
                }
            }
        }
    }

    private void validatePreprocessorConfigJson(JsonNode json) {
        if (!json.isObject()) {
            throw new IllegalArgumentException("Preprocessor config JSON must be an object");
        }
        String service = requireConfigText(json, "service", "Preprocessor config field is required: ");
        if (!List.of("ocr", "asr").contains(service)) {
            throw new IllegalArgumentException("Unsupported preprocessor service");
        }
        String preprocessor = requireConfigText(json, "preprocessor", "Preprocessor config field is required: ");
        if ("ocr".equals(service) && !"aliyun_ocr".equals(preprocessor)) {
            throw new IllegalArgumentException("OCR preprocessor must be aliyun_ocr");
        }
        if ("asr".equals(service) && !"aliyun_asr".equals(preprocessor)) {
            throw new IllegalArgumentException("ASR preprocessor must be aliyun_asr");
        }
        String credentialRef = requireConfigText(json, "credentialRef", "Preprocessor config field is required: ");
        if (!isSecretReference(credentialRef)) {
            throw new IllegalArgumentException("Preprocessor credentialRef must reference env, KMS or secret storage");
        }
        if (json.hasNonNull("region")) {
            requireConfigText(json, "region", "Preprocessor config field is required: ");
        }
        if (json.hasNonNull("endpoint")) {
            String endpoint = requireConfigText(json, "endpoint", "Preprocessor config field is required: ");
            if (!endpoint.startsWith("https://") && !endpoint.startsWith("http://")) {
                throw new IllegalArgumentException("Preprocessor endpoint must be an HTTP(S) URL");
            }
        }
        if (json.hasNonNull("enabled") && !json.path("enabled").isBoolean()) {
            throw new IllegalArgumentException("Preprocessor enabled must be boolean");
        }
        validateNoInlineSecrets(json, "");
    }

    private String requireConfigText(JsonNode json, String field, String messagePrefix) {
        JsonNode value = json.path(field);
        if (!value.isTextual() || value.asText().isBlank()) {
            throw new IllegalArgumentException(messagePrefix + field);
        }
        return value.asText().trim();
    }

    private <E, T> Specification<E> optionalEquals(String field, T value) {
        if (value == null) {
            return Specification.where(null);
        }
        if (value instanceof String text && text.isBlank()) {
            return Specification.where(null);
        }
        return (root, query, criteria) -> criteria.equal(root.get(field), value);
    }

    public record CreateAiConfigRequest(
            @NotBlank String configType,
            @NotBlank String configKey,
            @NotBlank String displayName,
            @NotBlank String provider,
            @NotBlank String status,
            @NotBlank String configJson,
            @NotBlank String versionLabel,
            @NotBlank String createdBy
    ) {
    }

    public record AdminAiDraftConfirmationResponse(
            UUID confirmationId,
            UUID familyId,
            UUID userId,
            String subjectType,
            UUID subjectId,
            String provider,
            String model,
            String purpose,
            String draftPreview,
            String recordIdsJson,
            String reportIdsJson,
            String todoIdsJson,
            OffsetDateTime confirmedAt
    ) {
        static AdminAiDraftConfirmationResponse from(AiDraftConfirmation confirmation) {
            return new AdminAiDraftConfirmationResponse(
                    confirmation.getId(),
                    confirmation.getFamilyId(),
                    confirmation.getUserId(),
                    confirmation.getSubjectType(),
                    confirmation.getSubjectId(),
                    confirmation.getProvider(),
                    confirmation.getModel(),
                    confirmation.getPurpose(),
                    confirmation.getDraftPreview(),
                    confirmation.getRecordIdsJson(),
                    confirmation.getReportIdsJson(),
                    confirmation.getTodoIdsJson(),
                    confirmation.getConfirmedAt()
            );
        }
    }
}
