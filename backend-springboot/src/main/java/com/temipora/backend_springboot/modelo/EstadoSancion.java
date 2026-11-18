package com.temipora.backend_springboot.modelo;

/**
 * Estado persistido de una sanción. El estado "vencida" no se guarda: se
 * deriva de fecha_fin al construir los DTO.
 */
public enum EstadoSancion {
    ACTIVA,
    REVERTIDA
}
