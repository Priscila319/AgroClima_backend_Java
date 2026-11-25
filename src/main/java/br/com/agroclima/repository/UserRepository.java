package br.com.agroclima.repository;

import br.com.agroclima.domain.UserEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<UserEntity, UUID> {
    Optional<UserEntity> findByEmailIgnoreCase(String email);
    boolean existsByEmailIgnoreCase(String email);

    @EntityGraph(attributePaths = {"roles", "roles.permissions"})
    Optional<UserEntity> findWithRolesByEmailIgnoreCase(String email);

    @EntityGraph(attributePaths = {"roles", "roles.permissions"})
    Optional<UserEntity> findWithRolesById(UUID id);

    @EntityGraph(attributePaths = {"roles", "roles.permissions"})
    List<UserEntity> findAllWithRolesByOrderByCreatedAtDesc();

    @Query("select count(distinct u.id) from UserEntity u join u.roles r where r.name = :role and u.status = :status")
    long countUsersInRoleWithStatus(@Param("role") String role, @Param("status") UserEntity.Status status);
}
