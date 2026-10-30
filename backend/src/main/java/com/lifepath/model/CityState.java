package com.lifepath.model;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "city_state")
public class CityState {

    @Id
    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "coins", nullable = false)
    private int coins = 0;

    @Column(name = "premium_currency", nullable = false)
    private int premiumCurrency = 0;

    @Column(name = "city_name", nullable = false, length = 100)
    private String cityName = "My City";

    @Column(name = "population", nullable = false)
    private int population = 0;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }

    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }
    public int getCoins() { return coins; }
    public void setCoins(int coins) { this.coins = coins; }
    public int getPremiumCurrency() { return premiumCurrency; }
    public void setPremiumCurrency(int premiumCurrency) { this.premiumCurrency = premiumCurrency; }
    public String getCityName() { return cityName; }
    public void setCityName(String cityName) { this.cityName = cityName; }
    public int getPopulation() { return population; }
    public void setPopulation(int population) { this.population = population; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
