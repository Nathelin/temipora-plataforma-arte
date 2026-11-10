package com.temipora.backend_springboot.repositorios;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.temipora.backend_springboot.modelo.EstadoVariante;
import com.temipora.backend_springboot.modelo.ProductoVariante;

import jakarta.persistence.LockModeType;

public interface ProductoVarianteRepository
        extends JpaRepository<ProductoVariante, Long> {

    Optional<ProductoVariante> findByIdVariante(Long idVariante);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT v FROM ProductoVariante v WHERE v.idVariante = :idVariante")
    Optional<ProductoVariante> findByIdVarianteForUpdate(@Param("idVariante") Long idVariante);

    List<ProductoVariante> findByProductoIdProductoOrderByOrdenAsc(Long idProducto);

    @EntityGraph(attributePaths = "propietarioActual")
    Page<ProductoVariante> findByProductoIdProducto(Long idProducto, Pageable pageable);

    boolean existsBySha256AndEstadoNot(String sha256, EstadoVariante estado);

    boolean existsBySha256AndEstadoNotAndProductoIdProductoNot(
            String sha256, EstadoVariante estado, Long idProducto);

    @Query("SELECT v.imagenUrl FROM ProductoVariante v WHERE v.imagenUrl IS NOT NULL")
    List<String> findUrlsImagen();

    boolean existsByImagenUrl(String imagenUrl);
}
