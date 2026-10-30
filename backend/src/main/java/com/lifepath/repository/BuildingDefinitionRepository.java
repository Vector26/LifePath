package com.lifepath.repository;

import com.lifepath.model.BuildingDefinition;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BuildingDefinitionRepository extends JpaRepository<BuildingDefinition, UUID> {
    Optional<BuildingDefinition> findByType(String type);
    List<BuildingDefinition> findAllByOrderByCostAscNameAsc();
}
