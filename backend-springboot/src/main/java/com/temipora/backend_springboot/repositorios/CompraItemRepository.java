package com.temipora.backend_springboot.repositorios;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.temipora.backend_springboot.modelo.CompraItem;
import com.temipora.backend_springboot.modelo.EstadoCompraGrupo;
import com.temipora.backend_springboot.modelo.EstadoEntregaItem;

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
}
