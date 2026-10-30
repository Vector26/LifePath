package com.lifepath.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "user_buildings")
public class UserBuilding {

    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "building_definition_id", nullable = false)
    private UUID buildingDefinitionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "building_definition_id", insertable = false, updatable = false)
    private BuildingDefinition buildingDefinition;

    @Column(name = "grid_x", nullable = false)
    private int gridX;

    @Column(name = "grid_y", nullable = false)
    private int gridY;

    @Column(name = "placed_at", nullable = false)
    private Instant placedAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "decayed_at")
    private Instant decayedAt;

    @Column(name = "remove_after_at")
    private Instant removeAfterAt;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public UUID getBuildingDefinitionId() {
        return buildingDefinitionId;
    }

    public void setBuildingDefinitionId(UUID buildingDefinitionId) {
        this.buildingDefinitionId = buildingDefinitionId;
    }

    public BuildingDefinition getBuildingDefinition() {
        return buildingDefinition;
    }

    public int getGridX() {
        return gridX;
    }

    public void setGridX(int gridX) {
        this.gridX = gridX;
    }

    public int getGridY() {
        return gridY;
    }

    public void setGridY(int gridY) {
        this.gridY = gridY;
    }

    public Instant getPlacedAt() {
        return placedAt;
    }

    public void setPlacedAt(Instant placedAt) {
        this.placedAt = placedAt;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }

    public Instant getDecayedAt() {
        return decayedAt;
    }

    public void setDecayedAt(Instant decayedAt) {
        this.decayedAt = decayedAt;
    }

    public Instant getRemoveAfterAt() {
        return removeAfterAt;
    }

    public void setRemoveAfterAt(Instant removeAfterAt) {
        this.removeAfterAt = removeAfterAt;
    }
}
