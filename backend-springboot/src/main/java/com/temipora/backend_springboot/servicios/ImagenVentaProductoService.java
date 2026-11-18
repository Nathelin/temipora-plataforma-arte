package com.temipora.backend_springboot.servicios;

import java.util.Comparator;

import org.springframework.stereotype.Service;

import com.temipora.backend_springboot.modelo.CompraItem;
import com.temipora.backend_springboot.modelo.Producto;
import com.temipora.backend_springboot.modelo.ProductoImagen;
import com.temipora.backend_springboot.modelo.ProductoVariante;
import com.temipora.backend_springboot.repositorios.ProductoVarianteImagenRepository;

/**
 * Resuelve la miniatura de un producto vendido respetando la versión exacta
 * que se compró (sha256) y cayendo a la imagen de la variante o la portada.
 */
@Service
public class ImagenVentaProductoService {

    private final ProductoVarianteImagenRepository varianteImagenRepository;

    public ImagenVentaProductoService(
            ProductoVarianteImagenRepository varianteImagenRepository) {
        this.varianteImagenRepository = varianteImagenRepository;
    }

    public String url(CompraItem item) {
        ProductoVariante variante = item.getVariante();
        if (variante != null) {
            if (item.getSha256Archivo() != null
                    && !item.getSha256Archivo().isBlank()) {
                var version = varianteImagenRepository
                    .findFirstByVarianteIdVarianteAndSha256OrderByNumeroDesc(
                        variante.getIdVariante(), item.getSha256Archivo());
                if (version.isPresent() && version.get().getUrl() != null) {
                    return version.get().getUrl();
                }
            }
            if (variante.getImagenUrl() != null
                    && !variante.getImagenUrl().isBlank()) {
                return variante.getImagenUrl();
            }
        }
        Producto producto = item.getProducto();
        if (producto != null) {
            if (producto.getImagenPortada() != null
                    && !producto.getImagenPortada().isBlank()) {
                return producto.getImagenPortada();
            }
            if (producto.getImagenes() != null) {
                return producto.getImagenes().stream()
                    .filter(imagen -> imagen != null && imagen.getUrl() != null)
                    .sorted(Comparator.comparing(ProductoImagen::getOrden,
                        Comparator.nullsLast(Integer::compareTo)))
                    .map(ProductoImagen::getUrl)
                    .findFirst()
                    .orElse(null);
            }
        }
        return null;
    }
}
