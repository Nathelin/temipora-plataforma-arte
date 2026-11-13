package com.temipora.backend_springboot.repositorios;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.temipora.backend_springboot.modelo.CompraItem;
import com.temipora.backend_springboot.modelo.EstadoCompraGrupo;
import com.temipora.backend_springboot.modelo.EstadoEntregaItem;
import com.temipora.backend_springboot.modelo.Usuario;

public interface CompraItemRepository extends JpaRepository<CompraItem, Long> {

    List<CompraItem> findByGrupoIdCompraGrupo(Long idCompraGrupo);

    @Query("""
        SELECT i FROM CompraItem i
        JOIN FETCH i.producto
        JOIN FETCH i.variante
        WHERE i.grupo.idCompraGrupo IN :ids
        """)
    List<CompraItem> buscarPorGrupos(@Param("ids") Collection<Long> ids);

    List<CompraItem> findByGrupoCompraIdCompra(Long idCompra);

    /**
     * Compra pagada más reciente de un usuario para un producto. Habilita la
     * reseña y define desde cuándo corre la ventana de creación.
     */
    Optional<CompraItem> findFirstByProductoIdProductoAndGrupoCompraClienteIdUsuarioAndGrupoEstadoOrderByGrupoFechaPagoDesc(
            Long idProducto, Long idUsuario, EstadoCompraGrupo estado);

    /**
     * Todas las compras pagadas de un usuario para un producto, incluidas las
     * de distintas variantes y las reventas. Se usa para dejar constancia en la
     * reseña de cuántas variantes compró.
     */
    @Query("""
        SELECT i FROM CompraItem i
        JOIN FETCH i.grupo g
        WHERE i.producto.idProducto = :idProducto
          AND g.compra.cliente.idUsuario = :idUsuario
          AND g.estado = :estado
        ORDER BY g.fechaPago ASC, i.idCompraItem ASC
        """)
    List<CompraItem> findPagadosDeProducto(
        @Param("idProducto") Long idProducto,
        @Param("idUsuario") Long idUsuario,
        @Param("estado") EstadoCompraGrupo estado);

    Optional<CompraItem> findByIdCompraItemAndGrupoCompraClienteIdUsuario(
            Long idCompraItem, Long idUsuario);

    Optional<CompraItem> findFirstByVarianteIdVarianteAndEstadoEntregaOrderByFechaEntregaAsc(
            Long idVariante, EstadoEntregaItem estadoEntrega);

    Optional<CompraItem> findFirstByIdPublicacionReventa(Long idPublicacionReventa);

    @Query("""
        SELECT DISTINCT i FROM CompraItem i
        JOIN FETCH i.variante v
        LEFT JOIN FETCH v.propietarioActual
        JOIN FETCH i.producto p
        JOIN FETCH p.artista
        JOIN FETCH p.moneda
        JOIN FETCH i.monedaOriginal
        WHERE i.grupo.compra.cliente.idUsuario = :idUsuario
          AND i.estadoEntrega = :estado
        ORDER BY i.fechaEntrega DESC
        """)
    List<CompraItem> findEntregadosDeCliente(
            @Param("idUsuario") Long idUsuario,
            @Param("estado") EstadoEntregaItem estado);

