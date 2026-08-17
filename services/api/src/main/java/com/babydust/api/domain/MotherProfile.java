package com.babydust.api.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "mother_profiles")
public class MotherProfile extends BaseEntity {
    @Column(nullable = false)
    private UUID familyId;

    @Column(nullable = false)
    private UUID ownerUserId;

    private LocalDate birthday;

    private Integer heightCm;

    private BigDecimal prePregnancyWeightKg;

    private String bloodType;

    protected MotherProfile() {
    }

    public MotherProfile(UUID familyId, UUID ownerUserId, LocalDate birthday, Integer heightCm, BigDecimal prePregnancyWeightKg, String bloodType) {
        this.familyId = familyId;
        this.ownerUserId = ownerUserId;
        this.birthday = birthday;
        this.heightCm = heightCm;
        this.prePregnancyWeightKg = prePregnancyWeightKg;
        this.bloodType = bloodType;
    }

    public UUID getFamilyId() {
        return familyId;
    }

    public UUID getOwnerUserId() {
        return ownerUserId;
    }

    public LocalDate getBirthday() {
        return birthday;
    }

    public Integer getHeightCm() {
        return heightCm;
    }

    public BigDecimal getPrePregnancyWeightKg() {
        return prePregnancyWeightKg;
    }

    public String getBloodType() {
        return bloodType;
    }
}
