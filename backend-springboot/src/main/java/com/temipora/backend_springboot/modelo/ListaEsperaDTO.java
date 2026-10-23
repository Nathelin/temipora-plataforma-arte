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
public class ListaEsperaDTO {

    private Long idListaEspera;
    private Long idSolicitud;
    private Long idServicio;
    private Long idCliente;
    private String nombreCliente;
    private Integer cantidadDibujos;
    private Integer posicion;
    private LocalDateTime fechaIngreso;
    private String estado;

    public static ListaEsperaDTO desdeListaEspera(ListaEspera lista) {
        return ListaEsperaDTO.builder()
            .idListaEspera(lista.getIdListaEspera())
            .idSolicitud(lista.getSolicitud() != null ? lista.getSolicitud().getIdSolicitud() : null)
            .idServicio(lista.getServicio() != null ? lista.getServicio().getIdServicio() : null)
            .idCliente(lista.getCliente() != null ? lista.getCliente().getIdUsuario() : null)
            .nombreCliente(lista.getCliente() != null ? lista.getCliente().getNombreUsuario() : null)
            .cantidadDibujos(lista.getCantidadDibujos())
            .posicion(lista.getPosicion())
            .fechaIngreso(lista.getFechaIngreso())
            .estado(lista.getEstado())
            .build();
    }
}
