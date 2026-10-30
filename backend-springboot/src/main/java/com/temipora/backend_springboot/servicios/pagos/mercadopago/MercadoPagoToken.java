package com.temipora.backend_springboot.servicios.pagos.mercadopago;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record MercadoPagoToken(
        @JsonProperty("access_token") String accessToken,
        @JsonProperty("refresh_token") String refreshToken,
        @JsonProperty("expires_in") Long expiresIn,
        @JsonProperty("user_id") Long userId,
        @JsonProperty("scope") String scope,
        @JsonProperty("token_type") String tokenType) {
}
