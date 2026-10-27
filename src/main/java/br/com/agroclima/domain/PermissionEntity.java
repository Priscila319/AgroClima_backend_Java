package br.com.agroclima.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name = "app_permission")
public class PermissionEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "permission_key", nullable = false, unique = true, length = 100)
    private String key;

    @Column(nullable = false, length = 240)
    private String description = "";

    protected PermissionEntity() {}

    public PermissionEntity(String key, String description) {
        this.key = key;
        this.description = description == null ? "" : description;
    }

    public UUID getId() { return id; }
    public String getKey() { return key; }
    public String getDescription() { return description; }
}
