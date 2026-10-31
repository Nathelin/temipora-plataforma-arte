package com.temipora.backend_springboot.repositorios;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.temipora.backend_springboot.modelo.EstadoPago;
import com.temipora.backend_springboot.modelo.Pago;

import jakarta.persistence.LockModeType;

public interface PagoRepository extends JpaRepository<Pago, Long> {

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
}
