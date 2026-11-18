package com.temipora.backend_springboot.servicios;

import java.util.Optional;

/**
 * Servicio de sellado de tiempo externo (blockchain de Bitcoin vía
 * OpenTimestamps). Se abstrae en una interfaz para poder mockearlo en tests y
 * para encapsular la implementacion concreta.
 */
public interface ProveedorSellado {

    /** Resultado del sellado inicial: el archivo .ots (aun pendiente de minar). */
    record ResultadoSellado(byte[] pruebaOts, String calendarios) {
    }

    /** Resultado de una confirmacion: la prueba ya anclada en Bitcoin. */
    record ResultadoConfirmacion(
            byte[] pruebaOts, String hashBloqueBitcoin, long bitcoinAltura) {
    }

    /**
     * Sella el digest (32 bytes) en Bitcoin a través de los calendarios
     * configurados. Devuelve el archivo .ots (pendiente).
     */
    ResultadoSellado sellar(byte[] digest);

    /**
     * Intenta "actualizar" una prueba pendiente. Devuelve la confirmacion si
     * ya esta anclada en un bloque de Bitcoin; vacio si sigue pendiente.
     */
    Optional<ResultadoConfirmacion> confirmar(byte[] pruebaOts);
}
