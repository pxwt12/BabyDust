package com.babydust.api.service;

import java.time.OffsetDateTime;
import java.util.List;

public interface AiPreprocessorClient {
    AiPreprocessResult preprocess(AiPreprocessRequest request);

    record AiPreprocessRequest(
            String purpose,
            String preprocessor,
            String fileUrl,
            String suppliedText
    ) {
    }

    record AiPreprocessResult(
            String preprocessor,
            String text,
            boolean fallbackUsed,
            String errorCode,
            List<String> warnings,
            OffsetDateTime processedAt
    ) {
    }
}
