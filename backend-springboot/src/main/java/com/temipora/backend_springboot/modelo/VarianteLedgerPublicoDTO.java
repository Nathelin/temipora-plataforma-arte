package com.temipora.backend_springboot.modelo;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VarianteLedgerPublicoDTO {

    private Long idVariante;
    private String nombre;
    private boolean tieneLedger;
    private boolean integridad;
    private String mensaje;

    /** Código público del certificado de autenticidad (null si no es pieza única). */
    private String codigoCertificado;

    /** El historial ya quedó sellado en Bitcoin. */
    private boolean anclado;
    private LocalDateTime fechaAnclaje;
    private Long bitcoinAltura;

    @Builder.Default
    private List<EventoLedgerPublicoDTO> eventos = new ArrayList<>();
}
