package com.temipora.backend_springboot.modelo;

import java.math.BigDecimal;
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
public class ServicioDTO {

    private Long idServicio;
    private Long idArtista;
    private String nombreArtista;
    private Long idTipoServicio;
    private String nombreTipoServicio;
    private Long idMedio;
    private String nombreMedio;
    private String nombre;
    private String descripcion;
    private BigDecimal precioBase;
    private String contenidoSensible;
    private Integer tiempoEntregaValor;
    private String tiempoEntregaUnidad;
    private Integer tiempoEntregaDias;
    private Integer cupos;
    private boolean habilitarCuposTrasEntrega;
    private String imagenPortada;
    private List<String> imagenes;
    private boolean estado;

    public static ServicioDTO desdeServicio(Servicio s) {
        return ServicioDTO.builder()
            .idServicio(s.getIdServicio())
            .idArtista(s.getArtista() != null ? s.getArtista().getIdUsuario() : null)
            .nombreArtista(s.getArtista() != null ? s.getArtista().getNombreUsuario() : null)
            .idTipoServicio(s.getTipoServicio() != null ? s.getTipoServicio().getIdTipoServicio() : null)
            .nombreTipoServicio(s.getTipoServicio() != null ? s.getTipoServicio().getNombre() : null)
            .idMedio(s.getMedio() != null ? s.getMedio().getIdMedio() : null)
            .nombreMedio(s.getMedio() != null ? s.getMedio().getNombre() : null)
            .nombre(s.getNombre())
            .descripcion(s.getDescripcion())
            .precioBase(s.getPrecioBase())
            .contenidoSensible(s.getContenidoSensible() != null ? s.getContenidoSensible().name() : null)
            .tiempoEntregaValor(s.getTiempoEntregaValor())
            .tiempoEntregaUnidad(s.getTiempoEntregaUnidad() != null ? s.getTiempoEntregaUnidad().name() : null)
            .tiempoEntregaDias(s.getTiempoEntregaDias())
            .cupos(s.getCupos())
            .habilitarCuposTrasEntrega(s.isHabilitarCuposTrasEntrega())
            .imagenPortada(s.getImagenPortada())
            .imagenes(s.getImagenes().stream().map(ServicioImagen::getUrl).toList())
            .estado(s.isEstado())
            .build();
    }
}
