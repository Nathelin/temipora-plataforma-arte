package com.temipora.backend_springboot.repositorios;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.temipora.backend_springboot.modelo.EstadoEntregaHito;
import com.temipora.backend_springboot.modelo.EstadoPagoHito;
import com.temipora.backend_springboot.modelo.HitoPedido;

import jakarta.persistence.LockModeType;

public interface HitoPedidoRepository extends JpaRepository<HitoPedido, Long> {

    List<HitoPedido> findByPedidoIdPedidoOrderByOrdenAsc(Long idPedido);

    Optional<HitoPedido> findFirstByPedidoIdPedidoOrderByOrdenAsc(Long idPedido);

    Optional<HitoPedido>
        findFirstByPedidoIdPedidoAndOrdenGreaterThanOrderByOrdenAsc(
            Long idPedido, Integer orden);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select h from HitoPedido h where h.idHitoPedido = :id")
    Optional<HitoPedido> findByIdHitoPedidoForUpdate(@Param("id") Long id);

    List<HitoPedido> findByEstadoEntregaAndFechaLimiteAprobacionBefore(
        EstadoEntregaHito estado, LocalDateTime limite);

    List<HitoPedido> findByEstadoPagoAndFechaLimitePagoBefore(
        EstadoPagoHito estado, LocalDateTime limite);
}
