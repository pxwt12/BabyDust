package com.babydust.api.repository;

import com.babydust.api.domain.MedicalReport;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MedicalReportRepository extends JpaRepository<MedicalReport, UUID> {
    List<MedicalReport> findTop20ByFamilyIdOrderByExaminedAtDesc(UUID familyId);

    List<MedicalReport> findTop200ByFamilyIdOrderByExaminedAtAsc(UUID familyId);
}
