package com.temipora.backend_springboot.servicios.pagos.producto;

import java.util.List;
import java.util.Optional;

import com.temipora.backend_springboot.modelo.CompraGrupo;
import com.temipora.backend_springboot.modelo.IntentoCobro;
import com.temipora.backend_springboot.servicios.pagos.EstadoCobroProveedor;
import com.temipora.backend_springboot.servicios.pagos.ResultadoCobro;

public interface ProveedorPagoProducto {

    String nombre();

    /**
     * Crea el cobro de un intento. Hoy recibe un solo grupo (Checkout Pro 1:1).
     * Cuando se habilite Split 1:N, el mismo intento podrá traer varios grupos.
     */
    ResultadoCobro crearCobro(IntentoCobro intento, List<CompraGrupo> grupos);

    default Optional<EstadoCobroProveedor> consultarEstado(IntentoCobro intento) {
        return Optional.empty();
    }
}
