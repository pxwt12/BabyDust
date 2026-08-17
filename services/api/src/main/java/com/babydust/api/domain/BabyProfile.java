package com.babydust.api.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "baby_profiles")
public class BabyProfile extends BaseEntity {
    @Column(nullable = false)
    private UUID familyId;

    private UUID pregnancyId;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String gender;

    private OffsetDateTime birthDateTime;

    private BigDecimal birthWeightKg;

    private BigDecimal birthLengthCm;

    protected BabyProfile() {
    }

    public BabyProfile(UUID familyId, UUID pregnancyId, String name, String gender, OffsetDateTime birthDateTime, BigDecimal birthWeightKg, BigDecimal birthLengthCm) {
        this.familyId = familyId;
        this.pregnancyId = pregnancyId;
        this.name = name;
        this.gender = gender;
        this.birthDateTime = birthDateTime;
        this.birthWeightKg = birthWeightKg;
        this.birthLengthCm = birthLengthCm;
    }

    public UUID getFamilyId() {
        return familyId;
    }

    public UUID getPregnancyId() {
        return pregnancyId;
    }

    public String getName() {
        return name;
    }

    public String getGender() {
        return gender;
    }

    public OffsetDateTime getBirthDateTime() {
        return birthDateTime;
    }

    public BigDecimal getBirthWeightKg() {
        return birthWeightKg;
    }

    public BigDecimal getBirthLengthCm() {
        return birthLengthCm;
    }
}
