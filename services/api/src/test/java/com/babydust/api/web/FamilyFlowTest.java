package com.babydust.api.web;

import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class FamilyFlowTest {
    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void pregnancyP0AcceptanceFlowFromWeekSixProfileToSummaryAnalyticsAndReports() throws Exception {
        String token = login("p0-acceptance", "Mom");
        String familyId = createFamily(token, "P0 family");
        String pregnancyId = createPregnancy(token, familyId, "2026-04-13");

        createRecord(token, familyId, pregnancyId, "weight", "2026-05-25T08:00:00+08:00", "{\"weightKg\":56.2}");
        createRecord(token, familyId, pregnancyId, "blood_pressure", "2026-05-25T08:05:00+08:00", "{\"systolic\":118,\"diastolic\":76}");
        createRecord(token, familyId, pregnancyId, "symptom", "2026-05-25T20:00:00+08:00", "{\"name\":\"nausea\",\"severity\":2}");
        createRecord(token, familyId, pregnancyId, "supplement", "2026-05-25T21:00:00+08:00", "{\"name\":\"folic acid\",\"dose\":\"0.4mg\"}");

        mvc.perform(get("/api/v1/records?familyId=" + familyId + "&subjectType=pregnancy&subjectId=" + pregnancyId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(4))
                .andExpect(jsonPath("$.data[0].recordType").value("supplement"));

        String todosResponse = mvc.perform(get("/api/v1/todos?familyId=" + familyId + "&subjectType=pregnancy&subjectId=" + pregnancyId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(10))
                .andExpect(jsonPath("$.data[0].category").value("prenatal_checkup"))
                .andReturn()
                .getResponse()
                .getContentAsString();
        String firstTodoId = objectMapper.readTree(todosResponse).path("data").get(0).path("id").asText();

        mvc.perform(post("/api/v1/todos/" + firstTodoId + "/status")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"done\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("done"));

        createReport(token, familyId, pregnancyId, "blood", "Week 6 blood test", "2026-05-25", "{\"indicators\":[{\"code\":\"hcg\",\"value\":1000},{\"code\":\"progesterone\",\"value\":22.4}]}");

        mvc.perform(get("/api/v1/reports?familyId=" + familyId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].title").value("Week 6 blood test"));

        mvc.perform(get("/api/v1/analytics/series?familyId=" + familyId + "&metric=weight")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.metric").value("weight"))
                .andExpect(jsonPath("$.data.points.length()").value(1))
                .andExpect(jsonPath("$.data.points[0].value").value(56.2));

        mvc.perform(get("/api/v1/analytics/series?familyId=" + familyId + "&metric=blood_pressure_systolic")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.points[0].value").value(118.0));

        mvc.perform(get("/api/v1/home/summary?familyId=" + familyId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.stage").value("pregnancy"))
                .andExpect(jsonPath("$.data.pregnancy.gestationalWeekDisplay").exists())
                .andExpect(jsonPath("$.data.pregnancy.dueDate").value("2027-01-18"))
                .andExpect(jsonPath("$.data.recentRecords.length()").value(4))
                .andExpect(jsonPath("$.data.recentReports.length()").value(1))
                .andExpect(jsonPath("$.data.recentReports[0].title").value("Week 6 blood test"))
                .andExpect(jsonPath("$.data.prenatalPlanProgress.total").value(10))
                .andExpect(jsonPath("$.data.prenatalPlanProgress.completed").value(1))
                .andExpect(jsonPath("$.data.prenatalPlanProgress.completionRate").value(10))
                .andExpect(jsonPath("$.data.keyMetrics[0].metric").value("weight"))
                .andExpect(jsonPath("$.data.keyMetrics[0].points[0].value").value(56.2))
                .andExpect(jsonPath("$.data.keyMetrics[1].metric").value("blood_pressure_systolic"))
                .andExpect(jsonPath("$.data.keyMetrics[1].points[0].value").value(118.0))
                .andExpect(jsonPath("$.data.keyMetrics[2].metric").value("blood_pressure_diastolic"))
                .andExpect(jsonPath("$.data.keyMetrics[2].points[0].value").value(76.0));
    }

    @Test
    void createsFamilyPregnancyAndPregnancyRecord() throws Exception {
        String login = mvc.perform(post("/api/v1/auth/wechat-login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"flow-code\",\"nickname\":\"Mom\"}"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        String token = objectMapper.readTree(login).path("data").path("token").asText();

        String familyResponse = mvc.perform(post("/api/v1/families")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Our family\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id", notNullValue()))
                .andReturn()
                .getResponse()
                .getContentAsString();
        JsonNode family = objectMapper.readTree(familyResponse).path("data");
        String familyId = family.path("id").asText();

        String pregnancyResponse = mvc.perform(post("/api/v1/profiles/pregnancies")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"familyId\":\"" + familyId + "\",\"lmpDate\":\"2026-04-13\",\"fetusCount\":1}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.dueDate").value("2027-01-18"))
                .andReturn()
                .getResponse()
                .getContentAsString();
        String pregnancyId = objectMapper.readTree(pregnancyResponse).path("data").path("id").asText();

        mvc.perform(post("/api/v1/records")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"familyId\":\"" + familyId + "\",\"subjectType\":\"pregnancy\",\"subjectId\":\"" + pregnancyId + "\",\"recordType\":\"weight\",\"occurredAt\":\"2026-05-25T08:00:00+08:00\",\"payloadJson\":\"{\\\"weightKg\\\":56.2}\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.recordType").value("weight"));

        mvc.perform(post("/api/v1/records")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"familyId\":\"" + familyId + "\",\"subjectType\":\"pregnancy\",\"subjectId\":\"" + pregnancyId + "\",\"recordType\":\"blood_pressure\",\"occurredAt\":\"2026-05-26T08:00:00+08:00\",\"payloadJson\":\"{\\\"systolic\\\":118,\\\"diastolic\\\":76}\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.recordType").value("blood_pressure"));

        mvc.perform(get("/api/v1/records?familyId=" + familyId + "&subjectType=pregnancy&subjectId=" + pregnancyId + "&recordType=weight&fromDate=2026-05-01&toDate=2026-05-31")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].recordType").value("weight"));

        String todosResponse = mvc.perform(get("/api/v1/todos?familyId=" + familyId + "&subjectType=pregnancy&subjectId=" + pregnancyId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].category").value("prenatal_checkup"))
                .andReturn()
                .getResponse()
                .getContentAsString();
        String todoId = objectMapper.readTree(todosResponse).path("data").get(0).path("id").asText();

        mvc.perform(post("/api/v1/todos/" + todoId + "/status")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"done\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("done"));

        mvc.perform(get("/api/v1/home/summary?familyId=" + familyId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.stage").value("pregnancy"))
                .andExpect(jsonPath("$.data.pregnancy.gestationalWeekDisplay").exists())
                .andExpect(jsonPath("$.data.pregnancy.dueDate").value("2027-01-18"))
                .andExpect(jsonPath("$.data.upcomingTodos[0].category").value("prenatal_checkup"))
                .andExpect(jsonPath("$.data.keyMetrics[0].metric").value("weight"))
                .andExpect(jsonPath("$.data.keyMetrics[0].points[0].value").value(56.2));
    }

    @Test
    void inviteRequiresFamilyMembership() throws Exception {
        String ownerLogin = mvc.perform(post("/api/v1/auth/wechat-login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"invite-owner\",\"nickname\":\"Owner\"}"))
                .andReturn()
                .getResponse()
                .getContentAsString();
        String ownerToken = objectMapper.readTree(ownerLogin).path("data").path("token").asText();

        String familyResponse = mvc.perform(post("/api/v1/families")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Invite family\"}"))
                .andReturn()
                .getResponse()
                .getContentAsString();
        String familyId = objectMapper.readTree(familyResponse).path("data").path("id").asText();

        String outsiderLogin = mvc.perform(post("/api/v1/auth/wechat-login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"invite-outsider\",\"nickname\":\"Outsider\"}"))
                .andReturn()
                .getResponse()
                .getContentAsString();
        String outsiderToken = objectMapper.readTree(outsiderLogin).path("data").path("token").asText();

        mvc.perform(post("/api/v1/families/" + familyId + "/invites")
                        .header("Authorization", "Bearer " + outsiderToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("PERMISSION_DENIED"));
    }

    @Test
    void recordPayloadMustMatchSupportedType() throws Exception {
        String login = mvc.perform(post("/api/v1/auth/wechat-login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"record-validation\",\"nickname\":\"Mom\"}"))
                .andReturn()
                .getResponse()
                .getContentAsString();
        String token = objectMapper.readTree(login).path("data").path("token").asText();

        String familyResponse = mvc.perform(post("/api/v1/families")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Record family\"}"))
                .andReturn()
                .getResponse()
                .getContentAsString();
        String familyId = objectMapper.readTree(familyResponse).path("data").path("id").asText();

        String pregnancyResponse = mvc.perform(post("/api/v1/profiles/pregnancies")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"familyId\":\"" + familyId + "\",\"lmpDate\":\"2026-04-13\",\"fetusCount\":1}"))
                .andReturn()
                .getResponse()
                .getContentAsString();
        String pregnancyId = objectMapper.readTree(pregnancyResponse).path("data").path("id").asText();

        mvc.perform(post("/api/v1/records")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"familyId\":\"" + familyId + "\",\"subjectType\":\"pregnancy\",\"subjectId\":\"" + pregnancyId + "\",\"recordType\":\"weight\",\"occurredAt\":\"2026-05-25T08:00:00+08:00\",\"payloadJson\":\"{\\\"value\\\":56.2}\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_ARGUMENT"));
    }

    @Test
    void reportSubjectMustBelongToFamily() throws Exception {
        String login = mvc.perform(post("/api/v1/auth/wechat-login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"report-validation\",\"nickname\":\"Mom\"}"))
                .andReturn()
                .getResponse()
                .getContentAsString();
        String token = objectMapper.readTree(login).path("data").path("token").asText();

        String familyResponse = mvc.perform(post("/api/v1/families")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Report family\"}"))
                .andReturn()
                .getResponse()
                .getContentAsString();
        String familyId = objectMapper.readTree(familyResponse).path("data").path("id").asText();

        mvc.perform(post("/api/v1/reports")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"familyId\":\"" + familyId + "\",\"subjectType\":\"pregnancy\",\"subjectId\":\"00000000-0000-0000-0000-000000000001\",\"reportType\":\"blood\",\"title\":\"Blood test\",\"examinedAt\":\"2026-05-25\",\"indicatorsJson\":\"{\\\"hcg\\\":1000}\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("RESOURCE_NOT_FOUND"));
    }

    @Test
    void reportIndicatorsUseStructuredArrayAndDefinitions() throws Exception {
        String login = mvc.perform(post("/api/v1/auth/wechat-login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"report-indicators\",\"nickname\":\"Mom\"}"))
                .andReturn()
                .getResponse()
                .getContentAsString();
        String token = objectMapper.readTree(login).path("data").path("token").asText();

        String familyResponse = mvc.perform(post("/api/v1/families")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Indicator family\"}"))
                .andReturn()
                .getResponse()
                .getContentAsString();
        String familyId = objectMapper.readTree(familyResponse).path("data").path("id").asText();

        String pregnancyResponse = mvc.perform(post("/api/v1/profiles/pregnancies")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"familyId\":\"" + familyId + "\",\"lmpDate\":\"2026-04-13\",\"fetusCount\":1}"))
                .andReturn()
                .getResponse()
                .getContentAsString();
        String pregnancyId = objectMapper.readTree(pregnancyResponse).path("data").path("id").asText();

        mvc.perform(get("/api/v1/reports/indicator-definitions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].code").value("hcg"));

        mvc.perform(post("/api/v1/reports")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"familyId\":\"" + familyId + "\",\"subjectType\":\"pregnancy\",\"subjectId\":\"" + pregnancyId + "\",\"reportType\":\"blood\",\"title\":\"Blood test\",\"examinedAt\":\"2026-05-25\",\"indicatorsJson\":\"{\\\"indicators\\\":[{\\\"code\\\":\\\"hcg\\\",\\\"value\\\":1000},{\\\"code\\\":\\\"progesterone\\\",\\\"value\\\":22.4}]}\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.reportType").value("blood"));

        mvc.perform(post("/api/v1/reports")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"familyId\":\"" + familyId + "\",\"subjectType\":\"pregnancy\",\"subjectId\":\"" + pregnancyId + "\",\"reportType\":\"blood\",\"title\":\"Bad report\",\"examinedAt\":\"2026-05-25\",\"indicatorsJson\":\"{\\\"hcg\\\":1000}\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_ARGUMENT"));
    }

    @Test
    void dictionariesExposeRecordTypesIndicatorsAndI18nForClientBootstrap() throws Exception {
        mvc.perform(get("/api/v1/records/types"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].type").value("weight"))
                .andExpect(jsonPath("$.data[0].requiredFields[0]").value("weightKg"));

        mvc.perform(get("/api/v1/reports/indicator-definitions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].code").value("hcg"))
                .andExpect(jsonPath("$.data[0].reportType").value("blood"));

        mvc.perform(post("/api/v1/admin/i18n")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"locale\":\"zh-CN\",\"key\":\"appName\",\"value\":\"接好孕\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("draft"))
                .andExpect(jsonPath("$.data.message.locale").value("zh-CN"))
                .andExpect(jsonPath("$.data.message.value").value("接好孕"));
    }

    @Test
    void crossFamilyRecordReportTodoAndAnalyticsAccessIsRejected() throws Exception {
        String ownerToken = login("cross-family-owner", "Owner");
        String ownerFamilyId = createFamily(ownerToken, "Owner family");
        String ownerPregnancyId = createPregnancy(ownerToken, ownerFamilyId, "2026-04-13");
        String recordId = createRecordAndReturnId(ownerToken, ownerFamilyId, ownerPregnancyId, "weight", "2026-05-25T08:00:00+08:00", "{\"weightKg\":56.2}");
        String reportId = createReportAndReturnId(ownerToken, ownerFamilyId, ownerPregnancyId, "blood", "Blood test", "2026-05-25", "{\"indicators\":[{\"code\":\"hcg\",\"value\":1000}]}");
        String reminderId = createReminderAndReturnId(ownerToken, ownerFamilyId, "Prenatal checkup", "prenatal_checkup", "pregnancy", ownerPregnancyId, "2026-06-01T09:00:00+08:00");
        String todoId = objectMapper.readTree(mvc.perform(get("/api/v1/todos?familyId=" + ownerFamilyId + "&subjectType=pregnancy&subjectId=" + ownerPregnancyId)
                        .header("Authorization", "Bearer " + ownerToken))
                .andReturn()
                .getResponse()
                .getContentAsString()).path("data").get(0).path("id").asText();

        String outsiderToken = login("cross-family-outsider", "Outsider");
        String outsiderFamilyId = createFamily(outsiderToken, "Outsider family");

        mvc.perform(get("/api/v1/home/summary?familyId=" + ownerFamilyId)
                        .header("Authorization", "Bearer " + outsiderToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("PERMISSION_DENIED"));

        mvc.perform(get("/api/v1/records/" + recordId)
                        .header("Authorization", "Bearer " + outsiderToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("PERMISSION_DENIED"));

        mvc.perform(get("/api/v1/reports/" + reportId)
                        .header("Authorization", "Bearer " + outsiderToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("PERMISSION_DENIED"));

        mvc.perform(post("/api/v1/todos/" + todoId + "/status")
                        .header("Authorization", "Bearer " + outsiderToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"done\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("PERMISSION_DENIED"));

        mvc.perform(post("/api/v1/reminders/" + reminderId + "/status")
                        .header("Authorization", "Bearer " + outsiderToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"done\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("PERMISSION_DENIED"));

        mvc.perform(get("/api/v1/analytics/series?familyId=" + ownerFamilyId + "&metric=weight")
                        .header("Authorization", "Bearer " + outsiderToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("PERMISSION_DENIED"));

        mvc.perform(get("/api/v1/home/summary?familyId=" + outsiderFamilyId)
                        .header("Authorization", "Bearer " + outsiderToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.stage").value("not_set"));
    }

    @Test
    void phaseFourReminderCenterSupportsCreateListAndStatusFlow() throws Exception {
        String token = login("phase-four-reminders", "Mom");
        String familyId = createFamily(token, "Reminder family");
        String pregnancyId = createPregnancy(token, familyId, "2026-04-13");

        String reminderId = createReminderAndReturnId(token, familyId, "NT checkup", "prenatal_checkup", "pregnancy", pregnancyId, "2026-06-08T09:00:00+08:00");
        createReminderAndReturnId(token, familyId, "Take folic acid", "supplement", "family", familyId, "2026-05-26T21:00:00+08:00");

        mvc.perform(get("/api/v1/reminders?familyId=" + familyId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].scene").value("supplement"))
                .andExpect(jsonPath("$.data[0].status").value("scheduled"));

        mvc.perform(post("/api/v1/reminders/" + reminderId + "/status")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"done\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("done"));

        mvc.perform(post("/api/v1/reminders/" + reminderId + "/status")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"cancelled\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("cancelled"));

        mvc.perform(post("/api/v1/reminders/" + reminderId + "/status")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"unsupported\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_ARGUMENT"));
    }

    @Test
    void phaseFourExportsPregnancyRecordsAndReportsWithFamilyIsolation() throws Exception {
        String token = login("phase-four-exports", "Mom");
        String familyId = createFamily(token, "Export family");
        String pregnancyId = createPregnancy(token, familyId, "2026-04-13");
        createRecord(token, familyId, pregnancyId, "weight", "2026-05-25T08:00:00+08:00", "{\"weightKg\":56.2}");
        createRecord(token, familyId, pregnancyId, "blood_pressure", "2026-05-26T08:00:00+08:00", "{\"systolic\":118,\"diastolic\":76}");
        createReport(token, familyId, pregnancyId, "blood", "First blood test", "2026-05-27", "{\"indicators\":[{\"code\":\"hcg\",\"value\":1000}]}");

        mvc.perform(get("/api/v1/exports/pregnancy-records?familyId=" + familyId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.exportType").value("pregnancy_records"))
                .andExpect(jsonPath("$.data.format").value("csv"))
                .andExpect(jsonPath("$.data.rowCount").value(2))
                .andExpect(jsonPath("$.data.rows[0].recordType").value("weight"))
                .andExpect(jsonPath("$.data.rows[0].privacyLevel").value("family"))
                .andExpect(jsonPath("$.data.csvContent").value(org.hamcrest.Matchers.containsString("id,subjectType,subjectId,recordType,occurredAt,payloadJson,privacyLevel")))
                .andExpect(jsonPath("$.data.csvContent").value(org.hamcrest.Matchers.containsString("\"blood_pressure\"")));

        mvc.perform(get("/api/v1/exports/reports?familyId=" + familyId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.exportType").value("medical_reports"))
                .andExpect(jsonPath("$.data.format").value("csv"))
                .andExpect(jsonPath("$.data.rowCount").value(1))
                .andExpect(jsonPath("$.data.rows[0].title").value("First blood test"))
                .andExpect(jsonPath("$.data.csvContent").value(org.hamcrest.Matchers.containsString("id,subjectType,subjectId,reportType,title,examinedAt,indicatorsJson")))
                .andExpect(jsonPath("$.data.csvContent").value(org.hamcrest.Matchers.containsString("\"First blood test\"")));

        String outsiderToken = login("phase-four-exports-outsider", "Outsider");
        mvc.perform(get("/api/v1/exports/pregnancy-records?familyId=" + familyId)
                        .header("Authorization", "Bearer " + outsiderToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("PERMISSION_DENIED"));
    }

    @Test
    void phaseFourAnalyticsIncludesBabyGrowthSeries() throws Exception {
        String token = login("phase-four-baby-growth-analytics", "Mom");
        String familyId = createFamily(token, "Baby growth analytics family");
        String babyId = createBaby(token, familyId, "Baby A");

        createBabyRecord(token, familyId, babyId, "baby_growth", "2027-01-12T09:00:00+08:00", "{\"babyWeightKg\":3.2,\"heightCm\":50}");
        createBabyRecord(token, familyId, babyId, "baby_growth", "2027-02-12T09:00:00+08:00", "{\"babyWeightKg\":4.5,\"heightCm\":55}");

        mvc.perform(get("/api/v1/analytics/series?familyId=" + familyId + "&metric=baby_weight")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.metric").value("baby_weight"))
                .andExpect(jsonPath("$.data.unit").value("kg"))
                .andExpect(jsonPath("$.data.points.length()").value(2))
                .andExpect(jsonPath("$.data.points[0].value").value(3.2))
                .andExpect(jsonPath("$.data.points[1].value").value(4.5));

        mvc.perform(get("/api/v1/analytics/series?familyId=" + familyId + "&metric=baby_height")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.metric").value("baby_height"))
                .andExpect(jsonPath("$.data.unit").value("cm"))
                .andExpect(jsonPath("$.data.points.length()").value(2))
                .andExpect(jsonPath("$.data.points[0].value").value(50.0))
                .andExpect(jsonPath("$.data.points[1].value").value(55.0));
    }

    @Test
    void phaseFourAnalyticsOverviewIncludesCheckupProgressAndMedicationCounts() throws Exception {
        String token = login("phase-four-analytics-overview", "Mom");
        String familyId = createFamily(token, "Analytics overview family");
        String pregnancyId = createPregnancy(token, familyId, "2026-04-13");
        createRecord(token, familyId, pregnancyId, "medication", "2026-05-25T08:00:00+08:00", "{\"name\":\"progesterone\",\"dose\":\"10mg\"}");
        createRecord(token, familyId, pregnancyId, "supplement", "2026-05-25T21:00:00+08:00", "{\"name\":\"folic acid\",\"dose\":\"0.4mg\"}");

        String todosResponse = mvc.perform(get("/api/v1/todos?familyId=" + familyId + "&subjectType=pregnancy&subjectId=" + pregnancyId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        String firstTodoId = objectMapper.readTree(todosResponse).path("data").get(0).path("id").asText();
        mvc.perform(post("/api/v1/todos/" + firstTodoId + "/status")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"done\"}"))
                .andExpect(status().isOk());

        mvc.perform(get("/api/v1/analytics/overview?familyId=" + familyId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.prenatalPlan.total").value(10))
                .andExpect(jsonPath("$.data.prenatalPlan.completed").value(1))
                .andExpect(jsonPath("$.data.prenatalPlan.completionRate").value(10))
                .andExpect(jsonPath("$.data.medicationSupplement.medicationRecords").value(1))
                .andExpect(jsonPath("$.data.medicationSupplement.supplementRecords").value(1))
                .andExpect(jsonPath("$.data.medicationSupplement.totalRecords").value(2));
    }

    @Test
    void phaseFiveAiGatewayReturnsDraftsAndBlocksHighRiskMedicalDecisions() throws Exception {
        mvc.perform(post("/api/v1/ai/extract-record")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"text\":\"今天体重 56kg，手机号 13812345678，晚上提醒我复查\",\"inputType\":\"text\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("draft"))
                .andExpect(jsonPath("$.data.purpose").value("record_extraction"))
                .andExpect(jsonPath("$.data.provider").value("deepseek"))
                .andExpect(jsonPath("$.data.source").value("ai_draft"))
                .andExpect(jsonPath("$.data.needsUserConfirmation").value(true))
                .andExpect(jsonPath("$.data.blocked").value(false))
                .andExpect(jsonPath("$.data.fallbackUsed").value(true))
                .andExpect(jsonPath("$.data.errorCode").value("MODEL_CLIENT_NOT_CONFIGURED"))
                .andExpect(jsonPath("$.data.records[0].recordType").value("weight"))
                .andExpect(jsonPath("$.data.todos.length()").value(1));

        mvc.perform(post("/api/v1/ai/extract-report")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"text\":\"HCG 1000，孕酮 22.4\",\"inputType\":\"ocr_text\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("draft"))
                .andExpect(jsonPath("$.data.purpose").value("report_extraction"))
                .andExpect(jsonPath("$.data.fallbackUsed").value(true))
                .andExpect(jsonPath("$.data.reports[0].reportType").value("blood"))
                .andExpect(jsonPath("$.data.warnings.length()").value(1));

        mvc.perform(post("/api/v1/ai/extract-record")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"text\":\"流血很多并且剧痛，我该吃什么药\",\"inputType\":\"text\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("draft"))
                .andExpect(jsonPath("$.data.blocked").value(true))
                .andExpect(jsonPath("$.data.riskLevel").value("high"))
                .andExpect(jsonPath("$.data.fallbackUsed").value(false))
                .andExpect(jsonPath("$.data.errorCode").value("HIGH_RISK_BLOCKED"))
                .andExpect(jsonPath("$.data.records.length()").value(0))
                .andExpect(jsonPath("$.data.warnings[0]").value(org.hamcrest.Matchers.containsString("AI 不能给出医疗结论")));

        mvc.perform(get("/api/v1/ai/audit-logs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(org.hamcrest.Matchers.greaterThanOrEqualTo(3)))
                .andExpect(jsonPath("$.data[0].purpose").exists())
                .andExpect(jsonPath("$.data[0].provider").value("deepseek"))
                .andExpect(jsonPath("$.data[0].latencyMs").exists())
                .andExpect(jsonPath("$.data[0].promptTokens").value(0))
                .andExpect(jsonPath("$.data[0].completionTokens").value(0))
                .andExpect(jsonPath("$.data[0].totalTokens").value(0))
                .andExpect(jsonPath("$.data[0].costCurrency").value("CNY"))
                .andExpect(jsonPath("$.data[0].estimatedCost").value(0))
                .andExpect(jsonPath("$.data[?(@.fallbackUsed==true && @.errorCode=='MODEL_CLIENT_NOT_CONFIGURED')]").exists())
                .andExpect(jsonPath("$.data[?(@.purpose=='record_extraction' && @.inputPreview =~ /.*PHONE.*/)]").exists())
                .andExpect(jsonPath("$.data[?(@.blocked==true && @.riskLevel=='high')]").exists());
    }

    @Test
    void phaseFiveAiQaAllowsEducationAndBlocksMedicalDecisions() throws Exception {
        String defaultQaPolicy = objectMapper.writeValueAsString(java.util.Map.of(
                "safetyPolicy", "no_medical_decision",
                "locales", java.util.Map.of(
                        "zh-CN", java.util.Map.of(
                                "educationAnswer", "我可以帮你整理孕期相关信息、准备复诊问题清单，但不能判断是否正常、不能诊断，也不能建议用药或调整剂量。",
                                "safetyAnswer", "这个问题可能涉及诊断、急症或用药决策。AI 不能给出医疗结论，请及时联系产检医生、医院或急诊。",
                                "suggestedQuestions", java.util.List.of("这件事下次产检需要问医生什么？", "我应该准备哪些记录给医生看？", "哪些情况需要及时联系医院？"),
                                "safetyQuestions", java.util.List.of("我现在需要立刻就医吗？", "这个症状需要做哪些检查？", "我正在使用的药物是否需要由医生复核？")
                        )
                )
        ));
        createAiConfig("qa_policy", "qa-policy-default-test", "Default Q&A safety policy", "deepseek", "active", defaultQaPolicy, "qa-default");

        mvc.perform(post("/api/v1/ai/qa")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"question\":\"孕 6 周产检前我应该准备哪些记录？\",\"locale\":\"zh-CN\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.purpose").value("qa"))
                .andExpect(jsonPath("$.data.answerType").value("education"))
                .andExpect(jsonPath("$.data.blocked").value(false))
                .andExpect(jsonPath("$.data.riskLevel").value("low"))
                .andExpect(jsonPath("$.data.errorCode").value("OK"))
                .andExpect(jsonPath("$.data.answer").value(org.hamcrest.Matchers.containsString("不能判断是否正常")))
                .andExpect(jsonPath("$.data.suggestedQuestions.length()").value(3));

        mvc.perform(post("/api/v1/ai/qa")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"question\":\"流血很多而且剧痛，我应该吃什么药？\",\"locale\":\"zh-CN\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.purpose").value("qa"))
                .andExpect(jsonPath("$.data.answerType").value("safety"))
                .andExpect(jsonPath("$.data.blocked").value(true))
                .andExpect(jsonPath("$.data.riskLevel").value("high"))
                .andExpect(jsonPath("$.data.errorCode").value("HIGH_RISK_BLOCKED"))
                .andExpect(jsonPath("$.data.answer").value(org.hamcrest.Matchers.containsString("AI 不能给出医疗结论")));

        mvc.perform(get("/api/v1/admin/ai-audit-logs?riskLevel=high&blocked=true&errorCode=HIGH_RISK_BLOCKED&limit=10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[?(@.purpose=='qa' && @.inputType=='question')]").exists())
                .andExpect(jsonPath("$.data[?(@.purpose=='qa' && @.policyVersion=='qa-default' && @.policyConfigured==true)]").exists())
                .andExpect(jsonPath("$.data[?(@.purpose=='qa' && @.safetyPolicy=='no_medical_decision')]").exists())
                .andExpect(jsonPath("$.data[?(@.purpose=='qa' && @.riskReasons =~ /.*urgent_symptom.*/)]").exists());
    }

    @Test
    void phaseFiveAiRateLimitReturnsStableErrorCode() throws Exception {
        String subject = "rate-limit-" + System.nanoTime();
        for (int i = 0; i < 100; i++) {
            mvc.perform(post("/api/v1/ai/qa")
                            .header("X-Dev-User-Id", subject)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"question\":\"孕 6 周产检前我应该准备哪些记录？\",\"locale\":\"zh-CN\"}"))
                    .andExpect(status().isOk());
        }

        mvc.perform(post("/api/v1/ai/qa")
                        .header("X-Dev-User-Id", subject)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"question\":\"孕 6 周产检前我应该准备哪些记录？\",\"locale\":\"zh-CN\"}"))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("RATE_LIMITED"));
    }

    @Test
    void phaseFiveAiQaUsesActiveAdminQaPolicyWithoutWeakeningSafetyBoundary() throws Exception {
        String qaPolicy = objectMapper.writeValueAsString(java.util.Map.of(
                "safetyPolicy", "no_medical_decision",
                "locales", java.util.Map.of(
                        "zh-CN", java.util.Map.of(
                                "educationAnswer", "运营配置的孕期科普整理回答。",
                                "safetyAnswer", "运营配置的高风险安全回复，请联系医生。",
                                "suggestedQuestions", java.util.List.of("配置问题一", "配置问题二"),
                                "safetyQuestions", java.util.List.of("配置安全问题一", "配置安全问题二"),
                                "warnings", java.util.List.of("配置安全提示")
                        )
                )
        ));
        createAiConfig("qa_policy", "qa-policy-active", "Q&A safety policy", "deepseek", "active", qaPolicy, "qa-v1");

        mvc.perform(post("/api/v1/ai/qa")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"question\":\"孕 6 周产检前我应该准备哪些记录？\",\"locale\":\"zh-CN\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.answerType").value("education"))
                .andExpect(jsonPath("$.data.answer").value("运营配置的孕期科普整理回答。"))
                .andExpect(jsonPath("$.data.suggestedQuestions[0]").value("配置问题一"))
                .andExpect(jsonPath("$.data.warnings[?(@=='配置安全提示')]").exists());

        mvc.perform(post("/api/v1/ai/qa")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"question\":\"流血很多而且剧痛，我应该吃什么药？\",\"locale\":\"zh-CN\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.answerType").value("safety"))
                .andExpect(jsonPath("$.data.blocked").value(true))
                .andExpect(jsonPath("$.data.errorCode").value("HIGH_RISK_BLOCKED"))
                .andExpect(jsonPath("$.data.answer").value("运营配置的高风险安全回复，请联系医生。"))
                .andExpect(jsonPath("$.data.suggestedQuestions[0]").value("配置安全问题一"));

        mvc.perform(get("/api/v1/admin/ai-audit-logs?purpose=qa&policyConfigured=true&safetyPolicy=no_medical_decision&limit=10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[?(@.policyVersion=='qa-v1' && @.riskReasons =~ /.*medication_decision.*/)]").exists());
    }

    @Test
    void phaseFiveConfirmAiDraftPersistsFormalRecordsReportsAndTodosOnlyAfterUserConfirmation() throws Exception {
        String token = login("ai-confirm-draft", "Mom");
        String familyId = createFamily(token, "AI confirm family");
        String pregnancyId = createPregnancy(token, familyId, "2026-04-13");

        String draftResponse = mvc.perform(post("/api/v1/ai/extract-record")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"text\":\"weight 56kg todo review\",\"inputType\":\"text\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.source").value("ai_draft"))
                .andExpect(jsonPath("$.data.needsUserConfirmation").value(true))
                .andExpect(jsonPath("$.data.blocked").value(false))
                .andReturn()
                .getResponse()
                .getContentAsString();
        String draftJson = objectMapper.writeValueAsString(objectMapper.readTree(draftResponse).path("data"));

        String confirmResponse = mvc.perform(post("/api/v1/ai/confirm-draft")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(java.util.Map.of(
                                "familyId", familyId,
                                "subjectType", "pregnancy",
                                "subjectId", pregnancyId,
                                "draft", draftJson
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("confirmed"))
                .andExpect(jsonPath("$.data.confirmationId").exists())
                .andExpect(jsonPath("$.data.records.length()").value(1))
                .andExpect(jsonPath("$.data.records[0].recordType").value("weight"))
                .andExpect(jsonPath("$.data.todos.length()").value(1))
                .andExpect(jsonPath("$.data.todos[0].status").value("pending"))
                .andExpect(jsonPath("$.data.reports.length()").value(0))
                .andReturn()
                .getResponse()
                .getContentAsString();
        String confirmationId = objectMapper.readTree(confirmResponse).path("data").path("confirmationId").asText();

        mvc.perform(get("/api/v1/records?familyId=" + familyId + "&subjectType=pregnancy&subjectId=" + pregnancyId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].recordType").value("weight"));

        mvc.perform(get("/api/v1/ai/draft-confirmations?familyId=" + familyId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value(confirmationId))
                .andExpect(jsonPath("$.data[0].familyId").value(familyId))
                .andExpect(jsonPath("$.data[0].subjectId").value(pregnancyId))
                .andExpect(jsonPath("$.data[0].recordIdsJson").value(org.hamcrest.Matchers.containsString("\"")))
                .andExpect(jsonPath("$.data[0].todoIdsJson").value(org.hamcrest.Matchers.containsString("\"")))
                .andExpect(jsonPath("$.data[0].draftPreview").value(org.hamcrest.Matchers.containsString("ai_draft")));

        mvc.perform(get("/api/v1/admin/ai-draft-confirmations?familyId=" + familyId + "&purpose=record_extraction&limit=5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].confirmationId").value(confirmationId))
                .andExpect(jsonPath("$.data[0].familyId").value(familyId))
                .andExpect(jsonPath("$.data[0].userId").exists())
                .andExpect(jsonPath("$.data[0].subjectType").value("pregnancy"))
                .andExpect(jsonPath("$.data[0].subjectId").value(pregnancyId))
                .andExpect(jsonPath("$.data[0].provider").value("deepseek"))
                .andExpect(jsonPath("$.data[0].model").exists())
                .andExpect(jsonPath("$.data[0].purpose").value("record_extraction"))
                .andExpect(jsonPath("$.data[0].recordIdsJson").value(org.hamcrest.Matchers.containsString("\"")))
                .andExpect(jsonPath("$.data[0].todoIdsJson").value(org.hamcrest.Matchers.containsString("\"")))
                .andExpect(jsonPath("$.data[0].draftPreview").value(org.hamcrest.Matchers.containsString("ai_draft")));

        mvc.perform(get("/api/v1/admin/ai-draft-confirmations?familyId=" + familyId + "&purpose=report_extraction&limit=5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(0));

        String blockedDraft = objectMapper.writeValueAsString(java.util.Map.of(
                "source", "ai_draft",
                "needsUserConfirmation", true,
                "blocked", true,
                "records", java.util.List.of(java.util.Map.of("recordType", "weight", "payload", java.util.Map.of("weightKg", 56))),
                "reports", java.util.List.of(),
                "todos", java.util.List.of()
        ));
        mvc.perform(post("/api/v1/ai/confirm-draft")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(java.util.Map.of(
                                "familyId", familyId,
                                "subjectType", "pregnancy",
                                "subjectId", pregnancyId,
                                "draft", blockedDraft
                        ))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_ARGUMENT"));
    }

    @Test
    void phaseFiveOcrAndAsrPreprocessorsReturnDraftsAndAuditLogs() throws Exception {
        mvc.perform(post("/api/v1/ai/ocr-report")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fileUrl\":\"oss://reports/week6.jpg\",\"text\":\"HCG 1000 孕酮 22.4\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.purpose").value("ocr_report"))
                .andExpect(jsonPath("$.data.preprocessor").value("aliyun_ocr"))
                .andExpect(jsonPath("$.data.fallbackUsed").value(false))
                .andExpect(jsonPath("$.data.errorCode").value("OK"))
                .andExpect(jsonPath("$.data.processedAt").exists())
                .andExpect(jsonPath("$.data.text").value(org.hamcrest.Matchers.containsString("HCG")))
                .andExpect(jsonPath("$.data.draft.purpose").value("report_extraction"))
                .andExpect(jsonPath("$.data.draft.inputType").value("ocr_text"))
                .andExpect(jsonPath("$.data.draft.reports[0].reportType").value("blood"));

        mvc.perform(post("/api/v1/ai/ocr-report")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fileUrl\":\"oss://reports/week6-empty.jpg\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.fallbackUsed").value(true))
                .andExpect(jsonPath("$.data.errorCode").value("ALIYUN_PREPROCESSOR_DISABLED"))
                .andExpect(jsonPath("$.data.warnings[0]").value(org.hamcrest.Matchers.containsString("Preprocessor fallback used")))
                .andExpect(jsonPath("$.data.draft.reports[0].reportType").value("blood"));

        mvc.perform(post("/api/v1/ai/asr-record")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fileUrl\":\"oss://voice/check.m4a\",\"text\":\"weight 56kg todo review\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.purpose").value("asr_record"))
                .andExpect(jsonPath("$.data.preprocessor").value("aliyun_asr"))
                .andExpect(jsonPath("$.data.fallbackUsed").value(false))
                .andExpect(jsonPath("$.data.errorCode").value("OK"))
                .andExpect(jsonPath("$.data.draft.purpose").value("record_extraction"))
                .andExpect(jsonPath("$.data.draft.inputType").value("asr_text"))
                .andExpect(jsonPath("$.data.draft.records[0].recordType").value("weight"));

        mvc.perform(get("/api/v1/ai/audit-logs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[?(@.inputType=='ocr_text')]").exists())
                .andExpect(jsonPath("$.data[?(@.inputType=='asr_text')]").exists());

        mvc.perform(get("/api/v1/admin/ai-preprocess-audit-logs?preprocessor=aliyun_ocr&errorCode=ALIYUN_PREPROCESSOR_DISABLED&limit=5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].purpose").value("ocr_report"))
                .andExpect(jsonPath("$.data[0].provider").value("aliyun"))
                .andExpect(jsonPath("$.data[0].preprocessor").value("aliyun_ocr"))
                .andExpect(jsonPath("$.data[0].fallbackUsed").value(true))
                .andExpect(jsonPath("$.data[0].errorCode").value("ALIYUN_PREPROCESSOR_DISABLED"))
                .andExpect(jsonPath("$.data[0].textLength").value(org.hamcrest.Matchers.greaterThan(0)))
                .andExpect(jsonPath("$.data[0].latencyMs").exists())
                .andExpect(jsonPath("$.data[0].fileUrlPreview").value(org.hamcrest.Matchers.containsString("oss://reports/week6-empty.jpg")));
    }

    @Test
    void phaseFiveAdminAiConfigsPersistProviderPromptAndSchema() throws Exception {
        createAiConfig("provider", "deepseek-public", "DeepSeek public account", "deepseek", "active", "{\"model\":\"deepseek-chat\",\"credentialRef\":\"env:DEEPSEEK_API_KEY\"}");
        createAiConfig("prompt", "record-extraction-v1", "Record extraction prompt", "deepseek", "draft", "{\"purpose\":\"record_extraction\",\"locale\":\"zh-CN\"}");
        createAiConfig("schema", "report-draft-v1", "Report draft schema", "deepseek", "draft", "{\"type\":\"object\",\"required\":[\"reports\"]}");
        createAiConfig("preprocessor", "aliyun-ocr-v1", "Aliyun OCR preprocessor", "aliyun", "draft", "{\"service\":\"ocr\",\"preprocessor\":\"aliyun_ocr\",\"credentialRef\":\"env:ALIYUN_ACCESS_KEY\",\"region\":\"cn-shanghai\",\"endpoint\":\"https://ocr-api.cn-shanghai.aliyuncs.com\",\"enabled\":false}");
        createAiConfig("qa_policy", "qa-policy-v1", "Q&A safety policy", "deepseek", "draft", "{\"safetyPolicy\":\"no_medical_decision\",\"educationAnswer\":\"Education only\",\"suggestedQuestions\":[\"Ask doctor\"]}");

        mvc.perform(get("/api/v1/admin/ai-configs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(org.hamcrest.Matchers.greaterThanOrEqualTo(5)))
                .andExpect(jsonPath("$.data[?(@.configType=='provider' && @.configKey=='deepseek-public')]").exists())
                .andExpect(jsonPath("$.data[?(@.configType=='prompt' && @.configKey=='record-extraction-v1')]").exists())
                .andExpect(jsonPath("$.data[?(@.configType=='schema' && @.configKey=='report-draft-v1')]").exists())
                .andExpect(jsonPath("$.data[?(@.configType=='preprocessor' && @.configKey=='aliyun-ocr-v1')]").exists())
                .andExpect(jsonPath("$.data[?(@.configType=='qa_policy' && @.configKey=='qa-policy-v1')]").exists());

        mvc.perform(get("/api/v1/admin/ai-configs?configType=provider"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].configType").value("provider"))
                .andExpect(jsonPath("$.data[0].status").value("active"))
                .andExpect(jsonPath("$.data[0].configJson").value(org.hamcrest.Matchers.containsString("credentialRef")));

        mvc.perform(get("/api/v1/admin/ai-configs?configType=preprocessor"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].configType").value("preprocessor"))
                .andExpect(jsonPath("$.data[0].provider").value("aliyun"))
                .andExpect(jsonPath("$.data[0].configJson").value(org.hamcrest.Matchers.containsString("aliyun_ocr")));

        mvc.perform(get("/api/v1/admin/ai-configs?configType=qa_policy"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].configType").value("qa_policy"))
                .andExpect(jsonPath("$.data[0].configJson").value(org.hamcrest.Matchers.containsString("no_medical_decision")));

        mvc.perform(post("/api/v1/admin/ai-configs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(java.util.Map.of(
                                "configType", "prompt",
                                "configKey", "broken-prompt",
                                "displayName", "Broken prompt",
                                "provider", "deepseek",
                                "status", "draft",
                                "configJson", "{not-json}",
                                "versionLabel", "v1",
                                "createdBy", "admin"
                        ))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_ARGUMENT"));
    }

    @Test
    void phaseFiveAdminQaPolicyConfigsValidateSafetyContract() throws Exception {
        mvc.perform(post("/api/v1/admin/ai-configs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(java.util.Map.of(
                                "configType", "qa_policy",
                                "configKey", "safe-qa-policy",
                                "displayName", "Safe QA policy",
                                "provider", "deepseek",
                                "status", "draft",
                                "configJson", "{\"safetyPolicy\":\"no_medical_decision\",\"locales\":{\"zh-CN\":{\"educationAnswer\":\"只做科普整理\",\"safetyAnswer\":\"请联系医生\",\"suggestedQuestions\":[\"问医生什么\"],\"safetyQuestions\":[\"是否就医\"],\"warnings\":[\"不能替代医生\"]}}}",
                                "versionLabel", "v1",
                                "createdBy", "admin"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.configType").value("qa_policy"));

        mvc.perform(post("/api/v1/admin/ai-configs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(java.util.Map.of(
                                "configType", "qa_policy",
                                "configKey", "unsafe-qa-policy",
                                "displayName", "Unsafe QA policy",
                                "provider", "deepseek",
                                "status", "draft",
                                "configJson", "{\"safetyPolicy\":\"medical_advice\",\"educationAnswer\":\"可以判断是否正常\"}",
                                "versionLabel", "v1",
                                "createdBy", "admin"
                        ))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_ARGUMENT"));

        mvc.perform(post("/api/v1/admin/ai-configs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(java.util.Map.of(
                                "configType", "qa_policy",
                                "configKey", "secret-qa-policy",
                                "displayName", "Secret QA policy",
                                "provider", "deepseek",
                                "status", "draft",
                                "configJson", "{\"safetyPolicy\":\"no_medical_decision\",\"apiKey\":\"sk-inline-secret\"}",
                                "versionLabel", "v1",
                                "createdBy", "admin"
                        ))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_ARGUMENT"));
    }

    @Test
    void phaseFiveAdminPreprocessorConfigsValidateAliyunSafetyContract() throws Exception {
        mvc.perform(post("/api/v1/admin/ai-configs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(java.util.Map.of(
                                "configType", "preprocessor",
                                "configKey", "aliyun-asr-v1",
                                "displayName", "Aliyun ASR preprocessor",
                                "provider", "aliyun",
                                "status", "draft",
                                "configJson", "{\"service\":\"asr\",\"preprocessor\":\"aliyun_asr\",\"credentialRef\":\"env:ALIYUN_ACCESS_KEY\",\"region\":\"cn-shanghai\",\"endpoint\":\"https://nls-gateway-cn-shanghai.aliyuncs.com\",\"enabled\":false}",
                                "versionLabel", "v1",
                                "createdBy", "admin"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.configType").value("preprocessor"))
                .andExpect(jsonPath("$.data.configJson").value(org.hamcrest.Matchers.containsString("aliyun_asr")));

        mvc.perform(post("/api/v1/admin/ai-configs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(java.util.Map.of(
                                "configType", "preprocessor",
                                "configKey", "inline-preprocessor-secret",
                                "displayName", "Inline secret preprocessor",
                                "provider", "aliyun",
                                "status", "draft",
                                "configJson", "{\"service\":\"ocr\",\"preprocessor\":\"aliyun_ocr\",\"credentialRef\":\"access-key-inline\"}",
                                "versionLabel", "v1",
                                "createdBy", "admin"
                        ))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_ARGUMENT"));

        mvc.perform(post("/api/v1/admin/ai-configs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(java.util.Map.of(
                                "configType", "preprocessor",
                                "configKey", "wrong-preprocessor",
                                "displayName", "Wrong preprocessor",
                                "provider", "aliyun",
                                "status", "draft",
                                "configJson", "{\"service\":\"ocr\",\"preprocessor\":\"aliyun_asr\",\"credentialRef\":\"env:ALIYUN_ACCESS_KEY\"}",
                                "versionLabel", "v1",
                                "createdBy", "admin"
                        ))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_ARGUMENT"));

        mvc.perform(post("/api/v1/admin/ai-configs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(java.util.Map.of(
                                "configType", "preprocessor",
                                "configKey", "inline-preprocessor-api-key",
                                "displayName", "Inline API key preprocessor",
                                "provider", "aliyun",
                                "status", "draft",
                                "configJson", "{\"service\":\"ocr\",\"preprocessor\":\"aliyun_ocr\",\"credentialRef\":\"env:ALIYUN_ACCESS_KEY\",\"apiKey\":\"inline-secret\"}",
                                "versionLabel", "v1",
                                "createdBy", "admin"
                        ))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_ARGUMENT"));
    }

    @Test
    void phaseFiveAdminProviderConfigsRejectInlineSecretsAndInvalidPricing() throws Exception {
        mvc.perform(post("/api/v1/admin/ai-configs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(java.util.Map.of(
                                "configType", "provider",
                                "configKey", "deepseek-priced-provider",
                                "displayName", "DeepSeek priced provider",
                                "provider", "deepseek",
                                "status", "draft",
                                "configJson", "{\"model\":\"deepseek-chat\",\"credentialRef\":\"env:DEEPSEEK_API_KEY\",\"pricing\":{\"currency\":\"CNY\",\"promptPer1K\":\"0.002\",\"completionPer1K\":\"0.008\"}}",
                                "versionLabel", "v1",
                                "createdBy", "admin"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.configJson").value(org.hamcrest.Matchers.containsString("promptPer1K")));

        mvc.perform(post("/api/v1/admin/ai-configs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(java.util.Map.of(
                                "configType", "provider",
                                "configKey", "inline-key",
                                "displayName", "Inline key",
                                "provider", "deepseek",
                                "status", "draft",
                                "configJson", "{\"model\":\"deepseek-chat\",\"credentialRef\":\"sk-inline-secret\"}",
                                "versionLabel", "v1",
                                "createdBy", "admin"
                        ))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_ARGUMENT"));

        mvc.perform(post("/api/v1/admin/ai-configs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(java.util.Map.of(
                                "configType", "provider",
                                "configKey", "inline-secret-field",
                                "displayName", "Inline secret field",
                                "provider", "deepseek",
                                "status", "draft",
                                "configJson", "{\"model\":\"deepseek-chat\",\"credentialRef\":\"env:DEEPSEEK_API_KEY\",\"apiKey\":\"sk-inline-secret\"}",
                                "versionLabel", "v1",
                                "createdBy", "admin"
                        ))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_ARGUMENT"));

        mvc.perform(post("/api/v1/admin/ai-configs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(java.util.Map.of(
                                "configType", "provider",
                                "configKey", "negative-pricing",
                                "displayName", "Negative pricing",
                                "provider", "deepseek",
                                "status", "draft",
                                "configJson", "{\"model\":\"deepseek-chat\",\"credentialRef\":\"env:DEEPSEEK_API_KEY\",\"pricing\":{\"promptPer1K\":\"-0.01\"}}",
                                "versionLabel", "v1",
                                "createdBy", "admin"
                        ))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_ARGUMENT"));
    }

    @Test
    void phaseFiveAdminPromptAndSchemaConfigsValidateDraftSafetyContract() throws Exception {
        mvc.perform(post("/api/v1/admin/ai-configs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(java.util.Map.of(
                                "configType", "prompt",
                                "configKey", "safe-prompt",
                                "displayName", "Safe prompt",
                                "provider", "deepseek",
                                "status", "draft",
                                "configJson", "{\"purpose\":\"record_extraction\",\"locale\":\"zh-CN\",\"safetyPolicy\":\"draft_only\"}",
                                "versionLabel", "v1",
                                "createdBy", "admin"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.configType").value("prompt"));

        mvc.perform(post("/api/v1/admin/ai-configs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(java.util.Map.of(
                                "configType", "prompt",
                                "configKey", "unsafe-prompt",
                                "displayName", "Unsafe prompt",
                                "provider", "deepseek",
                                "status", "draft",
                                "configJson", "{\"purpose\":\"record_extraction\",\"safetyPolicy\":\"medical_advice\"}",
                                "versionLabel", "v1",
                                "createdBy", "admin"
                        ))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_ARGUMENT"));

        mvc.perform(post("/api/v1/admin/ai-configs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(java.util.Map.of(
                                "configType", "schema",
                                "configKey", "safe-schema",
                                "displayName", "Safe schema",
                                "provider", "deepseek",
                                "status", "draft",
                                "configJson", "{\"type\":\"object\",\"required\":[\"records\",\"todos\"],\"properties\":{\"records\":{\"type\":\"array\"},\"todos\":{\"type\":\"array\"},\"reports\":{\"type\":\"array\"}}}",
                                "versionLabel", "v1",
                                "createdBy", "admin"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.configType").value("schema"));

        mvc.perform(post("/api/v1/admin/ai-configs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(java.util.Map.of(
                                "configType", "schema",
                                "configKey", "unsafe-schema",
                                "displayName", "Unsafe schema",
                                "provider", "deepseek",
                                "status", "draft",
                                "configJson", "{\"type\":\"object\",\"required\":[\"diagnosis\"],\"properties\":{\"diagnosis\":{\"type\":\"string\"}}}",
                                "versionLabel", "v1",
                                "createdBy", "admin"
                        ))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_ARGUMENT"));

        mvc.perform(post("/api/v1/admin/ai-configs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(java.util.Map.of(
                                "configType", "schema",
                                "configKey", "broken-array-schema",
                                "displayName", "Broken array schema",
                                "provider", "deepseek",
                                "status", "draft",
                                "configJson", "{\"type\":\"object\",\"required\":[\"records\"],\"properties\":{\"records\":{\"type\":\"object\"}}}",
                                "versionLabel", "v1",
                                "createdBy", "admin"
                        ))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_ARGUMENT"));
    }

    @Test
    void phaseFiveAdminAiAuditLogsSupportOperationalFilters() throws Exception {
        createAiConfig("provider", "deepseek-audit-provider", "DeepSeek audit provider", "deepseek", "active", "{\"model\":\"deepseek-audit-model\",\"credentialRef\":\"env:DEEPSEEK_API_KEY\"}", "audit-v1");

        mvc.perform(post("/api/v1/ai/extract-record")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"text\":\"weight 56kg todo review\",\"inputType\":\"text\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.model").value("deepseek-audit-model"))
                .andExpect(jsonPath("$.data.fallbackUsed").value(true));

        mvc.perform(get("/api/v1/admin/ai-audit-logs?provider=deepseek&model=deepseek-audit-model&fallbackUsed=true&errorCode=MODEL_CLIENT_NOT_CONFIGURED&limit=5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(org.hamcrest.Matchers.greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.data[0].provider").value("deepseek"))
                .andExpect(jsonPath("$.data[0].model").value("deepseek-audit-model"))
                .andExpect(jsonPath("$.data[0].fallbackUsed").value(true))
                .andExpect(jsonPath("$.data[0].errorCode").value("MODEL_CLIENT_NOT_CONFIGURED"))
                .andExpect(jsonPath("$.data[0].inputPreview").exists())
                .andExpect(jsonPath("$.data[0].totalTokens").value(0))
                .andExpect(jsonPath("$.data[0].estimatedCost").value(0));
    }

    @Test
    void phaseFiveAiGatewayUsesActiveAdminConfigVersions() throws Exception {
        createAiConfig("provider", "deepseek-active-provider", "DeepSeek active provider", "deepseek", "active", "{\"model\":\"deepseek-reasoner\",\"credentialRef\":\"env:DEEPSEEK_API_KEY\"}", "v2");
        createAiConfig("prompt", "record-active-prompt", "Record active prompt", "deepseek", "active", "{\"purpose\":\"record_extraction\",\"safetyPolicy\":\"draft_only\"}", "prompt-v2");
        createAiConfig("schema", "record-active-schema", "Record active schema", "deepseek", "active", "{\"type\":\"object\",\"required\":[\"records\"]}", "schema-v2");

        mvc.perform(post("/api/v1/ai/extract-record")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"text\":\"weight 56kg todo review\",\"inputType\":\"text\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.provider").value("deepseek"))
                .andExpect(jsonPath("$.data.model").value("deepseek-reasoner"))
                .andExpect(jsonPath("$.data.providerConfigKey").value("deepseek-active-provider"))
                .andExpect(jsonPath("$.data.promptVersion").value("prompt-v2"))
                .andExpect(jsonPath("$.data.schemaVersion").value("schema-v2"))
                .andExpect(jsonPath("$.data.fallbackUsed").value(true))
                .andExpect(jsonPath("$.data.errorCode").value("MODEL_CLIENT_NOT_CONFIGURED"))
                .andExpect(jsonPath("$.data.needsUserConfirmation").value(true));

        mvc.perform(get("/api/v1/ai/audit-logs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].model").value("deepseek-reasoner"))
                .andExpect(jsonPath("$.data[0].fallbackUsed").value(true))
                .andExpect(jsonPath("$.data[0].errorCode").value("MODEL_CLIENT_NOT_CONFIGURED"));
    }

    @Test
    void phaseFiveDeepSeekEnabledWithoutApiKeyFallsBackToRuleClient() throws Exception {
        createAiConfig("provider", "deepseek-missing-key", "DeepSeek missing key", "deepseek", "active", "{\"model\":\"deepseek-chat\",\"credentialRef\":\"env:MISSING_DEEPSEEK_API_KEY\"}", "v1");

        mvc.perform(post("/api/v1/ai/extract-record")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"text\":\"weight 56kg todo review\",\"inputType\":\"text\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.providerConfigKey").value("deepseek-missing-key"))
                .andExpect(jsonPath("$.data.fallbackUsed").value(true))
                .andExpect(jsonPath("$.data.errorCode").value(org.hamcrest.Matchers.anyOf(
                        org.hamcrest.Matchers.is("MODEL_CLIENT_NOT_CONFIGURED"),
                        org.hamcrest.Matchers.is("DEEPSEEK_API_KEY_MISSING")
                )))
                .andExpect(jsonPath("$.data.records[0].recordType").value("weight"))
                .andExpect(jsonPath("$.data.todos.length()").value(1));
    }

    @Test
    void phaseThreeFamilyStageRecordsShareUnifiedRecordApi() throws Exception {
        String token = login("phase-three-family-records", "Mom");
        String familyId = createFamily(token, "Full cycle family");
        String pregnancyId = createPregnancy(token, familyId, "2026-04-13");
        String babyId = createBaby(token, familyId, "Baby A");

        createFamilyRecord(token, familyId, "fertility_cycle", "2026-02-01T08:00:00+08:00", "{\"cycleDay\":12}");
        createFamilyRecord(token, familyId, "fertility_supplement", "2026-02-01T09:00:00+08:00", "{\"name\":\"folic acid\",\"dose\":\"0.4mg\"}");
        createRecord(token, familyId, pregnancyId, "weight", "2026-05-25T08:00:00+08:00", "{\"weightKg\":56.2}");
        createFamilyRecord(token, familyId, "delivery_event", "2027-01-10T08:00:00+08:00", "{\"event\":\"admission\",\"note\":\"arrived\"}");
        createFamilyRecord(token, familyId, "postpartum_lochia", "2027-01-12T08:00:00+08:00", "{\"level\":\"light\"}");
        createFamilyRecord(token, familyId, "postpartum_medication", "2027-01-12T08:30:00+08:00", "{\"name\":\"ibuprofen\",\"dose\":\"200mg\"}");
        createBabyRecord(token, familyId, babyId, "baby_feeding", "2027-01-12T09:00:00+08:00", "{\"amountMl\":60}");
        createBabyRecord(token, familyId, babyId, "baby_feeding", "2027-01-12T10:00:00+08:00", "{\"amountMl\":70,\"side\":\"left\"}");
        createBabyRecord(token, familyId, babyId, "baby_sleep", "2027-01-12T11:00:00+08:00", "{\"durationMinutes\":45,\"startedAt\":\"2027-01-12T11:00:00+08:00\"}");

        mvc.perform(get("/api/v1/records?familyId=" + familyId + "&subjectType=family&subjectId=" + familyId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(5))
                .andExpect(jsonPath("$.data[0].recordType").value("postpartum_medication"));

        mvc.perform(get("/api/v1/records?familyId=" + familyId + "&subjectType=pregnancy&subjectId=" + pregnancyId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].recordType").value("weight"));

        mvc.perform(get("/api/v1/records?familyId=" + familyId + "&subjectType=baby&subjectId=" + babyId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(3))
                .andExpect(jsonPath("$.data[0].recordType").value("baby_sleep"));

        mvc.perform(post("/api/v1/records")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(java.util.Map.of(
                                "familyId", familyId,
                                "subjectType", "family",
                                "subjectId", "00000000-0000-0000-0000-000000000001",
                                "recordType", "fertility_cycle",
                                "occurredAt", "2027-01-12T10:00:00+08:00",
                                "payloadJson", "{\"cycleDay\":12}"
                        ))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("RESOURCE_NOT_FOUND"));

        mvc.perform(post("/api/v1/records")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(java.util.Map.of(
                                "familyId", familyId,
                                "subjectType", "family",
                                "subjectId", familyId,
                                "recordType", "baby_sleep",
                                "occurredAt", "2027-01-12T10:00:00+08:00",
                                "payloadJson", "{\"durationMinutes\":90}"
                        ))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_ARGUMENT"));
    }

    @Test
    void recordDetailUpdateAndDeleteRequireFamilyAccess() throws Exception {
        String login = mvc.perform(post("/api/v1/auth/wechat-login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"record-detail\",\"nickname\":\"Mom\"}"))
                .andReturn()
                .getResponse()
                .getContentAsString();
        String token = objectMapper.readTree(login).path("data").path("token").asText();

        String familyResponse = mvc.perform(post("/api/v1/families")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Detail family\"}"))
                .andReturn()
                .getResponse()
                .getContentAsString();
        String familyId = objectMapper.readTree(familyResponse).path("data").path("id").asText();

        String pregnancyResponse = mvc.perform(post("/api/v1/profiles/pregnancies")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"familyId\":\"" + familyId + "\",\"lmpDate\":\"2026-04-13\",\"fetusCount\":1}"))
                .andReturn()
                .getResponse()
                .getContentAsString();
        String pregnancyId = objectMapper.readTree(pregnancyResponse).path("data").path("id").asText();

        String recordResponse = mvc.perform(post("/api/v1/records")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"familyId\":\"" + familyId + "\",\"subjectType\":\"pregnancy\",\"subjectId\":\"" + pregnancyId + "\",\"recordType\":\"weight\",\"occurredAt\":\"2026-05-25T08:00:00+08:00\",\"payloadJson\":\"{\\\"weightKg\\\":56.2}\"}"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        String recordId = objectMapper.readTree(recordResponse).path("data").path("id").asText();

        mvc.perform(get("/api/v1/records/" + recordId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(recordId));

        mvc.perform(post("/api/v1/records/" + recordId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"occurredAt\":\"2026-05-26T08:00:00+08:00\",\"payloadJson\":\"{\\\"weightKg\\\":57.1}\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.payloadJson").value("{\"weightKg\":57.1}"));

        mvc.perform(delete("/api/v1/records/" + recordId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.deleted").value(true));

        mvc.perform(get("/api/v1/records/" + recordId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    @Test
    void reportDetailUpdateAndDeleteRequireFamilyAccess() throws Exception {
        String login = mvc.perform(post("/api/v1/auth/wechat-login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"report-detail\",\"nickname\":\"Mom\"}"))
                .andReturn()
                .getResponse()
                .getContentAsString();
        String token = objectMapper.readTree(login).path("data").path("token").asText();

        String familyResponse = mvc.perform(post("/api/v1/families")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Report detail family\"}"))
                .andReturn()
                .getResponse()
                .getContentAsString();
        String familyId = objectMapper.readTree(familyResponse).path("data").path("id").asText();

        String pregnancyResponse = mvc.perform(post("/api/v1/profiles/pregnancies")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"familyId\":\"" + familyId + "\",\"lmpDate\":\"2026-04-13\",\"fetusCount\":1}"))
                .andReturn()
                .getResponse()
                .getContentAsString();
        String pregnancyId = objectMapper.readTree(pregnancyResponse).path("data").path("id").asText();

        String reportResponse = mvc.perform(post("/api/v1/reports")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"familyId\":\"" + familyId + "\",\"subjectType\":\"pregnancy\",\"subjectId\":\"" + pregnancyId + "\",\"reportType\":\"blood\",\"title\":\"Blood test\",\"examinedAt\":\"2026-05-25\",\"indicatorsJson\":\"{\\\"indicators\\\":[{\\\"code\\\":\\\"hcg\\\",\\\"value\\\":1000}]}\"}"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        String reportId = objectMapper.readTree(reportResponse).path("data").path("id").asText();

        mvc.perform(get("/api/v1/reports/" + reportId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(reportId));

        mvc.perform(post("/api/v1/reports/" + reportId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Updated blood test\",\"examinedAt\":\"2026-05-26\",\"indicatorsJson\":\"{\\\"indicators\\\":[{\\\"code\\\":\\\"hcg\\\",\\\"value\\\":1200}]}\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("Updated blood test"));

        mvc.perform(delete("/api/v1/reports/" + reportId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.deleted").value(true));

        mvc.perform(get("/api/v1/reports/" + reportId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    private String login(String code, String nickname) throws Exception {
        String login = mvc.perform(post("/api/v1/auth/wechat-login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"" + code + "\",\"nickname\":\"" + nickname + "\"}"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(login).path("data").path("token").asText();
    }

    private String createFamily(String token, String name) throws Exception {
        String familyResponse = mvc.perform(post("/api/v1/families")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"" + name + "\"}"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(familyResponse).path("data").path("id").asText();
    }

    private String createPregnancy(String token, String familyId, String lmpDate) throws Exception {
        String pregnancyResponse = mvc.perform(post("/api/v1/profiles/pregnancies")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"familyId\":\"" + familyId + "\",\"lmpDate\":\"" + lmpDate + "\",\"fetusCount\":1}"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(pregnancyResponse).path("data").path("id").asText();
    }

    private void createRecord(String token, String familyId, String pregnancyId, String recordType, String occurredAt, String payloadJson) throws Exception {
        createRecordAndReturnId(token, familyId, pregnancyId, recordType, occurredAt, payloadJson);
    }

    private String createRecordAndReturnId(String token, String familyId, String pregnancyId, String recordType, String occurredAt, String payloadJson) throws Exception {
        String response = mvc.perform(post("/api/v1/records")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(java.util.Map.of(
                                "familyId", familyId,
                                "subjectType", "pregnancy",
                                "subjectId", pregnancyId,
                                "recordType", recordType,
                                "occurredAt", occurredAt,
                                "payloadJson", payloadJson
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.recordType").value(recordType))
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(response).path("data").path("id").asText();
    }

    private void createReport(String token, String familyId, String pregnancyId, String reportType, String title, String examinedAt, String indicatorsJson) throws Exception {
        createReportAndReturnId(token, familyId, pregnancyId, reportType, title, examinedAt, indicatorsJson);
    }

    private void createFamilyRecord(String token, String familyId, String recordType, String occurredAt, String payloadJson) throws Exception {
        mvc.perform(post("/api/v1/records")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(java.util.Map.of(
                                "familyId", familyId,
                                "subjectType", "family",
                                "subjectId", familyId,
                                "recordType", recordType,
                                "occurredAt", occurredAt,
                                "payloadJson", payloadJson
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.recordType").value(recordType));
    }

    private String createBaby(String token, String familyId, String name) throws Exception {
        String response = mvc.perform(post("/api/v1/profiles/babies")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(java.util.Map.of(
                                "familyId", familyId,
                                "name", name,
                                "gender", "unknown"
                        ))))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(response).path("data").path("id").asText();
    }

    private void createBabyRecord(String token, String familyId, String babyId, String recordType, String occurredAt, String payloadJson) throws Exception {
        mvc.perform(post("/api/v1/records")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(java.util.Map.of(
                                "familyId", familyId,
                                "subjectType", "baby",
                                "subjectId", babyId,
                                "recordType", recordType,
                                "occurredAt", occurredAt,
                                "payloadJson", payloadJson
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.recordType").value(recordType));
    }

    private String createReportAndReturnId(String token, String familyId, String pregnancyId, String reportType, String title, String examinedAt, String indicatorsJson) throws Exception {
        String response = mvc.perform(post("/api/v1/reports")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(java.util.Map.of(
                                "familyId", familyId,
                                "subjectType", "pregnancy",
                                "subjectId", pregnancyId,
                                "reportType", reportType,
                                "title", title,
                                "examinedAt", examinedAt,
                                "indicatorsJson", indicatorsJson
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.reportType").value(reportType))
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(response).path("data").path("id").asText();
    }

    private String createReminderAndReturnId(String token, String familyId, String title, String scene, String subjectType, String subjectId, String triggerAt) throws Exception {
        String response = mvc.perform(post("/api/v1/reminders")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(java.util.Map.of(
                                "familyId", familyId,
                                "title", title,
                                "scene", scene,
                                "subjectType", subjectType,
                                "subjectId", subjectId,
                                "triggerAt", triggerAt
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.scene").value(scene))
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(response).path("data").path("id").asText();
    }

    private void createAiConfig(String configType, String configKey, String displayName, String provider, String status, String configJson) throws Exception {
        createAiConfig(configType, configKey, displayName, provider, status, configJson, "v1");
    }

    private void createAiConfig(String configType, String configKey, String displayName, String provider, String status, String configJson, String versionLabel) throws Exception {
        mvc.perform(post("/api/v1/admin/ai-configs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(java.util.Map.of(
                                "configType", configType,
                                "configKey", configKey,
                                "displayName", displayName,
                                "provider", provider,
                                "status", status,
                                "configJson", configJson,
                                "versionLabel", versionLabel,
                                "createdBy", "admin"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.configType").value(configType))
                .andExpect(jsonPath("$.data.configKey").value(configKey));
    }

}
