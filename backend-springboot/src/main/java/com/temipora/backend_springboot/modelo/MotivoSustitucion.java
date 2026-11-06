package com.temipora.backend_springboot.modelo;

/**
 * Motivo por el que un servicio no puede recibir una solicitud nueva.
 */
public enum MotivoSustitucion {
    SIN_CUPOS,
    SOLO_ESPERA,
    CERRADA,
    PAUSADO,
    ARCHIVADO
}
