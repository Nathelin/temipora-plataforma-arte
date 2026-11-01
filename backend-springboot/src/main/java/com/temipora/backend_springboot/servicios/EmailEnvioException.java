package com.temipora.backend_springboot.servicios;

public class EmailEnvioException extends RuntimeException {

    public EmailEnvioException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
