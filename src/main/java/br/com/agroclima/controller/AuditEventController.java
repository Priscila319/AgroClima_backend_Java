package br.com.agroclima.controller;

import br.com.agroclima.dto.AuditDtos;
import br.com.agroclima.security.AgroUserPrincipal;
import br.com.agroclima.service.AuditService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/audit-events")
public class AuditEventController {
    private final AuditService audit;

    public AuditEventController(AuditService audit) { this.audit = audit; }

    @GetMapping
    @PreAuthorize("hasAuthority('logs.read')")
    public List<AuditDtos.AuditEventResponse> latest() { return audit.latest(); }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("isAuthenticated()")
    public void record(@Valid @RequestBody AuditDtos.CreateAuditEventRequest request,
                       @AuthenticationPrincipal AgroUserPrincipal actor) {
        audit.record(actor, request.action(), request.resource(), request.details());
    }
}
