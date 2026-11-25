package br.com.agroclima.service;

import br.com.agroclima.domain.ClimateReadingEntity;
import br.com.agroclima.dto.ClimateDtos;
import br.com.agroclima.exception.ApiException;
import br.com.agroclima.mapper.ClimateReadingMapper;
import br.com.agroclima.repository.ClimateReadingRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
public class ClimateReadingService {
    private static final Set<String> QUALITY_STATUSES = Set.of("VALID", "REVIEW", "REJECTED");
    private final ClimateReadingRepository readings;

    public ClimateReadingService(ClimateReadingRepository readings) { this.readings = readings; }

    @Transactional
    public ClimateDtos.ClimateBatchResponse ingest(ClimateDtos.ClimateBatchRequest request) {
        int inserted = 0;
        int alreadyPresent = 0;
        for (ClimateDtos.ClimateReadingInput input : request.readings()) {
            validate(input);
            String status = input.qualityStatus() == null || input.qualityStatus().isBlank()
                ? "VALID" : input.qualityStatus().trim().toUpperCase(Locale.ROOT);
            if (!QUALITY_STATUSES.contains(status)) throw ApiException.badRequest("Status de qualidade climática inválido.");
            if (readings.existsByChannelIdAndEntryId(input.channelId().trim(), input.entryId())) {
                alreadyPresent++;
                continue;
            }
            readings.save(new ClimateReadingEntity(input.channelId().trim(), input.entryId(), input.sensorCode(),
                input.capturedAt(), normalizeMeasurements(input.measurements()), status));
            inserted++;
        }
        return new ClimateDtos.ClimateBatchResponse(request.readings().size(), inserted, alreadyPresent);
    }

    @Transactional(readOnly = true)
    public List<ClimateDtos.ClimateReadingResponse> history(Instant from, Instant to, String channelId) {
        if (from == null || to == null || !from.isBefore(to)) {
            throw ApiException.badRequest("Informe um intervalo válido: início anterior ao fim.");
        }
        List<ClimateReadingEntity> result = channelId == null || channelId.isBlank()
            ? readings.findTop5000ByCapturedAtBetweenOrderByCapturedAtAsc(from, to)
            : readings.findTop5000ByChannelIdAndCapturedAtBetweenOrderByCapturedAtAsc(channelId.trim(), from, to);
        return result.stream().map(ClimateReadingMapper::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<ClimateDtos.ClimateReadingResponse> latest() {
        return readings.findTop500ByOrderByCapturedAtDesc().stream().map(ClimateReadingMapper::toResponse).toList();
    }

    private void validate(ClimateDtos.ClimateReadingInput input) {
        if (input.channelId() == null || input.channelId().isBlank() || input.channelId().trim().length() > 100) {
            throw ApiException.badRequest("Canal ThingSpeak inválido.");
        }
        if (input.entryId() == null || input.entryId() <= 0) throw ApiException.badRequest("Identificador de registro inválido.");
        if (input.capturedAt() == null) throw ApiException.badRequest("Data de captura obrigatória.");
        if (input.measurements() == null || input.measurements().isEmpty()) throw ApiException.badRequest("Informe ao menos uma medição.");
        if (input.capturedAt().isAfter(Instant.now().plusSeconds(300))) throw ApiException.badRequest("A data de captura não pode estar no futuro.");
        normalizeMeasurements(input.measurements());
    }

    private Map<String, BigDecimal> normalizeMeasurements(Map<String, BigDecimal> values) {
        java.util.LinkedHashMap<String, BigDecimal> normalized = new java.util.LinkedHashMap<>();
        values.forEach((rawKey, value) -> {
            String key = rawKey == null ? "" : rawKey.trim();
            if (!key.matches("[A-Za-z][A-Za-z0-9_]{0,63}")) throw ApiException.badRequest("Nome de medição inválido.");
            if (value == null || value.precision() > 12 || Math.abs(value.scale()) > 4) {
                throw ApiException.badRequest("Valor de medição inválido.");
            }
            String canonical = key.toLowerCase(Locale.ROOT).replace("_", "");
            validateKnownRange(canonical, value);
            normalized.put(key, value.stripTrailingZeros());
        });
        if (normalized.isEmpty()) throw ApiException.badRequest("Informe ao menos uma medição numérica.");
        return Map.copyOf(normalized);
    }

    private void validateKnownRange(String key, BigDecimal value) {
        BigDecimal zero = BigDecimal.ZERO;
        if ((key.equals("humidity") || key.equals("soilhumidity") || key.equals("relativehumidity"))
            && (value.compareTo(zero) < 0 || value.compareTo(new BigDecimal("100")) > 0)) {
            throw ApiException.badRequest("Umidade deve estar entre 0 e 100.");
        }
        if ((key.equals("temperature") || key.equals("temperaturec") || key.equals("tempc"))
            && (value.compareTo(new BigDecimal("-80")) < 0 || value.compareTo(new BigDecimal("80")) > 0)) {
            throw ApiException.badRequest("Temperatura fora da faixa física aceita.");
        }
        if (key.contains("rain") || key.equals("precipitation")) {
            if (value.compareTo(zero) < 0) throw ApiException.badRequest("A precipitação não pode ser negativa.");
        }
        if ((key.equals("luminosity") || key.equals("light")) && value.compareTo(zero) < 0) {
            throw ApiException.badRequest("A luminosidade não pode ser negativa.");
        }
    }
}
