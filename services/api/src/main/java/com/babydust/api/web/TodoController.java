package com.babydust.api.web;

import com.babydust.api.common.ApiResponse;
import com.babydust.api.common.ResourceNotFoundException;
import com.babydust.api.domain.TodoItem;
import com.babydust.api.repository.TodoItemRepository;
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
@RequestMapping("/api/v1/todos")
public class TodoController {
    private final TodoItemRepository todos;
    private final PermissionService permissions;
    private final CurrentUser currentUser;
    private final SubjectAccessService subjects;

    public TodoController(TodoItemRepository todos, PermissionService permissions, CurrentUser currentUser, SubjectAccessService subjects) {
        this.todos = todos;
        this.permissions = permissions;
        this.currentUser = currentUser;
        this.subjects = subjects;
    }

    @GetMapping
    public ApiResponse<List<TodoItem>> list(
            @RequestParam UUID familyId,
            @RequestParam(required = false) String subjectType,
            @RequestParam(required = false) UUID subjectId,
            HttpServletRequest request
    ) {
        permissions.requireFamilyMember(familyId, currentUser.id(request));
        if (subjectType != null || subjectId != null) {
            subjects.requireSubjectInFamily(subjectType, subjectId, familyId);
            return ApiResponse.ok(todos.findTop100ByFamilyIdAndSubjectTypeAndSubjectIdOrderByDueAtAsc(familyId, subjectType, subjectId));
        }
        return ApiResponse.ok(todos.findTop50ByFamilyIdOrderByDueAtAsc(familyId));
    }

    @PostMapping
    public ApiResponse<TodoItem> create(@Valid @RequestBody CreateTodoRequest body, HttpServletRequest request) {
        permissions.requireFamilyMember(body.familyId(), currentUser.id(request));
        subjects.requireSubjectInFamily(body.subjectType(), body.subjectId(), body.familyId());
        return ApiResponse.ok(todos.save(new TodoItem(body.familyId(), body.title(), body.category(), body.subjectType(), body.subjectId(), body.dueAt())));
    }

    @PostMapping("/{todoId}/status")
    public ApiResponse<TodoItem> updateStatus(@PathVariable UUID todoId, @Valid @RequestBody UpdateTodoStatusRequest body, HttpServletRequest request) {
        TodoItem todo = todos.findById(todoId).orElseThrow(() -> new ResourceNotFoundException("Todo not found"));
        permissions.requireFamilyMember(todo.getFamilyId(), currentUser.id(request));
        if (!List.of("pending", "done", "cancelled").contains(body.status())) {
            throw new IllegalArgumentException("Unsupported todo status: " + body.status());
        }
        todo.markStatus(body.status());
        return ApiResponse.ok(todos.save(todo));
    }

    public record CreateTodoRequest(@NotNull UUID familyId, @NotBlank String title, @NotBlank String category, String subjectType, UUID subjectId, OffsetDateTime dueAt) {
    }

    public record UpdateTodoStatusRequest(@NotBlank String status) {
    }
}
