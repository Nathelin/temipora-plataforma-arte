package com.temipora.backend_springboot.servicios;

import java.math.BigDecimal;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Cliente HTTP de la API pública de cotizaciones (dolarapi.com).
 * Aislado para poder mockearlo en los tests.
 */
@Component
public class CotizacionApiClient {

    private final RestClient restClient;
    private final String apiUrl;

    public CotizacionApiClient(
            @Value("${cotizacion.api.url:https://dolarapi.com/v1/dolares/oficial}") String apiUrl) {
        this.restClient = RestClient.create();
        this.apiUrl = apiUrl;
    }

    public BigDecimal obtenerFactorVenta() {
        Map<String, Object> respuesta = restClient.get()
            .uri(apiUrl)
            .retrieve()
            .body(new ParameterizedTypeReference<Map<String, Object>>() {});

        if (respuesta == null) {
            throw new IllegalStateException("La API de cotizaciones devolvió una respuesta vacía.");
        }
        Object venta = respuesta.get("venta");
        if (!(venta instanceof Number) && !(venta instanceof String)) {
            throw new IllegalStateException(
                "La API de cotizaciones no devolvió un valor de venta válido.");
        }
        BigDecimal factor = new BigDecimal(venta.toString());
        if (factor.signum() <= 0) {
            throw new IllegalStateException("La API de cotizaciones devolvió un factor inválido.");
        }
        return factor;
    }
}
