package br.com.agroclima.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.UUID;

public final class AuditDtos {
    private AuditDtos() {}

    public record AuditEventResponse(UUID id, String user, String email, String action, String resource,
                                     Instant occurredAt, String source, String status, String details) {}

    public record CreateAuditEventRequest(@NotBlank @Size(max = 160) String action,
                                          @NotBlank @Size(max = 160) String resource,
                                          @Size(max = 4000) String details) {}
}
