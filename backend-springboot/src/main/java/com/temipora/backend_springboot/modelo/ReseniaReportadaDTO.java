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
public class ReseniaReportadaDTO {

    private Long idResenia;
    private Long idPedido;
    private String nombreServicio;
    private Long idProducto;
    private String nombreProducto;
    private UsuarioResumenDTO autor;
    private boolean mostrarNombre;
    private EstadoResenia estado;
    private LocalDateTime fechaCreacion;
    private LocalDateTime fechaVersion;
    private Integer calificacionGeneral;
    private String descripcion;
    private List<ReseniaValorDTO> valores;
    private List<ReseniaOpcionDTO> opciones;
    private LocalDateTime moderadaEn;
    private String motivoModeracion;
    private long reportesPendientes;
    private long reportesTotales;
    private List<ReporteReseniaDTO> reportes;
}
