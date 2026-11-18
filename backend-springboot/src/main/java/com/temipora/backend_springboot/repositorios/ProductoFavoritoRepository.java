package com.temipora.backend_springboot.repositorios;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.temipora.backend_springboot.modelo.ProductoFavorito;
import com.temipora.backend_springboot.modelo.ProductoFavoritoId;

public interface ProductoFavoritoRepository
        extends JpaRepository<ProductoFavorito, ProductoFavoritoId> {

    Optional<ProductoFavorito> findByUsuarioIdUsuarioAndProductoIdProducto(
            Long idUsuario, Long idProducto);

    boolean existsByUsuarioIdUsuarioAndProductoIdProducto(
            Long idUsuario, Long idProducto);

    @Query("SELECT f.id.idProducto FROM ProductoFavorito f WHERE f.id.idUsuario = :idUsuario")
    List<Long> findIdsProductoByUsuario(@Param("idUsuario") Long idUsuario);
}
