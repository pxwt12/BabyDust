package com.babydust.api.web;

import com.babydust.api.common.ApiResponse;
import com.babydust.api.domain.TodoItem;
import com.babydust.api.repository.JsonRecordRepository;
import com.babydust.api.repository.TodoItemRepository;
import com.babydust.api.security.CurrentUser;
import com.babydust.api.service.AnalyticsService;
import com.babydust.api.service.PermissionService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/analytics")
public class AnalyticsController {
    private final AnalyticsService analytics;
    private final PermissionService permissions;
    private final CurrentUser currentUser;
    private final TodoItemRepository todos;
    private final JsonRecordRepository records;

    public AnalyticsController(AnalyticsService analytics, PermissionService permissions, CurrentUser currentUser, TodoItemRepository todos, JsonRecordRepository records) {
        this.analytics = analytics;
        this.permissions = permissions;
        this.currentUser = currentUser;
        this.todos = todos;
        this.records = records;
    }

    @GetMapping("/series")
    public ApiResponse<AnalyticsService.SeriesResponse> series(@RequestParam UUID familyId, @RequestParam String metric, HttpServletRequest request) {
        permissions.requireFamilyMember(familyId, currentUser.id(request));
        return ApiResponse.ok(analytics.series(familyId, metric));
    }

    @GetMapping("/overview")
    public ApiResponse<OverviewResponse> overview(@RequestParam UUID familyId, HttpServletRequest request) {
        permissions.requireFamilyMember(familyId, currentUser.id(request));
        List<TodoItem> prenatalTodos = todos.findTop200ByFamilyIdAndCategoryInOrderByDueAtAsc(familyId, List.of("prenatal_checkup", "delivery_prepare"));
        long completedPrenatalTodos = prenatalTodos.stream().filter(todo -> "done".equals(todo.getStatus())).count();
        int prenatalCompletionRate = prenatalTodos.isEmpty() ? 0 : (int) Math.round(completedPrenatalTodos * 100.0 / prenatalTodos.size());
        long medicationRecords = records.countByFamilyIdAndRecordType(familyId, "medication")
                + records.countByFamilyIdAndRecordType(familyId, "postpartum_medication");
        long supplementRecords = records.countByFamilyIdAndRecordType(familyId, "supplement")
                + records.countByFamilyIdAndRecordType(familyId, "fertility_supplement");
        return ApiResponse.ok(new OverviewResponse(
                new CompletionMetric(prenatalTodos.size(), (int) completedPrenatalTodos, prenatalCompletionRate),
                new RecordCountMetric((int) medicationRecords, (int) supplementRecords, (int) (medicationRecords + supplementRecords))
        ));
    }

    public record OverviewResponse(CompletionMetric prenatalPlan, RecordCountMetric medicationSupplement) {
    }

    public record CompletionMetric(int total, int completed, int completionRate) {
    }

    public record RecordCountMetric(int medicationRecords, int supplementRecords, int totalRecords) {
    }
}
