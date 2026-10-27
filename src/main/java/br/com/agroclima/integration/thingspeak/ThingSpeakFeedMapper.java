package br.com.agroclima.integration.thingspeak;

import br.com.agroclima.config.ThingSpeakProperties;
import br.com.agroclima.dto.ClimateDtos;
import br.com.agroclima.dto.ThingSpeakDtos;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Converte feeds do ThingSpeak em leituras persistíveis. Usa somente o field-map validado
 * (temperature -> field1, humidity -> field2); os demais campos do canal são ignorados.
 */
@Component
public class ThingSpeakFeedMapper {
    static final Duration FUTURE_TOLERANCE = Duration.ofMinutes(5);
    private static final int SCALE = 4;
    private static final Map<String, BigDecimal[]> RANGES = Map.of(
        "temperature", new BigDecimal[] {new BigDecimal("-80"), new BigDecimal("80")},
        "humidity", new BigDecimal[] {BigDecimal.ZERO, new BigDecimal("100")});

    private final ThingSpeakProperties properties;

    public ThingSpeakFeedMapper(ThingSpeakProperties properties) {
        this.properties = properties;
    }

    public record Mapping(List<ClimateDtos.ClimateReadingInput> readings, int rejected) {}

    public Mapping map(List<ThingSpeakDtos.Feed> feeds) {
        Map<String, String> fieldMapping = properties.fieldMapping();
        Instant limit = Instant.now().plus(FUTURE_TOLERANCE);
        Set<Long> seenEntries = new HashSet<>();
        List<ClimateDtos.ClimateReadingInput> readings = new ArrayList<>();
        int rejected = 0;

        for (ThingSpeakDtos.Feed feed : feeds) {
            if (feed == null || feed.entryId() == null || feed.entryId() <= 0
                || feed.createdAt() == null || feed.createdAt().isAfter(limit)) {
                rejected++;
                continue;
            }
            if (!seenEntries.add(feed.entryId())) {
                continue; // mesma entry_id repetida na resposta: mantém a primeira
            }
            Map<String, BigDecimal> measurements = new LinkedHashMap<>();
            boolean outOfRange = false;
            for (Map.Entry<String, String> mapping : fieldMapping.entrySet()) {
                String raw = rawValue(feed, mapping.getValue());
                if (raw == null || raw.isBlank()) continue;
                BigDecimal value = parse(raw);
                if (value == null || !inRange(mapping.getKey(), value)) {
                    outOfRange = true;
                    continue;
                }
                measurements.put(mapping.getKey(), value);
            }
            if (measurements.isEmpty()) {
                rejected++;
                continue;
            }
            readings.add(new ClimateDtos.ClimateReadingInput(
                properties.channelId(),
                feed.entryId(),
                "thingspeak-" + properties.channelId(),
                feed.createdAt(),
                measurements,
                outOfRange ? "REVIEW" : "VALID"));
        }
        return new Mapping(readings, rejected);
    }

    private static String rawValue(ThingSpeakDtos.Feed feed, String field) {
        return switch (field) {
            case "field1" -> feed.field1();
            case "field2" -> feed.field2();
            default -> null;
        };
    }

    private static BigDecimal parse(String raw) {
        try {
            return new BigDecimal(raw.trim()).setScale(SCALE, RoundingMode.HALF_UP);
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private static boolean inRange(String metric, BigDecimal value) {
        BigDecimal[] range = RANGES.get(metric);
        return range == null || (value.compareTo(range[0]) >= 0 && value.compareTo(range[1]) <= 0);
    }
}
