package br.com.agroclima.config;

import br.com.agroclima.domain.ClimateReadingEntity;
import br.com.agroclima.repository.ClimateReadingRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Seed de demonstração: ao subir a API, insere 960 leituras (24 por dia x 40 dias) em climate_reading.
 *
 * <p>Cada dia local tem as 24 horas, então conta como "dia completo" (mínimo de 18 h) para o módulo analítico.
 * Os dias terminam em ontem (fuso America/Recife, UTC-3). É idempotente: usa entry_id de 9000001 a 9000960
 * (faixa alta, sem colidir com os entry_id reais do ThingSpeak) e só insere o que ainda não existe.
 *
 * <p>Desligue com {@code SEED_CLIMATE_ENABLED=false} (app.seed.climate.enabled=false). Dados fictícios: não use em produção.
 * Para remover: {@code DELETE FROM climate_reading WHERE entry_id BETWEEN 9000001 AND 9000960;}
 */
@Component
@ConditionalOnProperty(prefix = "app.seed.climate", name = "enabled", havingValue = "true")
public class ClimateSeedRunner implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(ClimateSeedRunner.class);

    static final String DEFAULT_CHANNEL_ID = "3499301";
    static final long ENTRY_ID_START = 9_000_001L;
    static final int DAYS = 1095;
    static final int HOURS_PER_DAY = 24;
    static final ZoneId LOCAL_ZONE = ZoneId.of("America/Recife");

    private final ClimateReadingRepository readings;
    private final String channelId;

    public ClimateSeedRunner(ClimateReadingRepository readings, ThingSpeakProperties thingSpeak) {
        this.readings = readings;
        this.channelId = thingSpeak.channelId().isBlank() ? DEFAULT_CHANNEL_ID : thingSpeak.channelId();
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        long expected = (long) DAYS * HOURS_PER_DAY;
        long entryEnd = ENTRY_ID_START + expected - 1;
        long existing = readings.countByChannelIdAndEntryIdBetween(channelId, ENTRY_ID_START, entryEnd);
        if (existing >= expected) {
            log.info("Seed de leituras climáticas já existe ({} leituras); nenhuma alteração foi feita.", existing);
            return;
        }

        LocalDate lastDay = LocalDate.now(LOCAL_ZONE).minusDays(1);
        LocalDate firstDay = lastDay.minusDays(DAYS - 1L);
        Random random = new Random(42);
        List<ClimateReadingEntity> batch = new ArrayList<>();
        double anomaly = 0.0;
        long entryId = ENTRY_ID_START;

        for (int d = 0; d < DAYS; d++) {
            LocalDate day = firstDay.plusDays(d);
            // "tempo" do dia: anomalia que persiste de um dia para o outro + leve aquecimento sazonal
            anomaly = 0.65 * anomaly + random.nextGaussian() * 1.3;
            double meanTemp = 28.5 + 0.04 * d + anomaly;
            double amplitude = 5.0 + (random.nextDouble() * 2.0 - 0.8);
            double meanHumidity = clamp(52.0 - 3.5 * anomaly + random.nextGaussian() * 3.0, 30.0, 75.0);
            for (int hour = 0; hour < HOURS_PER_DAY; hour++, entryId++) {
                // pico de temperatura ~14h30, mínimo ~02h30
                double diurnal = Math.cos(2 * Math.PI * (hour - 14.5) / 24.0);
                double temperature = clamp(meanTemp + amplitude * diurnal + random.nextGaussian() * 0.35, 15.0, 42.0);
                double humidity = clamp(meanHumidity - 2.6 * amplitude * diurnal + random.nextGaussian() * 1.8, 12.0, 98.0);
                if (existing > 0 && readings.existsByChannelIdAndEntryId(channelId, entryId)) {
                    continue;
                }
                Map<String, BigDecimal> measurements = new LinkedHashMap<>();
                measurements.put("temperature", BigDecimal.valueOf(temperature).setScale(2, RoundingMode.HALF_UP));
                measurements.put("humidity", BigDecimal.valueOf(humidity).setScale(2, RoundingMode.HALF_UP));
                Instant capturedAt = day.atTime(hour, 0).atZone(LOCAL_ZONE).toInstant();
                batch.add(new ClimateReadingEntity(channelId, entryId, "thingspeak-" + channelId,
                    capturedAt, measurements, "VALID"));
            }
        }
        readings.saveAll(batch);
        log.info("Seed de leituras climáticas criado: {} leituras, de {} a {} (fuso {}).",
            batch.size(), firstDay, lastDay, LOCAL_ZONE);
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}
