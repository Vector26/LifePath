package com.lifepath.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name = "building_definitions")
public class BuildingDefinition {

    @Id
    private UUID id;

    @Column(name = "type", nullable = false, unique = true)
    private String type;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "cost", nullable = false)
    private int cost;

    @Column(name = "sprite_key", nullable = false)
    private String spriteKey;

    @Column(name = "tile_gid", nullable = false)
    private int tileGid;

    @Column(name = "ttl_seconds", nullable = false)
    private int ttlSeconds;

    @Column(name = "decay_grace_seconds", nullable = false)
    private int decayGraceSeconds;

    @Column(name = "maintenance_cost", nullable = false)
    private int maintenanceCost;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getCost() {
        return cost;
    }

    public void setCost(int cost) {
        this.cost = cost;
    }

    public String getSpriteKey() {
        return spriteKey;
    }

    public void setSpriteKey(String spriteKey) {
        this.spriteKey = spriteKey;
    }

    public int getTileGid() {
        return tileGid;
    }

    public void setTileGid(int tileGid) {
        this.tileGid = tileGid;
    }

    public int getTtlSeconds() {
        return ttlSeconds;
    }

    public void setTtlSeconds(int ttlSeconds) {
        this.ttlSeconds = ttlSeconds;
    }

    public int getDecayGraceSeconds() {
        return decayGraceSeconds;
    }

    public void setDecayGraceSeconds(int decayGraceSeconds) {
        this.decayGraceSeconds = decayGraceSeconds;
    }

    public int getMaintenanceCost() {
        return maintenanceCost;
    }

    public void setMaintenanceCost(int maintenanceCost) {
        this.maintenanceCost = maintenanceCost;
    }
}
