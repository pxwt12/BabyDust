package com.babydust.api.repository;

import com.babydust.api.domain.FamilyMember;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FamilyMemberRepository extends JpaRepository<FamilyMember, UUID> {
    Optional<FamilyMember> findByFamilyIdAndUserId(UUID familyId, UUID userId);

    List<FamilyMember> findByUserId(UUID userId);
}
