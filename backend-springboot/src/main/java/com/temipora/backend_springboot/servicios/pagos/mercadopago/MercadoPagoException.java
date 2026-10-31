package com.temipora.backend_springboot.servicios.pagos.mercadopago;

public class MercadoPagoException extends RuntimeException {

    public MercadoPagoException(String mensaje) {
        super(mensaje);
    }

    public MercadoPagoException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
