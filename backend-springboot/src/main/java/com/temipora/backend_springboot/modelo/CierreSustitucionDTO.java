package com.temipora.backend_springboot.modelo;

import java.time.LocalDateTime;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Resumen de un cierre (rechazo, retiro, vencimiento, baja o impago) junto con
 * las alternativas sugeridas. Lo consume la pantalla de resultado.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CierreSustitucionDTO {

    private String contexto;
    private Long idSolicitud;
    private Long idPedido;
    private Long idSustitucion;
    private String motivo;
    private String titulo;
    private String detalle;
    private LocalDateTime fecha;
    private ServicioDTO servicio;
    private boolean procesoHabilitado;
    private List<SustitucionOpcionDTO> opciones;
}
