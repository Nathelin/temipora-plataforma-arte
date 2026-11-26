package com.temipora.backend_springboot.modelo;

/**
 * Ciclo de vida de una propuesta de prórroga del plazo de entrega. Solo una
 * propuesta puede estar pendiente por pedido; las resueltas se conservan como
 * historial y no se reabren.
 */
public enum EstadoProrroga {
    PENDIENTE,
    ACEPTADA,
    RECHAZADA,
    VENCIDA
}
