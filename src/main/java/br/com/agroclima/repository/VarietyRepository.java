package br.com.agroclima.repository;

import br.com.agroclima.domain.VarietyEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface VarietyRepository extends JpaRepository<VarietyEntity, UUID> {
    List<VarietyEntity> findAllByActiveTrueOrderByNameAsc();
    List<VarietyEntity> findAllByOrderByNameAsc();
    Optional<VarietyEntity> findByNameIgnoreCase(String name);
    boolean existsByNameIgnoreCase(String name);
}
