package com.temipora.backend_springboot.repositorios;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.temipora.backend_springboot.modelo.Producto;

/**
 * Señales de interés para el recomendador de productos.
 *
 * El tipo de entidad es sólo un ancla de Spring Data: todas las consultas son
 * nativas y leen las tablas de interacción. Las compras se consideran válidas
 * únicamente cuando el grupo quedó PAGADO, para no contar carritos ni compras
 * canceladas.
 */
public interface RecomendacionProductoRepository
        extends JpaRepository<Producto, Long> {

    @Query(value = """
        SELECT id_producto
        FROM producto_favorito
        WHERE id_usuario = :idUsuario
        """, nativeQuery = true)
    List<Long> productosFavoritos(@Param("idUsuario") Long idUsuario);

    @Query(value = """
        SELECT DISTINCT pe.id_etiqueta
        FROM producto_favorito pf
        JOIN producto_etiqueta pe ON pe.id_producto = pf.id_producto
        WHERE pf.id_usuario = :idUsuario
        """, nativeQuery = true)
    List<Long> etiquetasFavoritas(@Param("idUsuario") Long idUsuario);

    @Query(value = """
        SELECT DISTINCT cg.id_artista
        FROM compra c
        JOIN compra_grupo cg ON cg.id_compra = c.id_compra
        WHERE c.id_cliente = :idUsuario
          AND cg.estado = 'PAGADO'
        """, nativeQuery = true)
    List<Long> artistasComprados(@Param("idUsuario") Long idUsuario);

    @Query(value = """
        SELECT DISTINCT pe.id_etiqueta
        FROM compra c
        JOIN compra_grupo cg ON cg.id_compra = c.id_compra
        JOIN compra_item ci ON ci.id_compra_grupo = cg.id_compra_grupo
        JOIN producto_etiqueta pe ON pe.id_producto = ci.id_producto
        WHERE c.id_cliente = :idUsuario
          AND cg.estado = 'PAGADO'
        """, nativeQuery = true)
    List<Long> etiquetasCompradas(@Param("idUsuario") Long idUsuario);

    /**
     * Co-compra a nivel producto: productos que compraron los usuarios que
     * compraron alguno de mis productos, excluyendo los que ya compré.
     */
    @Query(value = """
        WITH mis_productos AS (
            SELECT DISTINCT ci.id_producto
            FROM compra c
            JOIN compra_grupo cg ON cg.id_compra = c.id_compra
            JOIN compra_item ci ON ci.id_compra_grupo = cg.id_compra_grupo
            WHERE c.id_cliente = :idUsuario
              AND cg.estado = 'PAGADO'
        ),
        usuarios_similares AS (
            SELECT DISTINCT c.id_cliente
            FROM compra c
            JOIN compra_grupo cg ON cg.id_compra = c.id_compra
            JOIN compra_item ci ON ci.id_compra_grupo = cg.id_compra_grupo
            WHERE cg.estado = 'PAGADO'
              AND ci.id_producto IN (SELECT id_producto FROM mis_productos)
              AND c.id_cliente <> :idUsuario
        )
        SELECT ci.id_producto, COUNT(DISTINCT c.id_cliente)
        FROM compra c
        JOIN compra_grupo cg ON cg.id_compra = c.id_compra
        JOIN compra_item ci ON ci.id_compra_grupo = cg.id_compra_grupo
        WHERE cg.estado = 'PAGADO'
          AND c.id_cliente IN (SELECT id_cliente FROM usuarios_similares)
          AND ci.id_producto NOT IN (SELECT id_producto FROM mis_productos)
        GROUP BY ci.id_producto
        """, nativeQuery = true)
    List<Object[]> coCompra(@Param("idUsuario") Long idUsuario);

    @Query(value = """
        SELECT ci.id_producto, COUNT(DISTINCT c.id_cliente)
        FROM compra c
        JOIN compra_grupo cg ON cg.id_compra = c.id_compra
        JOIN compra_item ci ON ci.id_compra_grupo = cg.id_compra_grupo
        WHERE cg.estado = 'PAGADO'
          AND ci.id_producto IN (:ids)
        GROUP BY ci.id_producto
        """, nativeQuery = true)
    List<Object[]> popularidadCompra(@Param("ids") Collection<Long> ids);

    @Query(value = """
        SELECT cg.id_artista, MAX(c.fecha_creacion)
        FROM compra c
        JOIN compra_grupo cg ON cg.id_compra = c.id_compra
        WHERE c.id_cliente = :idUsuario
          AND cg.estado = 'PAGADO'
        GROUP BY cg.id_artista
        """, nativeQuery = true)
    List<Object[]> ultimaCompraPorArtista(@Param("idUsuario") Long idUsuario);

    /**
     * Promedio de precio original comprado por moneda. Se devuelve agrupado
     * para que el servicio convierta cada grupo a la moneda del viewer.
     */
    @Query(value = """
        SELECT ci.id_moneda_original, AVG(ci.precio_original)
        FROM compra c
        JOIN compra_grupo cg ON cg.id_compra = c.id_compra
        JOIN compra_item ci ON ci.id_compra_grupo = cg.id_compra_grupo
        WHERE c.id_cliente = :idUsuario
          AND cg.estado = 'PAGADO'
        GROUP BY ci.id_moneda_original
        """, nativeQuery = true)
    List<Object[]> promedioPrecioComprado(@Param("idUsuario") Long idUsuario);
}
