package com.temipora.backend_springboot.repositorios;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.temipora.backend_springboot.modelo.EstadoPago;
import com.temipora.backend_springboot.modelo.Pago;

import jakarta.persistence.LockModeType;

public interface PagoRepository
        extends JpaRepository<Pago, Long>, JpaSpecificationExecutor<Pago> {

    Optional<Pago> findFirstByHitoPedidoIdHitoPedidoAndEstado(
        Long idHitoPedido, EstadoPago estado);

    List<Pago> findByHitoPedidoIdHitoPedidoOrderByFechaCreacionDesc(
        Long idHitoPedido);

    List<Pago> findByHitoPedidoIdHitoPedidoAndEstado(
        Long idHitoPedido, EstadoPago estado);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Pago p where p.idPago = :id")
    Optional<Pago> findByIdPagoForUpdate(@Param("id") Long id);

    @Query("""
        select p from Pago p
        join fetch p.hitoPedido h
        join fetch h.pedido
        join fetch p.usuarioPagador
        where p.idPago = :id
        """)
    Optional<Pago> findByIdPagoConPedido(@Param("id") Long id);

    @Query("""
        select sum(p.montoCobro) from Pago p
        where p.estado = :estado
          and coalesce(p.fechaAcreditacion, p.fechaAprobacion,
                p.fechaCreacion) >= :desde
          and coalesce(p.fechaAcreditacion, p.fechaAprobacion,
                p.fechaCreacion) < :hasta
        """)
    BigDecimal sumarMontoCobro(
        @Param("estado") EstadoPago estado,
        @Param("desde") LocalDateTime desde,
        @Param("hasta") LocalDateTime hasta);

    @Query("""
        select sum(p.comisionMontoCobro) from Pago p
        where p.estado = :estado
          and coalesce(p.fechaAcreditacion, p.fechaAprobacion,
                p.fechaCreacion) >= :desde
          and coalesce(p.fechaAcreditacion, p.fechaAprobacion,
                p.fechaCreacion) < :hasta
        """)
    BigDecimal sumarComisionCobro(
        @Param("estado") EstadoPago estado,
        @Param("desde") LocalDateTime desde,
        @Param("hasta") LocalDateTime hasta);

    @Query("""
        select p.hitoPedido.pedido.idPedido, sum(p.montoCobro)
        from Pago p
        where p.estado = :estado
          and coalesce(p.fechaAcreditacion, p.fechaAprobacion,
                p.fechaCreacion) >= :desde
          and coalesce(p.fechaAcreditacion, p.fechaAprobacion,
                p.fechaCreacion) < :hasta
        group by p.hitoPedido.pedido.idPedido
        """)
    List<Object[]> volumenPorPedido(
        @Param("estado") EstadoPago estado,
        @Param("desde") LocalDateTime desde,
        @Param("hasta") LocalDateTime hasta);

    /**
     * Pagos aprobados del artista en el período, con la fecha efectiva de
     * acreditación. Alimenta el informe de ventas.
     */
    @Query(value = """
        SELECT CAST(COALESCE(p.fecha_acreditacion, p.fecha_aprobacion,
                    p.fecha_creacion) AS date),
               pe.id_servicio,
               s.nombre,
               p.monto_cobro,
               p.comision_monto_cobro,
               COALESCE(p.arancel_mp, 0),
               COALESCE(p.neto_recibido_mp, p.monto_neto_cobro),
               CASE WHEN p.neto_recibido_mp IS NULL THEN 1 ELSE 0 END,
               pe.id_pedido
        FROM pago p
        JOIN hito_pedido h ON h.id_hito_pedido = p.id_hito_pedido
        JOIN pedido pe ON pe.id_pedido = h.id_pedido
        LEFT JOIN servicio s ON s.id_servicio = pe.id_servicio
        WHERE pe.id_artista = :idArtista
          AND p.estado = 'APROBADO'
          AND COALESCE(p.fecha_acreditacion, p.fecha_aprobacion,
                p.fecha_creacion) >= :desde
          AND COALESCE(p.fecha_acreditacion, p.fecha_aprobacion,
                p.fecha_creacion) < :hasta
        """, nativeQuery = true)
    List<Object[]> filasInformeServicios(
        @Param("idArtista") Long idArtista,
        @Param("desde") LocalDateTime desde,
        @Param("hasta") LocalDateTime hasta);

    /**
     * Neto realmente liquidado por el medio de pago y fecha efectiva de cada
     * cobro. Alimenta los totales del panel del artista.
     */
    @Query(value = """
        SELECT COALESCE(p.neto_recibido_mp, p.monto_neto_cobro),
               COALESCE(p.fecha_acreditacion, p.fecha_aprobacion,
                    p.fecha_creacion)
        FROM pago p
        JOIN hito_pedido h ON h.id_hito_pedido = p.id_hito_pedido
        JOIN pedido pe ON pe.id_pedido = h.id_pedido
        WHERE pe.id_artista = :idArtista
          AND p.estado = 'APROBADO'
        """, nativeQuery = true)
    List<Object[]> netosRealesDeArtista(@Param("idArtista") Long idArtista);

    /**
     * Pagos aprobados de toda la plataforma en el período, con vendedor y
     * servicio. Alimenta el informe de ventas y comisiones del admin.
     */
    @Query(value = """
        SELECT CAST(COALESCE(p.fecha_acreditacion, p.fecha_aprobacion,
                    p.fecha_creacion) AS date),
               pe.id_artista,
               u.nombre_usuario,
               pe.id_servicio,
               s.nombre,
               p.monto_cobro,
               p.comision_monto_cobro,
               p.monto_neto_cobro,
               COALESCE(p.arancel_mp, 0),
               COALESCE(p.neto_recibido_mp, p.monto_neto_cobro),
               CASE WHEN p.neto_recibido_mp IS NULL THEN 1 ELSE 0 END,
               pe.id_pedido
        FROM pago p
        JOIN hito_pedido h ON h.id_hito_pedido = p.id_hito_pedido
        JOIN pedido pe ON pe.id_pedido = h.id_pedido
        JOIN usuario u ON u.id_usuario = pe.id_artista
        LEFT JOIN servicio s ON s.id_servicio = pe.id_servicio
        WHERE p.estado = 'APROBADO'
          AND COALESCE(p.fecha_acreditacion, p.fecha_aprobacion,
                p.fecha_creacion) >= :desde
          AND COALESCE(p.fecha_acreditacion, p.fecha_aprobacion,
                p.fecha_creacion) < :hasta
        """, nativeQuery = true)
    List<Object[]> filasInformeAdminServicios(
        @Param("desde") LocalDateTime desde,
        @Param("hasta") LocalDateTime hasta);
}
