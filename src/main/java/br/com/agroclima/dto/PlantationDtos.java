package br.com.agroclima.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.UUID;

public final class PlantationDtos {
    private PlantationDtos() {}

    public record CreatePlantationRequest(
        @NotBlank @Size(max = 100) String variety,
        @NotNull @Positive Integer quantity,
        @Size(max = 80) String field,
        @Size(max = 500) String notes
    ) {}

    public record UpdatePlantationRequest(
        @NotBlank @Size(max = 100) String variety,
        @NotNull @Positive Integer quantity,
        @Size(max = 80) String field,
        @Size(max = 500) String notes
    ) {}

    public record PlantationResponse(UUID id, UUID ownerId, String ownerName, String variety, int quantity,
                                    String field, String notes, String status, Instant plantedAt,
                                    Instant harvestedAt, Instant createdAt, Instant archivedAt) {}
}
