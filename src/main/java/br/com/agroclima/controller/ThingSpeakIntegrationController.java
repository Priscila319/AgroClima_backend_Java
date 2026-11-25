package br.com.agroclima.controller;

import br.com.agroclima.dto.ThingSpeakDtos;
import br.com.agroclima.service.ThingSpeakSyncService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

/** Estado e sincronização manual do ThingSpeak. Restrito a ADMIN, como a regra anterior do Python. */
@RestController
@RequestMapping("/api/integrations/thingspeak")
@PreAuthorize("hasRole('ADMIN')")
public class ThingSpeakIntegrationController {
    private final ThingSpeakSyncService sync;

    public ThingSpeakIntegrationController(ThingSpeakSyncService sync) {
        this.sync = sync;
    }

    @GetMapping("/status")
    public ThingSpeakDtos.Status status() {
        return sync.status();
    }

    @PostMapping("/sync")
    public ThingSpeakDtos.SyncResult sync(
        @RequestParam(name = "from", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
        @RequestParam(name = "to", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to
    ) {
        return sync.sync(from, to);
    }
}
