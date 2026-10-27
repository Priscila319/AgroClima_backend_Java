package br.com.agroclima.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "system_setting")
public class AppSettingEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "setting_key", nullable = false, unique = true, length = 100)
    private String key;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "json_value", nullable = false, columnDefinition = "json")
    private JsonNode jsonValue;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "updated_by")
    private UserEntity updatedBy;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected AppSettingEntity() {}

    public AppSettingEntity(String key, JsonNode jsonValue, UserEntity updatedBy) {
        this.key = key;
        this.jsonValue = jsonValue;
        this.updatedBy = updatedBy;
    }

    public void updateValue(JsonNode jsonValue, UserEntity updatedBy) {
        this.jsonValue = jsonValue;
        this.updatedBy = updatedBy;
    }

    @PrePersist @PreUpdate
    void touch() { updatedAt = Instant.now(); }

    public UUID getId() { return id; }
    public String getKey() { return key; }
    public JsonNode getJsonValue() { return jsonValue; }
    public Instant getUpdatedAt() { return updatedAt; }
    public UserEntity getUpdatedBy() { return updatedBy; }
}
