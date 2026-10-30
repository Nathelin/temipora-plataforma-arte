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

    @Autowired
    public MercadoPagoClient(
            @Value("${pagos.mercadopago.base-url:https://api.mercadopago.com}") String baseUrl,
            @Value("${pagos.mercadopago.access-token:}") String accessToken) {
        this(builderConTimeouts(), baseUrl, accessToken);
    }

    private static RestClient.Builder builderConTimeouts() {
        SimpleClientHttpRequestFactory factory =
            new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5000);
        factory.setReadTimeout(15000);
        return RestClient.builder().requestFactory(factory);
    }

    MercadoPagoClient(RestClient.Builder builder, String baseUrl, String accessToken) {
        this.restClient = builder.baseUrl(baseUrl).build();
        this.accessToken = accessToken;
    }

    public MercadoPagoPreferencia crearPreferencia(
            Map<String, Object> cuerpo, String claveIdempotencia) {
        return ejecutar(() -> restClient.post()
            .uri("/checkout/preferences")
            .header(HttpHeaders.AUTHORIZATION, autorizacion())
            .header("X-Idempotency-Key", claveIdempotencia)
            .contentType(MediaType.APPLICATION_JSON)
            .body(cuerpo)
            .retrieve()
            .body(MercadoPagoPreferencia.class));
    }

    public MercadoPagoPago consultarPago(String idPago) {
        return ejecutar(() -> restClient.get()
            .uri("/v1/payments/{id}", idPago)
            .header(HttpHeaders.AUTHORIZATION, autorizacion())
            .retrieve()
            .body(MercadoPagoPago.class));
    }

    public List<MercadoPagoPago> buscarPagos(String externalReference) {
        MercadoPagoBusquedaPagos busqueda = ejecutar(() -> restClient.get()
            .uri(uriBuilder -> uriBuilder.path("/v1/payments/search")
                .queryParam("external_reference", externalReference)
                .queryParam("sort", "date_created")
                .queryParam("criteria", "desc")
                .build())
            .header(HttpHeaders.AUTHORIZATION, autorizacion())
            .retrieve()
            .body(MercadoPagoBusquedaPagos.class));
        return busqueda.results() == null
            ? List.of()
            : busqueda.results();
    }

    private String autorizacion() {
        if (accessToken == null || accessToken.isBlank()) {
            throw new MercadoPagoException(
                "Falta configurar la variable MP_ACCESS_TK.");
        }
        return "Bearer " + accessToken;
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
