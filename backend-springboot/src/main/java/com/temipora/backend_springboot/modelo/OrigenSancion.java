package com.temipora.backend_springboot.modelo;

/**
 * Quién aplicó una sanción. Las automáticas no tienen administrador y son
 * apelables.
 */
public enum OrigenSancion {
    MANUAL,
    AUTOMATICA
}
