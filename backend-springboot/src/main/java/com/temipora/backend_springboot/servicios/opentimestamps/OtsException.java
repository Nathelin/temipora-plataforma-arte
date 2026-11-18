package com.temipora.backend_springboot.servicios.opentimestamps;

/** Error al interpretar o generar un sello de OpenTimestamps. */
public class OtsException extends RuntimeException {

    public OtsException(String message) {
        super(message);
    }

    public OtsException(String message, Throwable cause) {
        super(message, cause);
    }
}
