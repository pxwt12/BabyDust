package com.babydust.api.service;

import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class RuleBasedAiPreprocessorClient implements AiPreprocessorClient {
    @Override
    public AiPreprocessResult preprocess(AiPreprocessRequest request) {
        if ("ocr_report".equals(request.purpose())) {
            return new AiPreprocessResult(
                    request.preprocessor(),
                    "HCG 1000, progesterone 22.4",
                    true,
                    "RULE_BASED_PREPROCESSOR_FALLBACK",
                    List.of("OCR service is not configured. Demo recognized text was used for draft generation."),
                    OffsetDateTime.now()
            );
        }
        return new AiPreprocessResult(
                request.preprocessor(),
                "weight 56kg, todo review",
                true,
                "RULE_BASED_PREPROCESSOR_FALLBACK",
                List.of("ASR service is not configured. Demo transcript text was used for draft generation."),
                OffsetDateTime.now()
        );
    }
}
