package br.com.agroclima.security;

import br.com.agroclima.domain.RoleEntity;
import br.com.agroclima.domain.UserEntity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.Instant;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

public record AgroUserPrincipal(UUID id, String fullName, String email, String passwordHash,
                                UserEntity.Status status, Instant lockedUntil, Set<String> roleKeys,
                                Set<String> permissionKeys) implements UserDetails {
    public static AgroUserPrincipal from(UserEntity user) {
        Set<String> roles = new LinkedHashSet<>();
        Set<String> permissions = new LinkedHashSet<>();
        for (RoleEntity role : user.getRoles()) {
            roles.add(role.getName());
            role.getPermissions().forEach(permission -> permissions.add(permission.getKey()));
        }
        return new AgroUserPrincipal(user.getId(), user.getFullName(), user.getEmail(), user.getPasswordHash(),
            user.getStatus(), user.getLockedUntil(), Set.copyOf(roles), Set.copyOf(permissions));
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        Set<GrantedAuthority> authorities = new LinkedHashSet<>();
        roleKeys.forEach(role -> authorities.add(new SimpleGrantedAuthority("ROLE_" + role)));
        permissionKeys.forEach(permission -> authorities.add(new SimpleGrantedAuthority(permission)));
        return Set.copyOf(authorities);
    }

    @Override public String getPassword() { return passwordHash; }
    @Override public String getUsername() { return email; }
    @Override public boolean isAccountNonExpired() { return true; }
    @Override public boolean isAccountNonLocked() { return status == UserEntity.Status.ACTIVE && (lockedUntil == null || !Instant.now().isBefore(lockedUntil)); }
    @Override public boolean isCredentialsNonExpired() { return true; }
    @Override public boolean isEnabled() { return status == UserEntity.Status.ACTIVE; }
}
