package com.lifepath.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.lifepath.model.BuildingDefinition;

public class BuildingDefinitionResponse {

    private String type;
    private String name;
    private int cost;

    @JsonProperty("sprite_key")
    private String spriteKey;

    @JsonProperty("tile_gid")
    private int tileGid;

    @JsonProperty("ttl_seconds")
    private int ttlSeconds;

    @JsonProperty("decay_grace_seconds")
    private int decayGraceSeconds;

    @JsonProperty("maintenance_cost")
    private int maintenanceCost;

    public static BuildingDefinitionResponse from(BuildingDefinition definition) {
        BuildingDefinitionResponse response = new BuildingDefinitionResponse();
        response.type = definition.getType();
        response.name = definition.getName();
        response.cost = definition.getCost();
        response.spriteKey = definition.getSpriteKey();
        response.tileGid = definition.getTileGid();
        response.ttlSeconds = definition.getTtlSeconds();
        response.decayGraceSeconds = definition.getDecayGraceSeconds();
        response.maintenanceCost = definition.getMaintenanceCost();
        return response;
    }

    public String getType() {
        return type;
    }

    public String getName() {
        return name;
    }

    public int getCost() {
        return cost;
    }

    public String getSpriteKey() {
        return spriteKey;
    }

    public int getTileGid() {
        return tileGid;
    }

    public int getTtlSeconds() {
        return ttlSeconds;
    }

    public int getDecayGraceSeconds() {
        return decayGraceSeconds;
    }

    public int getMaintenanceCost() {
        return maintenanceCost;
    }
}
