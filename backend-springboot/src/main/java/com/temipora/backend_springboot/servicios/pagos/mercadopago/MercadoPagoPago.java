package com.temipora.backend_springboot.servicios.pagos.mercadopago;

import java.math.BigDecimal;
import java.util.List;

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
        @JsonProperty("date_approved") String dateApproved,
        @JsonProperty("money_release_date") String moneyReleaseDate,
        @JsonProperty("money_release_status") String moneyReleaseStatus,
        @JsonProperty("transaction_details") TransactionDetails transactionDetails,
        @JsonProperty("fee_details") List<FeeDetail> feeDetails,
        Integer installments,
        Card card) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record TransactionDetails(
            @JsonProperty("net_received_amount") BigDecimal netReceivedAmount,
            @JsonProperty("total_paid_amount") BigDecimal totalPaidAmount) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record FeeDetail(
            String type,
            BigDecimal amount,
            @JsonProperty("fee_payer") String feePayer) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Card(
            @JsonProperty("last_four_digits") String lastFourDigits) {
    }
}
