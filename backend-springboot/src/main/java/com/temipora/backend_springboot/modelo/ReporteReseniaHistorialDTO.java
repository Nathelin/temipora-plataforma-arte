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
public class ReporteReseniaHistorialDTO {

    private Long idReporte;
    private Long idResenia;
    private Long idPedido;
    private String nombreServicio;
    private String autorResenia;
    private boolean reseniaAnonima;
    private EstadoResenia estadoResenia;
    private MotivoReporteResenia motivo;
    private String etiquetaMotivo;
    private String comentario;
    private EstadoReporteResenia estado;
    private AccionReporteResenia accion;
    private LocalDateTime fechaCreacion;
    private LocalDateTime fechaResolucion;
    private UsuarioResumenDTO reportante;
    private UsuarioResumenDTO resueltoPor;
}
