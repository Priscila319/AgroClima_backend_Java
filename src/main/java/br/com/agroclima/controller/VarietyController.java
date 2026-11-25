package br.com.agroclima.controller;

import br.com.agroclima.dto.VarietyDtos;
import br.com.agroclima.security.AgroUserPrincipal;
import br.com.agroclima.service.VarietyService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
@RequestMapping("/api/varieties")
public class VarietyController {
    private final VarietyService varieties;

    public VarietyController(VarietyService varieties) { this.varieties = varieties; }

    @GetMapping
    public List<VarietyDtos.VarietyResponse> list() { return varieties.list(false); }

    @GetMapping("/all")
    @PreAuthorize("hasRole('ADMIN')")
    public List<VarietyDtos.VarietyResponse> listAll() { return varieties.list(true); }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    public VarietyDtos.VarietyResponse create(@Valid @RequestBody VarietyDtos.CreateVarietyRequest request,
                                               @AuthenticationPrincipal AgroUserPrincipal actor) {
        return varieties.create(request, actor);
    }

    @PatchMapping("/{id}/active")
    @PreAuthorize("hasRole('ADMIN')")
    public VarietyDtos.VarietyResponse setActive(@PathVariable UUID id,
                                                  @Valid @RequestBody VarietyDtos.ActiveRequest request,
                                                  @AuthenticationPrincipal AgroUserPrincipal actor) {
        return varieties.setActive(id, request.active(), actor);
    }
}
