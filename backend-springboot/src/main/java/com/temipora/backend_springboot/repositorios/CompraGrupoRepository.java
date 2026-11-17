package com.temipora.backend_springboot.repositorios;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.temipora.backend_springboot.modelo.CompraGrupo;
import com.temipora.backend_springboot.modelo.EstadoCompraGrupo;

import jakarta.persistence.LockModeType;

public interface CompraGrupoRepository extends JpaRepository<CompraGrupo, Long>,
        JpaSpecificationExecutor<CompraGrupo> {

    List<CompraGrupo> findByCompraIdCompraOrderByFechaCreacionAsc(Long idCompra);

    Optional<CompraGrupo> findByIdCompraGrupoAndCompraIdCompra(
            Long idCompraGrupo, Long idCompra);

    Optional<CompraGrupo> findByIdCompraGrupoAndCompraClienteIdUsuario(
            Long idCompraGrupo, Long idUsuario);

    long countByCompraIdCompraAndEstado(
            Long idCompra, EstadoCompraGrupo estado);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT g FROM CompraGrupo g WHERE g.idCompraGrupo = :idCompraGrupo")
    Optional<CompraGrupo> findByIdCompraGrupoForUpdate(
            @Param("idCompraGrupo") Long idCompraGrupo);

    @Query("""
        SELECT g FROM CompraGrupo g
        JOIN FETCH g.compra c
        JOIN FETCH c.cliente
        WHERE g.idCompraGrupo IN :ids
        """)
    List<CompraGrupo> cargarDetalle(@Param("ids") Collection<Long> ids);

    @Query("""
        SELECT g.fechaPago, g.netoArtista
        FROM CompraGrupo g
        WHERE g.artista.idUsuario = :idArtista
          AND g.estado = :estado
        """)
    List<Object[]> netosPagadosDeArtista(
            @Param("idArtista") Long idArtista,
            @Param("estado") EstadoCompraGrupo estado);

    /**
     * Grupos pagados del artista en el período. El neto real sale del intento
     * de cobro aprobado; si no hay dato del medio de pago se usa el neto del
     * artista. Un grupo puede tener más de un intento, por eso se toma el
     * último aprobado.
     */
    @Query(value = """
        SELECT CAST(g.fecha_pago AS date),
               g.id_compra_grupo,
               g.subtotal,
               g.comision,
               COALESCE(ic.arancel_mp, 0),
               COALESCE(ic.neto_recibido_mp, g.neto_artista),
               CASE WHEN ic.neto_recibido_mp IS NULL THEN 1 ELSE 0 END,
               COALESCE(u.unidades, 0)
        FROM compra_grupo g
        LEFT JOIN (
            SELECT icg.id_compra_grupo AS id_grupo,
                   MAX(icg.id_intento_cobro) AS id_intento
            FROM intento_cobro_grupo icg
            WHERE icg.estado = 'APROBADO'
            GROUP BY icg.id_compra_grupo
        ) a ON a.id_grupo = g.id_compra_grupo
        LEFT JOIN intento_cobro ic ON ic.id_intento_cobro = a.id_intento
        LEFT JOIN (
            SELECT ci.id_compra_grupo AS id_grupo, SUM(ci.cantidad) AS unidades
            FROM compra_item ci
            GROUP BY ci.id_compra_grupo
        ) u ON u.id_grupo = g.id_compra_grupo
        WHERE g.id_artista = :idArtista
          AND g.estado = 'PAGADO'
          AND g.fecha_pago >= :desde
          AND g.fecha_pago < :hasta
        """, nativeQuery = true)
    List<Object[]> filasInformeGrupos(
        @Param("idArtista") Long idArtista,
        @Param("desde") LocalDateTime desde,
        @Param("hasta") LocalDateTime hasta);
}
