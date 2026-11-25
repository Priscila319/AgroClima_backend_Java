package br.com.agroclima.integration.thingspeak;

import br.com.agroclima.exception.ApiException;
import br.com.agroclima.service.ThingSpeakSyncService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Sincroniza o canal periodicamente. Intervalo em segundos via THINGSPEAK_POLL_INTERVAL_SECONDS
 * (convertido para milissegundos pela expressão SpEL). Falhas são registradas e não encerram o agendador.
 */
@Component
@ConditionalOnProperty(prefix = "app.thingspeak", name = "enabled", havingValue = "true")
public class ThingSpeakScheduler {
    private static final Logger log = LoggerFactory.getLogger(ThingSpeakScheduler.class);

    private final ThingSpeakSyncService sync;

    public ThingSpeakScheduler(ThingSpeakSyncService sync) {
        this.sync = sync;
    }

    @Scheduled(
        fixedDelayString = "#{${app.thingspeak.poll-interval-seconds:300} * 1000}",
        initialDelayString = "#{${app.thingspeak.initial-delay-seconds:15} * 1000}")
    public void synchronizeLatest() {
        try {
            sync.syncLatest();
        } catch (ApiException exception) {
            log.warn("Sincronização ThingSpeak falhou: {}", exception.getMessage());
        } catch (RuntimeException exception) {
            sync.recordFailure("Falha inesperada na sincronização.");
            log.error("Falha inesperada na sincronização ThingSpeak ({})", exception.getClass().getSimpleName());
        }
    }
}
