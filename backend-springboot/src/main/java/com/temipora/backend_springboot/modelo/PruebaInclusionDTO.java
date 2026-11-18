package com.temipora.backend_springboot.modelo;

import java.util.List;

/**
 * Prueba de inclusion de un evento del ledger dentro de la raiz de Merkle de su
 * anclaje. Permite a un tercero comprobar que el evento pertenece a la raiz
 * sellada en Bitcoin sin ver el resto del lote.
 */
public record PruebaInclusionDTO(
        String hashHoja,
        String raiz,
        List<PasoInclusionDTO> pasos) {

    /** Un paso de la prueba: el hash hermano y de que lado va. */
    public record PasoInclusionDTO(String hashHermano, boolean hermanoDerecha) {
    }
}
