package com.temipora.backend_springboot.servicios;

import java.time.LocalDateTime;

public class ReactivacionVencidaException extends RuntimeException {

    private final String contacto;
    private final LocalDateTime reactivarHasta;

    public ReactivacionVencidaException(String contacto, LocalDateTime reactivarHasta) {
        super("El plazo para reactivar la cuenta venció.");
        this.contacto = contacto;
        this.reactivarHasta = reactivarHasta;
    }

    public String getContacto() {
        return contacto;
    }

    public LocalDateTime getReactivarHasta() {
        return reactivarHasta;
    }
}
