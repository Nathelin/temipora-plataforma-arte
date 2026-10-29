package com.temipora.backend_springboot.modelo;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Comparator;
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
    private String descripcionTipoServicio;
    private Long idMedio;
    private String nombreMedio;
    private String nombre;
    private String descripcion;
    private BigDecimal precioBase;
    private Long idMoneda;
    private String moneda;
    // Precio y moneda listos para mostrar según la preferencia del usuario.
    // Si no hay conversión disponible coinciden con precioBase/moneda.
    private BigDecimal precioBaseMostrado;
    private String monedaMostrada;
    private boolean contenidoSensible;
    private List<String> contenidosSensibles;
    private boolean requiereAdvertencia;
    private Integer tiempoEntregaValor;
    private String tiempoEntregaUnidad;
    private Integer tiempoEntregaDias;
    private Integer cupos;
    private Integer cuposOcupados;
    private Integer cuposDisponibles;
    private String modoRecepcion;
    private Integer maxDibujosPorSolicitud;
    private boolean permiteListaEspera;
    private boolean habilitarCuposTrasEntrega;
    private String imagenPortada;
    private List<String> imagenes;
    private List<EtiquetaDTO> etiquetas;
    private EstadoServicio estado;
    private Long solicitudesActivas;
    private BigDecimal calificacionPromedio;
    private Long cantidadResenias;
    private LocalDateTime pausadoEn;
    private LocalDateTime archivadoEn;

    public static ServicioDTO desdeServicio(Servicio s) {
        return desdeServicio(s, false, null, null);
    }

    public static ServicioDTO desdeServicio(Servicio s, boolean requiereAdvertencia) {
        return desdeServicio(s, requiereAdvertencia, null, null);
    }

    public static ServicioDTO desdeServicio(
            Servicio s,
            boolean requiereAdvertencia,
            Moneda monedaDestino,
            BigDecimal factor) {
        Moneda origen = s.getMoneda();
        String codigoOrigen = origen != null ? origen.getCodigo() : null;
        String codigoMostrado = codigoOrigen;
        BigDecimal precioMostrado = s.getPrecioBase();
        if (factor != null && monedaDestino != null && origen != null
                && !monedaDestino.getIdMoneda().equals(origen.getIdMoneda())
                && s.getPrecioBase() != null) {
            codigoMostrado = monedaDestino.getCodigo();
            precioMostrado = s.getPrecioBase().multiply(factor).setScale(2, RoundingMode.HALF_UP);
        }

        return ServicioDTO.builder()
            .idServicio(s.getIdServicio())
            .idArtista(s.getArtista() != null ? s.getArtista().getIdUsuario() : null)
            .nombreArtista(s.getArtista() != null ? s.getArtista().getNombreUsuario() : null)
            .idTipoServicio(s.getTipoServicio() != null ? s.getTipoServicio().getIdTipoServicio() : null)
            .nombreTipoServicio(s.getTipoServicio() != null ? s.getTipoServicio().getNombre() : null)
            .descripcionTipoServicio(s.getTipoServicio() != null ? s.getTipoServicio().getDescripcion() : null)
            .idMedio(s.getMedio() != null ? s.getMedio().getIdMedio() : null)
            .nombreMedio(s.getMedio() != null ? s.getMedio().getNombre() : null)
            .nombre(s.getNombre())
            .descripcion(s.getDescripcion())
            .precioBase(s.getPrecioBase())
            .idMoneda(origen != null ? origen.getIdMoneda() : null)
            .moneda(codigoOrigen)
            .precioBaseMostrado(precioMostrado)
            .monedaMostrada(codigoMostrado)
            .contenidoSensible(s.isContenidoSensible())
            .contenidosSensibles(s.getContenidosSensibles().stream()
                .map(Enum::name)
                .toList())
            .requiereAdvertencia(requiereAdvertencia)
            .tiempoEntregaValor(s.getTiempoEntregaValor())
            .tiempoEntregaUnidad(s.getTiempoEntregaUnidad() != null ? s.getTiempoEntregaUnidad().name() : null)
            .tiempoEntregaDias(s.getTiempoEntregaDias())
            .cupos(s.getCupos())
            .cuposOcupados(0)
            .cuposDisponibles(s.getCupos())
            .modoRecepcion(s.getModoRecepcion() != null ? s.getModoRecepcion().name() : null)
            .maxDibujosPorSolicitud(s.getMaxDibujosPorSolicitud())
            .permiteListaEspera(s.isPermiteListaEspera())
            .habilitarCuposTrasEntrega(s.isHabilitarCuposTrasEntrega())
            .imagenPortada(s.getImagenPortada())
            .imagenes(s.getImagenes().stream().map(ServicioImagen::getUrl).toList())
            .etiquetas(s.getEtiquetas().stream()
                .map(ServicioEtiqueta::getEtiqueta)
                .sorted(Comparator.comparing(Etiqueta::getNombre, String.CASE_INSENSITIVE_ORDER))
                .map(EtiquetaDTO::desdeEtiqueta)
                .toList())
            .estado(s.getEstado())
            .pausadoEn(s.getPausadoEn())
            .archivadoEn(s.getArchivadoEn())
            .build();
    }
}
