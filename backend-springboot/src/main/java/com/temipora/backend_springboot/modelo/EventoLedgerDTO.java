package com.temipora.backend_springboot.modelo;

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
public class EventoLedgerDTO {

    private Long idEventoLedger;
    private Long secuencia;
    private TipoEventoLedger tipo;
    private LocalDateTime ocurridoEn;
    private Long idActorOrigen;
    private String nombreOrigen;
    private Long idActorDestino;
    private String nombreDestino;
    private String hashPrevio;
    private String hashEvento;
    private boolean verificado;
}
