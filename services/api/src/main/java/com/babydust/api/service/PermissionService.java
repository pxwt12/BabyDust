package com.babydust.api.service;

import com.babydust.api.common.AccessDeniedException;
import com.babydust.api.repository.FamilyMemberRepository;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class PermissionService {
    private final FamilyMemberRepository members;

    public PermissionService(FamilyMemberRepository members) {
        this.members = members;
    }

    public void requireFamilyMember(UUID familyId, UUID userId) {
        members.findByFamilyIdAndUserId(familyId, userId)
                .orElseThrow(() -> new AccessDeniedException("No permission for family " + familyId));
    }
}
