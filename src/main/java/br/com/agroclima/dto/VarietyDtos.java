package br.com.agroclima.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public final class VarietyDtos {
    private VarietyDtos() {}

    public record VarietyResponse(UUID id, String name, String category, boolean active) {}
    public record CreateVarietyRequest(@NotBlank @Size(max = 100) String name,
                                       @Size(max = 100) String category) {}
    public record ActiveRequest(boolean active) {}
}
