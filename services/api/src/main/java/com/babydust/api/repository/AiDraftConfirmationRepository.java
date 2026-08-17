package com.babydust.api.repository;

import com.babydust.api.domain.AiDraftConfirmation;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface AiDraftConfirmationRepository extends JpaRepository<AiDraftConfirmation, UUID>, JpaSpecificationExecutor<AiDraftConfirmation> {
    List<AiDraftConfirmation> findTop50ByFamilyIdOrderByConfirmedAtDesc(UUID familyId);
}
