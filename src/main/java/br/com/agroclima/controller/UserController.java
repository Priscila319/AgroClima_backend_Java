package br.com.agroclima.controller;

import br.com.agroclima.dto.UserDtos;
import br.com.agroclima.security.AgroUserPrincipal;
import br.com.agroclima.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/users")
@PreAuthorize("hasAuthority('users.manage')")
public class UserController {
    private final UserService users;

    public UserController(UserService users) { this.users = users; }

    @GetMapping
    public List<UserDtos.UserResponse> list() { return users.list(); }

    @GetMapping("/{id}")
    public UserDtos.UserResponse get(@PathVariable UUID id) { return users.get(id); }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UserDtos.UserResponse create(@Valid @RequestBody UserDtos.CreateUserRequest request,
                                        @AuthenticationPrincipal AgroUserPrincipal actor) {
        return users.registerProducer(request, request.roleKey(), false, actor);
    }

    @PatchMapping("/{id}")
    public UserDtos.UserResponse updateProfile(@PathVariable UUID id,
                                                @Valid @RequestBody UserDtos.UpdateProfileRequest request,
                                                @AuthenticationPrincipal AgroUserPrincipal actor) {
        return users.updateProfile(id, request, actor);
    }

    @PatchMapping("/{id}/role")
    public UserDtos.UserResponse changeRole(@PathVariable UUID id,
                                             @Valid @RequestBody UserDtos.ChangeRoleRequest request,
                                             @AuthenticationPrincipal AgroUserPrincipal actor) {
        return users.changeRole(id, request.roleKey(), actor);
    }

    @PatchMapping("/{id}/status")
    public UserDtos.UserResponse changeStatus(@PathVariable UUID id,
                                               @Valid @RequestBody UserDtos.ChangeStatusRequest request,
                                               @AuthenticationPrincipal AgroUserPrincipal actor) {
        return users.changeStatus(id, request.status(), actor);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deactivate(@PathVariable UUID id, @AuthenticationPrincipal AgroUserPrincipal actor) {
        users.changeStatus(id, br.com.agroclima.domain.UserEntity.Status.BLOCKED, actor);
    }
}
