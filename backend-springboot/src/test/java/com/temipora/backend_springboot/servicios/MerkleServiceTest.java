package com.temipora.backend_springboot.servicios;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

class MerkleServiceTest {

    private final MerkleService merkle = new MerkleService();

    private static final String A =
        "aa00000000000000000000000000000000000000000000000000000000000000";
    private static final String B =
        "bb11111111111111111111111111111111111111111111111111111111111111";
    private static final String C =
        "cc22222222222222222222222222222222222222222222222222222222222222";

    @Test
    void raizEsDeterminista() {
        assertEquals(merkle.raiz(List.of(A, B, C)), merkle.raiz(List.of(A, B, C)));
    }

    @Test
    void raizDeUnaSolaHojaEsLaHoja() {
        assertEquals(A, merkle.raiz(List.of(A)));
    }

    @Test
    void pruebaDeInclusionVerificaContraLaRaiz() {
        List<String> hojas = List.of(A, B, C);
        String raiz = merkle.raiz(hojas);
        for (int i = 0; i < hojas.size(); i++) {
            assertTrue(merkle.verificar(
                hojas.get(i), merkle.prueba(hojas, i), raiz),
                "la hoja " + i + " debería verificar");
        }
    }

    @Test
    void unaHojaAlteradaNoVerifica() {
        List<String> hojas = List.of(A, B, C);
        String raiz = merkle.raiz(hojas);
        List<MerkleService.PasoMerkle> prueba = merkle.prueba(hojas, 1);
        assertFalse(merkle.verificar(A, prueba, raiz));
    }

    @Test
    void cambiarLaListaCambiaLaRaiz() {
        assertFalse(merkle.raiz(List.of(A, B)).equals(merkle.raiz(List.of(A, C))));
    }
}
