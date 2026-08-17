package com.babydust.api.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "families")
public class Family extends BaseEntity {
    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private UUID ownerUserId;

    protected Family() {
    }

    public Family(String name, UUID ownerUserId) {
        this.name = name;
        this.ownerUserId = ownerUserId;
    }

    public String getName() {
        return name;
    }

    public UUID getOwnerUserId() {
        return ownerUserId;
    }
}
