package br.com.agroclima.controller;

import br.com.agroclima.dto.ClimateDtos;
import br.com.agroclima.service.ClimateReadingService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/internal/climate-readings")
@PreAuthorize("hasRole('SERVICE')")
public class InternalClimateReadingController {
    private final ClimateReadingService readings;

    public InternalClimateReadingController(ClimateReadingService readings) { this.readings = readings; }

    @PostMapping("/batch")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public ClimateDtos.ClimateBatchResponse ingest(@Valid @RequestBody ClimateDtos.ClimateBatchRequest request) {
        return readings.ingest(request);
    }

    @GetMapping
    public List<ClimateDtos.ClimateReadingResponse> history(
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
        @RequestParam(required = false) String channelId
    ) {
        return readings.history(from, to, channelId);
    }
}
