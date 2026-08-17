package com.babydust.api.repository;

import com.babydust.api.domain.BabyProfile;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BabyProfileRepository extends JpaRepository<BabyProfile, UUID> {
    List<BabyProfile> findByFamilyId(UUID familyId);
}
