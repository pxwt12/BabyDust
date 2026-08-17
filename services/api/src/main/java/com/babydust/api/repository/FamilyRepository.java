package com.babydust.api.repository;

import com.babydust.api.domain.Family;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FamilyRepository extends JpaRepository<Family, UUID> {
    List<Family> findByOwnerUserId(UUID ownerUserId);
}
