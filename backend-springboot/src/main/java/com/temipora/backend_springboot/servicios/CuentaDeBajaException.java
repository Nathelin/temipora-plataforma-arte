package com.temipora.backend_springboot.servicios;

import java.time.LocalDateTime;

import org.springframework.security.authentication.DisabledException;

/**
 * Se lanza al abrir una sesión con una cuenta dada de baja voluntariamente.
 * Lleva los datos que el front necesita para ofrecer la reactivación.
 */
public class CuentaDeBajaException extends DisabledException {

    private final LocalDateTime fechaBaja;
    private final LocalDateTime reactivarHasta;
    private final boolean puedeReactivar;
    private final String contacto;

    public CuentaDeBajaException(
            LocalDateTime fechaBaja,
            LocalDateTime reactivarHasta,
            boolean puedeReactivar,
            String contacto) {
        super("La cuenta está dada de baja.");
        this.fechaBaja = fechaBaja;
        this.reactivarHasta = reactivarHasta;
        this.puedeReactivar = puedeReactivar;
        this.contacto = contacto;
    }

    public LocalDateTime getFechaBaja() {
        return fechaBaja;
    }

    public LocalDateTime getReactivarHasta() {
        return reactivarHasta;
    }

    public boolean isPuedeReactivar() {
        return puedeReactivar;
    }

    public String getContacto() {
        return contacto;
    }
}
