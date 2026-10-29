package com.temipora.backend_springboot.modelo;

import java.time.LocalDateTime;

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
public class ReporteReseniaDTO {

    private Long idReporte;
    private MotivoReporteResenia motivo;
    private String etiquetaMotivo;
    private String comentario;
    private EstadoReporteResenia estado;
    private AccionReporteResenia accion;
    private LocalDateTime fechaCreacion;
    private LocalDateTime fechaResolucion;
    private UsuarioResumenDTO reportante;
    private UsuarioResumenDTO resueltoPor;

    public static ReporteReseniaDTO desde(ReporteResenia reporte) {
        return ReporteReseniaDTO.builder()
            .idReporte(reporte.getIdReporte())
            .motivo(reporte.getMotivo())
            .etiquetaMotivo(reporte.getMotivo() != null
                ? reporte.getMotivo().getEtiqueta()
                : null)
            .comentario(reporte.getComentario())
            .estado(reporte.getEstado())
            .accion(reporte.getAccion())
            .fechaCreacion(reporte.getFechaCreacion())
            .fechaResolucion(reporte.getFechaResolucion())
            .reportante(UsuarioResumenDTO.desdeUsuario(reporte.getReportante()))
            .resueltoPor(UsuarioResumenDTO.desdeUsuario(reporte.getResueltoPor()))
            .build();
    }
}
