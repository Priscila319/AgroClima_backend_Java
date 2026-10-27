package br.com.agroclima.integration.thingspeak;

import br.com.agroclima.config.ThingSpeakProperties;
import br.com.agroclima.dto.ThingSpeakDtos;
import br.com.agroclima.exception.ApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Cliente HTTP do ThingSpeak. Só é chamado quando app.thingspeak.enabled=true (via sync manual ou agendador).
 * A Read API Key nunca é registrada em log nem incluída em mensagens de erro.
 */
@Component
public class ThingSpeakClient {
    private static final Logger log = LoggerFactory.getLogger(ThingSpeakClient.class);
    private static final DateTimeFormatter THINGSPEAK_DATE =
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneOffset.UTC);
    static final int MAX_RESULTS = 8000;

    private final ThingSpeakProperties properties;
    private final RestClient restClient;

    public ThingSpeakClient(ThingSpeakProperties properties, RestClient.Builder builder) {
        this.properties = properties;
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(5));
        requestFactory.setReadTimeout(Duration.ofSeconds(15));
        this.restClient = builder.baseUrl(properties.baseUrl()).requestFactory(requestFactory).build();
    }

    /**
     * Busca feeds do canal. Sem start/end, o ThingSpeak devolve os últimos {@code results} registros.
     * Datas são enviadas em UTC no formato exigido pela API: yyyy-MM-dd HH:mm:ss.
     */
    public List<ThingSpeakDtos.Feed> fetchFeeds(Instant start, Instant end, int results) {
        String channelId = properties.channelId();
        int limit = Math.min(Math.max(results, 1), MAX_RESULTS);
        ThingSpeakDtos.FeedsResponse body;
        try {
            body = restClient.get()
                .uri(uriBuilder -> {
                    uriBuilder.path("/channels/{channelId}/feeds.json").queryParam("results", limit);
                    if (properties.readKeyConfigured()) uriBuilder.queryParam("api_key", properties.readApiKey());
                    if (start != null) uriBuilder.queryParam("start", THINGSPEAK_DATE.format(start));
                    if (end != null) uriBuilder.queryParam("end", THINGSPEAK_DATE.format(end));
                    return uriBuilder.build(channelId);
                })
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .body(ThingSpeakDtos.FeedsResponse.class);
        } catch (RestClientResponseException exception) {
            throw translate(exception);
        } catch (ResourceAccessException exception) {
            // A mensagem da exceção pode conter a URL com api_key: registra apenas o tipo.
            log.warn("Falha de conexão com o ThingSpeak ({})", exception.getCause() == null
                ? exception.getClass().getSimpleName() : exception.getCause().getClass().getSimpleName());
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "Não foi possível conectar ao ThingSpeak.");
        } catch (RestClientException exception) {
            log.warn("Resposta inesperada do ThingSpeak ({})", exception.getClass().getSimpleName());
            throw new ApiException(HttpStatus.BAD_GATEWAY, "Resposta inválida do ThingSpeak.");
        }

        if (body == null || body.feeds() == null) {
            throw new ApiException(HttpStatus.BAD_GATEWAY, "Resposta inválida do ThingSpeak.");
        }
        if (body.channel() != null && body.channel().id() != null
            && !channelId.equals(String.valueOf(body.channel().id()))) {
            throw new ApiException(HttpStatus.BAD_GATEWAY, "A resposta do ThingSpeak não corresponde ao canal configurado.");
        }
        return body.feeds();
    }

    private ApiException translate(RestClientResponseException exception) {
        int status = exception.getStatusCode().value();
        log.warn("ThingSpeak respondeu HTTP {}", status);
        if (status == 401 || status == 403) {
            return new ApiException(HttpStatus.BAD_GATEWAY, "O ThingSpeak recusou o acesso; verifique THINGSPEAK_READ_API_KEY.");
        }
        if (status == 404) {
            return new ApiException(HttpStatus.BAD_GATEWAY, "O canal ThingSpeak configurado não foi encontrado.");
        }
        if (status == 429) {
            return new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "O ThingSpeak limitou as requisições; tente novamente em instantes.");
        }
        return new ApiException(HttpStatus.BAD_GATEWAY, "O ThingSpeak não conseguiu fornecer as leituras.");
    }
}
