package br.com.agroclima.integration.thingspeak;

import br.com.agroclima.config.ThingSpeakProperties;
import br.com.agroclima.dto.ClimateDtos;
import br.com.agroclima.dto.ThingSpeakDtos;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ThingSpeakFeedMapperTest {

    private final ThingSpeakProperties properties =
        new ThingSpeakProperties(true, "3499301", "read-key", "https://api.thingspeak.com", "", 300, 15, 500);
    private final ThingSpeakFeedMapper mapper = new ThingSpeakFeedMapper(properties);

    private static ThingSpeakDtos.Feed feed(long entryId, String field1, String field2) {
        return new ThingSpeakDtos.Feed(entryId, Instant.now().minusSeconds(60), field1, field2);
    }

    @Test
    void mapsField1ToTemperatureAndField2ToHumidity() {
        ThingSpeakFeedMapper.Mapping result = mapper.map(List.of(feed(10L, "24.5", "61.2")));

        assertEquals(1, result.readings().size());
        assertEquals(0, result.rejected());
        ClimateDtos.ClimateReadingInput reading = result.readings().get(0);
        assertEquals("3499301", reading.channelId());
        assertEquals(10L, reading.entryId());
        assertEquals("thingspeak-3499301", reading.sensorCode());
        assertEquals("VALID", reading.qualityStatus());
        assertEquals(2, reading.measurements().size());
        assertEquals(0, reading.measurements().get("temperature").compareTo(new BigDecimal("24.5")));
        assertEquals(0, reading.measurements().get("humidity").compareTo(new BigDecimal("61.2")));
    }

    @Test
    void outOfRangeHumidityKeepsTemperatureAndMarksReviewWithoutHumidity() {
        ThingSpeakFeedMapper.Mapping result = mapper.map(List.of(feed(11L, "25.0", "180")));

        ClimateDtos.ClimateReadingInput reading = result.readings().get(0);
        assertEquals("REVIEW", reading.qualityStatus());
        assertEquals(List.of("temperature"), List.copyOf(reading.measurements().keySet()));
    }

    @Test
    void nonNumericValueIsTreatedAsReviewNotAsZero() {
        ThingSpeakFeedMapper.Mapping result = mapper.map(List.of(feed(12L, "abc", "55")));

        ClimateDtos.ClimateReadingInput reading = result.readings().get(0);
        assertEquals("REVIEW", reading.qualityStatus());
        assertEquals(List.of("humidity"), List.copyOf(reading.measurements().keySet()));
    }

    @Test
    void rejectsFeedWithoutAnyUsableMeasurementOrWithInvalidIdentity() {
        ThingSpeakFeedMapper.Mapping result = mapper.map(List.of(
            feed(13L, "", null),
            new ThingSpeakDtos.Feed(null, Instant.now(), "20", "50"),
            new ThingSpeakDtos.Feed(14L, null, "20", "50"),
            new ThingSpeakDtos.Feed(15L, Instant.now().plusSeconds(3600), "20", "50")));

        assertTrue(result.readings().isEmpty());
        assertEquals(4, result.rejected());
    }

    @Test
    void ignoresDuplicatedEntryIdInsideSameResponse() {
        ThingSpeakFeedMapper.Mapping result = mapper.map(List.of(feed(16L, "20", "50"), feed(16L, "21", "51")));

        assertEquals(1, result.readings().size());
        assertEquals(0, result.readings().get(0).measurements().get("temperature").compareTo(new BigDecimal("20")));
    }
}
