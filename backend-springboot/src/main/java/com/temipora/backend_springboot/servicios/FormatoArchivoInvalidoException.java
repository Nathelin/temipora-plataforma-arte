package com.temipora.backend_springboot.servicios;

public class FormatoArchivoInvalidoException extends RuntimeException {

    public FormatoArchivoInvalidoException() {
        super("El formato es inválido.");
    }
}