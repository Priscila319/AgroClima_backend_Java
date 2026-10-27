package br.com.agroclima.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "climate_reading")
public class ClimateReadingEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "channel_id", nullable = false, length = 100)
    private String channelId;

    @Column(name = "entry_id", nullable = false)
    private Long entryId;

    @Column(name = "sensor_code", nullable = false, length = 100)
    private String sensorCode;

    @Column(name = "captured_at", nullable = false)
    private Instant capturedAt;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "measurements", nullable = false, columnDefinition = "json")
    private Map<String, BigDecimal> measurements = new LinkedHashMap<>();

    @Column(name = "quality_status", nullable = false, length = 24)
    private String qualityStatus = "VALID";

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected ClimateReadingEntity() {}

    public ClimateReadingEntity(String channelId, Long entryId, String sensorCode, Instant capturedAt,
                                Map<String, BigDecimal> measurements, String qualityStatus) {
        this.channelId = channelId;
        this.entryId = entryId;
        this.sensorCode = sensorCode == null || sensorCode.isBlank() ? channelId : sensorCode.trim();
        this.capturedAt = capturedAt;
        this.measurements = new LinkedHashMap<>(measurements);
        this.qualityStatus = qualityStatus == null || qualityStatus.isBlank() ? "VALID" : qualityStatus;
    }

    @PrePersist
    void onCreate() { if (createdAt == null) createdAt = Instant.now(); }

    public UUID getId() { return id; }
    public String getChannelId() { return channelId; }
    public Long getEntryId() { return entryId; }
    public String getSensorCode() { return sensorCode; }
    public Instant getCapturedAt() { return capturedAt; }
    public Map<String, BigDecimal> getMeasurements() { return Map.copyOf(measurements); }
    public String getQualityStatus() { return qualityStatus; }
    public Instant getCreatedAt() { return createdAt; }
}
