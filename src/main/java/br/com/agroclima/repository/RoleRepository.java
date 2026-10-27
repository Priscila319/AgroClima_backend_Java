package br.com.agroclima.repository;

import br.com.agroclima.domain.RoleEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RoleRepository extends JpaRepository<RoleEntity, UUID> {
    Optional<RoleEntity> findByNameIgnoreCase(String name);
    @EntityGraph(attributePaths = "permissions")
    List<RoleEntity> findAllByOrderByNameAsc();
}
