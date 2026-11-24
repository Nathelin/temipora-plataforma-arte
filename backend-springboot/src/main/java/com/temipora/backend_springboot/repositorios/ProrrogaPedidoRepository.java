package com.temipora.backend_springboot.repositorios;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.temipora.backend_springboot.modelo.EstadoProrroga;
import com.temipora.backend_springboot.modelo.ProrrogaPedido;

import jakarta.persistence.LockModeType;

public interface ProrrogaPedidoRepository
        extends JpaRepository<ProrrogaPedido, Long> {

    List<ProrrogaPedido> findByPedidoIdPedidoOrderByFechaSolicitudDesc(
        Long idPedido);

    boolean existsByPedidoIdPedido(Long idPedido);

    List<ProrrogaPedido> findByEstadoAndFechaLimiteRespuestaBefore(
        EstadoProrroga estado, LocalDateTime limite);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from ProrrogaPedido p where p.idProrroga = :id")
    Optional<ProrrogaPedido> findByIdProrrogaForUpdate(
        @Param("id") Long id);
}
