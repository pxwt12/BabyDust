package com.babydust.api.repository;

import com.babydust.api.domain.PregnancyProfile;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PregnancyProfileRepository extends JpaRepository<PregnancyProfile, UUID> {
    List<PregnancyProfile> findByFamilyId(UUID familyId);
}
