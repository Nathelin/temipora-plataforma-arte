package com.temipora.backend_springboot.modelo;

import java.math.BigDecimal;
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
public class SolicitudResumenDTO {

    private Long idSolicitud;
    private Long idServicio;
    private String nombreServicio;
    private EstadoServicio estadoServicio;
    private UsuarioResumenDTO cliente;
    private UsuarioResumenDTO artista;
    private EstadoSolicitud estado;
    private Integer cantidadDibujos;
    private BigDecimal precioBase;
    private String moneda;
    private boolean prioridadDefinidaPorCliente;
    private LocalDateTime fechaCreacion;
    private LocalDateTime fechaActualizacion;

    public static SolicitudResumenDTO desdeSolicitud(Solicitud solicitud) {
        return SolicitudResumenDTO.builder()
            .idSolicitud(solicitud.getIdSolicitud())
            .idServicio(solicitud.getServicio() != null
                ? solicitud.getServicio().getIdServicio()
                : null)
            .nombreServicio(solicitud.getServicio() != null
                ? solicitud.getServicio().getNombre()
                : null)
            .estadoServicio(solicitud.getServicio() != null
                ? solicitud.getServicio().getEstado()
                : null)
            .cliente(UsuarioResumenDTO.desdeUsuario(solicitud.getCliente()))
            .artista(UsuarioResumenDTO.desdeUsuario(solicitud.getArtista()))
            .estado(solicitud.getEstado())
            .cantidadDibujos(solicitud.getCantidadDibujos())
            .precioBase(solicitud.getPrecioBase())
            .moneda(solicitud.getMoneda() != null ? solicitud.getMoneda().getCodigo() : null)
            .prioridadDefinidaPorCliente(solicitud.isPrioridadDefinidaPorCliente())
            .fechaCreacion(solicitud.getFechaCreacion())
            .fechaActualizacion(solicitud.getFechaActualizacion())
            .build();
    }
}
