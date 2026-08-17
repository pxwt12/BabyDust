package com.babydust.api.service;

import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

@Primary
@Component
public class CompositeAiModelClient implements AiModelClient {
    private final DeepSeekAiModelClient deepSeek;
    private final RuleBasedAiModelClient fallback;

    public CompositeAiModelClient(DeepSeekAiModelClient deepSeek, RuleBasedAiModelClient fallback) {
        this.deepSeek = deepSeek;
        this.fallback = fallback;
    }

    @Override
    public AiModelResult extract(AiModelRequest request) {
        if (!deepSeek.supports(request)) {
            return fallback.extract(request);
        }
        AiModelResult result = deepSeek.extract(request);
        if (!result.records().isEmpty() || !result.todos().isEmpty() || !result.reports().isEmpty()) {
            return result;
        }
        AiModelResult fallbackResult = fallback.extract(request);
        return new AiModelResult(
                true,
                result.errorCode(),
                result.rawOutputPreview().isBlank() ? fallbackResult.rawOutputPreview() : result.rawOutputPreview(),
                fallbackResult.records(),
                fallbackResult.todos(),
                fallbackResult.reports(),
                result.promptTokens(),
                result.completionTokens(),
                result.totalTokens(),
                result.costCurrency(),
                result.estimatedCost()
        );
    }
}
