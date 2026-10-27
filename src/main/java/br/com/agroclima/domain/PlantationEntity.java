package br.com.agroclima.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "plantation")
public class PlantationEntity {
    public enum Status { EM_CULTIVO, COLHIDA, ARQUIVADA }

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id", nullable = false)
    private UserEntity owner;

    @Column(nullable = false, length = 100)
    private String variety;

    @Column(nullable = false)
    private int quantity;

    @Column(name = "field_name", nullable = false, length = 80)
    private String fieldName = "";

    @Column(nullable = false, length = 500)
    private String notes = "";

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    private Status status = Status.EM_CULTIVO;

    @Column(name = "planted_at", nullable = false)
    private Instant plantedAt;

    @Column(name = "harvested_at")
    private Instant harvestedAt;

    @Column(name = "archived_at")
    private Instant archivedAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected PlantationEntity() {}

    public PlantationEntity(UserEntity owner, String variety, int quantity, String fieldName, String notes) {
        this.owner = owner;
        this.variety = variety;
        this.quantity = quantity;
        this.fieldName = fieldName == null ? "" : fieldName.trim();
        this.notes = notes == null ? "" : notes.trim();
    }

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        plantedAt = now;
        createdAt = now;
        updatedAt = now;
        status = Status.EM_CULTIVO;
        harvestedAt = null;
        archivedAt = null;
    }

    @PreUpdate
    void onUpdate() { updatedAt = Instant.now(); }

    public void updateDetails(String variety, int quantity, String fieldName, String notes) {
        if (status != Status.EM_CULTIVO) throw new IllegalStateException("Somente plantações em cultivo podem ser editadas.");
        this.variety = variety;
        this.quantity = quantity;
        this.fieldName = fieldName == null ? "" : fieldName.trim();
        this.notes = notes == null ? "" : notes.trim();
    }

    public void markHarvested() {
        if (status != Status.EM_CULTIVO) throw new IllegalStateException("Esta plantação não está disponível para colheita.");
        harvestedAt = Instant.now();
        status = Status.COLHIDA;
    }

    public void archive() {
        if (status == Status.ARQUIVADA) throw new IllegalStateException("Esta plantação já foi arquivada.");
        archivedAt = Instant.now();
        status = Status.ARQUIVADA;
    }

    public UUID getId() { return id; }
    public UserEntity getOwner() { return owner; }
    public String getVariety() { return variety; }
    public int getQuantity() { return quantity; }
    public String getFieldName() { return fieldName; }
    public String getNotes() { return notes; }
    public Status getStatus() { return status; }
    public Instant getPlantedAt() { return plantedAt; }
    public Instant getHarvestedAt() { return harvestedAt; }
    public Instant getArchivedAt() { return archivedAt; }
    public Instant getCreatedAt() { return createdAt; }
}
