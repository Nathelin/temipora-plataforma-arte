package com.temipora.backend_springboot.repositorios;

import java.util.List;
import java.util.Optional;

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

    boolean existsBySha256AndEstadoNot(String sha256, EstadoVariante estado);

    boolean existsBySha256AndEstadoNotAndProductoIdProductoNot(
            String sha256, EstadoVariante estado, Long idProducto);
}
