package com.temipora.backend_springboot.repositorios;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.temipora.backend_springboot.modelo.EstadoPedido;
import com.temipora.backend_springboot.modelo.EstadoReporteResenia;
import com.temipora.backend_springboot.modelo.EstadoResenia;
import com.temipora.backend_springboot.modelo.Resenia;

import jakarta.persistence.LockModeType;

public interface ReseniaRepository extends JpaRepository<Resenia, Long> {

    Optional<Resenia> findByPedidoIdPedidoAndAutorIdUsuario(
        Long idPedido, Long idAutor);

    boolean existsByPedidoIdPedidoAndAutorIdUsuario(Long idPedido, Long idAutor);

    List<Resenia> findByPedidoIdPedidoAndEstadoOrderByFechaCreacionAsc(
        Long idPedido, EstadoResenia estado);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT r FROM Resenia r
        WHERE r.pedido.idPedido = :idPedido AND r.autor.idUsuario = :idAutor
        """)
    Optional<Resenia> findForUpdateByPedidoAndAutor(
        @Param("idPedido") Long idPedido,
        @Param("idAutor") Long idAutor);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM Resenia r WHERE r.idResenia = :idResenia")
    Optional<Resenia> findForUpdateById(
        @Param("idResenia") Long idResenia);

    // -----------------------------------------------------------------
    // Reseñas de producto
    // -----------------------------------------------------------------

    Optional<Resenia> findByProductoIdProductoAndAutorIdUsuario(
        Long idProducto, Long idAutor);

    boolean existsByProductoIdProductoAndAutorIdUsuario(
        Long idProducto, Long idAutor);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT r FROM Resenia r
        WHERE r.producto.idProducto = :idProducto
          AND r.autor.idUsuario = :idAutor
        """)
    Optional<Resenia> findForUpdateByProductoAndAutor(
        @Param("idProducto") Long idProducto,
        @Param("idAutor") Long idAutor);

    @Query(value = """
        SELECT r FROM Resenia r
        JOIN r.versiones v
        JOIN FETCH r.producto prod
        LEFT JOIN FETCH r.compraItem ci
        JOIN FETCH r.autor a
        WHERE prod.idProducto = :idProducto
          AND r.estado = :estado
          AND v.numero = (
              SELECT MAX(v2.numero) FROM ReseniaVersion v2 WHERE v2.resenia = r
          )
          AND (:calificacion IS NULL OR v.calificacionGeneral = :calificacion)
          AND (:idOpcion IS NULL OR EXISTS (
              SELECT 1 FROM ReseniaOpcion ro
              WHERE ro.version = v
                AND ro.opcion.idOpcionCriterio = :idOpcion
          ))
        ORDER BY
          CASE WHEN :orden = 'MEJOR' THEN v.calificacionGeneral END DESC,
          CASE WHEN :orden = 'PEOR' THEN v.calificacionGeneral END ASC,
          r.fechaCreacion DESC
        """,
        countQuery = """
        SELECT COUNT(r) FROM Resenia r
        JOIN r.versiones v
        JOIN r.producto prod
        WHERE prod.idProducto = :idProducto
          AND r.estado = :estado
          AND v.numero = (
              SELECT MAX(v2.numero) FROM ReseniaVersion v2 WHERE v2.resenia = r
          )
          AND (:calificacion IS NULL OR v.calificacionGeneral = :calificacion)
          AND (:idOpcion IS NULL OR EXISTS (
              SELECT 1 FROM ReseniaOpcion ro
              WHERE ro.version = v
                AND ro.opcion.idOpcionCriterio = :idOpcion
          ))
        """)
    Page<Resenia> findPublicasPorProducto(
        @Param("idProducto") Long idProducto,
        @Param("idOpcion") Long idOpcion,
        @Param("calificacion") Integer calificacion,
        @Param("orden") String orden,
        @Param("estado") EstadoResenia estado,
        Pageable pageable);

    @Query("""
        SELECT COUNT(r), AVG(v.calificacionGeneral)
        FROM Resenia r
        JOIN r.versiones v
        JOIN r.producto prod
        WHERE r.estado = :estado
          AND prod.idProducto = :idProducto
          AND v.numero = (
              SELECT MAX(v2.numero) FROM ReseniaVersion v2 WHERE v2.resenia = r
          )
        """)
    List<Object[]> resumenConteoPromedioProducto(
        @Param("idProducto") Long idProducto,
        @Param("estado") EstadoResenia estado);

    @Query("""
        SELECT r FROM Resenia r
        JOIN FETCH r.producto prod
        JOIN FETCH r.autor a
        WHERE prod.idProducto IN :idsProducto
          AND a.idUsuario = :idAutor
          AND r.estado = :estado
        """)
    List<Resenia> findPublicadasPorProductosYAutor(
        @Param("idsProducto") Collection<Long> idsProducto,
        @Param("idAutor") Long idAutor,
        @Param("estado") EstadoResenia estado);

    @Query("""
        SELECT v.calificacionGeneral, COUNT(r)
        FROM Resenia r
        JOIN r.versiones v
        JOIN r.producto prod
        WHERE r.estado = :estado
          AND prod.idProducto = :idProducto
          AND v.numero = (
              SELECT MAX(v2.numero) FROM ReseniaVersion v2 WHERE v2.resenia = r
          )
        GROUP BY v.calificacionGeneral
        """)
    List<Object[]> distribucionPublicaProducto(
        @Param("idProducto") Long idProducto,
        @Param("estado") EstadoResenia estado);

    @Query("""
        SELECT rv.criterio.idCriterioResenia, rv.criterio.nombre,
               rv.criterio.orden, AVG(rv.valor), COUNT(DISTINCT r)
        FROM Resenia r
        JOIN r.versiones v
        JOIN v.valores rv
        JOIN r.producto prod
        WHERE r.estado = :estado
          AND prod.idProducto = :idProducto
          AND v.numero = (
              SELECT MAX(v2.numero) FROM ReseniaVersion v2 WHERE v2.resenia = r
          )
        GROUP BY rv.criterio.idCriterioResenia, rv.criterio.nombre, rv.criterio.orden
        ORDER BY rv.criterio.orden ASC, rv.criterio.nombre ASC
        """)
    List<Object[]> criteriosPublicosProducto(
        @Param("idProducto") Long idProducto,
        @Param("estado") EstadoResenia estado);

    @Query("""
        SELECT ro.opcion.idOpcionCriterio, ro.opcion.etiqueta, ro.opcion.polaridad,
               COUNT(DISTINCT r)
        FROM Resenia r
        JOIN r.versiones v
        JOIN v.opciones ro
        JOIN r.producto prod
        WHERE r.estado = :estado
          AND prod.idProducto = :idProducto
          AND v.numero = (
              SELECT MAX(v2.numero) FROM ReseniaVersion v2 WHERE v2.resenia = r
          )
        GROUP BY ro.opcion.idOpcionCriterio, ro.opcion.etiqueta, ro.opcion.polaridad
        ORDER BY COUNT(DISTINCT r) DESC, ro.opcion.etiqueta ASC
        """)
    List<Object[]> opcionesDestacadasPublicasProducto(
        @Param("idProducto") Long idProducto,
        @Param("estado") EstadoResenia estado);

    // -----------------------------------------------------------------
    // Moderación
    // -----------------------------------------------------------------

    @Query(value = """
        SELECT r FROM Resenia r
        LEFT JOIN FETCH r.pedido p
        LEFT JOIN FETCH p.servicio s
        LEFT JOIN FETCH r.producto prod
        JOIN FETCH r.autor a
        WHERE EXISTS (
            SELECT 1 FROM ReporteResenia rr
            WHERE rr.resenia = r AND rr.estado = :estadoReporte
        )
        ORDER BY r.fechaCreacion DESC
        """,
        countQuery = """
        SELECT COUNT(r) FROM Resenia r
        WHERE EXISTS (
            SELECT 1 FROM ReporteResenia rr
            WHERE rr.resenia = r AND rr.estado = :estadoReporte
        )
        """)
    Page<Resenia> findConReportesPendientes(
        @Param("estadoReporte") EstadoReporteResenia estadoReporte,
        Pageable pageable);

    @Query(value = """
        SELECT r FROM Resenia r
        LEFT JOIN FETCH r.pedido p
        LEFT JOIN FETCH p.servicio s
        LEFT JOIN FETCH r.producto prod
        JOIN FETCH r.autor a
        WHERE r.estado = :estado
        ORDER BY COALESCE(r.moderadaEn, r.fechaCreacion) DESC
        """,
        countQuery = """
        SELECT COUNT(r) FROM Resenia r
        WHERE r.estado = :estado
        """)
    Page<Resenia> findPorEstado(
        @Param("estado") EstadoResenia estado,
        Pageable pageable);

    @Query("""
        SELECT COUNT(r) FROM Resenia r
        WHERE r.pedido.cliente.idUsuario = :idCliente
          AND r.autor.idUsuario = r.pedido.artista.idUsuario
          AND r.estado = :estado
        """)
    long contarReseniasDeCliente(
        @Param("idCliente") Long idCliente,
        @Param("estado") EstadoResenia estado);

    @Query("""
        SELECT AVG(v.calificacionGeneral) FROM Resenia r
        JOIN r.versiones v
        WHERE r.pedido.cliente.idUsuario = :idCliente
          AND r.autor.idUsuario = r.pedido.artista.idUsuario
          AND r.estado = :estado
          AND v.numero = (
              SELECT MAX(v2.numero) FROM ReseniaVersion v2 WHERE v2.resenia = r
          )
        """)
    Double promedioReseniasDeCliente(
        @Param("idCliente") Long idCliente,
        @Param("estado") EstadoResenia estado);

    // -----------------------------------------------------------------
    // Reseñas públicas del servicio (autor = cliente)
    // -----------------------------------------------------------------

    @Query(value = """
        SELECT r FROM Resenia r
        JOIN r.versiones v
        JOIN FETCH r.pedido p
        JOIN FETCH p.servicio s
        JOIN FETCH r.autor a
        WHERE s.idServicio = :idServicio
          AND r.estado = :estado
          AND p.estado = :estadoPedido
          AND r.autor.idUsuario = p.cliente.idUsuario
          AND v.numero = (
              SELECT MAX(v2.numero) FROM ReseniaVersion v2 WHERE v2.resenia = r
          )
          AND (:calificacion IS NULL OR v.calificacionGeneral = :calificacion)
          AND (:idOpcion IS NULL OR EXISTS (
              SELECT 1 FROM ReseniaOpcion ro
              WHERE ro.version = v
                AND ro.opcion.idOpcionCriterio = :idOpcion
          ))
        ORDER BY
          CASE WHEN :orden = 'MEJOR' THEN v.calificacionGeneral END DESC,
          CASE WHEN :orden = 'PEOR' THEN v.calificacionGeneral END ASC,
          r.fechaCreacion DESC
        """,
        countQuery = """
        SELECT COUNT(r) FROM Resenia r
        JOIN r.versiones v
        JOIN r.pedido p
        JOIN p.servicio s
        WHERE s.idServicio = :idServicio
          AND r.estado = :estado
          AND p.estado = :estadoPedido
          AND r.autor.idUsuario = p.cliente.idUsuario
          AND v.numero = (
              SELECT MAX(v2.numero) FROM ReseniaVersion v2 WHERE v2.resenia = r
          )
          AND (:calificacion IS NULL OR v.calificacionGeneral = :calificacion)
          AND (:idOpcion IS NULL OR EXISTS (
              SELECT 1 FROM ReseniaOpcion ro
              WHERE ro.version = v
                AND ro.opcion.idOpcionCriterio = :idOpcion
          ))
        """)
    Page<Resenia> findPublicasPorServicio(
        @Param("idServicio") Long idServicio,
        @Param("idOpcion") Long idOpcion,
        @Param("calificacion") Integer calificacion,
        @Param("orden") String orden,
        @Param("estado") EstadoResenia estado,
        @Param("estadoPedido") EstadoPedido estadoPedido,
        Pageable pageable);

    @Query(value = """
        SELECT r FROM Resenia r
        JOIN r.versiones v
        LEFT JOIN FETCH r.pedido p
        LEFT JOIN FETCH p.servicio s
        LEFT JOIN FETCH r.producto prod
        JOIN FETCH r.autor a
        WHERE (
            (:incluirServicios = true
             AND r.pedido IS NOT NULL
             AND s.artista.idUsuario = :idArtista
             AND p.estado = :estadoPedido
             AND r.autor.idUsuario = p.cliente.idUsuario)
            OR
            (:incluirProductos = true
             AND r.producto IS NOT NULL
             AND prod.artista.idUsuario = :idArtista)
        )
          AND (:idServicio IS NULL OR s.idServicio = :idServicio)
          AND (:idProducto IS NULL OR prod.idProducto = :idProducto)
          AND r.estado = :estado
          AND v.numero = (
              SELECT MAX(v2.numero) FROM ReseniaVersion v2 WHERE v2.resenia = r
          )
          AND (:calificacion IS NULL OR v.calificacionGeneral = :calificacion)
        ORDER BY
          CASE WHEN :orden = 'MEJOR' THEN v.calificacionGeneral END DESC,
          CASE WHEN :orden = 'PEOR' THEN v.calificacionGeneral END ASC,
          r.fechaCreacion DESC
        """,
        countQuery = """
        SELECT COUNT(r) FROM Resenia r
        JOIN r.versiones v
        LEFT JOIN r.pedido p
        LEFT JOIN p.servicio s
        LEFT JOIN r.producto prod
        WHERE (
            (:incluirServicios = true
             AND r.pedido IS NOT NULL
             AND s.artista.idUsuario = :idArtista
             AND p.estado = :estadoPedido
             AND r.autor.idUsuario = p.cliente.idUsuario)
            OR
            (:incluirProductos = true
             AND r.producto IS NOT NULL
             AND prod.artista.idUsuario = :idArtista)
        )
          AND (:idServicio IS NULL OR s.idServicio = :idServicio)
          AND (:idProducto IS NULL OR prod.idProducto = :idProducto)
          AND r.estado = :estado
          AND v.numero = (
              SELECT MAX(v2.numero) FROM ReseniaVersion v2 WHERE v2.resenia = r
          )
          AND (:calificacion IS NULL OR v.calificacionGeneral = :calificacion)
        """)
    Page<Resenia> findPublicasPorArtista(
        @Param("idArtista") Long idArtista,
        @Param("idServicio") Long idServicio,
        @Param("idProducto") Long idProducto,
        @Param("incluirServicios") boolean incluirServicios,
        @Param("incluirProductos") boolean incluirProductos,
        @Param("calificacion") Integer calificacion,
        @Param("orden") String orden,
        @Param("estado") EstadoResenia estado,
        @Param("estadoPedido") EstadoPedido estadoPedido,
        Pageable pageable);

    @Query("""
        SELECT r FROM Resenia r
        JOIN FETCH r.pedido p
        JOIN FETCH p.servicio s
        JOIN FETCH r.autor a
        WHERE p.idPedido IN :ids
          AND r.estado = :estado
          AND p.estado = :estadoPedido
          AND r.autor.idUsuario = p.cliente.idUsuario
        """)
    List<Resenia> findPublicasPorPedidos(
        @Param("ids") Collection<Long> ids,
        @Param("estado") EstadoResenia estado,
        @Param("estadoPedido") EstadoPedido estadoPedido);

    @Query("""
        SELECT s.idServicio, COUNT(r), AVG(v.calificacionGeneral)
        FROM Resenia r
        JOIN r.versiones v
        JOIN r.pedido p
        JOIN p.servicio s
        WHERE s.idServicio IN :ids
          AND r.estado = :estado
          AND p.estado = :estadoPedido
          AND r.autor.idUsuario = p.cliente.idUsuario
          AND v.numero = (
              SELECT MAX(v2.numero) FROM ReseniaVersion v2 WHERE v2.resenia = r
          )
        GROUP BY s.idServicio
        """)
    List<Object[]> promediosPorServicios(
        @Param("ids") Collection<Long> ids,
        @Param("estado") EstadoResenia estado,
        @Param("estadoPedido") EstadoPedido estadoPedido);

    @Query("""
        SELECT prod.idProducto, COUNT(r), AVG(v.calificacionGeneral)
        FROM Resenia r
        JOIN r.versiones v
        JOIN r.producto prod
        WHERE prod.idProducto IN :ids
          AND r.estado = :estado
          AND v.numero = (
              SELECT MAX(v2.numero) FROM ReseniaVersion v2 WHERE v2.resenia = r
          )
        GROUP BY prod.idProducto
        """)
    List<Object[]> promediosPorProductos(
        @Param("ids") Collection<Long> ids,
        @Param("estado") EstadoResenia estado);

    // -----------------------------------------------------------------
    // Agregados del resumen público
    // -----------------------------------------------------------------

    @Query("""
        SELECT COUNT(r), AVG(v.calificacionGeneral)
        FROM Resenia r
        JOIN r.versiones v
        LEFT JOIN r.pedido p
        LEFT JOIN p.servicio s
        LEFT JOIN r.producto prod
        WHERE r.estado = :estado
          AND v.numero = (
              SELECT MAX(v2.numero) FROM ReseniaVersion v2 WHERE v2.resenia = r
          )
          AND (
              (r.pedido IS NOT NULL
               AND p.estado = :estadoPedido
               AND r.autor.idUsuario = p.cliente.idUsuario)
              OR r.producto IS NOT NULL
          )
          AND (:idServicio IS NULL OR s.idServicio = :idServicio)
          AND (:idArtista IS NULL
               OR s.artista.idUsuario = :idArtista
               OR prod.artista.idUsuario = :idArtista)
        """)
    List<Object[]> resumenConteoPromedio(
        @Param("idServicio") Long idServicio,
        @Param("idArtista") Long idArtista,
        @Param("estado") EstadoResenia estado,
        @Param("estadoPedido") EstadoPedido estadoPedido);

    @Query("""
        SELECT v.calificacionGeneral, COUNT(r)
        FROM Resenia r
        JOIN r.versiones v
        LEFT JOIN r.pedido p
        LEFT JOIN p.servicio s
        LEFT JOIN r.producto prod
        WHERE r.estado = :estado
          AND v.numero = (
              SELECT MAX(v2.numero) FROM ReseniaVersion v2 WHERE v2.resenia = r
          )
          AND (
              (r.pedido IS NOT NULL
               AND p.estado = :estadoPedido
               AND r.autor.idUsuario = p.cliente.idUsuario)
              OR r.producto IS NOT NULL
          )
          AND (:idServicio IS NULL OR s.idServicio = :idServicio)
          AND (:idArtista IS NULL
               OR s.artista.idUsuario = :idArtista
               OR prod.artista.idUsuario = :idArtista)
        GROUP BY v.calificacionGeneral
        """)
    List<Object[]> distribucionPublica(
        @Param("idServicio") Long idServicio,
        @Param("idArtista") Long idArtista,
        @Param("estado") EstadoResenia estado,
        @Param("estadoPedido") EstadoPedido estadoPedido);

    @Query("""
        SELECT rv.criterio.idCriterioResenia, rv.criterio.nombre,
               rv.criterio.orden, AVG(rv.valor), COUNT(DISTINCT r)
        FROM Resenia r
        JOIN r.versiones v
        JOIN v.valores rv
        JOIN r.pedido p
        JOIN p.servicio s
        WHERE r.estado = :estado
          AND p.estado = :estadoPedido
          AND r.autor.idUsuario = p.cliente.idUsuario
          AND v.numero = (
              SELECT MAX(v2.numero) FROM ReseniaVersion v2 WHERE v2.resenia = r
          )
          AND (:idServicio IS NULL OR s.idServicio = :idServicio)
          AND (:idArtista IS NULL OR s.artista.idUsuario = :idArtista)
        GROUP BY rv.criterio.idCriterioResenia, rv.criterio.nombre, rv.criterio.orden
        ORDER BY rv.criterio.orden ASC, rv.criterio.nombre ASC
        """)
    List<Object[]> criteriosPublicos(
        @Param("idServicio") Long idServicio,
        @Param("idArtista") Long idArtista,
        @Param("estado") EstadoResenia estado,
        @Param("estadoPedido") EstadoPedido estadoPedido);

    @Query("""
        SELECT ro.opcion.idOpcionCriterio, ro.opcion.etiqueta, ro.opcion.polaridad,
               COUNT(DISTINCT r)
        FROM Resenia r
        JOIN r.versiones v
        JOIN v.opciones ro
        JOIN r.pedido p
        JOIN p.servicio s
        WHERE r.estado = :estado
          AND p.estado = :estadoPedido
          AND r.autor.idUsuario = p.cliente.idUsuario
          AND v.numero = (
              SELECT MAX(v2.numero) FROM ReseniaVersion v2 WHERE v2.resenia = r
          )
          AND (:idServicio IS NULL OR s.idServicio = :idServicio)
          AND (:idArtista IS NULL OR s.artista.idUsuario = :idArtista)
        GROUP BY ro.opcion.idOpcionCriterio, ro.opcion.etiqueta, ro.opcion.polaridad
        ORDER BY COUNT(DISTINCT r) DESC, ro.opcion.etiqueta ASC
        """)
    List<Object[]> opcionesDestacadasPublicas(
        @Param("idServicio") Long idServicio,
        @Param("idArtista") Long idArtista,
        @Param("estado") EstadoResenia estado,
        @Param("estadoPedido") EstadoPedido estadoPedido);

    @Query("""
        SELECT DISTINCT s.idServicio, s.nombre
        FROM Resenia r
        JOIN r.pedido p
        JOIN p.servicio s
        WHERE s.artista.idUsuario = :idArtista
          AND r.estado = :estado
          AND r.autor.idUsuario = p.cliente.idUsuario
        ORDER BY s.nombre ASC
        """)
    List<Object[]> serviciosConResenias(
        @Param("idArtista") Long idArtista,
        @Param("estado") EstadoResenia estado);

    @Query("""
        SELECT DISTINCT prod.idProducto, prod.titulo
        FROM Resenia r
        JOIN r.producto prod
        WHERE prod.artista.idUsuario = :idArtista
          AND r.estado = :estado
        ORDER BY prod.titulo ASC
        """)
    List<Object[]> productosConResenias(
        @Param("idArtista") Long idArtista,
        @Param("estado") EstadoResenia estado);

    // -----------------------------------------------------------------
    // Reputación del cliente en lote (para la bandeja de solicitudes)
    // -----------------------------------------------------------------

    @Query("""
        SELECT p.cliente.idUsuario, COUNT(r), AVG(v.calificacionGeneral)
        FROM Resenia r
        JOIN r.versiones v
        JOIN r.pedido p
        WHERE p.cliente.idUsuario IN :ids
          AND r.autor.idUsuario = p.artista.idUsuario
          AND r.estado = :estado
          AND v.numero = (
              SELECT MAX(v2.numero) FROM ReseniaVersion v2 WHERE v2.resenia = r
          )
        GROUP BY p.cliente.idUsuario
        """)
    List<Object[]> reputacionPorClientes(
        @Param("ids") Collection<Long> ids,
        @Param("estado") EstadoResenia estado);
}
