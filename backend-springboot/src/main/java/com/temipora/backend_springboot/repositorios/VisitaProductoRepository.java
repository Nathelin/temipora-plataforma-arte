package com.temipora.backend_springboot.repositorios;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.temipora.backend_springboot.modelo.VisitaProducto;
import com.temipora.backend_springboot.modelo.VisitaProductoId;

public interface VisitaProductoRepository
        extends JpaRepository<VisitaProducto, VisitaProductoId> {

    @Modifying
    @Query(value = """
        INSERT INTO visita_producto (id_usuario, id_producto, fecha, cantidad, ultima_visita)
        VALUES (:idUsuario, :idProducto, CURRENT_DATE, 1, CURRENT_TIMESTAMP)
        ON CONFLICT (id_usuario, id_producto, fecha)
        DO UPDATE SET cantidad = visita_producto.cantidad + 1,
                      ultima_visita = CURRENT_TIMESTAMP
        """, nativeQuery = true)
    void registrar(
        @Param("idUsuario") Long idUsuario,
        @Param("idProducto") Long idProducto);

    @Query(value = """
        SELECT id_producto, SUM(cantidad), MAX(ultima_visita)
        FROM visita_producto
        WHERE id_usuario = :idUsuario
          AND fecha >= :desde
        GROUP BY id_producto
        """, nativeQuery = true)
    List<Object[]> resumenPorUsuario(
        @Param("idUsuario") Long idUsuario,
        @Param("desde") LocalDate desde);

    @Query(value = """
        SELECT v2.id_producto, COUNT(DISTINCT v2.id_usuario)
        FROM visita_producto v1
        JOIN visita_producto v2 ON v2.id_usuario = v1.id_usuario
        WHERE v1.id_usuario = :idUsuario
          AND v1.fecha >= :desde
          AND v2.fecha >= :desde
          AND v2.id_producto <> v1.id_producto
        GROUP BY v2.id_producto
        """, nativeQuery = true)
    List<Object[]> coVisitas(
        @Param("idUsuario") Long idUsuario,
        @Param("desde") LocalDate desde);

    @Query(value = """
        SELECT id_producto, COUNT(DISTINCT id_usuario)
        FROM visita_producto
        WHERE id_producto IN (:ids)
        GROUP BY id_producto
        """, nativeQuery = true)
    List<Object[]> popularidad(@Param("ids") Collection<Long> ids);

    @Query(value = """
        SELECT DISTINCT pe.id_etiqueta
        FROM visita_producto vp
        JOIN producto_etiqueta pe ON pe.id_producto = vp.id_producto
        WHERE vp.id_usuario = :idUsuario
          AND vp.fecha >= :desde
        """, nativeQuery = true)
    List<Long> etiquetasVisitadas(
        @Param("idUsuario") Long idUsuario,
        @Param("desde") LocalDate desde);
}
