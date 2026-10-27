package br.com.agroclima.repository;

import br.com.agroclima.domain.ClimateReadingEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface ClimateReadingRepository extends JpaRepository<ClimateReadingEntity, UUID> {
    boolean existsByChannelIdAndEntryId(String channelId, Long entryId);
    List<ClimateReadingEntity> findTop500ByOrderByCapturedAtDesc();
    List<ClimateReadingEntity> findTop5000ByCapturedAtBetweenOrderByCapturedAtAsc(Instant from, Instant to);
    List<ClimateReadingEntity> findTop5000ByChannelIdAndCapturedAtBetweenOrderByCapturedAtAsc(String channelId, Instant from, Instant to);
}
