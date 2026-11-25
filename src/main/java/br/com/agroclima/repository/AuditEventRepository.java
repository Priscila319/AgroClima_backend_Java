package br.com.agroclima.repository;

import br.com.agroclima.domain.AuditEventEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AuditEventRepository extends JpaRepository<AuditEventEntity, UUID> {
    List<AuditEventEntity> findTop500ByOrderByOccurredAtDesc();
}
