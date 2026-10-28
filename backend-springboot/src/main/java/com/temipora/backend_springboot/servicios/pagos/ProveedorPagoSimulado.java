package com.temipora.backend_springboot.servicios.pagos;

import java.util.UUID;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import com.temipora.backend_springboot.modelo.Pago;

/**
 * Proveedor de desarrollo: crea un cobro pendiente que se confirma desde
 * {@code POST /api/pagos/simulado/{idPago}/confirmar}.
 */
@Component
@ConditionalOnProperty(
    name = "pagos.proveedor",
    havingValue = "simulado",
    matchIfMissing = true)
public class ProveedorPagoSimulado implements ProveedorPago {

    public static final String NOMBRE = "SIMULADO";

    @Override
    public String nombre() {
        return NOMBRE;
    }

    @Override
    public ResultadoCobro crearCobro(Pago pago) {
        return new ResultadoCobro(
            "SIM-" + UUID.randomUUID(),
            "Pago simulado pendiente. Confirmalo desde la pantalla del pedido.");
    }
}
