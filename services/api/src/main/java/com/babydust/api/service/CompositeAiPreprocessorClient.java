package com.babydust.api.service;

import java.util.ArrayList;
import java.util.List;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

@Primary
@Component
public class CompositeAiPreprocessorClient implements AiPreprocessorClient {
    private final AliyunAiPreprocessorClient aliyun;
    private final RuleBasedAiPreprocessorClient fallback;

    public CompositeAiPreprocessorClient(AliyunAiPreprocessorClient aliyun, RuleBasedAiPreprocessorClient fallback) {
        this.aliyun = aliyun;
        this.fallback = fallback;
    }

    @Override
    public AiPreprocessResult preprocess(AiPreprocessRequest request) {
        AiPreprocessResult result = aliyun.preprocess(request);
        if (!result.text().isBlank()) {
            return result;
        }
        AiPreprocessResult fallbackResult = fallback.preprocess(request);
        List<String> warnings = new ArrayList<>();
        warnings.add("Preprocessor fallback used: " + result.errorCode());
        warnings.addAll(fallbackResult.warnings());
        return new AiPreprocessResult(
                result.preprocessor(),
                fallbackResult.text(),
                true,
                result.errorCode(),
                warnings,
                fallbackResult.processedAt()
        );
    }
}
