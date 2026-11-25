package br.com.agroclima.repository;

import br.com.agroclima.domain.PlantationEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PlantationRepository extends JpaRepository<PlantationEntity, UUID> {
    @EntityGraph(attributePaths = "owner")
    List<PlantationEntity> findAllByArchivedAtIsNullAndOwner_IdOrderByPlantedAtDesc(UUID ownerId);

    @EntityGraph(attributePaths = "owner")
    List<PlantationEntity> findAllByArchivedAtIsNullOrderByPlantedAtDesc();

    @EntityGraph(attributePaths = "owner")
    List<PlantationEntity> findAllByOwner_IdOrderByPlantedAtDesc(UUID ownerId);

    @EntityGraph(attributePaths = "owner")
    List<PlantationEntity> findAllByOrderByPlantedAtDesc();
}
