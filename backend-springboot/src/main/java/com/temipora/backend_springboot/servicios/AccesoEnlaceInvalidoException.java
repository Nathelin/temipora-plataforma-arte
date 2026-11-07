package com.temipora.backend_springboot.servicios;

public class AccesoEnlaceInvalidoException extends RuntimeException {

    public AccesoEnlaceInvalidoException(String mensaje) {
        super(mensaje);
    }
}
