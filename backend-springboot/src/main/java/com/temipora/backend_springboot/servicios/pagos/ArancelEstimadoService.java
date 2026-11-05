package com.temipora.backend_springboot.servicios.pagos;

import java.math.BigDecimal;
import java.math.RoundingMode;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.temipora.backend_springboot.modelo.ArancelEstimadoDTO;

/**
 * Tasa estimada del arancel de MercadoPago. El valor real depende del medio de
 * pago y de las cuotas, y se conoce recién cuando MP informa el pago.
 */
@Service
public class ArancelEstimadoService {

    private static final BigDecimal CIEN = new BigDecimal("100");

    private final BigDecimal porcentaje;
    private final BigDecimal ivaPorcentaje;

    public ArancelEstimadoService(
            @Value("${pagos.mercadopago.arancel-estimado:3.49}") BigDecimal porcentaje,
            @Value("${pagos.mercadopago.arancel-iva:21}") BigDecimal ivaPorcentaje) {
        this.porcentaje = porcentaje != null
            ? porcentaje
            : new BigDecimal("3.49");
        this.ivaPorcentaje = ivaPorcentaje != null
            ? ivaPorcentaje
            : BigDecimal.ZERO;
    }

    public ArancelEstimadoDTO estimar() {
        BigDecimal factorIva = BigDecimal.ONE.add(
            ivaPorcentaje.divide(CIEN, 6, RoundingMode.HALF_UP));
        BigDecimal total = porcentaje.multiply(factorIva)
            .setScale(4, RoundingMode.HALF_UP);
        return new ArancelEstimadoDTO(porcentaje, ivaPorcentaje, total);
    }
}
