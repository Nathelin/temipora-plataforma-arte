package com.temipora.backend_springboot.servicios.pagos.mercadopago;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record MercadoPagoPago(
        Long id,
        String status,
        @JsonProperty("status_detail") String statusDetail,
        @JsonProperty("transaction_amount") BigDecimal transactionAmount,
        @JsonProperty("currency_id") String currencyId,
        @JsonProperty("external_reference") String externalReference,
        @JsonProperty("payment_method_id") String paymentMethodId,
        @JsonProperty("payment_type_id") String paymentTypeId,
        @JsonProperty("date_approved") String dateApproved) {
}
