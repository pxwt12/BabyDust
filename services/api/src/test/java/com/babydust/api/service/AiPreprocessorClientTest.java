package com.babydust.api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.babydust.api.domain.AiConfig;
import com.babydust.api.repository.AiConfigRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

class AiPreprocessorClientTest {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void aliyunPreprocessorUsesSuppliedTextWithoutFallback() {
        AliyunAiPreprocessorClient client = new AliyunAiPreprocessorClient(
                false,
                false,
                "",
                "",
                new AiCredentialResolver(new MockEnvironment()),
                emptyConfigRepository(),
                objectMapper
        );

        AiPreprocessorClient.AiPreprocessResult result = client.preprocess(new AiPreprocessorClient.AiPreprocessRequest(
                "ocr_report",
                "aliyun_ocr",
                "oss://reports/week6.jpg",
                "HCG 1000"
        ));

        assertThat(result.text()).isEqualTo("HCG 1000");
        assertThat(result.fallbackUsed()).isFalse();
        assertThat(result.errorCode()).isEqualTo("OK");
        assertThat(result.warnings()).hasSize(1);
    }

    @Test
    void compositePreprocessorFallsBackWhenAliyunIsDisabled() {
        CompositeAiPreprocessorClient client = new CompositeAiPreprocessorClient(
                new AliyunAiPreprocessorClient(
                        false,
                        false,
                        "",
                        "",
                        new AiCredentialResolver(new MockEnvironment()),
                        emptyConfigRepository(),
                        objectMapper
                ),
                new RuleBasedAiPreprocessorClient()
        );

        AiPreprocessorClient.AiPreprocessResult result = client.preprocess(new AiPreprocessorClient.AiPreprocessRequest(
                "asr_record",
                "aliyun_asr",
                "oss://voice/demo.m4a",
                ""
        ));

        assertThat(result.text()).contains("weight 56kg");
        assertThat(result.fallbackUsed()).isTrue();
        assertThat(result.errorCode()).isEqualTo("ALIYUN_PREPROCESSOR_DISABLED");
        assertThat(result.warnings()).anyMatch(warning -> warning.contains("Preprocessor fallback used"));
    }

    @Test
    void activePreprocessorConfigEnablesRuntimeBeforeRealAliyunClientIsImplemented() {
        AiConfigRepository configs = mock(AiConfigRepository.class);
        when(configs.findTop50ByConfigTypeAndStatusOrderByCreatedAtDesc("preprocessor", "active"))
                .thenReturn(List.of(new AiConfig(
                        "preprocessor",
                        "aliyun-ocr-active",
                        "Aliyun OCR active",
                        "aliyun",
                        "active",
                        "{\"service\":\"ocr\",\"preprocessor\":\"aliyun_ocr\",\"credentialRef\":\"env:MISSING_ALIYUN_ACCESS_KEY\",\"enabled\":true}",
                        "v1",
                        "admin"
                )));
        AliyunAiPreprocessorClient client = new AliyunAiPreprocessorClient(
                false,
                false,
                "",
                "",
                new AiCredentialResolver(new MockEnvironment()),
                configs,
                objectMapper
        );

        AiPreprocessorClient.AiPreprocessResult result = client.preprocess(new AiPreprocessorClient.AiPreprocessRequest(
                "ocr_report",
                "aliyun_ocr",
                "oss://reports/week6.jpg",
                ""
        ));

        assertThat(result.fallbackUsed()).isTrue();
        assertThat(result.errorCode()).isEqualTo("ALIYUN_ACCESS_KEY_MISSING");
        assertThat(result.text()).isEmpty();
    }

    private AiConfigRepository emptyConfigRepository() {
        AiConfigRepository configs = mock(AiConfigRepository.class);
        when(configs.findTop50ByConfigTypeAndStatusOrderByCreatedAtDesc("preprocessor", "active")).thenReturn(List.of());
        return configs;
    }
}
