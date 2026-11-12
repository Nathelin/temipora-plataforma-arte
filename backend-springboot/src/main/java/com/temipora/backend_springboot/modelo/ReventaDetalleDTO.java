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
public class ReventaDetalleDTO {

    private Long idPublicacionReventa;
    private Long idProducto;
    private Long idVariante;
    private String titulo;
    private String nombreVariante;
    private String descripcion;
    private String imagenUrl;
    private String sha256;
    private String moneda;
    private BigDecimal precio;
    private EstadoPublicacionReventa estado;

    private Long idArtista;
    private String nombreArtista;
    private String nombreDisplayArtista;
    private String urlFotoArtista;

    private Long idVendedor;
    private String nombreVendedor;

    private boolean esPropia;
    private boolean puedeComprar;
    private boolean contenidoSensible;
    private List<String> contenidosSensibles;
    private TipoEntrega tipoEntrega;
    private LicenciaCompraDTO licencia;
}
