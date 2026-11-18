package com.temipora.backend_springboot.modelo;

/**
 * Alcance de una sanción. En la v1 solo se usa ACCESO (bloqueo de inicio de
 * sesión). CONGELACION queda reservado para una versión futura que además
 * pausa las operaciones de la cuenta.
 */
public enum AlcanceSancion {
    ACCESO,
    CONGELACION
}
