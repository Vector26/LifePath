package com.lifepath.controller;

import com.lifepath.dto.BuildingDefinitionResponse;
import com.lifepath.dto.CityStateResponse;
import com.lifepath.dto.PlaceBuildingRequest;
import com.lifepath.dto.UserBuildingResponse;
import com.lifepath.service.CityService;
import com.lifepath.util.SecurityUtil;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/city")
public class CityController {

    private final CityService cityService;

    public CityController(CityService cityService) {
        this.cityService = cityService;
    }

    @GetMapping("/state")
    public CityStateResponse getState() {
        return cityService.getState(SecurityUtil.currentUserId());
    }

    @GetMapping("/shop")
    public List<BuildingDefinitionResponse> getShop() {
        return cityService.getShop();
    }

    @GetMapping("/buildings")
    public List<UserBuildingResponse> listBuildings() {
        return cityService.listBuildings(SecurityUtil.currentUserId());
    }

    @PostMapping("/buildings")
    @ResponseStatus(HttpStatus.CREATED)
    public UserBuildingResponse placeBuilding(@Valid @RequestBody PlaceBuildingRequest request) {
        return cityService.placeBuilding(
                SecurityUtil.currentUserId(),
                request.getType(),
                request.getGridX(),
                request.getGridY()
        );
    }

    @PostMapping("/buildings/{id}/maintain")
    public UserBuildingResponse maintainBuilding(@PathVariable UUID id) {
        return cityService.maintainBuilding(SecurityUtil.currentUserId(), id);
    }

    @DeleteMapping("/buildings/{id}")
    public Map<String, Integer> deleteBuilding(@PathVariable UUID id) {
        cityService.deleteBuilding(SecurityUtil.currentUserId(), id);
        return Map.of("coins_refunded", 0);
    }

    @PatchMapping("/state/coins")
    public CityStateResponse syncCoins(@RequestBody Map<String, Integer> body) {
        int coins = body.getOrDefault("coins", 0);
        return cityService.setCoins(SecurityUtil.currentUserId(), coins);
    }
}
