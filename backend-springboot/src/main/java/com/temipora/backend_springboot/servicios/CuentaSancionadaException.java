package com.temipora.backend_springboot.servicios;

import java.time.LocalDateTime;

import org.springframework.security.authentication.DisabledException;

/**
 * Se lanza al autenticar una cuenta con una sanción vigente. Extiende
 * DisabledException para que Spring la trate como fallo de autenticación.
 */
public class CuentaSancionadaException extends DisabledException {

    private final String motivo;
    private final String contacto;
    private final LocalDateTime fechaFin;

    public CuentaSancionadaException(String motivo, String contacto, LocalDateTime fechaFin) {
        super("La cuenta se encuentra inhabilitada.");
        this.motivo = motivo;
        this.contacto = contacto;
        this.fechaFin = fechaFin;
    }

    public String getMotivo() {
        return motivo;
    }

    public String getContacto() {
        return contacto;
    }

    public LocalDateTime getFechaFin() {
        return fechaFin;
    }
}
