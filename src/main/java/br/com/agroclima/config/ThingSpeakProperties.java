package br.com.agroclima.config;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Configuração do canal ThingSpeak consumido diretamente pelo backend Java.
 *
 * <p>O canal possui apenas dois campos ativos: field1 = temperatura (°C) e field2 = umidade (%).
 * O mapeamento é validado na inicialização; qualquer outro valor impede o boot com mensagem clara.
 */
@ConfigurationProperties(prefix = "app.thingspeak")
public record ThingSpeakProperties(
    boolean enabled,
    String channelId,
    String readApiKey,
    String baseUrl,
    String fieldMap,
    long pollIntervalSeconds,
    long initialDelaySeconds,
    int resultsLimit
) {
    static final String DEFAULT_FIELD_MAP = "{\"temperature\":\"field1\",\"humidity\":\"field2\"}";
    private static final String DEFAULT_BASE_URL = "https://api.thingspeak.com";
    private static final Map<String, String> REQUIRED_MAPPING = Map.of("temperature", "field1", "humidity", "field2");
    private static final Pattern NUMERIC_CHANNEL = Pattern.compile("[0-9]{1,12}");
    private static final ObjectMapper MAPPER = new ObjectMapper();

    public ThingSpeakProperties {
        channelId = channelId == null ? "" : channelId.trim();
        readApiKey = readApiKey == null ? "" : readApiKey.trim();
        baseUrl = (baseUrl == null || baseUrl.isBlank()) ? DEFAULT_BASE_URL : stripTrailingSlash(baseUrl.trim());
        fieldMap = (fieldMap == null || fieldMap.isBlank()) ? DEFAULT_FIELD_MAP : fieldMap.trim();
        if (pollIntervalSeconds == 0) pollIntervalSeconds = 300;
        if (initialDelaySeconds < 0) throw new IllegalStateException("THINGSPEAK_INITIAL_DELAY_SECONDS não pode ser negativo.");
        if (resultsLimit == 0) resultsLimit = 500;

        if (!baseUrl.startsWith("https://") && !baseUrl.startsWith("http://")) {
            throw new IllegalStateException("THINGSPEAK_URL deve começar com http:// ou https://.");
        }
        if (!channelId.isEmpty() && !NUMERIC_CHANNEL.matcher(channelId).matches()) {
            throw new IllegalStateException("THINGSPEAK_CHANNEL_ID deve conter apenas números.");
        }
        if (enabled && channelId.isEmpty()) {
            throw new IllegalStateException("THINGSPEAK_ENABLED=true exige THINGSPEAK_CHANNEL_ID.");
        }
        if (pollIntervalSeconds < 15 || pollIntervalSeconds > 86_400) {
            throw new IllegalStateException("THINGSPEAK_POLL_INTERVAL_SECONDS deve estar entre 15 e 86400.");
        }
        if (resultsLimit < 1 || resultsLimit > 8000) {
            throw new IllegalStateException("THINGSPEAK_RESULTS_LIMIT deve estar entre 1 e 8000.");
        }
        validateFieldMap(fieldMap);
    }

    public boolean configured() {
        return !channelId.isEmpty();
    }

    public boolean readKeyConfigured() {
        return !readApiKey.isEmpty();
    }

    /** Métrica -> campo do canal, já validado (temperature -> field1, humidity -> field2). */
    public Map<String, String> fieldMapping() {
        return parseFieldMap(fieldMap);
    }

    private static void validateFieldMap(String json) {
        if (!REQUIRED_MAPPING.equals(parseFieldMap(json))) {
            throw new IllegalStateException(
                "THINGSPEAK_FIELD_MAP deve ser exatamente {\"temperature\":\"field1\",\"humidity\":\"field2\"}. "
                    + "O canal possui apenas temperatura (field1) e umidade (field2).");
        }
    }

    private static Map<String, String> parseFieldMap(String json) {
        try {
            Map<String, String> parsed = MAPPER.readValue(json, new TypeReference<LinkedHashMap<String, String>>() {});
            if (parsed == null) throw new IllegalStateException("THINGSPEAK_FIELD_MAP deve ser um objeto JSON.");
            Map<String, String> normalized = new LinkedHashMap<>();
            for (Map.Entry<String, String> entry : parsed.entrySet()) {
                if (entry.getKey() == null || entry.getValue() == null) {
                    throw new IllegalStateException("THINGSPEAK_FIELD_MAP não aceita chaves ou valores nulos.");
                }
                normalized.put(entry.getKey().trim(), entry.getValue().trim().toLowerCase(Locale.ROOT));
            }
            return normalized;
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("THINGSPEAK_FIELD_MAP deve ser um JSON válido.", exception);
        }
    }

    private static String stripTrailingSlash(String value) {
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }
}
