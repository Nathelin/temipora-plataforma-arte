package com.temipora.backend_springboot.modelo;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Historial completo de procedencia de un ítem comprado. Se expone solo al
 * comprador dueño de la compra.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CompraLedgerDTO {

    private Long idCompra;
    private Long idCompraItem;
    private Long idProducto;
    private Long idVariante;
    private String nombreVariante;
    private boolean tieneLedger;
    private boolean integridad;
    private String mensaje;

    /** Código público del certificado de autenticidad (null si no es pieza única). */
    private String codigoCertificado;

    /** El historial de la pieza ya está sellado en Bitcoin (OpenTimestamps). */
    private boolean anclado;
    private Long bitcoinAltura;
    private LocalDateTime fechaAnclaje;

    @Builder.Default
    private List<EventoLedgerCompraDTO> eventos = new ArrayList<>();
}
