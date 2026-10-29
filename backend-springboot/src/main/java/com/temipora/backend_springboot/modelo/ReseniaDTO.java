package com.temipora.backend_springboot.modelo;

import java.time.LocalDateTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

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
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ReseniaDTO {

    private Long idResenia;
    private Long idPedido;
    private String nombreServicio;
    private AmbitoResenia direccion;
    private UsuarioResumenDTO autor;
    private EstadoResenia estado;
    private boolean editada;
    private int ediciones;
    private LocalDateTime fechaCreacion;
    private LocalDateTime fechaActualizacion;
    private int numeroVersion;
    private LocalDateTime fechaVersion;
    private String descripcion;
    private int calificacionGeneral;
    private boolean mostrarNombre;
    private String motivoModeracion;
    private List<ReseniaValorDTO> valores;
    private List<ReseniaOpcionDTO> opciones;
    private List<ReseniaVersionDTO> historial;

    public static ReseniaDTO desde(Resenia resenia, boolean incluirHistorial) {
        ReseniaVersion actual = resenia.versionActual();
        boolean esCliente = resenia.getAutor().getIdUsuario()
            .equals(resenia.getPedido().getCliente().getIdUsuario());

        ReseniaDTOBuilder builder = ReseniaDTO.builder()
            .idResenia(resenia.getIdResenia())
            .idPedido(resenia.getPedido().getIdPedido())
            .nombreServicio(resenia.getPedido().getServicio() != null
                ? resenia.getPedido().getServicio().getNombre()
                : null)
            .direccion(esCliente ? AmbitoResenia.SERVICIO : AmbitoResenia.CLIENTE)
            .autor(UsuarioResumenDTO.desdeUsuario(resenia.getAutor()))
            .estado(resenia.getEstado())
            .mostrarNombre(resenia.isMostrarNombre())
            .motivoModeracion(resenia.getMotivoModeracion())
            .editada(resenia.estaEditada())
            .ediciones(Math.max(0, resenia.getVersiones().size() - 1))
            .fechaCreacion(resenia.getFechaCreacion())
            .fechaActualizacion(resenia.getFechaActualizacion());

        if (actual != null) {
            builder.numeroVersion(actual.getNumero())
                .fechaVersion(actual.getFechaVersion())
                .descripcion(actual.getDescripcion())
                .calificacionGeneral(actual.getCalificacionGeneral())
                .valores(actual.getValores().stream()
                    .map(ReseniaValorDTO::desdeValor)
                    .toList())
                .opciones(actual.getOpciones().stream()
                    .map(ReseniaOpcionDTO::desdeOpcion)
                    .toList());
        }

        if (incluirHistorial) {
            builder.historial(resenia.getVersiones().stream()
                .map(version -> ReseniaVersionDTO.desdeVersion(
                    version, actual != null && version.getNumero() == actual.getNumero()))
                .toList());
        }

        return builder.build();
    }
}
