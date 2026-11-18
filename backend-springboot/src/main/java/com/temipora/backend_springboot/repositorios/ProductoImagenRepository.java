package com.temipora.backend_springboot.repositorios;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.temipora.backend_springboot.modelo.ProductoImagen;

public interface ProductoImagenRepository extends JpaRepository<ProductoImagen, Long> {

    List<ProductoImagen> findByProductoIdProductoOrderByOrdenAsc(Long idProducto);

    boolean existsBySha256AndProductoIdProductoNot(String sha256, Long idProducto);

    boolean existsBySha256(String sha256);

    @Query("SELECT i.url FROM ProductoImagen i WHERE i.url IS NOT NULL")
    List<String> findUrls();

    boolean existsByUrl(String url);
}
