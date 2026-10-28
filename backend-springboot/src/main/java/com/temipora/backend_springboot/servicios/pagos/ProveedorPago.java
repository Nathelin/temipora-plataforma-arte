package com.temipora.backend_springboot.servicios.pagos;

import com.temipora.backend_springboot.modelo.Pago;

public interface ProveedorPago {

    String nombre();

    ResultadoCobro crearCobro(Pago pago);
}
