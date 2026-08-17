package com.babydust.api.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "pregnancy_profiles")
public class PregnancyProfile extends BaseEntity {
    @Column(nullable = false)
    private UUID familyId;

    @Column(nullable = false)
    private LocalDate lmpDate;

    @Column(nullable = false)
    private LocalDate dueDate;

    @Column(nullable = false)
    private Integer fetusCount;

    @Column(nullable = false)
    private String status;

    protected PregnancyProfile() {
    }

    public PregnancyProfile(UUID familyId, LocalDate lmpDate, LocalDate dueDate, Integer fetusCount) {
        this.familyId = familyId;
        this.lmpDate = lmpDate;
        this.dueDate = dueDate;
        this.fetusCount = fetusCount;
        this.status = "pregnant";
    }

    public UUID getFamilyId() {
        return familyId;
    }

    public LocalDate getLmpDate() {
        return lmpDate;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public Integer getFetusCount() {
        return fetusCount;
    }

    public String getStatus() {
        return status;
    }
}
