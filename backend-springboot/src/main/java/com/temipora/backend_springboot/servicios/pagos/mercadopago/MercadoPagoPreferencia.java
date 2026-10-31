package com.temipora.backend_springboot.servicios.pagos.mercadopago;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record MercadoPagoPreferencia(
        String id,
        @JsonProperty("init_point") String initPoint,
        @JsonProperty("sandbox_init_point") String sandboxInitPoint) {
}
