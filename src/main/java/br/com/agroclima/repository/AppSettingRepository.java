package br.com.agroclima.repository;

import br.com.agroclima.domain.AppSettingEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface AppSettingRepository extends JpaRepository<AppSettingEntity, UUID> {
    Optional<AppSettingEntity> findByKey(String key);
}
