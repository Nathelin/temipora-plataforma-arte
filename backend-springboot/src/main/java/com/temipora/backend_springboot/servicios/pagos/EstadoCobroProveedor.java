package com.temipora.backend_springboot.servicios.pagos;

import com.temipora.backend_springboot.modelo.EstadoPago;

public record EstadoCobroProveedor(
        EstadoPago estado,
        String idPagoExterno,
        String estadoExterno,
        String detalle) {
}
