package com.lifepath.service;

import com.lifepath.dto.BuildingDefinitionResponse;
import com.lifepath.dto.CityStateResponse;
import com.lifepath.dto.UserBuildingResponse;
import com.lifepath.exception.ConflictException;
import com.lifepath.exception.UnprocessableEntityException;
import com.lifepath.model.BuildingDefinition;
import com.lifepath.model.CityState;
import com.lifepath.model.UserBuilding;
import com.lifepath.repository.BuildingDefinitionRepository;
import com.lifepath.repository.CityStateRepository;
import com.lifepath.repository.UserBuildingRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class CityService {

    private static final int MAP_WIDTH_TILES = 40;
    private static final int MAP_HEIGHT_TILES = 40;

    private final CityStateRepository cityStateRepository;
    private final BuildingDefinitionRepository buildingDefinitionRepository;
    private final UserBuildingRepository userBuildingRepository;

    public CityService(CityStateRepository cityStateRepository,
                       BuildingDefinitionRepository buildingDefinitionRepository,
                       UserBuildingRepository userBuildingRepository) {
        this.cityStateRepository = cityStateRepository;
        this.buildingDefinitionRepository = buildingDefinitionRepository;
        this.userBuildingRepository = userBuildingRepository;
    }

    @Transactional
    public CityStateResponse getState(UUID userId) {
        return CityStateResponse.from(getOrCreateCityState(userId));
    }

    @Transactional(readOnly = true)
    public List<BuildingDefinitionResponse> getShop() {
        return buildingDefinitionRepository.findAllByOrderByCostAscNameAsc()
                .stream()
                .map(BuildingDefinitionResponse::from)
                .collect(Collectors.toList());
    }

    @Transactional
    public List<UserBuildingResponse> listBuildings(UUID userId) {
        normalizeDecayStateForUser(userId, Instant.now());

        List<UserBuilding> buildings = userBuildingRepository.findByUserId(userId);
        Map<UUID, BuildingDefinition> definitions = loadDefinitionMap(buildings);
        return buildings.stream()
                .map(building -> toResponse(building, definitions))
                .collect(Collectors.toList());
    }

    @Transactional
    public UserBuildingResponse placeBuilding(UUID userId, String type, int gridX, int gridY) {
        normalizeDecayStateForUser(userId, Instant.now());
        validateGridPosition(gridX, gridY);

        if (userBuildingRepository.existsByUserIdAndGridXAndGridY(userId, gridX, gridY)) {
            throw new ConflictException("CELL_OCCUPIED", "That tile already has a placed item");
        }

        BuildingDefinition definition = buildingDefinitionRepository.findByType(type)
                .orElseThrow(() -> new NoSuchElementException("Building definition not found"));

        CityState cityState = getOrCreateCityState(userId);
        if (cityState.getCoins() < definition.getCost()) {
            throw new UnprocessableEntityException("INSUFFICIENT_COINS", "Not enough coins to purchase this item");
        }

        cityState.setCoins(cityState.getCoins() - definition.getCost());
        cityStateRepository.save(cityState);

        Instant now = Instant.now();
        UserBuilding userBuilding = new UserBuilding();
        userBuilding.setUserId(userId);
        userBuilding.setBuildingDefinitionId(definition.getId());
        userBuilding.setGridX(gridX);
        userBuilding.setGridY(gridY);
        userBuilding.setPlacedAt(now);
        userBuilding.setExpiresAt(now.plusSeconds(definition.getTtlSeconds()));
        userBuilding = userBuildingRepository.save(userBuilding);

        return UserBuildingResponse.from(userBuilding, definition);
    }

    @Transactional
    public UserBuildingResponse maintainBuilding(UUID userId, UUID buildingId) {
        normalizeDecayStateForUser(userId, Instant.now());

        UserBuilding userBuilding = userBuildingRepository.findByIdAndUserId(buildingId, userId)
                .orElseThrow(() -> new NoSuchElementException("Placed item not found"));

        BuildingDefinition definition = buildingDefinitionRepository.findById(userBuilding.getBuildingDefinitionId())
                .orElseThrow(() -> new NoSuchElementException("Building definition not found"));

        CityState cityState = getOrCreateCityState(userId);
        if (cityState.getCoins() < definition.getMaintenanceCost()) {
            throw new UnprocessableEntityException("INSUFFICIENT_COINS", "Not enough coins to maintain this item");
        }

        cityState.setCoins(cityState.getCoins() - definition.getMaintenanceCost());
        cityStateRepository.save(cityState);

        Instant now = Instant.now();
        userBuilding.setDecayedAt(null);
        userBuilding.setRemoveAfterAt(null);
        userBuilding.setExpiresAt(now.plusSeconds(definition.getTtlSeconds()));
        userBuilding = userBuildingRepository.save(userBuilding);

        return UserBuildingResponse.from(userBuilding, definition);
    }

    @Transactional
    public void deleteBuilding(UUID userId, UUID buildingId) {
        UserBuilding userBuilding = userBuildingRepository.findByIdAndUserId(buildingId, userId)
                .orElseThrow(() -> new NoSuchElementException("Placed item not found"));
        userBuildingRepository.delete(userBuilding);
    }

    @Transactional
    public void normalizeDecayStateForAllUsers(Instant now) {
        markExpiredAsDecayed(userBuildingRepository.findByDecayedAtIsNullAndExpiresAtBefore(now), now);
        deleteFullyExpired(userBuildingRepository.findByDecayedAtIsNotNullAndRemoveAfterAtBefore(now));
    }

    private void normalizeDecayStateForUser(UUID userId, Instant now) {
        markExpiredAsDecayed(userBuildingRepository.findByUserIdAndDecayedAtIsNullAndExpiresAtBefore(userId, now), now);
        deleteFullyExpired(userBuildingRepository.findByUserIdAndDecayedAtIsNotNullAndRemoveAfterAtBefore(userId, now));
    }

    private void markExpiredAsDecayed(List<UserBuilding> expired, Instant now) {
        if (expired.isEmpty()) {
            return;
        }

        Map<UUID, BuildingDefinition> definitions = loadDefinitionMap(expired);
        for (UserBuilding building : expired) {
            BuildingDefinition definition = definitions.get(building.getBuildingDefinitionId());
            if (definition == null) {
                continue;
            }
            building.setDecayedAt(now);
            building.setRemoveAfterAt(now.plusSeconds(definition.getDecayGraceSeconds()));
        }
        userBuildingRepository.saveAll(expired);
    }

    private void deleteFullyExpired(List<UserBuilding> fullyExpired) {
        if (fullyExpired.isEmpty()) {
            return;
        }
        userBuildingRepository.deleteAll(fullyExpired);
    }

    private Map<UUID, BuildingDefinition> loadDefinitionMap(List<UserBuilding> buildings) {
        Map<UUID, BuildingDefinition> definitions = new HashMap<>();
        if (buildings.isEmpty()) {
            return definitions;
        }
        List<UUID> ids = buildings.stream()
                .map(UserBuilding::getBuildingDefinitionId)
                .distinct()
                .collect(Collectors.toList());
        for (BuildingDefinition definition : buildingDefinitionRepository.findAllById(ids)) {
            definitions.put(definition.getId(), definition);
        }
        return definitions;
    }

    private UserBuildingResponse toResponse(UserBuilding building, Map<UUID, BuildingDefinition> definitions) {
        BuildingDefinition definition = definitions.get(building.getBuildingDefinitionId());
        if (definition == null) {
            throw new NoSuchElementException("Building definition not found");
        }
        return UserBuildingResponse.from(building, definition);
    }

    private void validateGridPosition(int gridX, int gridY) {
        if (gridX < 0 || gridX >= MAP_WIDTH_TILES || gridY < 0 || gridY >= MAP_HEIGHT_TILES) {
            throw new UnprocessableEntityException("INVALID_GRID_POSITION", "Placement tile is outside map bounds");
        }
    }

    @Transactional
    public CityStateResponse setCoins(UUID userId, int coins) {
        CityState state = getOrCreateCityState(userId);
        state.setCoins(Math.max(0, coins));
        cityStateRepository.save(state);
        return CityStateResponse.from(state);
    }

    private CityState getOrCreateCityState(UUID userId) {
        return cityStateRepository.findByUserId(userId).orElseGet(() -> {
            CityState state = new CityState();
            state.setUserId(userId);
            state.setCoins(0);
            state.setCityName("My City");
            return cityStateRepository.save(state);
        });
    }
}
