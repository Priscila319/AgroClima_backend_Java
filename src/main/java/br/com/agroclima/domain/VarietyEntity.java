package br.com.agroclima.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name = "grape_variety")
public class VarietyEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true, length = 100)
    private String name;

    @Column(nullable = false, length = 100)
    private String category;

    @Column(nullable = false)
    private boolean active = true;

    protected VarietyEntity() {}

    public VarietyEntity(String name, String category) {
        this.name = name.trim();
        this.category = category == null ? "" : category.trim();
        this.active = true;
    }

    public void deactivate() { this.active = false; }
    public void activate() { this.active = true; }

    public UUID getId() { return id; }
    public String getName() { return name; }
    public String getCategory() { return category; }
    public boolean isActive() { return active; }
}
