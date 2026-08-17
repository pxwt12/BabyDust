package com.babydust.api.service;

import com.babydust.api.domain.AiAuditLog;
import com.babydust.api.domain.AiConfig;
import com.babydust.api.domain.AiPreprocessAuditLog;
import com.babydust.api.repository.AiAuditLogRepository;
import com.babydust.api.repository.AiConfigRepository;
import com.babydust.api.repository.AiPreprocessAuditLogRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class AiGatewayService {
    private final String provider;
    private final String model;
    private final AiAuditLogRepository auditLogs;
    private final AiPreprocessAuditLogRepository preprocessAuditLogs;
    private final AiConfigRepository aiConfigs;
    private final ObjectMapper objectMapper;
    private final AiModelClient modelClient;
    private final AiPreprocessorClient preprocessorClient;
    private final AiTextSanitizer sanitizer;

    public AiGatewayService(
            @Value("${babydust.ai.default-provider:deepseek}") String provider,
            @Value("${babydust.ai.deepseek.model:deepseek-chat}") String model,
            AiAuditLogRepository auditLogs,
            AiPreprocessAuditLogRepository preprocessAuditLogs,
            AiConfigRepository aiConfigs,
            ObjectMapper objectMapper,
            AiModelClient modelClient,
            AiPreprocessorClient preprocessorClient,
            AiTextSanitizer sanitizer
    ) {
        this.provider = provider;
        this.model = model;
        this.auditLogs = auditLogs;
        this.preprocessAuditLogs = preprocessAuditLogs;
        this.aiConfigs = aiConfigs;
        this.objectMapper = objectMapper;
        this.modelClient = modelClient;
        this.preprocessorClient = preprocessorClient;
        this.sanitizer = sanitizer;
    }

    public AiDraftResponse extractRecord(String text, String inputType) {
        long startedAt = System.nanoTime();
        GatewayContext gateway = gatewayContext();
        RiskResult risk = riskResult(text);
        AiModelClient.AiModelResult modelResult = risk.blocked() ? blockedModelResult() : callModel("record_extraction", text, inputType, gateway);
        AiDraftResponse response = new AiDraftResponse(
                "draft",
                "record_extraction",
                gateway.provider(),
                gateway.model(),
                gateway.providerConfigKey(),
                gateway.promptVersion(),
                gateway.schemaVersion(),
                inputType,
                "ai_draft",
                true,
                risk.blocked(),
                risk.level(),
                modelResult.fallbackUsed(),
                modelResult.errorCode(),
                modelResult.rawOutputPreview(),
                modelResult.promptTokens(),
                modelResult.completionTokens(),
                modelResult.totalTokens(),
                modelResult.costCurrency(),
                modelResult.estimatedCost(),
                modelResult.records(),
                modelResult.todos(),
                List.of(),
                risk.warnings(),
                OffsetDateTime.now()
        );
        audit(text, response, elapsedMs(startedAt));
        return response;
    }

    public AiDraftResponse extractReport(String text, String inputType) {
        long startedAt = System.nanoTime();
        GatewayContext gateway = gatewayContext();
        RiskResult risk = riskResult(text);
        AiModelClient.AiModelResult modelResult = risk.blocked() ? blockedModelResult() : callModel("report_extraction", text, inputType, gateway);
        AiDraftResponse response = new AiDraftResponse(
                "draft",
                "report_extraction",
                gateway.provider(),
                gateway.model(),
                gateway.providerConfigKey(),
                gateway.promptVersion(),
                gateway.schemaVersion(),
                inputType,
                "ai_draft",
                true,
                risk.blocked(),
                risk.level(),
                modelResult.fallbackUsed(),
                modelResult.errorCode(),
                modelResult.rawOutputPreview(),
                modelResult.promptTokens(),
                modelResult.completionTokens(),
                modelResult.totalTokens(),
                modelResult.costCurrency(),
                modelResult.estimatedCost(),
                List.of(),
                List.of(),
                modelResult.reports(),
                risk.warnings(),
                OffsetDateTime.now()
        );
        audit(text, response, elapsedMs(startedAt));
        return response;
    }

    public AiPreprocessResponse ocrReport(String fileUrl, String recognizedText) {
        long startedAt = System.nanoTime();
        AiPreprocessorClient.AiPreprocessResult preprocess = preprocessorClient.preprocess(new AiPreprocessorClient.AiPreprocessRequest(
                "ocr_report",
                "aliyun_ocr",
                fileUrl,
                recognizedText
        ));
        auditPreprocess("ocr_report", "aliyun", fileUrl, preprocess, elapsedMs(startedAt));
        AiDraftResponse draft = extractReport(preprocess.text(), "ocr_text");
        return new AiPreprocessResponse(
                "ocr_report",
                draft.provider(),
                preprocess.preprocessor(),
                fileUrl,
                preprocess.text(),
                preprocess.fallbackUsed(),
                preprocess.errorCode(),
                draft,
                mergeWarnings(preprocess.warnings(), "OCR text is a draft extraction. Please compare it with the original report image."),
                preprocess.processedAt()
        );
    }

    public AiPreprocessResponse asrRecord(String fileUrl, String transcriptText) {
        long startedAt = System.nanoTime();
        AiPreprocessorClient.AiPreprocessResult preprocess = preprocessorClient.preprocess(new AiPreprocessorClient.AiPreprocessRequest(
                "asr_record",
                "aliyun_asr",
                fileUrl,
                transcriptText
        ));
        auditPreprocess("asr_record", "aliyun", fileUrl, preprocess, elapsedMs(startedAt));
        AiDraftResponse draft = extractRecord(preprocess.text(), "asr_text");
        return new AiPreprocessResponse(
                "asr_record",
                draft.provider(),
                preprocess.preprocessor(),
                fileUrl,
                preprocess.text(),
                preprocess.fallbackUsed(),
                preprocess.errorCode(),
                draft,
                mergeWarnings(preprocess.warnings(), "ASR transcript is a draft extraction. Please confirm the transcript before saving records."),
                preprocess.processedAt()
        );
    }

    public AiQaResponse answerQuestion(String question, String locale) {
        long startedAt = System.nanoTime();
        GatewayContext gateway = gatewayContext();
        RiskResult risk = riskResult(question);
        QaPolicy policy = qaPolicy(locale);
        AiQaResponse response;
        if (risk.blocked()) {
            response = new AiQaResponse(
                    "qa",
                    gateway.provider(),
                    gateway.model(),
                    "safety",
                    true,
                    risk.level(),
                    "HIGH_RISK_BLOCKED",
                    policy.safetyAnswer(),
                    policy.safetyQuestions(),
                    mergeWarnings(risk.warnings(), policy.warnings()),
                    OffsetDateTime.now()
            );
        } else {
            response = new AiQaResponse(
                    "qa",
                    gateway.provider(),
                    gateway.model(),
                    "education",
                    false,
                    risk.level(),
                    "OK",
                    safeEducationAnswer(question, locale, policy),
                    policy.suggestedQuestions(),
                    mergeWarnings(risk.warnings(), policy.warnings()),
                    OffsetDateTime.now()
            );
        }
        auditQa(question, response, policy, risk, elapsedMs(startedAt));
        return response;
    }

    private List<String> mergeWarnings(List<String> warnings, String requiredWarning) {
        List<String> merged = new ArrayList<>(warnings);
        merged.add(requiredWarning);
        return merged;
    }

    private List<String> mergeWarnings(List<String> warnings, List<String> configuredWarnings) {
        List<String> merged = new ArrayList<>(warnings);
        merged.addAll(configuredWarnings);
        return merged;
    }

    private RiskResult riskResult(String text) {
        String normalized = text.toLowerCase(java.util.Locale.ROOT);
        List<RiskTerm> highRiskTerms = List.of(
                new RiskTerm("diagnosis", List.of("诊断", "diagnose")),
                new RiskTerm("medication_decision", List.of("吃什么药", "用药剂量", "dosage")),
                new RiskTerm("urgent_symptom", List.of("流血很多", "剧痛", "胎动消失", "emergency"))
        );
        List<String> reasons = highRiskTerms.stream()
                .filter(term -> term.keywords().stream().anyMatch(normalized::contains))
                .map(RiskTerm::reason)
                .toList();
        if (!reasons.isEmpty()) {
            return new RiskResult(true, "high", List.of("该内容可能涉及诊断、急症或用药决策。AI 不能给出医疗结论，请及时联系医生或急诊。"), reasons);
        }
        return new RiskResult(false, "low", new ArrayList<>(List.of("AI 仅生成整理草稿，确认后才可写入正式记录。医疗判断请咨询医生。")), List.of("low_risk"));
    }

    private long elapsedMs(long startedAt) {
        return Math.max(0, (System.nanoTime() - startedAt) / 1_000_000);
    }

    private AiModelClient.AiModelResult callModel(String purpose, String text, String inputType, GatewayContext gateway) {
        try {
            return modelClient.extract(new AiModelClient.AiModelRequest(
                    purpose,
                    gateway.provider(),
                    gateway.model(),
                    gateway.providerConfigKey(),
                    gateway.providerConfigJson(),
                    gateway.promptVersion(),
                    gateway.schemaVersion(),
                    safeInputType(inputType),
                    text
            ));
        } catch (Exception ex) {
            return new AiModelClient.AiModelResult(
                    true,
                    "MODEL_CLIENT_EXCEPTION",
                    ex.getClass().getSimpleName(),
                    List.of(),
                    List.of(),
                    List.of(),
                    0,
                    0,
                    0,
                    "CNY",
                    BigDecimal.ZERO
            );
        }
    }

    private AiModelClient.AiModelResult blockedModelResult() {
        return new AiModelClient.AiModelResult(false, "HIGH_RISK_BLOCKED", "", List.of(), List.of(), List.of(), 0, 0, 0, "CNY", BigDecimal.ZERO);
    }

    private GatewayContext gatewayContext() {
        Optional<AiConfig> providerConfig = activeConfig("provider");
        Optional<AiConfig> promptConfig = activeConfig("prompt");
        Optional<AiConfig> schemaConfig = activeConfig("schema");
        String resolvedProvider = providerConfig.map(AiConfig::getProvider).orElse(provider);
        String resolvedModel = providerConfig
                .map(AiConfig::getConfigJson)
                .map(this::modelFromConfig)
                .filter(value -> !value.isBlank())
                .orElse(model);
        return new GatewayContext(
                resolvedProvider,
                resolvedModel,
                providerConfig.map(AiConfig::getConfigKey).orElse("application-default"),
                providerConfig.map(AiConfig::getConfigJson).orElse("{}"),
                promptConfig.map(AiConfig::getVersionLabel).orElse("application-default"),
                schemaConfig.map(AiConfig::getVersionLabel).orElse("application-default")
        );
    }

    private Optional<AiConfig> activeConfig(String configType) {
        return aiConfigs.findFirstByConfigTypeAndStatusOrderByCreatedAtDesc(configType, "active");
    }

    private String modelFromConfig(String configJson) {
        try {
            JsonNode modelNode = objectMapper.readTree(configJson).path("model");
            return modelNode.isTextual() ? modelNode.asText() : "";
        } catch (Exception ex) {
            return "";
        }
    }

    private void audit(String text, AiDraftResponse response, long latencyMs) {
        auditLogs.save(new AiAuditLog(
                null,
                null,
                response.purpose(),
                response.provider(),
                response.model(),
                safeInputType(response.inputType()),
                text.length(),
                redactPreview(text),
                response.riskLevel(),
                response.blocked(),
                response.fallbackUsed(),
                response.errorCode(),
                response.promptTokens(),
                response.completionTokens(),
                response.totalTokens(),
                latencyMs,
                response.costCurrency(),
                response.estimatedCost(),
                response.status(),
                "n/a",
                false,
                "draft_only",
                response.blocked() ? response.riskLevel() : "low_risk"
        ));
    }

    private void auditPreprocess(String purpose, String provider, String fileUrl, AiPreprocessorClient.AiPreprocessResult preprocess, long latencyMs) {
        preprocessAuditLogs.save(new AiPreprocessAuditLog(
                purpose,
                provider,
                preprocess.preprocessor(),
                sanitizer.preview(fileUrl == null ? "" : fileUrl, 160),
                preprocess.text().length(),
                preprocess.fallbackUsed(),
                preprocess.errorCode(),
                latencyMs,
                "processed"
        ));
    }

    private void auditQa(String question, AiQaResponse response, QaPolicy policy, RiskResult risk, long latencyMs) {
        auditLogs.save(new AiAuditLog(
                null,
                null,
                "qa",
                response.provider(),
                response.model(),
                "question",
                question.length(),
                redactPreview(question),
                response.riskLevel(),
                response.blocked(),
                false,
                response.errorCode(),
                0,
                0,
                0,
                latencyMs,
                "CNY",
                BigDecimal.ZERO,
                response.answerType(),
                policy.version(),
                policy.configured(),
                policy.safetyPolicy(),
                String.join(",", risk.reasons())
        ));
    }

    private String safeEducationAnswer(String question, String locale, QaPolicy policy) {
        return policy.educationAnswer();
    }

    private QaPolicy qaPolicy(String locale) {
        return activeConfig("qa_policy")
                .flatMap(config -> parseQaPolicy(config, locale))
                .orElseGet(() -> defaultQaPolicy(locale));
    }

    private Optional<QaPolicy> parseQaPolicy(AiConfig config, String locale) {
        try {
            JsonNode root = objectMapper.readTree(config.getConfigJson());
            if (!"no_medical_decision".equals(root.path("safetyPolicy").asText(""))) {
                return Optional.empty();
            }
            JsonNode policy = localizedQaPolicyNode(root, locale);
            QaPolicy defaults = defaultQaPolicy(locale);
            return Optional.of(new QaPolicy(
                    config.getVersionLabel(),
                    true,
                    "no_medical_decision",
                    textOrDefault(policy.path("educationAnswer"), defaults.educationAnswer()),
                    textOrDefault(policy.path("safetyAnswer"), defaults.safetyAnswer()),
                    stringArrayOrDefault(policy.path("suggestedQuestions"), defaults.suggestedQuestions(), 6),
                    stringArrayOrDefault(policy.path("safetyQuestions"), defaults.safetyQuestions(), 6),
                    stringArrayOrDefault(policy.path("warnings"), defaults.warnings(), 6)
            ));
        } catch (Exception ex) {
            return Optional.empty();
        }
    }

    private JsonNode localizedQaPolicyNode(JsonNode root, String locale) {
        JsonNode locales = root.path("locales");
        if (!locales.isObject()) {
            return root;
        }
        String normalizedLocale = safeLocale(locale);
        JsonNode exact = locales.path(normalizedLocale);
        if (!exact.isMissingNode()) {
            return exact;
        }
        String language = normalizedLocale.split("-")[0];
        JsonNode languageMatch = locales.path(language);
        if (!languageMatch.isMissingNode()) {
            return languageMatch;
        }
        JsonNode zhCn = locales.path("zh-CN");
        if (!zhCn.isMissingNode()) {
            return zhCn;
        }
        return locales.elements().hasNext() ? locales.elements().next() : root;
    }

    private String safeLocale(String locale) {
        return locale == null || locale.isBlank() ? "zh-CN" : locale;
    }

    private String textOrDefault(JsonNode value, String fallback) {
        return value.isTextual() && !value.asText().isBlank() ? value.asText() : fallback;
    }

    private List<String> stringArrayOrDefault(JsonNode value, List<String> fallback, int maxItems) {
        if (!value.isArray() || value.isEmpty()) {
            return fallback;
        }
        List<String> items = new ArrayList<>();
        for (JsonNode item : value) {
            if (item.isTextual() && !item.asText().isBlank()) {
                items.add(item.asText());
            }
            if (items.size() >= maxItems) {
                break;
            }
        }
        return items.isEmpty() ? fallback : items;
    }

    private QaPolicy defaultQaPolicy(String locale) {
        if ("en-US".equalsIgnoreCase(locale) || "en".equalsIgnoreCase(locale)) {
            return new QaPolicy(
                    "application-default",
                    false,
                    "no_medical_decision",
                    "I can help organize pregnancy-related information and prepare questions for your clinician, but I cannot diagnose, judge whether a result is normal, or recommend medication changes. For this topic, keep a dated note, related symptoms, test results and current medicines, then confirm the next step with your prenatal clinician.",
                    "This question may involve diagnosis, urgent symptoms or medication decisions. AI cannot provide medical conclusions. Please contact your prenatal clinician, hospital or emergency care promptly.",
                    List.of(
                            "What should I ask at my next prenatal visit?",
                            "Which records should I prepare for my clinician?",
                            "What symptoms mean I should contact the hospital promptly?"
                    ),
                    List.of(
                            "Do I need urgent medical care now?",
                            "Which checks should a clinician consider for this symptom?",
                            "Should my current medicine be reviewed by a clinician?"
                    ),
                    List.of("AI can provide education and organization only; medical decisions must be made with a clinician.")
            );
        }
        return new QaPolicy(
                "application-default",
                false,
                "no_medical_decision",
                "我可以帮你整理孕期相关信息、准备复诊问题清单，但不能判断是否正常、不能诊断，也不能建议用药或调整剂量。建议把发生时间、症状变化、检查结果、正在使用的药物或补剂记录下来，并在产检或需要时向医生确认下一步。",
                "这个问题可能涉及诊断、急症或用药决策。AI 不能给出医疗结论，请及时联系产检医生、医院或急诊。",
                List.of(
                        "这件事下次产检需要问医生什么？",
                        "我应该准备哪些记录给医生看？",
                        "哪些情况需要及时联系医院？"
                ),
                List.of(
                        "我现在需要立刻就医吗？",
                        "这个症状需要做哪些检查？",
                        "我正在使用的药物是否需要由医生复核？"
                ),
                List.of("AI 仅提供科普整理和沟通准备，不能替代医生诊疗。")
        );
    }

    private String safeInputType(String inputType) {
        return inputType == null || inputType.isBlank() ? "text" : inputType;
    }

    private String redactPreview(String text) {
        return sanitizer.preview(text, 120);
    }

    private record RiskTerm(String reason, List<String> keywords) {
    }

    private record RiskResult(boolean blocked, String level, List<String> warnings, List<String> reasons) {
    }

    private record QaPolicy(
            String version,
            boolean configured,
            String safetyPolicy,
            String educationAnswer,
            String safetyAnswer,
            List<String> suggestedQuestions,
            List<String> safetyQuestions,
            List<String> warnings
    ) {
    }

    private record GatewayContext(
            String provider,
            String model,
            String providerConfigKey,
            String providerConfigJson,
            String promptVersion,
            String schemaVersion
    ) {
    }

    public record AiDraftResponse(
            String status,
            String purpose,
            String provider,
            String model,
            String providerConfigKey,
            String promptVersion,
            String schemaVersion,
            String inputType,
            String source,
            boolean needsUserConfirmation,
            boolean blocked,
            String riskLevel,
            boolean fallbackUsed,
            String errorCode,
            String rawOutputPreview,
            int promptTokens,
            int completionTokens,
            int totalTokens,
            String costCurrency,
            BigDecimal estimatedCost,
            List<Map<String, Object>> records,
            List<Map<String, Object>> todos,
            List<Map<String, Object>> reports,
            List<String> warnings,
            OffsetDateTime generatedAt
    ) {
    }

    public record AiPreprocessResponse(
            String purpose,
            String provider,
            String preprocessor,
            String fileUrl,
            String text,
            boolean fallbackUsed,
            String errorCode,
            AiDraftResponse draft,
            List<String> warnings,
            OffsetDateTime processedAt
    ) {
    }

    public record AiQaResponse(
            String purpose,
            String provider,
            String model,
            String answerType,
            boolean blocked,
            String riskLevel,
            String errorCode,
            String answer,
            List<String> suggestedQuestions,
            List<String> warnings,
            OffsetDateTime generatedAt
    ) {
    }
}
