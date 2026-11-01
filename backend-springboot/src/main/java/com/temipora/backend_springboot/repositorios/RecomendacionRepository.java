package com.temipora.backend_springboot.repositorios;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.temipora.backend_springboot.modelo.Servicio;

/**
 * Consultas de señales para el recomendador. El tipo de entidad es sólo un
 * ancla de Spring Data; todas las consultas son nativas y leen las tablas de
 * interacción (pedidos, favoritos, visitas).
 */
public interface RecomendacionRepository extends JpaRepository<Servicio, Long> {

    @Query(value = """
        SELECT DISTINCT p.id_artista
        FROM pedido p
        WHERE p.id_cliente = :idUsuario
        """, nativeQuery = true)
    List<Long> artistasComprados(@Param("idUsuario") Long idUsuario);

    @Query(value = """
        SELECT DISTINCT se.id_etiqueta
        FROM pedido p
        JOIN servicio_etiqueta se ON se.id_servicio = p.id_servicio
        WHERE p.id_cliente = :idUsuario
        """, nativeQuery = true)
    List<Long> etiquetasCompradas(@Param("idUsuario") Long idUsuario);

    @Query(value = """
        SELECT p2.id_artista, COUNT(DISTINCT p1.id_cliente)
        FROM pedido p1
        JOIN pedido p2 ON p2.id_cliente = p1.id_cliente
        WHERE p1.id_cliente = :idUsuario
          AND p2.id_artista <> p1.id_artista
        GROUP BY p2.id_artista
        """, nativeQuery = true)
    List<Object[]> coCompra(@Param("idUsuario") Long idUsuario);

    @Query(value = """
        SELECT p.id_artista, COUNT(DISTINCT p.id_cliente)
        FROM pedido p
        WHERE p.id_artista IN (:ids)
        GROUP BY p.id_artista
        """, nativeQuery = true)
    List<Object[]> popularidadCompra(@Param("ids") Collection<Long> ids);

    @Query(value = """
        SELECT id_artista
        FROM artista_favorito
        WHERE id_usuario = :idUsuario
        """, nativeQuery = true)
    List<Long> artistasFavoritos(@Param("idUsuario") Long idUsuario);

    @Query(value = """
        SELECT id_servicio
        FROM servicio_favorito
        WHERE id_usuario = :idUsuario
        """, nativeQuery = true)
    List<Long> serviciosFavoritos(@Param("idUsuario") Long idUsuario);

    @Query(value = """
        SELECT DISTINCT se.id_etiqueta
        FROM servicio_favorito sf
        JOIN servicio_etiqueta se ON se.id_servicio = sf.id_servicio
        WHERE sf.id_usuario = :idUsuario
        """, nativeQuery = true)
    List<Long> etiquetasFavoritas(@Param("idUsuario") Long idUsuario);

    @Query(value = """
        SELECT DISTINCT e.id_categoria
        FROM etiqueta e
        WHERE e.id_etiqueta IN (:ids)
        """, nativeQuery = true)
    List<Long> categoriasDeEtiquetas(@Param("ids") Collection<Long> ids);

    @Query(value = """
        SELECT p.id_artista, MAX(p.fecha_creacion)
        FROM pedido p
        WHERE p.id_cliente = :idUsuario
        GROUP BY p.id_artista
        """, nativeQuery = true)
    List<Object[]> ultimaCompraPorArtista(@Param("idUsuario") Long idUsuario);

    @Query(value = """
        SELECT id_artista, MAX(fecha_agregado)
        FROM artista_favorito
        WHERE id_usuario = :idUsuario
        GROUP BY id_artista
        """, nativeQuery = true)
    List<Object[]> ultimoFavoritoArtista(@Param("idUsuario") Long idUsuario);
}
