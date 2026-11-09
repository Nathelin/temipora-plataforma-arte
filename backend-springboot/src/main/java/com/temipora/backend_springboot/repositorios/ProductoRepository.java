package com.temipora.backend_springboot.repositorios;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.temipora.backend_springboot.modelo.EstadoProducto;
import com.temipora.backend_springboot.modelo.Producto;

import jakarta.persistence.LockModeType;

public interface ProductoRepository
        extends JpaRepository<Producto, Long>, JpaSpecificationExecutor<Producto> {

    @EntityGraph(attributePaths = {
        "artista",
        "tipoProducto",
        "medio",
        "moneda",
        "variantes",
        "variantes.propietarioActual",
        "imagenes",
        "etiquetas",
        "etiquetas.etiqueta",
        "etiquetas.etiqueta.categoria"
    })
    Optional<Producto> findByIdProducto(Long idProducto);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Producto p WHERE p.idProducto = :idProducto")
    Optional<Producto> findByIdProductoForUpdate(@Param("idProducto") Long idProducto);

    @EntityGraph(attributePaths = {
        "artista",
        "tipoProducto",
        "medio",
        "moneda",
        "variantes",
        "variantes.propietarioActual",
        "imagenes",
        "etiquetas",
        "etiquetas.etiqueta",
        "etiquetas.etiqueta.categoria"
    })
    List<Producto> findByArtistaIdUsuarioAndEstadoNotOrderByTituloAsc(
            Long idUsuario, EstadoProducto estado);

    boolean existsByArtistaIdUsuarioAndTipoProductoIdTipoProductoAndTituloIgnoreCaseAndEstadoNot(
            Long idUsuario, Long idTipoProducto, String titulo, EstadoProducto estado);

    boolean existsByArtistaIdUsuarioAndTipoProductoIdTipoProductoAndTituloIgnoreCaseAndIdProductoNotAndEstadoNot(
            Long idUsuario, Long idTipoProducto, String titulo, Long idProducto, EstadoProducto estado);
}
