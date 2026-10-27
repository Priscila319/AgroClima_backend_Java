package br.com.agroclima.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/** Contratos do ThingSpeak (entrada) e da integração exposta ao Python e ao front (saída). */
public final class ThingSpeakDtos {
    private ThingSpeakDtos() {}

    /** Resposta de GET /channels/{id}/feeds.json. Campos desconhecidos são ignorados. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record FeedsResponse(Channel channel, List<Feed> feeds) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Channel(Long id) {}

    /** Somente field1 (temperatura) e field2 (umidade) são lidos pelo backend. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Feed(
        @JsonProperty("entry_id") Long entryId,
        @JsonProperty("created_at") Instant createdAt,
        @JsonProperty("field1") String field1,
        @JsonProperty("field2") String field2
    ) {}

    public record SyncResult(
        boolean configured,
        String channelId,
        int received,
        int normalized,
        int rejected,
        int inserted,
        int alreadyPresent,
        Instant synchronizedAt,
        String message
    ) {}

    public record Status(
        boolean configured,
        String channelId,
        boolean readKeyConfigured,
        boolean schedulerEnabled,
        long pollIntervalSeconds,
        Map<String, String> fieldMap,
        Instant lastSyncAt,
        String lastSyncError
    ) {}
}
