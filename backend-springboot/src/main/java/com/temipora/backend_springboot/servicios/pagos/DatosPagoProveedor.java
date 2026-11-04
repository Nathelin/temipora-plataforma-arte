package com.temipora.backend_springboot.servicios.pagos;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record DatosPagoProveedor(
        BigDecimal arancel,
        BigDecimal netoRecibido,
        LocalDateTime fechaAprobacion,
        LocalDateTime fechaLiberacion,
        String medioPago,
        String tipoMedioPago,
        Integer cuotas,
        String ultimosCuatro) {
}
