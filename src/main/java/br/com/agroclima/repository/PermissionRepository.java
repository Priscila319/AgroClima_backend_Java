package br.com.agroclima.repository;

import br.com.agroclima.domain.PermissionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PermissionRepository extends JpaRepository<PermissionEntity, UUID> {
    Optional<PermissionEntity> findByKey(String key);
    List<PermissionEntity> findAllByKeyIn(List<String> keys);
}
