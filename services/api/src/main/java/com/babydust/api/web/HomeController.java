package com.babydust.api.web;

import com.babydust.api.common.ApiResponse;
import com.babydust.api.domain.JsonRecord;
import com.babydust.api.domain.MedicalReport;
import com.babydust.api.domain.PregnancyProfile;
import com.babydust.api.domain.TodoItem;
import com.babydust.api.repository.JsonRecordRepository;
import com.babydust.api.repository.MedicalReportRepository;
import com.babydust.api.repository.PregnancyProfileRepository;
import com.babydust.api.repository.TodoItemRepository;
import com.babydust.api.security.CurrentUser;
import com.babydust.api.service.AnalyticsService;
import com.babydust.api.service.PermissionService;
import com.babydust.api.service.PregnancyService;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Clock;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/home")
public class HomeController {
    private final PregnancyProfileRepository pregnancies;
    private final JsonRecordRepository records;
    private final MedicalReportRepository reports;
    private final TodoItemRepository todos;
    private final PermissionService permissions;
    private final CurrentUser currentUser;
    private final PregnancyService pregnancyService;
    private final AnalyticsService analytics;
    private final Clock clock;

    public HomeController(PregnancyProfileRepository pregnancies, JsonRecordRepository records, MedicalReportRepository reports, TodoItemRepository todos, PermissionService permissions, CurrentUser currentUser, PregnancyService pregnancyService, AnalyticsService analytics, Clock clock) {
        this.pregnancies = pregnancies;
        this.records = records;
        this.reports = reports;
        this.todos = todos;
        this.permissions = permissions;
        this.currentUser = currentUser;
        this.pregnancyService = pregnancyService;
        this.analytics = analytics;
        this.clock = clock;
    }

    @GetMapping("/summary")
    public ApiResponse<HomeSummaryResponse> summary(@RequestParam UUID familyId, HttpServletRequest request) {
        permissions.requireFamilyMember(familyId, currentUser.id(request));
        PregnancyProfile pregnancy = pregnancies.findByFamilyId(familyId).stream().findFirst().orElse(null);
        PregnancySummary pregnancySummary = null;
        if (pregnancy != null) {
            PregnancyService.GestationalAge age = pregnancyService.gestationalAge(pregnancy);
            long daysUntilDue = ChronoUnit.DAYS.between(LocalDate.now(clock), pregnancy.getDueDate());
            pregnancySummary = new PregnancySummary(
                    pregnancy.getId(),
                    pregnancy.getLmpDate(),
                    pregnancy.getDueDate(),
                    pregnancy.getFetusCount(),
                    pregnancy.getStatus(),
                    age.weeks(),
                    age.days(),
                    age.display(),
                    age.pregnancyDay(),
                    age.trimester(),
                    daysUntilDue
            );
        }
        List<TodoItem> upcomingTodos = todos.findTop50ByFamilyIdOrderByDueAtAsc(familyId).stream()
                .filter(todo -> !"done".equals(todo.getStatus()) && !"cancelled".equals(todo.getStatus()))
                .limit(8)
                .toList();
        List<TodoItem> prenatalTodos = todos.findTop50ByFamilyIdOrderByDueAtAsc(familyId).stream()
                .filter(todo -> "prenatal_checkup".equals(todo.getCategory()) || "delivery_prepare".equals(todo.getCategory()))
                .toList();
        long completedPrenatalTodos = prenatalTodos.stream().filter(todo -> "done".equals(todo.getStatus())).count();
        int prenatalCompletionRate = prenatalTodos.isEmpty() ? 0 : (int) Math.round(completedPrenatalTodos * 100.0 / prenatalTodos.size());
        HomeSummaryResponse response = new HomeSummaryResponse(
                pregnancy == null ? "not_set" : "pregnancy",
                pregnancySummary,
                records.findTop50ByFamilyIdOrderByOccurredAtDesc(familyId).stream().limit(8).map(RecordSummary::from).toList(),
                reports.findTop20ByFamilyIdOrderByExaminedAtDesc(familyId).stream().limit(5).map(ReportSummary::from).toList(),
                upcomingTodos.stream().map(TodoSummary::from).toList(),
                new PrenatalPlanProgress(prenatalTodos.size(), (int) completedPrenatalTodos, prenatalCompletionRate),
                List.of(
                        analytics.series(familyId, "weight"),
                        analytics.series(familyId, "blood_pressure_systolic"),
                        analytics.series(familyId, "blood_pressure_diastolic")
                ),
                OffsetDateTime.now()
        );
        return ApiResponse.ok(response);
    }

    public record HomeSummaryResponse(
            String stage,
            PregnancySummary pregnancy,
            List<RecordSummary> recentRecords,
            List<ReportSummary> recentReports,
            List<TodoSummary> upcomingTodos,
            PrenatalPlanProgress prenatalPlanProgress,
            List<AnalyticsService.SeriesResponse> keyMetrics,
            OffsetDateTime generatedAt
    ) {
    }

    public record PregnancySummary(
            UUID id,
            LocalDate lmpDate,
            LocalDate dueDate,
            Integer fetusCount,
            String status,
            int gestationalWeeks,
            int gestationalDays,
            String gestationalWeekDisplay,
            int pregnancyDay,
            int trimester,
            long daysUntilDue
    ) {
    }

    public record RecordSummary(UUID id, String subjectType, UUID subjectId, String recordType, OffsetDateTime occurredAt, String payloadJson) {
        static RecordSummary from(JsonRecord record) {
            return new RecordSummary(record.getId(), record.getSubjectType(), record.getSubjectId(), record.getRecordType(), record.getOccurredAt(), record.getPayloadJson());
        }
    }

    public record ReportSummary(UUID id, String subjectType, UUID subjectId, String reportType, String title, LocalDate examinedAt, String indicatorsJson) {
        static ReportSummary from(MedicalReport report) {
            return new ReportSummary(report.getId(), report.getSubjectType(), report.getSubjectId(), report.getReportType(), report.getTitle(), report.getExaminedAt(), report.getIndicatorsJson());
        }
    }

    public record TodoSummary(UUID id, String title, String category, String subjectType, UUID subjectId, OffsetDateTime dueAt, String status) {
        static TodoSummary from(TodoItem todo) {
            return new TodoSummary(todo.getId(), todo.getTitle(), todo.getCategory(), todo.getSubjectType(), todo.getSubjectId(), todo.getDueAt(), todo.getStatus());
        }
    }

    public record PrenatalPlanProgress(int total, int completed, int completionRate) {
    }
}
