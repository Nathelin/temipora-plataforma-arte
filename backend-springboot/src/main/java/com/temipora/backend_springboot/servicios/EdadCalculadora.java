package com.temipora.backend_springboot.servicios;

import java.time.LocalDate;
import java.time.Period;
import java.util.Objects;

/**
 * Reglas de edad mínima para usar la plataforma.
 *
 * <p>Por ahora la validación es declarativa: se controla la fecha de nacimiento
 * informada al registrarse. La verificación de identidad queda para una etapa
 * posterior.</p>
 */
public final class EdadCalculadora {

    public static final int EDAD_MINIMA = 18;

    private EdadCalculadora() {
    }

    /**
     * Edad cumplida a la fecha indicada.
     */
    public static int edad(LocalDate fechaNacimiento, LocalDate hoy) {
        Objects.requireNonNull(fechaNacimiento, "La fecha de nacimiento es obligatoria.");
        Objects.requireNonNull(hoy, "La fecha de referencia es obligatoria.");
        if (fechaNacimiento.isAfter(hoy)) {
            throw new IllegalArgumentException("La fecha de nacimiento no puede ser futura.");
        }
        return Period.between(fechaNacimiento, hoy).getYears();
    }

    /**
     * Indica si la persona cumple la edad mínima a la fecha indicada.
     * Una fecha nula o futura nunca cumple el requisito.
     */
    public static boolean esMayorDeEdad(LocalDate fechaNacimiento, LocalDate hoy) {
        if (fechaNacimiento == null || hoy == null || fechaNacimiento.isAfter(hoy)) {
            return false;
        }
        return edad(fechaNacimiento, hoy) >= EDAD_MINIMA;
    }
}
