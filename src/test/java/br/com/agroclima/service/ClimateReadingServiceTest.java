package br.com.agroclima.service;

import br.com.agroclima.dto.ClimateDtos;
import br.com.agroclima.exception.ApiException;
import br.com.agroclima.repository.ClimateReadingRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClimateReadingServiceTest {
    @Mock ClimateReadingRepository readings;
    @InjectMocks ClimateReadingService service;

    @Test
    void ingestionIsIdempotentForChannelAndEntry() {
        var input = new ClimateDtos.ClimateReadingInput("channel-1", 4L, "sensor-1", Instant.now(),
            Map.of("temperature", new BigDecimal("28.4")), "VALID");
        when(readings.existsByChannelIdAndEntryId("channel-1", 4L)).thenReturn(true);

        ClimateDtos.ClimateBatchResponse result = service.ingest(new ClimateDtos.ClimateBatchRequest(List.of(input)));

        assertEquals(1, result.received());
        assertEquals(0, result.inserted());
        assertEquals(1, result.alreadyPresent());
        verify(readings, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void rejectsHumidityOutsidePhysicalRange() {
        var input = new ClimateDtos.ClimateReadingInput("channel-1", 5L, "sensor-1", Instant.now(),
            Map.of("humidity", new BigDecimal("120")), "VALID");
        assertThrows(ApiException.class, () -> service.ingest(new ClimateDtos.ClimateBatchRequest(List.of(input))));
        verify(readings, never()).save(org.mockito.ArgumentMatchers.any());
    }
}
