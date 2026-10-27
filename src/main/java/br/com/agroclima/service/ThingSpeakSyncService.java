package br.com.agroclima.service;

import br.com.agroclima.config.ThingSpeakProperties;
import br.com.agroclima.dto.ClimateDtos;
import br.com.agroclima.dto.ThingSpeakDtos;
import br.com.agroclima.exception.ApiException;
import br.com.agroclima.integration.thingspeak.ThingSpeakClient;
import br.com.agroclima.integration.thingspeak.ThingSpeakFeedMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Orquestra a sincronização ThingSpeak -> MySQL. Usado pelo agendador e pelo endpoint manual (ADMIN).
 * Persiste pelo mesmo ClimateReadingService usado pelo ingest interno, garantindo a mesma validação e deduplicação.
 */
@Service
public class ThingSpeakSyncService {
    private static final Logger log = LoggerFactory.getLogger(ThingSpeakSyncService.class);
    static final int INGEST_BATCH_SIZE = 500;
    private static final Duration MAX_HISTORY_WINDOW = Duration.ofDays(366);

    private final ThingSpeakProperties properties;
    private final ThingSpeakClient client;
    private final ThingSpeakFeedMapper mapper;
    private final ClimateReadingService readings;
    private final AtomicReference<Instant> lastSyncAt = new AtomicReference<>();
    private final AtomicReference<String> lastError = new AtomicReference<>();

    public ThingSpeakSyncService(ThingSpeakProperties properties, ThingSpeakClient client,
                                 ThingSpeakFeedMapper mapper, ClimateReadingService readings) {
        this.properties = properties;
        this.client = client;
        this.mapper = mapper;
        this.readings = readings;
    }

    /** Sincroniza os últimos registros do canal (sem janela de datas). */
    public ThingSpeakDtos.SyncResult syncLatest() {
        return sync(null, null);
    }

    /** Sincroniza uma janela [start, end) em UTC; ambos nulos = últimos registros. */
    public ThingSpeakDtos.SyncResult sync(Instant start, Instant end) {
        if (!properties.configured()) {
            return new ThingSpeakDtos.SyncResult(false, null, 0, 0, 0, 0, 0, Instant.now(),
                "ThingSpeak não configurado. Defina THINGSPEAK_ENABLED=true e THINGSPEAK_CHANNEL_ID.");
        }
        validateWindow(start, end);

        List<ThingSpeakDtos.Feed> feeds;
        try {
            feeds = client.fetchFeeds(start, end, properties.resultsLimit());
        } catch (ApiException exception) {
            lastError.set(exception.getMessage());
            throw exception;
        }

        ThingSpeakFeedMapper.Mapping mapping = mapper.map(feeds);
        int inserted = 0;
        int alreadyPresent = 0;
        for (List<ClimateDtos.ClimateReadingInput> batch : partition(mapping.readings(), INGEST_BATCH_SIZE)) {
            ClimateDtos.ClimateBatchResponse response = readings.ingest(new ClimateDtos.ClimateBatchRequest(batch));
            inserted += response.inserted();
            alreadyPresent += response.alreadyPresent();
        }

        Instant now = Instant.now();
        lastSyncAt.set(now);
        lastError.set(null);
        log.info("ThingSpeak sincronizado: recebidos={}, normalizados={}, rejeitados={}, inseridos={}, existentes={}",
            feeds.size(), mapping.readings().size(), mapping.rejected(), inserted, alreadyPresent);
        return new ThingSpeakDtos.SyncResult(true, properties.channelId(), feeds.size(), mapping.readings().size(),
            mapping.rejected(), inserted, alreadyPresent, now, "Sincronização concluída.");
    }

    public ThingSpeakDtos.Status status() {
        return new ThingSpeakDtos.Status(
            properties.configured(),
            properties.configured() ? properties.channelId() : null,
            properties.readKeyConfigured(),
            properties.enabled(),
            properties.pollIntervalSeconds(),
            properties.fieldMapping(),
            lastSyncAt.get(),
            lastError.get());
    }

    public void recordFailure(String message) {
        lastError.set(message);
    }

    private static void validateWindow(Instant start, Instant end) {
        if (start == null && end == null) return;
        if (start == null || end == null || !start.isBefore(end)) {
            throw ApiException.badRequest("Informe 'from' e 'to' juntos, com início anterior ao fim.");
        }
        if (Duration.between(start, end).compareTo(MAX_HISTORY_WINDOW) > 0) {
            throw ApiException.badRequest("A janela de sincronização não pode exceder 366 dias.");
        }
    }

    private static <T> List<List<T>> partition(List<T> items, int size) {
        List<List<T>> batches = new ArrayList<>();
        for (int index = 0; index < items.size(); index += size) {
            batches.add(items.subList(index, Math.min(index + size, items.size())));
        }
        return batches;
    }
}
