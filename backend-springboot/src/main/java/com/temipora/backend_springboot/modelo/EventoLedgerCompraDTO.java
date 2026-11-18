package com.temipora.backend_springboot.modelo;

import java.math.BigDecimal;
import java.time.LocalDateTime;

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
public class EventoLedgerCompraDTO {

    private Long idEventoLedger;
    private Long secuencia;
    private TipoEventoLedger tipo;
    private LocalDateTime ocurridoEn;
    private Long idCompraItem;

    private Long idActorOrigen;
    private String nombreOrigen;
    private Long idActorDestino;
    private String nombreDestino;

    /** Solo se expone para el evento que corresponde a esta compra. */
    private BigDecimal monto;
    private String moneda;

    private String hashPrevio;
    private String hashEvento;
    private String hashDatos;
    private String hashImagen;
    private Integer versionHash;
    private boolean verificado;
    private boolean esEventoDeEstaCompra;
}
