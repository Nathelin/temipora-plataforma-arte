package com.temipora.backend_springboot.servicios.pagos.producto;

import java.util.List;
import java.util.UUID;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import com.temipora.backend_springboot.modelo.CompraGrupo;
import com.temipora.backend_springboot.modelo.IntentoCobro;
import com.temipora.backend_springboot.servicios.pagos.ResultadoCobro;

@Component
@ConditionalOnProperty(
    name = "pagos.proveedor",
    havingValue = "simulado",
    matchIfMissing = true)
public class ProveedorPagoProductoSimulado implements ProveedorPagoProducto {

    public static final String NOMBRE = "SIMULADO";

    @Override
    public String nombre() {
        return NOMBRE;
    }

    @Override
    public ResultadoCobro crearCobro(
            IntentoCobro intento, List<CompraGrupo> grupos) {
        return new ResultadoCobro(
            "SIM-" + UUID.randomUUID(),
            "Pago simulado pendiente. Confirmalo desde la pantalla de la compra.");
    }
}
