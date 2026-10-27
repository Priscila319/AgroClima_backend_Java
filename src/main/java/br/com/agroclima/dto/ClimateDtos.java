package br.com.agroclima.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class ClimateDtos {
    private ClimateDtos() {}

    public record ClimateReadingInput(
        @NotBlank @Size(max = 100) String channelId,
        @NotNull @Positive Long entryId,
        @Size(max = 100) String sensorCode,
        @NotNull Instant capturedAt,
        @NotEmpty Map<@NotBlank @Size(max = 64) String, @NotNull BigDecimal> measurements,
        @Size(max = 24) String qualityStatus
    ) {}

    public record ClimateBatchRequest(@NotEmpty @Size(max = 1000) List<@NotNull @Valid ClimateReadingInput> readings) {}

    public record ClimateReadingResponse(UUID id, String channelId, Long entryId, String sensorCode,
                                         Instant capturedAt, Map<String, BigDecimal> measurements,
                                         String qualityStatus) {}

    public record ClimateBatchResponse(int received, int inserted, int alreadyPresent) {}
}
