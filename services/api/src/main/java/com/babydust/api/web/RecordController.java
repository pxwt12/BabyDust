package com.babydust.api.web;

import com.babydust.api.common.ApiResponse;
import com.babydust.api.common.ResourceNotFoundException;
import com.babydust.api.domain.JsonRecord;
import com.babydust.api.repository.JsonRecordRepository;
import com.babydust.api.security.CurrentUser;
import com.babydust.api.service.PermissionService;
import com.babydust.api.service.RecordTypeCatalog;
import com.babydust.api.service.SubjectAccessService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/records")
public class RecordController {
    private final JsonRecordRepository records;
    private final PermissionService permissions;
    private final CurrentUser currentUser;
    private final RecordTypeCatalog recordTypes;
    private final SubjectAccessService subjects;

    public RecordController(JsonRecordRepository records, PermissionService permissions, CurrentUser currentUser, RecordTypeCatalog recordTypes, SubjectAccessService subjects) {
        this.records = records;
        this.permissions = permissions;
        this.currentUser = currentUser;
        this.recordTypes = recordTypes;
        this.subjects = subjects;
    }

    @GetMapping
    public ApiResponse<List<JsonRecord>> list(
            @RequestParam UUID familyId,
            @RequestParam(required = false) String subjectType,
            @RequestParam(required = false) UUID subjectId,
            @RequestParam(required = false) String recordType,
            @RequestParam(required = false) LocalDate fromDate,
            @RequestParam(required = false) LocalDate toDate,
            HttpServletRequest request
    ) {
        permissions.requireFamilyMember(familyId, currentUser.id(request));
        if (subjectType != null || subjectId != null) {
            subjects.requireSubjectInFamily(subjectType, subjectId, familyId);
        }
        Specification<JsonRecord> spec = (root, query, criteria) -> criteria.equal(root.get("familyId"), familyId);
        if (subjectType != null) {
            spec = spec.and((root, query, criteria) -> criteria.equal(root.get("subjectType"), subjectType));
        }
        if (subjectId != null) {
            spec = spec.and((root, query, criteria) -> criteria.equal(root.get("subjectId"), subjectId));
        }
        if (recordType != null && !recordType.isBlank()) {
            recordTypes.requireDefinition(recordType);
            spec = spec.and((root, query, criteria) -> criteria.equal(root.get("recordType"), recordType));
        }
        if (fromDate != null) {
            OffsetDateTime from = fromDate.atStartOfDay(ZoneId.systemDefault()).toOffsetDateTime();
            spec = spec.and((root, query, criteria) -> criteria.greaterThanOrEqualTo(root.get("occurredAt"), from));
        }
        if (toDate != null) {
            OffsetDateTime to = toDate.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toOffsetDateTime();
            spec = spec.and((root, query, criteria) -> criteria.lessThan(root.get("occurredAt"), to));
        }
        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(
                0,
                100,
                org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "occurredAt")
        );
        return ApiResponse.ok(records.findAll(spec, pageable).getContent());
    }

    @GetMapping("/types")
    public ApiResponse<List<RecordTypeCatalog.RecordTypeDefinition>> types() {
        return ApiResponse.ok(recordTypes.definitions());
    }

    @GetMapping("/{recordId}")
    public ApiResponse<JsonRecord> detail(@PathVariable UUID recordId, HttpServletRequest request) {
        JsonRecord record = records.findById(recordId).orElseThrow(() -> new ResourceNotFoundException("Record not found"));
        permissions.requireFamilyMember(record.getFamilyId(), currentUser.id(request));
        return ApiResponse.ok(record);
    }

    @PostMapping
    public ApiResponse<JsonRecord> create(@Valid @RequestBody CreateRecordRequest body, HttpServletRequest request) {
        permissions.requireFamilyMember(body.familyId(), currentUser.id(request));
        RecordTypeCatalog.RecordTypeDefinition definition = recordTypes.requireDefinition(body.recordType());
        if (!definition.subjectType().equals(body.subjectType())) {
            throw new IllegalArgumentException("Record type does not support subject type: " + body.subjectType());
        }
        subjects.requireSubjectInFamily(body.subjectType(), body.subjectId(), body.familyId());
        recordTypes.validatePayload(body.recordType(), body.payloadJson());
        JsonRecord record = new JsonRecord(body.familyId(), body.subjectType(), body.subjectId(), body.recordType(), body.occurredAt(), body.payloadJson());
        return ApiResponse.ok(records.save(record));
    }

    @PostMapping("/{recordId}")
    public ApiResponse<JsonRecord> update(@PathVariable UUID recordId, @Valid @RequestBody UpdateRecordRequest body, HttpServletRequest request) {
        JsonRecord record = records.findById(recordId).orElseThrow(() -> new ResourceNotFoundException("Record not found"));
        permissions.requireFamilyMember(record.getFamilyId(), currentUser.id(request));
        recordTypes.validatePayload(record.getRecordType(), body.payloadJson());
        record.update(body.occurredAt(), body.payloadJson());
        return ApiResponse.ok(records.save(record));
    }

    @DeleteMapping("/{recordId}")
    public ApiResponse<java.util.Map<String, Object>> delete(@PathVariable UUID recordId, HttpServletRequest request) {
        JsonRecord record = records.findById(recordId).orElseThrow(() -> new ResourceNotFoundException("Record not found"));
        permissions.requireFamilyMember(record.getFamilyId(), currentUser.id(request));
        records.delete(record);
        return ApiResponse.ok(java.util.Map.of("deleted", true, "id", recordId));
    }

    public record CreateRecordRequest(
            @NotNull UUID familyId,
            @NotBlank String subjectType,
            @NotNull UUID subjectId,
            @NotBlank String recordType,
            @NotNull OffsetDateTime occurredAt,
            @NotBlank String payloadJson
    ) {
    }

    public record UpdateRecordRequest(
            @NotNull OffsetDateTime occurredAt,
            @NotBlank String payloadJson
    ) {
    }
}
