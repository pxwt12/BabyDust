package com.babydust.api.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "family_members")
public class FamilyMember extends BaseEntity {
    @Column(nullable = false)
    private UUID familyId;

    @Column(nullable = false)
    private UUID userId;

    @Column(nullable = false)
    private String role;

    @Column(nullable = false)
    private String relation;

    protected FamilyMember() {
    }

    public FamilyMember(UUID familyId, UUID userId, String role, String relation) {
        this.familyId = familyId;
        this.userId = userId;
        this.role = role;
        this.relation = relation;
    }

    public UUID getFamilyId() {
        return familyId;
    }

    public UUID getUserId() {
        return userId;
    }

    public String getRole() {
        return role;
    }

    public String getRelation() {
        return relation;
    }
}
