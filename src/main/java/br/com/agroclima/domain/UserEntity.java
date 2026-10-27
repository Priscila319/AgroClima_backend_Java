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
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "app_user")
public class UserEntity {
    public enum Status { ACTIVE, BLOCKED }

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "full_name", nullable = false, length = 160)
    private String fullName;

    @Column(nullable = false, unique = true, length = 254)
    private String email;

    @Column(name = "password_hash", nullable = false, length = 100)
    private String passwordHash;

    @Column(nullable = false, length = 30)
    private String phone = "";

    @Column(nullable = false, length = 160)
    private String organization = "";

    @Column(name = "job_title", nullable = false, length = 120)
    private String jobTitle = "";

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    private Status status = Status.ACTIVE;

    @Column(name = "failed_login_attempts", nullable = false)
    private int failedLoginAttempts;

    @Column(name = "locked_until")
    private Instant lockedUntil;

    @Column(name = "last_login_at")
    private Instant lastLoginAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "user_roles",
        joinColumns = @JoinColumn(name = "user_id"),
        inverseJoinColumns = @JoinColumn(name = "role_id"))
    private Set<RoleEntity> roles = new LinkedHashSet<>();

    protected UserEntity() {}

    public UserEntity(String fullName, String email, String passwordHash, String phone,
                      String organization, String jobTitle, Set<RoleEntity> roles) {
        this.fullName = fullName.trim();
        this.email = email.trim().toLowerCase();
        this.passwordHash = passwordHash;
        this.phone = phone == null ? "" : phone.trim();
        this.organization = organization == null ? "" : organization.trim();
        this.jobTitle = jobTitle == null ? "" : jobTitle.trim();
        this.roles = new LinkedHashSet<>(roles);
        this.status = Status.ACTIVE;
    }

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        if (createdAt == null) createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() { updatedAt = Instant.now(); }

    public void recordLogin(Instant at) {
        this.lastLoginAt = at;
        this.failedLoginAttempts = 0;
        this.lockedUntil = null;
    }

    public void recordFailedLogin(Instant now, int maxAttempts, long lockoutMinutes) {
        if (status != Status.ACTIVE) return;
        if (lockedUntil != null && now.isBefore(lockedUntil)) return;
        if (lockedUntil != null && !now.isBefore(lockedUntil)) {
            failedLoginAttempts = 0;
            lockedUntil = null;
        }
        failedLoginAttempts++;
        if (failedLoginAttempts >= maxAttempts) {
            failedLoginAttempts = 0;
            lockedUntil = now.plusSeconds(lockoutMinutes * 60);
        }
    }

    public void setRoles(Set<RoleEntity> roles) { this.roles = new LinkedHashSet<>(roles); }
    public void setStatus(Status status) {
        this.status = status;
        if (status == Status.ACTIVE) {
            this.failedLoginAttempts = 0;
            this.lockedUntil = null;
        }
    }
    public void updateProfile(String fullName, String phone, String organization, String jobTitle) {
        this.fullName = fullName.trim();
        this.phone = phone == null ? "" : phone.trim();
        this.organization = organization == null ? "" : organization.trim();
        this.jobTitle = jobTitle == null ? "" : jobTitle.trim();
    }

    public UUID getId() { return id; }
    public String getFullName() { return fullName; }
    public String getEmail() { return email; }
    public String getPasswordHash() { return passwordHash; }
    public String getPhone() { return phone; }
    public String getOrganization() { return organization; }
    public String getJobTitle() { return jobTitle; }
    public Status getStatus() { return status; }
    public int getFailedLoginAttempts() { return failedLoginAttempts; }
    public Instant getLockedUntil() { return lockedUntil; }
    public Instant getLastLoginAt() { return lastLoginAt; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public Set<RoleEntity> getRoles() { return roles; }
}
