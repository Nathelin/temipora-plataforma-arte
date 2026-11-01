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
public class OfertaReactivacionDTO {

    private Long idOferta;
    private Long idCampana;
    private String tokenCampana;
    private Long idServicio;
    private String nombreServicio;
    private String descripcionServicio;
    private Long idArtista;
    private String nombreArtista;
    private String urlFotoArtista;
    private String imagen;
    private BigDecimal precioBase;
    private String moneda;
    private BigDecimal porcentajeDescuento;
    private LocalDateTime fechaVencimiento;
    private String motivo;
    private String estado;
    private boolean vigente;

    public static OfertaReactivacionDTO desdeEntidad(
            OfertaReactivacion oferta,
            LocalDateTime ahora) {
        return desdeEntidad(oferta, ahora, null);
    }

    public static OfertaReactivacionDTO desdeEntidad(
            OfertaReactivacion oferta,
            LocalDateTime ahora,
            BigDecimal comisionVigente) {
        Servicio servicio = oferta.getServicio();
        Usuario artista = oferta.getArtista() != null
            ? oferta.getArtista()
            : (servicio != null ? servicio.getArtista() : null);
        CampanaReactivacion campana = oferta.getCampana();

        boolean estadoVigente = oferta.getEstado() == EstadoOfertaReactivacion.OFRECIDA
            || oferta.getEstado() == EstadoOfertaReactivacion.RESERVADA;
        boolean vigente = estadoVigente
            && oferta.getFechaVencimiento() != null
            && oferta.getFechaVencimiento().isAfter(ahora)
            && campana != null
            && campana.getEstado() == EstadoCampanaReactivacion.ACTIVA
            && campana.getFechaVencimiento() != null
            && campana.getFechaVencimiento().isAfter(ahora);

        BigDecimal porcentaje = oferta.getPorcentajeDescuento();
        if (porcentaje != null && comisionVigente != null) {
            porcentaje = porcentaje.min(comisionVigente).max(BigDecimal.ZERO);
        }

        return OfertaReactivacionDTO.builder()
            .idOferta(oferta.getIdOferta())
            .idCampana(campana != null ? campana.getIdCampana() : null)
            .tokenCampana(campana != null ? campana.getToken() : null)
            .idServicio(servicio != null ? servicio.getIdServicio() : null)
            .nombreServicio(servicio != null ? servicio.getNombre() : null)
            .descripcionServicio(servicio != null ? servicio.getDescripcion() : null)
            .idArtista(artista != null ? artista.getIdUsuario() : null)
            .nombreArtista(artista != null ? artista.getNombreUsuario() : null)
            .urlFotoArtista(artista != null ? artista.getUrlFotoPerfil() : null)
            .imagen(servicio != null ? servicio.getImagenPortada() : null)
            .precioBase(servicio != null ? servicio.getPrecioBase() : null)
            .moneda(servicio != null && servicio.getMoneda() != null
                ? servicio.getMoneda().getCodigo()
                : null)
            .porcentajeDescuento(porcentaje)
            .fechaVencimiento(oferta.getFechaVencimiento())
            .motivo(oferta.getMotivo())
            .estado(oferta.getEstado() != null ? oferta.getEstado().name() : null)
            .vigente(vigente)
            .build();
    }
}
