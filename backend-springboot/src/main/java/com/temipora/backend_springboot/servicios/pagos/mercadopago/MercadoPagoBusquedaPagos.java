package com.temipora.backend_springboot.servicios.pagos.mercadopago;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record MercadoPagoBusquedaPagos(List<MercadoPagoPago> results) {
}
