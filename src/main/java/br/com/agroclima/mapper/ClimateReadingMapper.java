package br.com.agroclima.mapper;

import br.com.agroclima.domain.ClimateReadingEntity;
import br.com.agroclima.dto.ClimateDtos;

public final class ClimateReadingMapper {
    private ClimateReadingMapper() {}

    public static ClimateDtos.ClimateReadingResponse toResponse(ClimateReadingEntity reading) {
        return new ClimateDtos.ClimateReadingResponse(reading.getId(), reading.getChannelId(), reading.getEntryId(),
            reading.getSensorCode(), reading.getCapturedAt(), reading.getMeasurements(), reading.getQualityStatus());
    }
}
