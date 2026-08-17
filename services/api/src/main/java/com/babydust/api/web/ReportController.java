package com.babydust.api.web;

import com.babydust.api.common.ApiResponse;
import com.babydust.api.common.ResourceNotFoundException;
import com.babydust.api.domain.MedicalReport;
import com.babydust.api.repository.MedicalReportRepository;
import com.babydust.api.security.CurrentUser;
import com.babydust.api.service.PermissionService;
import com.babydust.api.service.ReportIndicatorCatalog;
import com.babydust.api.service.SubjectAccessService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/reports")
public class ReportController {
    private final MedicalReportRepository reports;
    private final PermissionService permissions;
    private final CurrentUser currentUser;
    private final SubjectAccessService subjects;
    private final ReportIndicatorCatalog indicators;

    public ReportController(MedicalReportRepository reports, PermissionService permissions, CurrentUser currentUser, SubjectAccessService subjects, ReportIndicatorCatalog indicators) {
        this.reports = reports;
        this.permissions = permissions;
        this.currentUser = currentUser;
        this.subjects = subjects;
        this.indicators = indicators;
    }

    @GetMapping
    public ApiResponse<List<MedicalReport>> list(@RequestParam UUID familyId, HttpServletRequest request) {
        permissions.requireFamilyMember(familyId, currentUser.id(request));
        return ApiResponse.ok(reports.findTop20ByFamilyIdOrderByExaminedAtDesc(familyId));
    }

    @GetMapping("/{reportId}")
    public ApiResponse<MedicalReport> detail(@PathVariable UUID reportId, HttpServletRequest request) {
        MedicalReport report = reports.findById(reportId).orElseThrow(() -> new ResourceNotFoundException("Report not found"));
        permissions.requireFamilyMember(report.getFamilyId(), currentUser.id(request));
        return ApiResponse.ok(report);
    }

    @PostMapping
    public ApiResponse<MedicalReport> create(@Valid @RequestBody CreateReportRequest body, HttpServletRequest request) {
        permissions.requireFamilyMember(body.familyId(), currentUser.id(request));
        subjects.requireSubjectInFamily(body.subjectType(), body.subjectId(), body.familyId());
        indicators.validateIndicators(body.indicatorsJson());
        MedicalReport report = new MedicalReport(
                body.familyId(),
                body.subjectType(),
                body.subjectId(),
                body.reportType(),
                body.title(),
                body.examinedAt(),
                body.indicatorsJson()
        );
        return ApiResponse.ok(reports.save(report));
    }

    @PostMapping("/{reportId}")
    public ApiResponse<MedicalReport> update(@PathVariable UUID reportId, @Valid @RequestBody UpdateReportRequest body, HttpServletRequest request) {
        MedicalReport report = reports.findById(reportId).orElseThrow(() -> new ResourceNotFoundException("Report not found"));
        permissions.requireFamilyMember(report.getFamilyId(), currentUser.id(request));
        indicators.validateIndicators(body.indicatorsJson());
        report.update(body.title(), body.examinedAt(), body.indicatorsJson());
        return ApiResponse.ok(reports.save(report));
    }

    @DeleteMapping("/{reportId}")
    public ApiResponse<java.util.Map<String, Object>> delete(@PathVariable UUID reportId, HttpServletRequest request) {
        MedicalReport report = reports.findById(reportId).orElseThrow(() -> new ResourceNotFoundException("Report not found"));
        permissions.requireFamilyMember(report.getFamilyId(), currentUser.id(request));
        reports.delete(report);
        return ApiResponse.ok(java.util.Map.of("deleted", true, "id", reportId));
    }

    @GetMapping("/indicator-definitions")
    public ApiResponse<List<ReportIndicatorCatalog.IndicatorDefinition>> indicatorDefinitions() {
        return ApiResponse.ok(indicators.definitions());
    }

    public record CreateReportRequest(
            @NotNull UUID familyId,
            @NotBlank String subjectType,
            @NotNull UUID subjectId,
            @NotBlank String reportType,
            @NotBlank String title,
            @NotNull LocalDate examinedAt,
            @NotBlank String indicatorsJson
    ) {
    }

    public record UpdateReportRequest(
            @NotBlank String title,
            @NotNull LocalDate examinedAt,
            @NotBlank String indicatorsJson
    ) {
    }
}
