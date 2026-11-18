package com.temipora.backend_springboot.servicios.opentimestamps;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Cliente del protocolo de calendario de OpenTimestamps.
 *
 * - {@code POST {calendario}/digest} con el digest (32 bytes) devuelve un sello
 *   parcial que termina en una atestacion pendiente.
 * - {@code GET {uri}/timestamp/{commitment}} devuelve el sello ya anclado en
 *   Bitcoin (con la atestacion de bloque) cuando el lote fue minado.
 *
 * No usa librerias de terceros: habla HTTP directo con los calendarios publicos.
 */
@Component
public class OpenTimestampsClient {

    private static final Logger log = LoggerFactory.getLogger(OpenTimestampsClient.class);
    private static final String ACEPTA = "application/vnd.opentimestamps.v1";

    private final RestClient restClient;

    public OpenTimestampsClient() {
        HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(httpClient);
        factory.setReadTimeout(Duration.ofSeconds(15));
        this.restClient = RestClient.builder().requestFactory(factory).build();
    }

    /** Envia el digest al calendario y devuelve el sello parcial (.ots sin header). */
    public Optional<byte[]> sellar(String calendarioUrl, byte[] digest) {
        try {
            byte[] cuerpo = restClient.post()
                .uri(calendarioUrl + "/digest")
                .header(HttpHeaders.ACCEPT, ACEPTA)
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(digest)
                .retrieve()
                .body(byte[].class);
            return Optional.ofNullable(cuerpo);
        } catch (Exception e) {
            log.warn("No se pudo sellar en el calendario {}: {}", calendarioUrl, e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Pide al calendario la version actualizada del sello para un compromiso.
     * Devuelve 404 mientras siga pendiente; el cuerpo cuando ya esta en Bitcoin.
     */
    public Optional<byte[]> actualizar(String uri, byte[] compromiso) {
        String url = uri + "/timestamp/" + OtsTimestamp.hex(compromiso);
        try {
            byte[] cuerpo = restClient.get()
                .uri(url)
                .header(HttpHeaders.ACCEPT, ACEPTA)
                .retrieve()
                .body(byte[].class);
            return Optional.ofNullable(cuerpo);
        } catch (Exception e) {
            log.debug("El sello de {} todavia no esta confirmado: {}", url, e.getMessage());
            return Optional.empty();
        }
    }
}
