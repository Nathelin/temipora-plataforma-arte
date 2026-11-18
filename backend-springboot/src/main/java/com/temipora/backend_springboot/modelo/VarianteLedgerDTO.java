package com.temipora.backend_springboot.modelo;

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
public class VarianteLedgerDTO {

    private Long idVariante;
    private String nombre;
    private boolean tieneLedger;
    private boolean integridad;
    private String mensaje;
    private List<EventoLedgerDTO> eventos;
}
