package com.temipora.backend_springboot.servicios.pagos.mercadopago;

import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

@Component
public class MercadoPagoClient {

    private final RestClient restClient;
    private final String accessToken;
    private final String clientId;
    private final String clientSecret;

    @Autowired
    public MercadoPagoClient(
            @Value("${pagos.mercadopago.base-url:https://api.mercadopago.com}") String baseUrl,
            @Value("${pagos.mercadopago.access-token:}") String accessToken,
            @Value("${pagos.mercadopago.client-id:}") String clientId,
            @Value("${pagos.mercadopago.client-secret:}") String clientSecret) {
        this(builderConTimeouts(), baseUrl, accessToken, clientId, clientSecret);
    }

    private static RestClient.Builder builderConTimeouts() {
        SimpleClientHttpRequestFactory factory =
            new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5000);
        factory.setReadTimeout(15000);
        return RestClient.builder().requestFactory(factory);
    }

    MercadoPagoClient(
            RestClient.Builder builder,
            String baseUrl,
            String accessToken) {
        this(builder, baseUrl, accessToken, "", "");
    }

    MercadoPagoClient(
            RestClient.Builder builder,
            String baseUrl,
            String accessToken,
            String clientId,
            String clientSecret) {
        this.restClient = builder.baseUrl(baseUrl).build();
        this.accessToken = accessToken;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
    }

    public MercadoPagoPreferencia crearPreferencia(
            Map<String, Object> cuerpo, String claveIdempotencia) {
        return crearPreferencia(cuerpo, claveIdempotencia, accessToken);
    }

    public MercadoPagoPreferencia crearPreferencia(
            Map<String, Object> cuerpo,
            String claveIdempotencia,
            String token) {
        return ejecutar(() -> restClient.post()
            .uri("/checkout/preferences")
            .header(HttpHeaders.AUTHORIZATION, autorizacion(token))
            .header("X-Idempotency-Key", claveIdempotencia)
            .contentType(MediaType.APPLICATION_JSON)
            .body(cuerpo)
            .retrieve()
            .body(MercadoPagoPreferencia.class));
    }

    public MercadoPagoPago consultarPago(String idPago) {
        return consultarPago(idPago, accessToken);
    }

    public MercadoPagoPago consultarPago(String idPago, String token) {
        return ejecutar(() -> restClient.get()
            .uri("/v1/payments/{id}", idPago)
            .header(HttpHeaders.AUTHORIZATION, autorizacion(token))
            .retrieve()
            .body(MercadoPagoPago.class));
    }

    public List<MercadoPagoPago> buscarPagos(String externalReference) {
        return buscarPagos(externalReference, accessToken);
    }

    public List<MercadoPagoPago> buscarPagos(
            String externalReference, String token) {
        MercadoPagoBusquedaPagos busqueda = ejecutar(() -> restClient.get()
            .uri(uriBuilder -> uriBuilder.path("/v1/payments/search")
                .queryParam("external_reference", externalReference)
                .queryParam("sort", "date_created")
                .queryParam("criteria", "desc")
                .build())
            .header(HttpHeaders.AUTHORIZATION, autorizacion(token))
            .retrieve()
            .body(MercadoPagoBusquedaPagos.class));
        return busqueda.results() == null
            ? List.of()
            : busqueda.results();
    }

    public MercadoPagoToken intercambiarCodigo(
            String code, String redirectUri, boolean testToken) {
        Map<String, Object> cuerpo = credenciales();
        cuerpo.put("code", code);
        cuerpo.put("grant_type", "authorization_code");
        cuerpo.put("redirect_uri", redirectUri);
        cuerpo.put("test_token", testToken);
        return ejecutar(() -> restClient.post()
            .uri("/oauth/token")
            .contentType(MediaType.APPLICATION_JSON)
            .body(cuerpo)
            .retrieve()
            .body(MercadoPagoToken.class));
    }

    public MercadoPagoToken refrescarToken(String refreshToken) {
        Map<String, Object> cuerpo = credenciales();
        cuerpo.put("grant_type", "refresh_token");
        cuerpo.put("refresh_token", refreshToken);
        return ejecutar(() -> restClient.post()
            .uri("/oauth/token")
            .contentType(MediaType.APPLICATION_JSON)
            .body(cuerpo)
            .retrieve()
            .body(MercadoPagoToken.class));
    }

    private Map<String, Object> credenciales() {
        if (clientId == null || clientId.isBlank()
                || clientSecret == null || clientSecret.isBlank()) {
            throw new MercadoPagoException(
                "Faltan configurar TEMIPORA_MP_CLIENT_ID y TEMIPORA_MP_CLIENT_SECRET.");
        }
        Map<String, Object> cuerpo = new java.util.LinkedHashMap<>();
        cuerpo.put("client_id", clientId);
        cuerpo.put("client_secret", clientSecret);
        return cuerpo;
    }

    private String autorizacion(String token) {
        if (token == null || token.isBlank()) {
            throw new MercadoPagoException(
                "Falta el access token de MercadoPago.");
        }
        return "Bearer " + token;
    }

    private <T> T ejecutar(Supplier<T> llamada) {
        try {
            T respuesta = llamada.get();
            if (respuesta == null) {
                throw new MercadoPagoException(
                    "MercadoPago devolvió una respuesta vacía.");
            }
            return respuesta;
        } catch (RestClientResponseException e) {
            throw new MercadoPagoException(
                "MercadoPago respondió " + e.getStatusCode().value()
                    + ": " + e.getResponseBodyAsString(),
                e);
        }
    }
}
