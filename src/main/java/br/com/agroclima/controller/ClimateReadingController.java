package br.com.agroclima.controller;

import br.com.agroclima.dto.ClimateDtos;
import br.com.agroclima.service.ClimateReadingService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@RestController
@RequestMapping("/api/climate/readings")
@PreAuthorize("hasAuthority('monitoring.view')")
public class ClimateReadingController {
    private final ClimateReadingService readings;

    public ClimateReadingController(ClimateReadingService readings) { this.readings = readings; }

    @GetMapping
    public List<ClimateDtos.ClimateReadingResponse> history(
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
        @RequestParam(required = false) String channelId
    ) {
        if (from == null && to == null) {
            Instant now = Instant.now();
            return readings.history(now.minus(24, ChronoUnit.HOURS), now, channelId);
        }
        return readings.history(from, to, channelId);
    }
}
