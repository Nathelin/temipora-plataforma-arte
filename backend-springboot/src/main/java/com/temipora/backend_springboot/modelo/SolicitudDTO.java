package com.temipora.backend_springboot.modelo;

import java.math.BigDecimal;
import java.time.LocalDateTime;
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
public class SolicitudDTO {

    private Long idSolicitud;
    private Long idServicio;
    private String nombreServicio;
    private EstadoServicio estadoServicio;
    private UsuarioResumenDTO cliente;
    private UsuarioResumenDTO artista;
    private Long idTerminos;
    private Integer terminosVersion;
    private LocalDateTime terminosAceptadosEn;
    private boolean prioridadDefinidaPorCliente;
    private EstadoSolicitud estado;
    private Integer cantidadDibujos;
    private BigDecimal precioBase;
    private String moneda;
    private LocalDateTime fechaCreacion;
    private Integer posicionListaEspera;
    private String motivoRechazo;
    private LocalDateTime fechaRechazo;
    private List<ItemSolicitudDTO> items;

    public static SolicitudDTO desdeSolicitud(Solicitud solicitud) {
        return SolicitudDTO.builder()
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
            .idTerminos(solicitud.getTerminos() != null
                ? solicitud.getTerminos().getIdTerminos()
                : null)
            .terminosVersion(solicitud.getTerminosVersion())
            .terminosAceptadosEn(solicitud.getTerminosAceptadosEn())
            .prioridadDefinidaPorCliente(solicitud.isPrioridadDefinidaPorCliente())
            .estado(solicitud.getEstado())
            .cantidadDibujos(solicitud.getCantidadDibujos())
            .precioBase(solicitud.getPrecioBase())
            .moneda(solicitud.getMoneda() != null ? solicitud.getMoneda().getCodigo() : null)
            .fechaCreacion(solicitud.getFechaCreacion())
            .posicionListaEspera(solicitud.getListaEspera() != null
                && "ESPERANDO".equals(solicitud.getListaEspera().getEstado())
                ? solicitud.getListaEspera().getPosicion()
                : null)
            .items(solicitud.getItems().stream()
                .map(ItemSolicitudDTO::desdeItem)
                .toList())
            .build();
    }
}
