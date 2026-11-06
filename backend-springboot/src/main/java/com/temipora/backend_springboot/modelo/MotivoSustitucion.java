package com.temipora.backend_springboot.modelo;

/**
 * Motivo por el que un servicio no puede recibir una solicitud nueva.
 */
public enum MotivoSustitucion {
    SIN_CUPOS,
    SOLO_ESPERA,
    CERRADA,
    PAUSADO,
    ARCHIVADO,
    SOLICITUD_RECHAZADA,
    PRESUPUESTO_RETIRADO,
    PRESUPUESTO_VENCIDO,
    SERVICIO_DADO_DE_BAJA,
    PEDIDO_IMPAGO
}
