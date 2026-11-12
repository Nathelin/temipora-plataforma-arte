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
public class PublicacionReventaDTO {

    private Long idPublicacionReventa;
    private Long idVariante;
    private String nombreVariante;
    private Long idProducto;
    private String titulo;
    private String tituloOriginal;
    private String imagenUrl;
    private Long idArtistaOriginal;
    private String nombreArtistaOriginal;
    private Long idCompraItemOrigen;

    private Long idVendedor;
    private String nombreVendedor;
    private BigDecimal precio;
    private BigDecimal precioTope;
    private String moneda;
    private String estado;
    private LocalDateTime fechaPublicacion;
    private LocalDateTime fechaCierre;

    private Long idComprador;
    private String nombreComprador;
    private BigDecimal montoComision;
    private BigDecimal montoNeto;

    public static PublicacionReventaDTO desde(PublicacionReventa publicacion) {
        ProductoVariante variante = publicacion.getVariante();
        Producto producto = variante != null ? variante.getProducto() : null;
        Usuario artista = producto != null ? producto.getArtista() : null;
        CompraItem origen = publicacion.getCompraItemOrigen();
        return PublicacionReventaDTO.builder()
            .idPublicacionReventa(publicacion.getIdPublicacionReventa())
            .idVariante(variante != null ? variante.getIdVariante() : null)
            .nombreVariante(origen != null ? origen.getNombreVariante()
                : (variante != null ? variante.getNombre() : null))
            .idProducto(producto != null ? producto.getIdProducto() : null)
            .titulo(origen != null ? origen.getNombreVariante()
                : (variante != null ? variante.getNombre() : null))
            .tituloOriginal(origen != null ? origen.getTituloProducto()
                : (producto != null ? producto.getTitulo() : null))
            .imagenUrl(variante != null ? variante.getImagenUrl() : null)
            .idArtistaOriginal(artista != null ? artista.getIdUsuario() : null)
            .nombreArtistaOriginal(artista != null
                ? artista.getNombreUsuario() : null)
            .idCompraItemOrigen(origen != null ? origen.getIdCompraItem() : null)
            .idVendedor(publicacion.getVendedor() != null
                ? publicacion.getVendedor().getIdUsuario() : null)
            .nombreVendedor(publicacion.getVendedor() != null
                ? publicacion.getVendedor().getNombreUsuario() : null)
            .precio(publicacion.getPrecio())
            .precioTope(variante != null ? variante.getPrecioTopeReventa() : null)
            .moneda(publicacion.getMoneda() != null
                ? publicacion.getMoneda().getCodigo() : null)
            .estado(publicacion.getEstado() != null
                ? publicacion.getEstado().name() : null)
            .fechaPublicacion(publicacion.getFechaPublicacion())
            .fechaCierre(publicacion.getFechaCierre())
            .build();
    }
}
