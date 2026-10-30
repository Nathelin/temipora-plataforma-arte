package com.temipora.backend_springboot.servicios.pagos;

import java.util.Optional;

import com.temipora.backend_springboot.modelo.Pago;

public interface ProveedorPago {

    String nombre();

    ResultadoCobro crearCobro(Pago pago);

    /**
     * Consulta el estado real del cobro en el proveedor. Se usa para
     * reconciliar cuando el webhook se demora o no llega.
     */
    default Optional<EstadoCobroProveedor> consultarEstado(Pago pago) {
        return Optional.empty();
    }
}
