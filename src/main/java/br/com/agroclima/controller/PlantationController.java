package br.com.agroclima.controller;

import br.com.agroclima.dto.PlantationDtos;
import br.com.agroclima.security.AgroUserPrincipal;
import br.com.agroclima.service.PlantationService;
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
@RequestMapping("/api/plantations")
public class PlantationController {
    private final PlantationService plantations;

    public PlantationController(PlantationService plantations) { this.plantations = plantations; }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('plantations.manage', 'plantations.read.all')")
    public List<PlantationDtos.PlantationResponse> list(@AuthenticationPrincipal AgroUserPrincipal actor) {
        return plantations.list(actor, false);
    }

    @GetMapping("/history")
    @PreAuthorize("hasAnyAuthority('plantations.manage', 'plantations.read.all', 'reports.view')")
    public List<PlantationDtos.PlantationResponse> history(@AuthenticationPrincipal AgroUserPrincipal actor) {
        return plantations.list(actor, true);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('plantations.manage')")
    public PlantationDtos.PlantationResponse create(@Valid @RequestBody PlantationDtos.CreatePlantationRequest request,
                                                     @AuthenticationPrincipal AgroUserPrincipal actor) {
        return plantations.create(request, actor);
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAuthority('plantations.manage')")
    public PlantationDtos.PlantationResponse update(@PathVariable UUID id,
                                                     @Valid @RequestBody PlantationDtos.UpdatePlantationRequest request,
                                                     @AuthenticationPrincipal AgroUserPrincipal actor) {
        return plantations.update(id, request, actor);
    }

    @PostMapping("/{id}/harvest")
    @PreAuthorize("hasAuthority('plantations.manage')")
    public PlantationDtos.PlantationResponse harvest(@PathVariable UUID id,
                                                      @AuthenticationPrincipal AgroUserPrincipal actor) {
        return plantations.harvest(id, actor);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('plantations.manage')")
    public void archive(@PathVariable UUID id, @AuthenticationPrincipal AgroUserPrincipal actor) {
        plantations.archive(id, actor);
    }
}
