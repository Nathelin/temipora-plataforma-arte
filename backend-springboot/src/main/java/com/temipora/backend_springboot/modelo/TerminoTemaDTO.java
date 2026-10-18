package com.temipora.backend_springboot.modelo;

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
public class TerminoTemaDTO {

    private Long idTemaTermino;
    private String nombre;
    private boolean personalizado;
    private String estado;

    public static TerminoTemaDTO desdeTerminoTema(TerminoTema terminoTema) {
        boolean personalizado = terminoTema.getTema() == null;
        return TerminoTemaDTO.builder()
            .idTemaTermino(personalizado ? null : terminoTema.getTema().getIdTemaTermino())
            .nombre(personalizado ? terminoTema.getNombrePersonalizado() : terminoTema.getTema().getNombre())
            .personalizado(personalizado)
            .estado(terminoTema.getEstado() != null ? terminoTema.getEstado().name() : null)
            .build();
    }
}
