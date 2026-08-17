package com.babydust.api.service;

import com.babydust.api.domain.AiConfig;
import com.babydust.api.repository.AiConfigRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class AliyunAiPreprocessorClient implements AiPreprocessorClient {
    private final boolean ocrEnabled;
    private final boolean asrEnabled;
    private final String accessKey;
    private final String accessKeyRef;
    private final AiCredentialResolver credentialResolver;
    private final AiConfigRepository aiConfigs;
    private final ObjectMapper objectMapper;

    public AliyunAiPreprocessorClient(
            @Value("${babydust.ai.aliyun.ocr.enabled:false}") boolean ocrEnabled,
            @Value("${babydust.ai.aliyun.asr.enabled:false}") boolean asrEnabled,
            @Value("${babydust.ai.aliyun.access-key:}") String accessKey,
            @Value("${babydust.ai.aliyun.access-key-ref:}") String accessKeyRef,
            AiCredentialResolver credentialResolver,
            AiConfigRepository aiConfigs,
            ObjectMapper objectMapper
    ) {
        this.ocrEnabled = ocrEnabled;
        this.asrEnabled = asrEnabled;
        this.accessKey = accessKey;
        this.accessKeyRef = accessKeyRef;
        this.credentialResolver = credentialResolver;
        this.aiConfigs = aiConfigs;
        this.objectMapper = objectMapper;
    }

    @Override
    public AiPreprocessResult preprocess(AiPreprocessRequest request) {
        if (request.suppliedText() != null && !request.suppliedText().isBlank()) {
            return ok(request.preprocessor(), request.suppliedText().trim(), List.of("Recognized text was supplied by the client and still requires user confirmation."));
        }
        RuntimeConfig config = runtimeConfig(request.preprocessor());
        if (!config.enabled()) {
            return unavailable(request.preprocessor(), "ALIYUN_PREPROCESSOR_DISABLED");
        }
        if (credentialResolver.resolve(config.credentialRef(), accessKey).isBlank()) {
            return unavailable(request.preprocessor(), "ALIYUN_ACCESS_KEY_MISSING");
        }
        return unavailable(request.preprocessor(), "ALIYUN_CLIENT_NOT_IMPLEMENTED");
    }

    private RuntimeConfig runtimeConfig(String preprocessor) {
        Optional<RuntimeConfig> activeConfig = aiConfigs.findTop50ByConfigTypeAndStatusOrderByCreatedAtDesc("preprocessor", "active")
                .stream()
                .map(this::parseConfig)
                .flatMap(Optional::stream)
                .filter(config -> preprocessor.equals(config.preprocessor()))
                .findFirst();
        return activeConfig.orElseGet(() -> new RuntimeConfig(preprocessor, enabledByProperties(preprocessor), accessKeyRef));
    }

    private Optional<RuntimeConfig> parseConfig(AiConfig config) {
        try {
            JsonNode json = objectMapper.readTree(config.getConfigJson());
            String preprocessor = json.path("preprocessor").asText("");
            boolean enabled = json.path("enabled").asBoolean(false);
            String credentialRef = json.path("credentialRef").asText("");
            if (preprocessor.isBlank()) {
                return Optional.empty();
            }
            return Optional.of(new RuntimeConfig(preprocessor, enabled, credentialRef));
        } catch (Exception ex) {
            return Optional.empty();
        }
    }

    private boolean enabledByProperties(String preprocessor) {
        if ("aliyun_ocr".equals(preprocessor)) {
            return ocrEnabled;
        }
        if ("aliyun_asr".equals(preprocessor)) {
            return asrEnabled;
        }
        return false;
    }

    private AiPreprocessResult ok(String preprocessor, String text, List<String> warnings) {
        return new AiPreprocessResult(preprocessor, text, false, "OK", warnings, OffsetDateTime.now());
    }

    private AiPreprocessResult unavailable(String preprocessor, String errorCode) {
        return new AiPreprocessResult(preprocessor, "", true, errorCode, List.of(), OffsetDateTime.now());
    }

    private record RuntimeConfig(String preprocessor, boolean enabled, String credentialRef) {
    }
}
