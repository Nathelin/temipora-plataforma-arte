package com.temipora.backend_springboot.modelo;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Evento de procedencia visible en el catálogo público. No incluye actores,
 * montos ni hashes: el detalle completo queda para el comprador.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EventoLedgerPublicoDTO {

    private Long idEventoLedger;
    private Long secuencia;
    private TipoEventoLedger tipo;
    private LocalDateTime ocurridoEn;
    private boolean verificado;

    /** El evento ya está sellado en Bitcoin (OpenTimestamps). */
    private boolean anclado;
    private LocalDateTime fechaAnclaje;
    private Long bitcoinAltura;
}
