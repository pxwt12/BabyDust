package com.babydust.api.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.mock.env.MockEnvironment;

class DeepSeekAiModelClientTest {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void enabledClientWithoutResolvedApiKeyReturnsMissingKeyFallback() {
        DeepSeekAiModelClient client = new DeepSeekAiModelClient(
                true,
                "https://api.deepseek.com",
                "",
                100,
                objectMapper,
                new AiCredentialResolver(new MockEnvironment()),
                new AiTextSanitizer(),
                new RestTemplateBuilder()
        );

        AiModelClient.AiModelResult result = client.extract(new AiModelClient.AiModelRequest(
                "record_extraction",
                "deepseek",
                "deepseek-chat",
                "deepseek-public",
                "{\"credentialRef\":\"env:MISSING_DEEPSEEK_API_KEY\"}",
                "prompt-v1",
                "schema-v1",
                "text",
                "weight 56kg"
        ));

        assertThat(result.fallbackUsed()).isTrue();
        assertThat(result.errorCode()).isEqualTo("DEEPSEEK_API_KEY_MISSING");
        assertThat(result.records()).isEmpty();
    }

    @Test
    void parsesValidDeepSeekJsonDraftContent() throws Exception {
        DeepSeekAiModelClient client = client();
        String content = objectMapper.writeValueAsString(Map.of(
                "records", java.util.List.of(Map.of("recordType", "weight", "payload", Map.of("weightKg", 56))),
                "todos", java.util.List.of(Map.of("title", "review report", "category", "custom")),
                "reports", java.util.List.of()
        ));
        String response = objectMapper.writeValueAsString(Map.of(
                "choices", java.util.List.of(Map.of("message", Map.of("content", content)))
        ));

        AiModelClient.AiModelResult result = client.parseResponse(response);

        assertThat(result.fallbackUsed()).isFalse();
        assertThat(result.errorCode()).isEqualTo("OK");
        assertThat(result.records()).hasSize(1);
        assertThat(result.records().get(0)).containsEntry("recordType", "weight");
        assertThat(result.todos()).hasSize(1);
        assertThat(result.reports()).isEmpty();
    }

    @Test
    void parsesUsageAndEstimatesCostFromProviderPricing() throws Exception {
        DeepSeekAiModelClient client = client();
        String content = objectMapper.writeValueAsString(Map.of(
                "records", java.util.List.of(Map.of("recordType", "weight", "payload", Map.of("weightKg", 56))),
                "todos", java.util.List.of(),
                "reports", java.util.List.of()
        ));
        String response = objectMapper.writeValueAsString(Map.of(
                "choices", java.util.List.of(Map.of("message", Map.of("content", content))),
                "usage", Map.of("prompt_tokens", 1000, "completion_tokens", 500, "total_tokens", 1500)
        ));
        String providerConfigJson = objectMapper.writeValueAsString(Map.of(
                "pricing", Map.of("currency", "CNY", "promptPer1K", "0.002", "completionPer1K", "0.008")
        ));

        AiModelClient.AiModelResult result = client.parseResponse(response, providerConfigJson);

        assertThat(result.promptTokens()).isEqualTo(1000);
        assertThat(result.completionTokens()).isEqualTo(500);
        assertThat(result.totalTokens()).isEqualTo(1500);
        assertThat(result.costCurrency()).isEqualTo("CNY");
        assertThat(result.estimatedCost()).isEqualByComparingTo("0.006000");
    }

    @Test
    void rejectsDeepSeekDraftWhenRequiredArraysAreMissing() throws Exception {
        DeepSeekAiModelClient client = client();
        String content = objectMapper.writeValueAsString(Map.of("records", java.util.List.of()));
        String response = objectMapper.writeValueAsString(Map.of(
                "choices", java.util.List.of(Map.of("message", Map.of("content", content)))
        ));

        AiModelClient.AiModelResult result = client.parseResponse(response);

        assertThat(result.fallbackUsed()).isTrue();
        assertThat(result.errorCode()).isEqualTo("DEEPSEEK_INVALID_DRAFT_SCHEMA");
    }

    @Test
    void rejectsDeepSeekDraftWhenContentIsNotJson() throws Exception {
        DeepSeekAiModelClient client = client();
        String response = objectMapper.writeValueAsString(Map.of(
                "choices", java.util.List.of(Map.of("message", Map.of("content", "not-json")))
        ));

        AiModelClient.AiModelResult result = client.parseResponse(response);

        assertThat(result.fallbackUsed()).isTrue();
        assertThat(result.errorCode()).isEqualTo("DEEPSEEK_INVALID_JSON");
    }

    @Test
    void redactsSensitiveValuesFromRawOutputPreview() throws Exception {
        DeepSeekAiModelClient client = client();
        String content = objectMapper.writeValueAsString(Map.of(
                "records", java.util.List.of(Map.of(
                        "recordType", "note",
                        "payload", Map.of("text", "phone 13812345678 email mom@example.com id 110105199001011234 api_key=sk-testsecret999")
                )),
                "todos", java.util.List.of(),
                "reports", java.util.List.of()
        ));
        String response = objectMapper.writeValueAsString(Map.of(
                "choices", java.util.List.of(Map.of("message", Map.of("content", content)))
        ));

        AiModelClient.AiModelResult result = client.parseResponse(response);

        assertThat(result.rawOutputPreview()).contains("***PHONE***");
        assertThat(result.rawOutputPreview()).contains("***EMAIL***");
        assertThat(result.rawOutputPreview()).contains("***ID_CARD***");
        assertThat(result.rawOutputPreview()).doesNotContain("13812345678", "mom@example.com", "110105199001011234", "sk-testsecret999");
    }

    @Test
    void credentialResolverReadsEnvironmentReferencesBeforeDirectFallback() {
        MockEnvironment environment = new MockEnvironment()
                .withProperty("DEEPSEEK_TEST_KEY", "test-secret");
        AiCredentialResolver resolver = new AiCredentialResolver(environment);

        assertThat(resolver.resolve("env:DEEPSEEK_TEST_KEY", "")).isEqualTo("test-secret");
        assertThat(resolver.resolve("env:DEEPSEEK_TEST_KEY", "direct-secret")).isEqualTo("direct-secret");
    }

    private DeepSeekAiModelClient client() {
        return new DeepSeekAiModelClient(
                true,
                "https://api.deepseek.com",
                "test-key",
                100,
                objectMapper,
                new AiCredentialResolver(new MockEnvironment()),
                new AiTextSanitizer(),
                new RestTemplateBuilder()
        );
    }
}
