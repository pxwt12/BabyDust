package com.babydust.api.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Component
public class DeepSeekAiModelClient {
    private final boolean enabled;
    private final String baseUrl;
    private final String defaultApiKey;
    private final ObjectMapper objectMapper;
    private final AiCredentialResolver credentialResolver;
    private final AiTextSanitizer sanitizer;
    private final RestTemplate restTemplate;

    public DeepSeekAiModelClient(
            @Value("${babydust.ai.deepseek.enabled:false}") boolean enabled,
            @Value("${babydust.ai.deepseek.base-url:https://api.deepseek.com}") String baseUrl,
            @Value("${babydust.ai.deepseek.api-key:}") String defaultApiKey,
            @Value("${babydust.ai.deepseek.timeout-ms:8000}") long timeoutMs,
            ObjectMapper objectMapper,
            AiCredentialResolver credentialResolver,
            AiTextSanitizer sanitizer,
            RestTemplateBuilder restTemplateBuilder
    ) {
        this.enabled = enabled;
        this.baseUrl = baseUrl;
        this.defaultApiKey = defaultApiKey;
        this.objectMapper = objectMapper;
        this.credentialResolver = credentialResolver;
        this.sanitizer = sanitizer;
        this.restTemplate = restTemplateBuilder
                .setConnectTimeout(Duration.ofMillis(timeoutMs))
                .setReadTimeout(Duration.ofMillis(timeoutMs))
                .build();
    }

    public boolean supports(AiModelClient.AiModelRequest request) {
        return enabled && "deepseek".equalsIgnoreCase(request.provider());
    }

