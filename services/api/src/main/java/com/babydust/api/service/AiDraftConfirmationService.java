package com.babydust.api.service;

import com.babydust.api.domain.AiDraftConfirmation;
import com.babydust.api.domain.JsonRecord;
import com.babydust.api.domain.MedicalReport;
import com.babydust.api.domain.TodoItem;
import com.babydust.api.repository.AiDraftConfirmationRepository;
import com.babydust.api.repository.JsonRecordRepository;
import com.babydust.api.repository.MedicalReportRepository;
import com.babydust.api.repository.TodoItemRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AiDraftConfirmationService {
    private final AiDraftConfirmationRepository confirmations;
    private final JsonRecordRepository records;
    private final MedicalReportRepository reports;
    private final TodoItemRepository todos;
    private final RecordTypeCatalog recordTypes;
    private final ReportIndicatorCatalog indicators;
    private final SubjectAccessService subjects;
    private final ObjectMapper objectMapper;

    public AiDraftConfirmationService(
            AiDraftConfirmationRepository confirmations,
            JsonRecordRepository records,
            MedicalReportRepository reports,
            TodoItemRepository todos,
            RecordTypeCatalog recordTypes,
            ReportIndicatorCatalog indicators,
            SubjectAccessService subjects,
            ObjectMapper objectMapper
    ) {
        this.confirmations = confirmations;
        this.records = records;
        this.reports = reports;
        this.todos = todos;
        this.recordTypes = recordTypes;
        this.indicators = indicators;
        this.subjects = subjects;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public AiDraftConfirmationResult confirm(AiDraftConfirmationRequest request) {
        if (request.draft() == null || request.draft().isBlank()) {
            throw new IllegalArgumentException("AI draft is required");
        }
        subjects.requireSubjectInFamily(request.subjectType(), request.subjectId(), request.familyId());
        JsonNode draft = parseDraft(request.draft());
        if (draft.path("blocked").asBoolean(false)) {
            throw new IllegalArgumentException("Blocked AI drafts cannot be confirmed");
        }
        if (!draft.path("needsUserConfirmation").asBoolean(false)) {
            throw new IllegalArgumentException("AI draft must require user confirmation");
        }
        if (!"ai_draft".equals(draft.path("source").asText(""))) {
            throw new IllegalArgumentException("Only AI draft sources can be confirmed");
        }
        OffsetDateTime confirmedAt = OffsetDateTime.now();
        List<JsonRecord> createdRecords = createRecords(request, draft.path("records"), confirmedAt);
        List<MedicalReport> createdReports = createReports(request, draft.path("reports"), confirmedAt.toLocalDate());
        List<TodoItem> createdTodos = createTodos(request, draft.path("todos"), confirmedAt);
        AiDraftConfirmation confirmation = confirmations.save(new AiDraftConfirmation(
                request.familyId(),
                request.userId(),
                request.subjectType(),
                request.subjectId(),
                textOrDefault(draft.path("provider"), "unknown"),
                textOrDefault(draft.path("model"), "unknown"),
                textOrDefault(draft.path("purpose"), "unknown"),
                preview(request.draft()),
                idsJson(createdRecords.stream().map(JsonRecord::getId).toList()),
                idsJson(createdReports.stream().map(MedicalReport::getId).toList()),
                idsJson(createdTodos.stream().map(TodoItem::getId).toList()),
                confirmedAt
        ));
        return new AiDraftConfirmationResult("confirmed", confirmation.getId(), createdRecords, createdReports, createdTodos, confirmedAt);
    }

    private JsonNode parseDraft(String draftJson) {
        try {
            JsonNode draft = objectMapper.readTree(draftJson);
            if (!draft.isObject()) {
                throw new IllegalArgumentException("AI draft must be a JSON object");
            }
            return draft;
        } catch (IllegalArgumentException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new IllegalArgumentException("AI draft must be valid JSON");
        }
    }

    private List<JsonRecord> createRecords(AiDraftConfirmationRequest request, JsonNode draftRecords, OffsetDateTime occurredAt) {
        if (draftRecords.isMissingNode() || draftRecords.isNull()) {
            return List.of();
        }
        if (!draftRecords.isArray()) {
            throw new IllegalArgumentException("AI draft records must be an array");
        }
        List<JsonRecord> created = new ArrayList<>();
        for (JsonNode draftRecord : draftRecords) {
            if (!draftRecord.isObject()) {
                throw new IllegalArgumentException("AI draft record items must be objects");
            }
            String recordType = requireText(draftRecord, "recordType");
            RecordTypeCatalog.RecordTypeDefinition definition = recordTypes.requireDefinition(recordType);
            if (!definition.subjectType().equals(request.subjectType())) {
                throw new IllegalArgumentException("Record type does not support subject type: " + request.subjectType());
            }
            String payloadJson = objectToJson(draftRecord.path("payload"), "AI draft record payload must be an object");
            recordTypes.validatePayload(recordType, payloadJson);
            created.add(records.save(new JsonRecord(
                    request.familyId(),
                    request.subjectType(),
                    request.subjectId(),
                    recordType,
                    occurredAt,
                    payloadJson
            )));
        }
        return created;
    }

    private List<MedicalReport> createReports(AiDraftConfirmationRequest request, JsonNode draftReports, LocalDate examinedAt) {
        if (draftReports.isMissingNode() || draftReports.isNull()) {
            return List.of();
        }
        if (!draftReports.isArray()) {
            throw new IllegalArgumentException("AI draft reports must be an array");
        }
        List<MedicalReport> created = new ArrayList<>();
        for (JsonNode draftReport : draftReports) {
            if (!draftReport.isObject()) {
                throw new IllegalArgumentException("AI draft report items must be objects");
            }
            String indicatorsJson = indicatorsJson(draftReport.path("indicators"));
            indicators.validateIndicators(indicatorsJson);
            created.add(reports.save(new MedicalReport(
                    request.familyId(),
                    request.subjectType(),
                    request.subjectId(),
                    requireText(draftReport, "reportType"),
                    textOrDefault(draftReport.path("title"), "AI report"),
                    examinedAt,
                    indicatorsJson
            )));
        }
        return created;
    }

    private List<TodoItem> createTodos(AiDraftConfirmationRequest request, JsonNode draftTodos, OffsetDateTime confirmedAt) {
        if (draftTodos.isMissingNode() || draftTodos.isNull()) {
            return List.of();
        }
        if (!draftTodos.isArray()) {
            throw new IllegalArgumentException("AI draft todos must be an array");
        }
        List<TodoItem> created = new ArrayList<>();
        for (JsonNode draftTodo : draftTodos) {
            if (!draftTodo.isObject()) {
                throw new IllegalArgumentException("AI draft todo items must be objects");
            }
            created.add(todos.save(new TodoItem(
                    request.familyId(),
                    requireText(draftTodo, "title"),
                    textOrDefault(draftTodo.path("category"), "custom"),
                    request.subjectType(),
                    request.subjectId(),
                    confirmedAt
            )));
        }
        return created;
    }

    private String indicatorsJson(JsonNode indicatorsNode) {
        JsonNode indicatorsArray = indicatorsNode.isArray() ? indicatorsNode : objectMapper.createArrayNode();
        return objectToJson(objectMapper.createObjectNode().set("indicators", indicatorsArray), "Report indicators must be a JSON object");
    }

    private String objectToJson(JsonNode node, String errorMessage) {
        if (!node.isObject()) {
            throw new IllegalArgumentException(errorMessage);
        }
        try {
            return objectMapper.writeValueAsString(node);
        } catch (Exception ex) {
            throw new IllegalArgumentException(errorMessage);
        }
    }

    private String requireText(JsonNode node, String field) {
        JsonNode value = node.path(field);
        if (!value.isTextual() || value.asText().isBlank()) {
            throw new IllegalArgumentException("AI draft field is required: " + field);
        }
        return value.asText().trim();
    }

    private String textOrDefault(JsonNode node, String defaultValue) {
        return node.isTextual() && !node.asText().isBlank() ? node.asText().trim() : defaultValue;
    }

    private String preview(String draftJson) {
        String compact = draftJson.replaceAll("\\s+", " ");
        return compact.length() > 240 ? compact.substring(0, 240) : compact;
    }

    private String idsJson(List<UUID> ids) {
        try {
            return objectMapper.writeValueAsString(ids);
        } catch (Exception ex) {
            throw new IllegalArgumentException("AI confirmation ids must be serializable");
        }
    }

    public record AiDraftConfirmationRequest(
            UUID familyId,
            UUID userId,
            String subjectType,
            UUID subjectId,
            String draft
    ) {
    }

    public record AiDraftConfirmationResult(
            String status,
            UUID confirmationId,
            List<JsonRecord> records,
            List<MedicalReport> reports,
            List<TodoItem> todos,
            OffsetDateTime confirmedAt
    ) {
    }
}
