package com.babydust.api.config;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class DemoDataInitializer implements ApplicationRunner {
    public static final UUID DEMO_USER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    public static final UUID DEMO_FAMILY_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    public static final UUID DEMO_PREGNANCY_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");
    public static final UUID DEMO_MOTHER_ID = UUID.fromString("44444444-4444-4444-4444-444444444444");
    public static final UUID DEMO_BABY_ID = UUID.fromString("55555555-5555-5555-5555-555555555555");
    public static final String DEMO_TOKEN = "bd_demo_seed_token";
    public static final String DEMO_OPENID = "dev_" + UUID.nameUUIDFromBytes("demo-seed".getBytes(StandardCharsets.UTF_8));

    private static final ZoneId DEMO_ZONE = ZoneId.of("Asia/Shanghai");

    private final JdbcTemplate jdbc;
    private final Clock clock;
    private final boolean enabled;

    public DemoDataInitializer(JdbcTemplate jdbc, Clock clock, @Value("${babydust.demo-data.enabled:true}") boolean enabled) {
        this.jdbc = jdbc;
        this.clock = clock;
        this.enabled = enabled;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (!enabled) {
            return;
        }
        OffsetDateTime now = OffsetDateTime.now(clock);
        LocalDate today = LocalDate.now(clock.withZone(DEMO_ZONE));
        LocalDate lmpDate = today.minusWeeks(17).minusDays(3);
        LocalDate dueDate = lmpDate.plusDays(280);

        clearDemoData();
        seedCoreProfiles(now, lmpDate, dueDate);
        seedPregnancyRecords(today);
        seedFullCycleRecords(today);
        seedReports(today);
        seedTodos(today);
        seedReminders(today);
        seedAiConfigs(now);
        seedAiAuditLogs(now);
        seedAiPreprocessLogs(now);
        seedAiConfirmations(now);
    }

    private void clearDemoData() {
        jdbc.update("delete from ai_draft_confirmations where family_id = ? or user_id = ?", DEMO_FAMILY_ID, DEMO_USER_ID);
        jdbc.update("delete from reminders where family_id = ?", DEMO_FAMILY_ID);
        jdbc.update("delete from todos where family_id = ?", DEMO_FAMILY_ID);
        jdbc.update("delete from medical_reports where family_id = ?", DEMO_FAMILY_ID);
        jdbc.update("delete from records where family_id = ?", DEMO_FAMILY_ID);
        jdbc.update("delete from baby_profiles where family_id = ?", DEMO_FAMILY_ID);
        jdbc.update("delete from pregnancy_profiles where family_id = ?", DEMO_FAMILY_ID);
        jdbc.update("delete from mother_profiles where family_id = ?", DEMO_FAMILY_ID);
        jdbc.update("delete from family_members where family_id = ? or user_id = ?", DEMO_FAMILY_ID, DEMO_USER_ID);
        jdbc.update("delete from families where id = ?", DEMO_FAMILY_ID);
        jdbc.update("delete from session_tokens where user_id = ? or token = ?", DEMO_USER_ID, DEMO_TOKEN);
        jdbc.update("delete from app_users where id = ? or openid = ?", DEMO_USER_ID, DEMO_OPENID);
        jdbc.update("delete from ai_audit_logs where user_id = ?", DEMO_USER_ID);
        jdbc.update("delete from ai_preprocess_audit_logs where file_url_preview like 'oss://demo/%'");
        jdbc.update("delete from ai_configs where config_key like 'demo-%'");
    }

    private void seedCoreProfiles(OffsetDateTime now, LocalDate lmpDate, LocalDate dueDate) {
        insert("app_users", DEMO_USER_ID, now, now, DEMO_OPENID, "林小满");
        insert("session_tokens", UUID.fromString("66666666-6666-6666-6666-666666666666"), now, now, DEMO_TOKEN, DEMO_USER_ID, now.plusDays(365));
        insert("families", DEMO_FAMILY_ID, now, now, "小满和阿哲的家", DEMO_USER_ID);
        insert("family_members", UUID.fromString("66666666-6666-6666-6666-666666666667"), now, now, DEMO_FAMILY_ID, DEMO_USER_ID, "admin", "mother");
        insert("mother_profiles", DEMO_MOTHER_ID, now, now, DEMO_FAMILY_ID, DEMO_USER_ID, LocalDate.of(1994, 8, 16), 165, new BigDecimal("54.20"), "O+");
        insert("pregnancy_profiles", DEMO_PREGNANCY_ID, now, now, DEMO_FAMILY_ID, lmpDate, dueDate, 1, "pregnant");
        insert("baby_profiles", DEMO_BABY_ID, now, now, DEMO_FAMILY_ID, null, "年年", "female", now.minusMonths(14), new BigDecimal("3.18"), new BigDecimal("50.00"));
    }

    private void seedPregnancyRecords(LocalDate today) {
        record("70000000-0000-0000-0000-000000000001", "pregnancy", DEMO_PREGNANCY_ID, "weight", today.minusDays(72), "08:10", "{\"weightKg\":55.1}");
        record("70000000-0000-0000-0000-000000000002", "pregnancy", DEMO_PREGNANCY_ID, "weight", today.minusDays(42), "08:05", "{\"weightKg\":56.0}");
        record("70000000-0000-0000-0000-000000000003", "pregnancy", DEMO_PREGNANCY_ID, "weight", today.minusDays(14), "08:12", "{\"weightKg\":57.3}");
        record("70000000-0000-0000-0000-000000000004", "pregnancy", DEMO_PREGNANCY_ID, "blood_pressure", today.minusDays(42), "08:20", "{\"systolic\":112,\"diastolic\":72,\"position\":\"sitting\"}");
        record("70000000-0000-0000-0000-000000000005", "pregnancy", DEMO_PREGNANCY_ID, "blood_pressure", today.minusDays(7), "08:25", "{\"systolic\":116,\"diastolic\":74,\"position\":\"sitting\"}");
        record("70000000-0000-0000-0000-000000000006", "pregnancy", DEMO_PREGNANCY_ID, "symptom", today.minusDays(67), "21:30", "{\"name\":\"morning sickness\",\"severity\":2,\"note\":\"午后更明显，少量多餐后缓解\"}");
        record("70000000-0000-0000-0000-000000000007", "pregnancy", DEMO_PREGNANCY_ID, "supplement", today.minusDays(30), "21:00", "{\"name\":\"folic acid\",\"dose\":\"0.4mg\",\"taken\":true}");
        record("70000000-0000-0000-0000-000000000008", "pregnancy", DEMO_PREGNANCY_ID, "medication", today.minusDays(18), "09:30", "{\"name\":\"doctor prescribed progesterone\",\"dose\":\"as prescribed\",\"note\":\"仅作为记录，不作为用药建议\"}");
        record("70000000-0000-0000-0000-000000000009", "pregnancy", DEMO_PREGNANCY_ID, "fetal_movement", today.minusDays(2), "20:20", "{\"count\":18,\"durationMinutes\":30,\"note\":\"晚饭后较活跃\"}");
        record("70000000-0000-0000-0000-000000000010", "pregnancy", DEMO_PREGNANCY_ID, "mood", today.minusDays(1), "22:00", "{\"mood\":\"calm\",\"note\":\"产检结果稳定，睡前散步20分钟\"}");
        record("70000000-0000-0000-0000-000000000011", "pregnancy", DEMO_PREGNANCY_ID, "note", today, "09:10", "{\"text\":\"今天准备整理下次产检问题清单，重点问胎动、体重增长和补剂安排。\"}");
    }

    private void seedFullCycleRecords(LocalDate today) {
        record("70000000-0000-0000-0000-000000000101", "family", DEMO_FAMILY_ID, "fertility_cycle", today.minusDays(210), "08:00", "{\"cycleDay\":1,\"flow\":\"normal\"}");
        record("70000000-0000-0000-0000-000000000102", "family", DEMO_FAMILY_ID, "ovulation_test", today.minusDays(196), "20:00", "{\"result\":\"strong positive\",\"brand\":\"LH test\"}");
        record("70000000-0000-0000-0000-000000000103", "family", DEMO_FAMILY_ID, "basal_temperature", today.minusDays(195), "07:00", "{\"temperatureC\":36.72}");
        record("70000000-0000-0000-0000-000000000104", "family", DEMO_FAMILY_ID, "intercourse", today.minusDays(195), "23:00", "{\"note\":\"排卵期安排，双方状态良好\"}");
        record("70000000-0000-0000-0000-000000000105", "family", DEMO_FAMILY_ID, "fertility_supplement", today.minusDays(190), "21:00", "{\"name\":\"folic acid\",\"dose\":\"0.4mg\"}");
        record("70000000-0000-0000-0000-000000000201", "family", DEMO_FAMILY_ID, "delivery_event", today.minusMonths(14), "06:40", "{\"event\":\"见红\",\"note\":\"少量，已联系医院\"}");
        record("70000000-0000-0000-0000-000000000202", "family", DEMO_FAMILY_ID, "contraction", today.minusMonths(14), "08:15", "{\"durationSeconds\":48,\"intervalMinutes\":6}");
        record("70000000-0000-0000-0000-000000000203", "family", DEMO_FAMILY_ID, "delivery_note", today.minusMonths(14), "13:20", "{\"text\":\"入院待产，资料和待产包已带齐。\"}");
        record("70000000-0000-0000-0000-000000000301", "family", DEMO_FAMILY_ID, "postpartum_lochia", today.minusMonths(13), "09:00", "{\"level\":\"light\",\"note\":\"颜色转淡\"}");
        record("70000000-0000-0000-0000-000000000302", "family", DEMO_FAMILY_ID, "postpartum_mood", today.minusMonths(13), "21:00", "{\"mood\":\"tired\",\"note\":\"夜间喂养较多，需要家人轮班\"}");
        record("70000000-0000-0000-0000-000000000303", "family", DEMO_FAMILY_ID, "postpartum_medication", today.minusMonths(13), "08:30", "{\"name\":\"iron supplement\",\"dose\":\"doctor prescribed\"}");
        record("70000000-0000-0000-0000-000000000401", "baby", DEMO_BABY_ID, "baby_feeding", today.minusDays(5), "08:00", "{\"amountMl\":160,\"type\":\"bottle\"}");
        record("70000000-0000-0000-0000-000000000402", "baby", DEMO_BABY_ID, "baby_sleep", today.minusDays(5), "12:30", "{\"durationMinutes\":95}");
        record("70000000-0000-0000-0000-000000000403", "baby", DEMO_BABY_ID, "baby_diaper", today.minusDays(4), "14:10", "{\"type\":\"wet\"}");
        record("70000000-0000-0000-0000-000000000404", "baby", DEMO_BABY_ID, "baby_growth", today.minusDays(40), "10:00", "{\"babyWeightKg\":9.2,\"heightCm\":75.0}");
        record("70000000-0000-0000-0000-000000000405", "baby", DEMO_BABY_ID, "baby_growth", today.minusDays(5), "10:00", "{\"babyWeightKg\":9.6,\"heightCm\":77.0}");
        record("70000000-0000-0000-0000-000000000406", "baby", DEMO_BABY_ID, "baby_note", today.minusDays(1), "19:30", "{\"text\":\"今天能扶站更稳，辅食吃了南瓜泥。\"}");
    }

    private void seedReports(LocalDate today) {
        report("80000000-0000-0000-0000-000000000001", "blood", "孕 7 周血检复查", today.minusDays(78), "{\"indicators\":[{\"code\":\"hcg\",\"value\":56820},{\"code\":\"progesterone\",\"value\":25.6}]}");
        report("80000000-0000-0000-0000-000000000002", "blood", "孕 12 周建档血常规", today.minusDays(42), "{\"indicators\":[{\"code\":\"hemoglobin\",\"value\":121},{\"code\":\"platelet\",\"value\":238}]}");
        report("80000000-0000-0000-0000-000000000003", "blood_pressure", "家庭血压阶段汇总", today.minusDays(7), "{\"indicators\":[{\"code\":\"systolic\",\"value\":116},{\"code\":\"diastolic\",\"value\":74}]}");
    }

    private void seedTodos(LocalDate today) {
        todo("90000000-0000-0000-0000-000000000001", "预约 20 周系统超声", "prenatal_checkup", "pregnancy", DEMO_PREGNANCY_ID, today.plusDays(12), "09:00", "pending");
        todo("90000000-0000-0000-0000-000000000002", "整理下次产检问题清单", "custom", "pregnancy", DEMO_PREGNANCY_ID, today.plusDays(1), "20:00", "pending");
        todo("90000000-0000-0000-0000-000000000003", "每日叶酸/复合维生素记录", "supplement", "pregnancy", DEMO_PREGNANCY_ID, today, "21:00", "pending");
        todo("90000000-0000-0000-0000-000000000004", "42 天复查资料归档示例", "postpartum_review", "family", DEMO_FAMILY_ID, today.minusMonths(13), "10:00", "done");
        todo("90000000-0000-0000-0000-000000000005", "年年下次疫苗提醒", "vaccine", "baby", DEMO_BABY_ID, today.plusDays(18), "09:30", "pending");
    }

    private void seedReminders(LocalDate today) {
        reminder("91000000-0000-0000-0000-000000000001", "产检前一天准备报告和医保卡", "prenatal_checkup", "pregnancy", DEMO_PREGNANCY_ID, today.plusDays(11), "20:00", "scheduled");
        reminder("91000000-0000-0000-0000-000000000002", "晚上记录补剂", "supplement", "pregnancy", DEMO_PREGNANCY_ID, today, "21:00", "scheduled");
        reminder("91000000-0000-0000-0000-000000000003", "年年疫苗接种", "vaccine", "baby", DEMO_BABY_ID, today.plusDays(18), "09:00", "scheduled");
        reminder("91000000-0000-0000-0000-000000000004", "孕期体重复盘", "custom", "pregnancy", DEMO_PREGNANCY_ID, today.plusDays(3), "08:30", "scheduled");
    }

    private void seedAiConfigs(OffsetDateTime now) {
        aiConfig("a1000000-0000-0000-0000-000000000001", "provider", "demo-deepseek-provider", "DeepSeek demo public account", "deepseek", "active", "{\"model\":\"deepseek-chat\",\"credentialRef\":\"env:DEEPSEEK_API_KEY\",\"baseUrl\":\"https://api.deepseek.com\",\"pricing\":{\"currency\":\"CNY\",\"promptPer1K\":\"0.002\",\"completionPer1K\":\"0.008\"}}", "demo-v1", now);
        aiConfig("a1000000-0000-0000-0000-000000000002", "prompt", "demo-record-prompt", "Record extraction prompt", "deepseek", "active", "{\"purpose\":\"record_extraction\",\"safetyPolicy\":\"draft_only\",\"systemPrompt\":\"Extract pregnancy records as drafts only.\"}", "demo-prompt-v1", now.minusMinutes(3));
        aiConfig("a1000000-0000-0000-0000-000000000003", "schema", "demo-draft-schema", "Draft schema", "deepseek", "active", "{\"type\":\"object\",\"required\":[\"records\",\"todos\",\"reports\"],\"properties\":{\"records\":{\"type\":\"array\"},\"todos\":{\"type\":\"array\"},\"reports\":{\"type\":\"array\"}}}", "demo-schema-v1", now.minusMinutes(2));
        aiConfig("a1000000-0000-0000-0000-000000000004", "preprocessor", "demo-aliyun-ocr", "Aliyun OCR demo preprocessor", "aliyun", "active", "{\"service\":\"ocr\",\"preprocessor\":\"aliyun_ocr\",\"credentialRef\":\"env:ALIYUN_ACCESS_KEY\",\"region\":\"cn-shanghai\",\"endpoint\":\"https://ocr-api.cn-shanghai.aliyuncs.com\",\"enabled\":false}", "demo-ocr-v1", now.minusMinutes(1));
        aiConfig("a1000000-0000-0000-0000-000000000005", "qa_policy", "demo-qa-policy", "AI Q&A safety policy", "deepseek", "active", "{\"safetyPolicy\":\"no_medical_decision\",\"locales\":{\"zh-CN\":{\"educationAnswer\":\"我可以帮你整理科普信息和复诊问题，但不能替代医生判断。\",\"safetyAnswer\":\"这个问题可能涉及诊断、急症或用药决策，请及时联系产检医生或医院。\",\"suggestedQuestions\":[\"这项指标需要复查吗？\",\"下次产检需要准备哪些资料？\"],\"safetyQuestions\":[\"是否需要立即就医？\",\"当前症状是否需要急诊评估？\"],\"warnings\":[\"AI 仅用于整理和沟通准备。\"]}}}", "demo-qa-v1", now);
    }

    private void seedAiAuditLogs(OffsetDateTime now) {
        aiAudit("a2000000-0000-0000-0000-000000000001", now.minusHours(5), "ai_draft", "text", "low", false, true, "MODEL_CLIENT_NOT_CONFIGURED", "体重57.3kg，今晚胎动30分钟18次，提醒明天整理产检问题", 0, 0, 0, "draft");
        aiAudit("a2000000-0000-0000-0000-000000000002", now.minusHours(4), "qa", "question", "high", true, false, "HIGH_RISK_BLOCKED", "能不能自己调整用药剂量", 0, 0, 0, "blocked");
        aiAudit("a2000000-0000-0000-0000-000000000003", now.minusHours(3), "qa", "question", "low", false, false, "OK", "下次产检要问医生哪些问题", 12, 48, 60, "answered");
    }

    private void seedAiPreprocessLogs(OffsetDateTime now) {
        insert("ai_preprocess_audit_logs", UUID.fromString("a3000000-0000-0000-0000-000000000001"), now.minusHours(2), now.minusHours(2), "ocr_report", "aliyun", "aliyun_ocr", "oss://demo/reports/week12-blood.jpg", 86, true, "ALIYUN_PREPROCESSOR_DISABLED", 38L, "fallback");
        insert("ai_preprocess_audit_logs", UUID.fromString("a3000000-0000-0000-0000-000000000002"), now.minusHours(1), now.minusHours(1), "asr_record", "aliyun", "aliyun_asr", "oss://demo/voice/checkup-note.m4a", 42, true, "ALIYUN_PREPROCESSOR_DISABLED", 31L, "fallback");
    }

    private void seedAiConfirmations(OffsetDateTime now) {
        insert("ai_draft_confirmations", UUID.fromString("a4000000-0000-0000-0000-000000000001"), now.minusHours(2), now.minusHours(2), DEMO_FAMILY_ID, DEMO_USER_ID, "pregnancy", DEMO_PREGNANCY_ID, "deepseek", "deepseek-chat", "record_extraction", "{\"records\":[{\"recordType\":\"weight\"}],\"todos\":[{\"title\":\"整理产检问题\"}]}", "[\"70000000-0000-0000-0000-000000000011\"]", "[]", "[\"90000000-0000-0000-0000-000000000002\"]", now.minusHours(2));
    }

    private void record(String id, String subjectType, UUID subjectId, String recordType, LocalDate date, String time, String payload) {
        OffsetDateTime occurredAt = at(date, time);
        insert("records", UUID.fromString(id), occurredAt.minusMinutes(2), occurredAt.minusMinutes(2), DEMO_FAMILY_ID, subjectType, subjectId, recordType, occurredAt, payload, "family");
    }

    private void report(String id, String reportType, String title, LocalDate examinedAt, String indicatorsJson) {
        OffsetDateTime now = at(examinedAt, "10:00");
        insert("medical_reports", UUID.fromString(id), now, now, DEMO_FAMILY_ID, "pregnancy", DEMO_PREGNANCY_ID, reportType, title, examinedAt, indicatorsJson);
    }

    private void todo(String id, String title, String category, String subjectType, UUID subjectId, LocalDate date, String time, String status) {
        OffsetDateTime dueAt = at(date, time);
        insert("todos", UUID.fromString(id), dueAt.minusMinutes(1), dueAt.minusMinutes(1), DEMO_FAMILY_ID, title, category, subjectType, subjectId, dueAt, status);
    }

    private void reminder(String id, String title, String scene, String subjectType, UUID subjectId, LocalDate date, String time, String status) {
        OffsetDateTime triggerAt = at(date, time);
        insert("reminders", UUID.fromString(id), triggerAt.minusMinutes(1), triggerAt.minusMinutes(1), DEMO_FAMILY_ID, title, scene, subjectType, subjectId, triggerAt, status);
    }

    private void aiConfig(String id, String type, String key, String name, String provider, String status, String json, String version, OffsetDateTime createdAt) {
        insert("ai_configs", UUID.fromString(id), createdAt, createdAt, type, key, name, provider, status, json, version, "demo-seed");
    }

    private void aiAudit(String id, OffsetDateTime createdAt, String purpose, String inputType, String riskLevel, boolean blocked, boolean fallback, String errorCode, String inputPreview, int promptTokens, int completionTokens, int totalTokens, String status) {
        insert("ai_audit_logs", UUID.fromString(id), createdAt, createdAt, DEMO_FAMILY_ID, DEMO_USER_ID, purpose, "deepseek", "deepseek-chat", inputType, inputPreview.length(), inputPreview, riskLevel, blocked, fallback, errorCode, promptTokens, completionTokens, totalTokens, 42L, "CNY", new BigDecimal("0.000000"), status, "demo-qa-v1", "qa".equals(purpose), "qa".equals(purpose) ? "no_medical_decision" : "draft_only", blocked ? "medication_decision" : "low_risk");
    }

    private OffsetDateTime at(LocalDate date, String time) {
        return date.atTime(LocalTime.parse(time)).atZone(DEMO_ZONE).toOffsetDateTime();
    }

    private void insert(String table, Object... values) {
        String columns = switch (table) {
            case "app_users" -> "id, created_at, updated_at, openid, nickname";
            case "session_tokens" -> "id, created_at, updated_at, token, user_id, expires_at";
            case "families" -> "id, created_at, updated_at, name, owner_user_id";
            case "family_members" -> "id, created_at, updated_at, family_id, user_id, role, relation";
            case "mother_profiles" -> "id, created_at, updated_at, family_id, owner_user_id, birthday, height_cm, pre_pregnancy_weight_kg, blood_type";
            case "pregnancy_profiles" -> "id, created_at, updated_at, family_id, lmp_date, due_date, fetus_count, status";
            case "baby_profiles" -> "id, created_at, updated_at, family_id, pregnancy_id, name, gender, birth_date_time, birth_weight_kg, birth_length_cm";
            case "records" -> "id, created_at, updated_at, family_id, subject_type, subject_id, record_type, occurred_at, payload_json, privacy_level";
            case "medical_reports" -> "id, created_at, updated_at, family_id, subject_type, subject_id, report_type, title, examined_at, indicators_json";
            case "todos" -> "id, created_at, updated_at, family_id, title, category, subject_type, subject_id, due_at, status";
            case "reminders" -> "id, created_at, updated_at, family_id, title, scene, subject_type, subject_id, trigger_at, status";
            case "ai_configs" -> "id, created_at, updated_at, config_type, config_key, display_name, provider, status, config_json, version_label, created_by";
            case "ai_audit_logs" -> "id, created_at, updated_at, family_id, user_id, purpose, provider, model, input_type, input_length, input_preview, risk_level, blocked, fallback_used, error_code, prompt_tokens, completion_tokens, total_tokens, latency_ms, cost_currency, estimated_cost, status, policy_version, policy_configured, safety_policy, risk_reasons";
            case "ai_preprocess_audit_logs" -> "id, created_at, updated_at, purpose, provider, preprocessor, file_url_preview, text_length, fallback_used, error_code, latency_ms, status";
            case "ai_draft_confirmations" -> "id, created_at, updated_at, family_id, user_id, subject_type, subject_id, provider, model, purpose, draft_preview, record_ids_json, report_ids_json, todo_ids_json, confirmed_at";
            default -> throw new IllegalArgumentException("Unsupported demo seed table: " + table);
        };
        String placeholders = String.join(", ", java.util.Collections.nCopies(values.length, "?"));
        jdbc.update("insert into " + table + " (" + columns + ") values (" + placeholders + ")", values);
    }
}
