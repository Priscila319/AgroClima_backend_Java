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
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "audit_event")
public class AuditEventEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "actor_id")
    private UserEntity actor;

    @Column(name = "actor_name", nullable = false, length = 160)
    private String actorName;

    @Column(name = "actor_email", nullable = false, length = 254)
    private String actorEmail = "";

    @Column(nullable = false, length = 160)
    private String action;

    @Column(nullable = false, length = 160)
    private String resource;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    @Column(nullable = false, length = 200)
    private String source = "API";

    @Column(nullable = false, length = 24)
    private String status = "CONCLUIDO";

    @Column(nullable = false, columnDefinition = "text")
    private String details = "";

    protected AuditEventEntity() {}

    public AuditEventEntity(UserEntity actor, String actorName, String actorEmail, String action,
                            String resource, String source, String status, String details) {
        this.actor = actor;
        this.actorName = actorName;
        this.actorEmail = actorEmail == null ? "" : actorEmail;
        this.action = action;
        this.resource = resource;
        this.source = source == null || source.isBlank() ? "API" : source;
        this.status = status == null || status.isBlank() ? "CONCLUIDO" : status;
        this.details = details == null ? "" : details;
    }

    @PrePersist
    void onCreate() { if (occurredAt == null) occurredAt = Instant.now(); }

    public UUID getId() { return id; }
    public UserEntity getActor() { return actor; }
    public String getActorName() { return actorName; }
    public String getActorEmail() { return actorEmail; }
    public String getAction() { return action; }
    public String getResource() { return resource; }
    public Instant getOccurredAt() { return occurredAt; }
    public String getSource() { return source; }
    public String getStatus() { return status; }
    public String getDetails() { return details; }
}
