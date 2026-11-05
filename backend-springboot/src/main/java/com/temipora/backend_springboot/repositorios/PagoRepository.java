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
          and p.fechaAcreditacion between :desde and :hasta
        """)
    BigDecimal sumarMontoCobro(
        @Param("estado") EstadoPago estado,
        @Param("desde") LocalDateTime desde,
        @Param("hasta") LocalDateTime hasta);

    @Query("""
        select sum(p.comisionMontoCobro) from Pago p
        where p.estado = :estado
          and p.fechaAcreditacion between :desde and :hasta
        """)
    BigDecimal sumarComisionCobro(
        @Param("estado") EstadoPago estado,
        @Param("desde") LocalDateTime desde,
        @Param("hasta") LocalDateTime hasta);

    @Query("""
        select p.hitoPedido.pedido.idPedido, sum(p.montoCobro)
        from Pago p
        where p.estado = :estado
          and p.fechaAcreditacion between :desde and :hasta
        group by p.hitoPedido.pedido.idPedido
        """)
    List<Object[]> volumenPorPedido(
        @Param("estado") EstadoPago estado,
        @Param("desde") LocalDateTime desde,
        @Param("hasta") LocalDateTime hasta);
}
