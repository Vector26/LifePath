package com.lifepath.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.lifepath.model.CityState;

import java.time.Instant;
import java.util.UUID;

public class CityStateResponse {

    @JsonProperty("user_id")
    private UUID userId;

    private int coins;

    @JsonProperty("premium_currency")
    private int premiumCurrency;

    @JsonProperty("city_name")
    private String cityName;

    private int population;

    @JsonProperty("created_at")
    private Instant createdAt;

    @JsonProperty("updated_at")
    private Instant updatedAt;

    public static CityStateResponse from(CityState state) {
        CityStateResponse response = new CityStateResponse();
        response.userId = state.getUserId();
        response.coins = state.getCoins();
        response.premiumCurrency = state.getPremiumCurrency();
        response.cityName = state.getCityName();
        response.population = state.getPopulation();
        response.createdAt = state.getCreatedAt();
        response.updatedAt = state.getUpdatedAt();
        return response;
    }

    public UUID getUserId() {
        return userId;
    }

    public int getCoins() {
        return coins;
    }

    public int getPremiumCurrency() {
        return premiumCurrency;
    }

    public String getCityName() {
        return cityName;
    }

    public int getPopulation() {
        return population;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
