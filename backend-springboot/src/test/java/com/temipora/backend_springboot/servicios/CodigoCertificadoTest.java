package com.temipora.backend_springboot.servicios;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class CodigoCertificadoTest {

    @Test
    void idaYVuelta() {
        assertEquals(12345L, CodigoCertificado.decodificar(
            CodigoCertificado.codificar(12345L)).orElseThrow());
    }

    @Test
    void codigoLlevaPrefijo() {
        assertTrue(CodigoCertificado.codificar(1L).startsWith("TP-"));
    }

    @Test
    void rechazaCodigosInvalidos() {
        assertFalse(CodigoCertificado.decodificar(null).isPresent());
        assertFalse(CodigoCertificado.decodificar("").isPresent());
        assertFalse(CodigoCertificado.decodificar("XX-123").isPresent());
        assertFalse(CodigoCertificado.decodificar("TP-").isPresent());
    }
}
