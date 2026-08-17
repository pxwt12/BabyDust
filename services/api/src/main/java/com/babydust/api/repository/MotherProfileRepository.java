package com.babydust.api.repository;

import com.babydust.api.domain.MotherProfile;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MotherProfileRepository extends JpaRepository<MotherProfile, UUID> {
    List<MotherProfile> findByFamilyId(UUID familyId);
}
