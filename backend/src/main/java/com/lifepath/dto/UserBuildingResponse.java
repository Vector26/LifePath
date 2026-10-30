package com.lifepath.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.lifepath.model.BuildingDefinition;
import com.lifepath.model.UserBuilding;

import java.time.Instant;
import java.util.UUID;

public class UserBuildingResponse {

    private UUID id;
    private String type;

    @JsonProperty("sprite_key")
    private String spriteKey;

    @JsonProperty("tile_gid")
    private int tileGid;

    @JsonProperty("grid_x")
    private int gridX;

    @JsonProperty("grid_y")
    private int gridY;

    @JsonProperty("placed_at")
    private Instant placedAt;

    @JsonProperty("expires_at")
    private Instant expiresAt;

    @JsonProperty("decayed_at")
    private Instant decayedAt;

    @JsonProperty("remove_after_at")
    private Instant removeAfterAt;

    @JsonProperty("maintenance_cost")
    private int maintenanceCost;

    public static UserBuildingResponse from(UserBuilding building, BuildingDefinition definition) {
        UserBuildingResponse response = new UserBuildingResponse();
        response.id = building.getId();
        response.type = definition.getType();
        response.spriteKey = definition.getSpriteKey();
        response.tileGid = definition.getTileGid();
        response.gridX = building.getGridX();
        response.gridY = building.getGridY();
        response.placedAt = building.getPlacedAt();
        response.expiresAt = building.getExpiresAt();
        response.decayedAt = building.getDecayedAt();
        response.removeAfterAt = building.getRemoveAfterAt();
        response.maintenanceCost = definition.getMaintenanceCost();
        return response;
    }

    public UUID getId() {
        return id;
    }

    public String getType() {
        return type;
    }

    public String getSpriteKey() {
        return spriteKey;
    }

    public int getTileGid() {
        return tileGid;
    }

    public int getGridX() {
        return gridX;
    }

    public int getGridY() {
        return gridY;
    }

    public Instant getPlacedAt() {
        return placedAt;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public Instant getDecayedAt() {
        return decayedAt;
    }

    public Instant getRemoveAfterAt() {
        return removeAfterAt;
    }

    public int getMaintenanceCost() {
        return maintenanceCost;
    }
}
