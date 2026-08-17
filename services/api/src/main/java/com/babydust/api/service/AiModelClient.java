package com.babydust.api.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public interface AiModelClient {
    AiModelResult extract(AiModelRequest request);

    record AiModelRequest(
            String purpose,
            String provider,
            String model,
            String providerConfigKey,
            String providerConfigJson,
            String promptVersion,
            String schemaVersion,
            String inputType,
            String text
    ) {
    }

    record AiModelResult(
            boolean fallbackUsed,
            String errorCode,
            String rawOutputPreview,
            List<Map<String, Object>> records,
            List<Map<String, Object>> todos,
            List<Map<String, Object>> reports,
            int promptTokens,
            int completionTokens,
            int totalTokens,
            String costCurrency,
            BigDecimal estimatedCost
    ) {
    }
}
