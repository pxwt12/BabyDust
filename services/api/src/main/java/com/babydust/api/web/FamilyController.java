package com.babydust.api.web;

import com.babydust.api.common.ApiResponse;
import com.babydust.api.domain.Family;
import com.babydust.api.domain.FamilyMember;
import com.babydust.api.repository.FamilyMemberRepository;
import com.babydust.api.repository.FamilyRepository;
import com.babydust.api.security.CurrentUser;
import com.babydust.api.service.PermissionService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/families")
public class FamilyController {
    private final FamilyRepository families;
    private final FamilyMemberRepository members;
    private final CurrentUser currentUser;
    private final PermissionService permissions;

    public FamilyController(FamilyRepository families, FamilyMemberRepository members, CurrentUser currentUser, PermissionService permissions) {
        this.families = families;
        this.members = members;
        this.currentUser = currentUser;
        this.permissions = permissions;
    }

    @GetMapping
    public ApiResponse<List<Family>> list(HttpServletRequest request) {
        UUID userId = currentUser.id(request);
        List<UUID> familyIds = members.findByUserId(userId).stream().map(FamilyMember::getFamilyId).toList();
        return ApiResponse.ok(families.findAllById(familyIds));
    }

    @PostMapping
    public ApiResponse<Family> create(@Valid @RequestBody CreateFamilyRequest body, HttpServletRequest request) {
        UUID userId = currentUser.id(request);
        Family family = families.save(new Family(body.name(), userId));
        members.save(new FamilyMember(family.getId(), userId, "admin", "mother"));
        return ApiResponse.ok(family);
    }

    @PostMapping("/{familyId}/invites")
    public ApiResponse<Map<String, Object>> invite(@PathVariable UUID familyId, HttpServletRequest request) {
        permissions.requireFamilyMember(familyId, currentUser.id(request));
        return ApiResponse.ok(Map.of("familyId", familyId, "inviteCode", "INVITE-" + familyId.toString().substring(0, 8)));
    }

    public record CreateFamilyRequest(@NotBlank String name) {
    }
}
