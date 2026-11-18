package com.temipora.backend_springboot.modelo;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Certificado de autenticidad de una pieza única: creador, propietario actual,
 * trayectoria de propiedad, integridad y estado del anclaje en Bitcoin.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CertificadoDTO {

    private Long idProducto;
    private String titulo;
    private Long idVariante;
    private String nombreVariante;
    private String imagenUrl;

    private Long idArtista;
    private String nombreArtista;
    private String nombreDisplayArtista;
    private String urlFotoArtista;

    private String nombrePropietarioActual;
    private boolean piezaUnica;

    private boolean integridad;
    private String mensaje;

    @Builder.Default
    private List<TrazaEvento> trayectoria = new ArrayList<>();

    private EstadoAnclaje estadoAnclaje;
    private LocalDateTime fechaAnclaje;
    private Long bitcoinAltura;
    private String hashRaiz;
    private String hashBitcoin;

    private PruebaInclusionDTO pruebaInclusion;

    private String codigoCertificado;
    private String urlVerificacion;

    /** Un movimiento del historial de propiedad. */
    public record TrazaEvento(
            Long secuencia,
            TipoEventoLedger tipo,
            LocalDateTime ocurridoEn,
            boolean verificado) {
    }
}
