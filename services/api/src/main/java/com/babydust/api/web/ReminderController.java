package com.babydust.api.web;

import com.babydust.api.common.ApiResponse;
import com.babydust.api.common.ResourceNotFoundException;
import com.babydust.api.domain.Reminder;
import com.babydust.api.repository.ReminderRepository;
import com.babydust.api.security.CurrentUser;
import com.babydust.api.service.PermissionService;
import com.babydust.api.service.SubjectAccessService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/reminders")
public class ReminderController {
    private final ReminderRepository reminders;
    private final PermissionService permissions;
    private final CurrentUser currentUser;
    private final SubjectAccessService subjects;

    public ReminderController(ReminderRepository reminders, PermissionService permissions, CurrentUser currentUser, SubjectAccessService subjects) {
        this.reminders = reminders;
        this.permissions = permissions;
        this.currentUser = currentUser;
        this.subjects = subjects;
    }

    @GetMapping
    public ApiResponse<List<Reminder>> list(@RequestParam UUID familyId, HttpServletRequest request) {
        permissions.requireFamilyMember(familyId, currentUser.id(request));
        return ApiResponse.ok(reminders.findTop50ByFamilyIdOrderByTriggerAtAsc(familyId));
    }

    @PostMapping
    public ApiResponse<Reminder> create(@Valid @RequestBody CreateReminderRequest body, HttpServletRequest request) {
        permissions.requireFamilyMember(body.familyId(), currentUser.id(request));
        subjects.requireSubjectInFamily(body.subjectType(), body.subjectId(), body.familyId());
        return ApiResponse.ok(reminders.save(new Reminder(body.familyId(), body.title(), body.scene(), body.subjectType(), body.subjectId(), body.triggerAt())));
    }

    @PostMapping("/{reminderId}/status")
    public ApiResponse<Reminder> updateStatus(@PathVariable UUID reminderId, @Valid @RequestBody UpdateReminderStatusRequest body, HttpServletRequest request) {
        Reminder reminder = reminders.findById(reminderId).orElseThrow(() -> new ResourceNotFoundException("Reminder not found"));
        permissions.requireFamilyMember(reminder.getFamilyId(), currentUser.id(request));
        if (!List.of("scheduled", "done", "cancelled").contains(body.status())) {
            throw new IllegalArgumentException("Unsupported reminder status: " + body.status());
        }
        reminder.markStatus(body.status());
        return ApiResponse.ok(reminders.save(reminder));
    }

    public record CreateReminderRequest(@NotNull UUID familyId, @NotBlank String title, @NotBlank String scene, String subjectType, UUID subjectId, @NotNull OffsetDateTime triggerAt) {
    }

    public record UpdateReminderStatusRequest(@NotBlank String status) {
    }
}
