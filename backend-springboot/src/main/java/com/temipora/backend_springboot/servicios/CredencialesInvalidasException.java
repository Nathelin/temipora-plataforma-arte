package com.temipora.backend_springboot.servicios;

public class CredencialesInvalidasException extends RuntimeException {

    public CredencialesInvalidasException() {
        super("Nombre de usuario o contraseña incorrecta.");
    }
}
