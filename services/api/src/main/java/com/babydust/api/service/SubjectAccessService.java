package com.babydust.api.service;

import com.babydust.api.common.ResourceNotFoundException;
import com.babydust.api.repository.BabyProfileRepository;
import com.babydust.api.repository.MotherProfileRepository;
import com.babydust.api.repository.PregnancyProfileRepository;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class SubjectAccessService {
    private final PregnancyProfileRepository pregnancies;
    private final MotherProfileRepository mothers;
    private final BabyProfileRepository babies;

    public SubjectAccessService(PregnancyProfileRepository pregnancies, MotherProfileRepository mothers, BabyProfileRepository babies) {
        this.pregnancies = pregnancies;
        this.mothers = mothers;
        this.babies = babies;
    }

    public void requireSubjectInFamily(String subjectType, UUID subjectId, UUID familyId) {
        if (subjectType == null && subjectId == null) {
            return;
        }
        if (subjectType == null || subjectId == null) {
            throw new IllegalArgumentException("subjectType and subjectId must be provided together");
        }
        switch (subjectType) {
            case "family" -> {
                if (!subjectId.equals(familyId)) {
                    throw new ResourceNotFoundException("Family subject not found");
                }
            }
            case "pregnancy" -> pregnancies.findById(subjectId)
                    .filter(pregnancy -> pregnancy.getFamilyId().equals(familyId))
                    .orElseThrow(() -> new ResourceNotFoundException("Pregnancy profile not found in family"));
            case "mother" -> mothers.findById(subjectId)
                    .filter(mother -> mother.getFamilyId().equals(familyId))
                    .orElseThrow(() -> new ResourceNotFoundException("Mother profile not found in family"));
            case "baby" -> babies.findById(subjectId)
                    .filter(baby -> baby.getFamilyId().equals(familyId))
                    .orElseThrow(() -> new ResourceNotFoundException("Baby profile not found in family"));
            default -> throw new IllegalArgumentException("Unsupported subject type: " + subjectType);
        }
    }
}
