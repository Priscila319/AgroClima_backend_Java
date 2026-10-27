package br.com.agroclima.integration.thingspeak;

import br.com.agroclima.config.ThingSpeakProperties;
import br.com.agroclima.dto.ClimateDtos;
import br.com.agroclima.dto.ThingSpeakDtos;
import br.com.agroclima.exception.ApiException;
import br.com.agroclima.service.ClimateReadingService;
import br.com.agroclima.service.ThingSpeakSyncService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ThingSpeakSyncServiceTest {
    @Mock ThingSpeakClient client;
    @Mock ClimateReadingService readings;

    private ThingSpeakProperties properties;
    private ThingSpeakSyncService service;

    @BeforeEach
    void setUp() {
        properties = new ThingSpeakProperties(true, "3499301", "read-key", "https://api.thingspeak.com", "", 300, 15, 500);
        service = new ThingSpeakSyncService(properties, client, new ThingSpeakFeedMapper(properties), readings);
    }

    @Test
    void doesNothingWhenChannelIsNotConfigured() {
        ThingSpeakProperties disabled = new ThingSpeakProperties(false, "", "", "", "", 300, 15, 500);
        ThingSpeakSyncService unconfigured = new ThingSpeakSyncService(disabled, client,
            new ThingSpeakFeedMapper(disabled), readings);

        ThingSpeakDtos.SyncResult result = unconfigured.syncLatest();

        assertFalse(result.configured());
        assertEquals(0, result.inserted());
        verifyNoInteractions(client, readings);
    }

    @Test
    void persistsFeedsInBatchesOf500ThroughTheSameIngestionService() {
        List<ThingSpeakDtos.Feed> feeds = new ArrayList<>();
        for (int i = 1; i <= 1200; i++) {
            feeds.add(new ThingSpeakDtos.Feed((long) i, Instant.now().minusSeconds(i * 60L), "22.5", "60.0"));
        }
        when(client.fetchFeeds(null, null, 500)).thenReturn(feeds);
        when(readings.ingest(any())).thenAnswer(invocation -> {
            ClimateDtos.ClimateBatchRequest request = invocation.getArgument(0);
            return new ClimateDtos.ClimateBatchResponse(request.readings().size(), request.readings().size(), 0);
        });

        ThingSpeakDtos.SyncResult result = service.syncLatest();

        ArgumentCaptor<ClimateDtos.ClimateBatchRequest> captor = ArgumentCaptor.forClass(ClimateDtos.ClimateBatchRequest.class);
        verify(readings, times(3)).ingest(captor.capture());
        List<ClimateDtos.ClimateBatchRequest> sent = captor.getAllValues();
        assertEquals(500, sent.get(0).readings().size());
        assertEquals(500, sent.get(1).readings().size());
        assertEquals(200, sent.get(2).readings().size());
        assertTrue(result.configured());
        assertEquals(1200, result.received());
        assertEquals(1200, result.inserted());
        assertEquals(0, result.rejected());
        assertEquals("3499301", result.channelId());
        assertNotNull(service.status().lastSyncAt());
    }

    @Test
    void upstreamErrorIsPropagatedAndStoredAsLastError() {
        when(client.fetchFeeds(null, null, 500))
            .thenThrow(new ApiException(HttpStatus.BAD_GATEWAY, "O ThingSpeak não conseguiu fornecer as leituras."));

        ApiException error = assertThrows(ApiException.class, service::syncLatest);

        assertEquals(HttpStatus.BAD_GATEWAY, error.getStatus());
        assertEquals("O ThingSpeak não conseguiu fornecer as leituras.", service.status().lastSyncError());
        verify(readings, never()).ingest(any());
    }

    @Test
    void rejectsIncompleteOrOversizedWindows() {
        Instant start = Instant.parse("2026-10-01T00:00:00Z");

        assertThrows(ApiException.class, () -> service.sync(start, null));
        assertThrows(ApiException.class, () -> service.sync(start, start));
        assertThrows(ApiException.class, () -> service.sync(start, start.plusSeconds(367L * 86_400L)));
        verifyNoInteractions(client);
    }

    @Test
    void statusNeverExposesTheReadKey() {
        ThingSpeakDtos.Status status = service.status();

        assertEquals("3499301", status.channelId());
        assertTrue(status.readKeyConfigured());
        assertEquals("field1", status.fieldMap().get("temperature"));
        assertEquals("field2", status.fieldMap().get("humidity"));
        assertFalse(status.toString().contains("read-key"));
    }
}