    /**
     * Colección pública de productos comprados. Solo incluye compras pagadas.
     * Para piezas únicas se descartan las compras anteriores a la más reciente
     * de la misma variante, para que la pieza revendida salga de la colección
     * del dueño anterior y no se duplique si la vuelve a comprar.
     */
    @Query(value = """
        SELECT i FROM CompraItem i
        JOIN FETCH i.grupo g
        JOIN FETCH g.compra c
        JOIN FETCH i.producto p
        JOIN FETCH p.artista a
        JOIN FETCH i.variante v
        WHERE c.cliente.idUsuario = :idCliente
          AND g.estado = :estadoPagado
          AND i.estadoEntrega <> :estadoCancelado
          AND (:esDuenio = true OR i.ocultoColeccion = false)
          AND (:idArtista IS NULL OR a.idUsuario = :idArtista)
          AND (:ocultarSensibles = false OR p.contenidoSensible = false)
          AND (
              NOT EXISTS (
                  SELECT 1 FROM EventoLedger e WHERE e.variante = v
              )
              OR NOT EXISTS (
                  SELECT 1 FROM CompraItem i2
                  JOIN i2.grupo g2
                  WHERE i2.variante = v
                    AND g2.estado = :estadoPagado
                    AND i2.estadoEntrega <> :estadoCancelado
                    AND (g2.fechaPago > g.fechaPago
                         OR (g2.fechaPago = g.fechaPago
                             AND i2.idCompraItem > i.idCompraItem))
              )
          )
        ORDER BY g.fechaPago DESC, i.idCompraItem DESC
        """, countQuery = """
        SELECT COUNT(i) FROM CompraItem i
        JOIN i.grupo g
        JOIN g.compra c
        JOIN i.producto p
        JOIN i.variante v
        WHERE c.cliente.idUsuario = :idCliente
          AND g.estado = :estadoPagado
          AND i.estadoEntrega <> :estadoCancelado
          AND (:esDuenio = true OR i.ocultoColeccion = false)
          AND (:idArtista IS NULL OR p.artista.idUsuario = :idArtista)
          AND (:ocultarSensibles = false OR p.contenidoSensible = false)
          AND (
              NOT EXISTS (
                  SELECT 1 FROM EventoLedger e WHERE e.variante = v
              )
              OR NOT EXISTS (
                  SELECT 1 FROM CompraItem i2
                  JOIN i2.grupo g2
                  WHERE i2.variante = v
                    AND g2.estado = :estadoPagado
                    AND i2.estadoEntrega <> :estadoCancelado
                    AND (g2.fechaPago > g.fechaPago
                         OR (g2.fechaPago = g.fechaPago
                             AND i2.idCompraItem > i.idCompraItem))
              )
          )
        """)
    Page<CompraItem> findColeccion(
        @Param("idCliente") Long idCliente,
        @Param("esDuenio") boolean esDuenio,
        @Param("ocultarSensibles") boolean ocultarSensibles,
        @Param("idArtista") Long idArtista,
        @Param("estadoPagado") EstadoCompraGrupo estadoPagado,
        @Param("estadoCancelado") EstadoEntregaItem estadoCancelado,
        Pageable pageable);

    @Query("""
        SELECT DISTINCT a FROM CompraItem i
        JOIN i.grupo g
        JOIN g.compra c
        JOIN i.producto p
        JOIN p.artista a
        JOIN i.variante v
        WHERE c.cliente.idUsuario = :idCliente
          AND g.estado = :estadoPagado
          AND i.estadoEntrega <> :estadoCancelado
          AND (:esDuenio = true OR i.ocultoColeccion = false)
          AND (:ocultarSensibles = false OR p.contenidoSensible = false)
          AND (
              NOT EXISTS (
                  SELECT 1 FROM EventoLedger e WHERE e.variante = v
              )
              OR NOT EXISTS (
                  SELECT 1 FROM CompraItem i2
                  JOIN i2.grupo g2
                  WHERE i2.variante = v
                    AND g2.estado = :estadoPagado
                    AND i2.estadoEntrega <> :estadoCancelado
                    AND (g2.fechaPago > g.fechaPago
                         OR (g2.fechaPago = g.fechaPago
                             AND i2.idCompraItem > i.idCompraItem))
              )
          )
        ORDER BY a.nombreUsuario
        """)
    List<Usuario> artistasDeColeccion(
        @Param("idCliente") Long idCliente,
        @Param("esDuenio") boolean esDuenio,
        @Param("ocultarSensibles") boolean ocultarSensibles,
        @Param("estadoPagado") EstadoCompraGrupo estadoPagado,
        @Param("estadoCancelado") EstadoEntregaItem estadoCancelado);

    @Query("""
        SELECT COUNT(i) FROM CompraItem i
        JOIN i.grupo g
        WHERE g.compra.cliente.idUsuario = :idCliente
          AND g.estado = :estadoPagado
          AND i.estadoEntrega <> :estadoCancelado
        """)
    long contarColeccionables(
        @Param("idCliente") Long idCliente,
        @Param("estadoPagado") EstadoCompraGrupo estadoPagado,
        @Param("estadoCancelado") EstadoEntregaItem estadoCancelado);
}
