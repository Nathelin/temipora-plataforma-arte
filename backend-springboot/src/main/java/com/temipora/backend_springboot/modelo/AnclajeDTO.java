package com.temipora.backend_springboot.modelo;

import java.time.LocalDateTime;

/** Resumen de un anclaje (sello OpenTimestamps) del ledger. */
public record AnclajeDTO(
        Long idAnclaje,
        String hashRaiz,
        int cantidadEventos,
        Long idEventoDesde,
        Long idEventoHasta,
        EstadoAnclaje estado,
        String calendarioUrl,
        String hashBitcoin,
        Long bitcoinAltura,
        LocalDateTime fechaSolicitud,
        LocalDateTime fechaConfirmacion) {
}
