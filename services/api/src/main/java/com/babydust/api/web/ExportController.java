package com.babydust.api.web;

import com.babydust.api.common.ApiResponse;
import com.babydust.api.domain.JsonRecord;
import com.babydust.api.domain.MedicalReport;
import com.babydust.api.repository.JsonRecordRepository;
import com.babydust.api.repository.MedicalReportRepository;
import com.babydust.api.security.CurrentUser;
import com.babydust.api.service.PermissionService;
import jakarta.servlet.http.HttpServletRequest;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/exports")
public class ExportController {
    private final JsonRecordRepository records;
    private final MedicalReportRepository reports;
    private final PermissionService permissions;
    private final CurrentUser currentUser;

    public ExportController(JsonRecordRepository records, MedicalReportRepository reports, PermissionService permissions, CurrentUser currentUser) {
        this.records = records;
        this.reports = reports;
        this.permissions = permissions;
        this.currentUser = currentUser;
    }

    @GetMapping("/pregnancy-records")
    public ApiResponse<ExportPackage<RecordExportRow>> pregnancyRecords(@RequestParam UUID familyId, HttpServletRequest request) {
        permissions.requireFamilyMember(familyId, currentUser.id(request));
        List<RecordExportRow> rows = records.findTop500ByFamilyIdOrderByOccurredAtAsc(familyId).stream()
                .filter(record -> "pregnancy".equals(record.getSubjectType()))
                .map(RecordExportRow::from)
                .toList();
        return ApiResponse.ok(new ExportPackage<>("pregnancy_records", "csv", OffsetDateTime.now(), rows.size(), rows, recordRowsToCsv(rows)));
    }

    @GetMapping("/reports")
    public ApiResponse<ExportPackage<ReportExportRow>> reports(@RequestParam UUID familyId, HttpServletRequest request) {
        permissions.requireFamilyMember(familyId, currentUser.id(request));
        List<ReportExportRow> rows = reports.findTop200ByFamilyIdOrderByExaminedAtAsc(familyId).stream()
                .map(ReportExportRow::from)
                .toList();
        return ApiResponse.ok(new ExportPackage<>("medical_reports", "csv", OffsetDateTime.now(), rows.size(), rows, reportRowsToCsv(rows)));
    }

    private String recordRowsToCsv(List<RecordExportRow> rows) {
        StringBuilder csv = new StringBuilder("id,subjectType,subjectId,recordType,occurredAt,payloadJson,privacyLevel\n");
        rows.forEach(row -> csv.append(csvRow(
                row.id(),
                row.subjectType(),
                row.subjectId(),
                row.recordType(),
                row.occurredAt(),
                row.payloadJson(),
                row.privacyLevel()
        )));
        return csv.toString();
    }

    private String reportRowsToCsv(List<ReportExportRow> rows) {
        StringBuilder csv = new StringBuilder("id,subjectType,subjectId,reportType,title,examinedAt,indicatorsJson\n");
        rows.forEach(row -> csv.append(csvRow(
                row.id(),
                row.subjectType(),
                row.subjectId(),
                row.reportType(),
                row.title(),
                row.examinedAt(),
                row.indicatorsJson()
        )));
        return csv.toString();
    }

    private String csvRow(Object... values) {
        return java.util.Arrays.stream(values)
                .map(this::csvCell)
                .collect(java.util.stream.Collectors.joining(",")) + "\n";
    }

    private String csvCell(Object value) {
        String text = value == null ? "" : String.valueOf(value);
        return "\"" + text.replace("\"", "\"\"") + "\"";
    }

    public record ExportPackage<T>(String exportType, String format, OffsetDateTime generatedAt, int rowCount, List<T> rows, String csvContent) {
    }

    public record RecordExportRow(UUID id, String subjectType, UUID subjectId, String recordType, OffsetDateTime occurredAt, String payloadJson, String privacyLevel) {
        static RecordExportRow from(JsonRecord record) {
            return new RecordExportRow(
                    record.getId(),
                    record.getSubjectType(),
                    record.getSubjectId(),
                    record.getRecordType(),
                    record.getOccurredAt(),
                    record.getPayloadJson(),
                    record.getPrivacyLevel()
            );
        }
    }

    public record ReportExportRow(UUID id, String subjectType, UUID subjectId, String reportType, String title, java.time.LocalDate examinedAt, String indicatorsJson) {
        static ReportExportRow from(MedicalReport report) {
            return new ReportExportRow(
                    report.getId(),
                    report.getSubjectType(),
                    report.getSubjectId(),
                    report.getReportType(),
                    report.getTitle(),
                    report.getExaminedAt(),
                    report.getIndicatorsJson()
            );
        }
    }
}