    public AiModelClient.AiModelResult extract(AiModelClient.AiModelRequest request) {
        String apiKey = credentialResolver.resolve(credentialRef(request.providerConfigJson()), defaultApiKey);
        if (apiKey.isBlank()) {
            return unavailable("DEEPSEEK_API_KEY_MISSING");
        }
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(apiKey);
            headers.setContentType(MediaType.APPLICATION_JSON);
            Map<String, Object> payload = Map.of(
                    "model", request.model(),
                    "messages", List.of(
                            Map.of("role", "system", "content", systemPrompt(request)),
                            Map.of("role", "user", "content", request.text())
                    ),
                    "temperature", 0.1,
                    "response_format", Map.of("type", "json_object")
            );
            String response = restTemplate.postForObject(
                    normalizedBaseUrl() + "/chat/completions",
                    new HttpEntity<>(payload, headers),
                    String.class
            );
            return parseResponse(response, request.providerConfigJson());
        } catch (RestClientException ex) {
            return unavailable("DEEPSEEK_HTTP_ERROR", ex.getClass().getSimpleName());
        }
    }

    AiModelClient.AiModelResult parseResponse(String response) {
        return parseResponse(response, "{}");
    }

    AiModelClient.AiModelResult parseResponse(String response, String providerConfigJson) {
        String rawPreview = preview(response);
        try {
            JsonNode root = objectMapper.readTree(response);
            JsonNode contentNode = root.path("choices").path(0).path("message").path("content");
            if (!contentNode.isTextual() || contentNode.asText().isBlank()) {
                return unavailable("DEEPSEEK_EMPTY_CONTENT", rawPreview);
            }
            JsonNode draft = objectMapper.readTree(contentNode.asText());
            if (!draft.isObject()) {
                return unavailable("DEEPSEEK_INVALID_DRAFT_SCHEMA", rawPreview);
            }
            JsonNode records = arrayNode(draft, "records");
            JsonNode todos = arrayNode(draft, "todos");
            JsonNode reports = arrayNode(draft, "reports");
            if (records == null || todos == null || reports == null) {
                return unavailable("DEEPSEEK_INVALID_DRAFT_SCHEMA", rawPreview);
            }
            UsageCost usage = usageCost(root, providerConfigJson);
            return new AiModelClient.AiModelResult(
                    false,
                    "OK",
                    preview(contentNode.asText()),
                    toObjectList(records),
                    toObjectList(todos),
                    toObjectList(reports),
                    usage.promptTokens(),
                    usage.completionTokens(),
                    usage.totalTokens(),
                    usage.costCurrency(),
                    usage.estimatedCost()
            );
        } catch (Exception ex) {
            return unavailable("DEEPSEEK_INVALID_JSON", rawPreview);
        }
    }

    private String credentialRef(String providerConfigJson) {
        try {
            JsonNode credentialRef = objectMapper.readTree(providerConfigJson).path("credentialRef");
            return credentialRef.isTextual() ? credentialRef.asText() : "";
        } catch (Exception ex) {
            return "";
        }
    }

    private String systemPrompt(AiModelClient.AiModelRequest request) {
        return "Return JSON draft only. Purpose: " + request.purpose()
                + ". Input type: " + request.inputType()
                + ". Do not provide medical diagnosis or medication decisions.";
    }

    private AiModelClient.AiModelResult unavailable(String errorCode) {
        return unavailable(errorCode, "");
    }

    private AiModelClient.AiModelResult unavailable(String errorCode, String rawOutputPreview) {
        return new AiModelClient.AiModelResult(true, errorCode, rawOutputPreview, List.of(), List.of(), List.of(), 0, 0, 0, "CNY", BigDecimal.ZERO);
    }

    private JsonNode arrayNode(JsonNode root, String fieldName) {
        JsonNode node = root.path(fieldName);
        return node.isArray() ? node : null;
    }

    private List<Map<String, Object>> toObjectList(JsonNode arrayNode) {
        List<Map<String, Object>> values = new ArrayList<>();
        for (JsonNode item : arrayNode) {
            if (!item.isObject()) {
                throw new IllegalArgumentException("Draft array items must be objects");
            }
            values.add(objectMapper.convertValue(item, new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {
            }));
        }
        return values;
    }

    private UsageCost usageCost(JsonNode responseRoot, String providerConfigJson) {
        int promptTokens = responseRoot.path("usage").path("prompt_tokens").asInt(0);
        int completionTokens = responseRoot.path("usage").path("completion_tokens").asInt(0);
        int totalTokens = responseRoot.path("usage").path("total_tokens").asInt(promptTokens + completionTokens);
        JsonNode pricing = pricing(providerConfigJson);
        String currency = pricing.path("currency").isTextual() ? pricing.path("currency").asText() : "CNY";
        BigDecimal promptPer1K = decimal(pricing.path("promptPer1K"));
        BigDecimal completionPer1K = decimal(pricing.path("completionPer1K"));
        BigDecimal estimatedCost = BigDecimal.valueOf(promptTokens)
                .multiply(promptPer1K)
                .add(BigDecimal.valueOf(completionTokens).multiply(completionPer1K))
                .divide(BigDecimal.valueOf(1000), 6, RoundingMode.HALF_UP);
        return new UsageCost(promptTokens, completionTokens, totalTokens, currency, estimatedCost);
    }

    private JsonNode pricing(String providerConfigJson) {
        try {
            return objectMapper.readTree(providerConfigJson).path("pricing");
        } catch (Exception ex) {
            return objectMapper.createObjectNode();
        }
    }

    private BigDecimal decimal(JsonNode node) {
        if (node.isNumber()) {
            return node.decimalValue();
        }
        if (node.isTextual()) {
            try {
                return new BigDecimal(node.asText());
            } catch (NumberFormatException ex) {
                return BigDecimal.ZERO;
            }
        }
        return BigDecimal.ZERO;
    }

    private record UsageCost(
            int promptTokens,
            int completionTokens,
            int totalTokens,
            String costCurrency,
            BigDecimal estimatedCost
    ) {
    }

    private String normalizedBaseUrl() {
        return baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }

    private String preview(String response) {
        return sanitizer.preview(response, 160);
    }
}
