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
public class ServicioResumenEstadoDTO {

    private Long idServicio;
    private long pendientes;
    private long enEspera;
    private long presupuestadas;
    private long pagadas;
    private long listaEspera;
}
