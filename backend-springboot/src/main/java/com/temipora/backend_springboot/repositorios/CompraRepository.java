package com.temipora.backend_springboot.repositorios;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.temipora.backend_springboot.modelo.Compra;
import com.temipora.backend_springboot.modelo.EstadoCompra;
import com.temipora.backend_springboot.modelo.EstadoCompraGrupo;

import jakarta.persistence.LockModeType;

public interface CompraRepository extends JpaRepository<Compra, Long> {

    Page<Compra> findByClienteIdUsuarioOrderByFechaCreacionDesc(
            Long idUsuario, Pageable pageable);

    Page<Compra> findByClienteIdUsuarioAndEstadoInOrderByFechaCreacionDesc(
            Long idUsuario, Collection<EstadoCompra> estados, Pageable pageable);

    @Query("""
        SELECT c.estado, COUNT(c)
        FROM Compra c
        WHERE c.cliente.idUsuario = :idUsuario
        GROUP BY c.estado
        """)
    List<Object[]> contarPorEstado(@Param("idUsuario") Long idUsuario);

    Optional<Compra> findByIdCompraAndClienteIdUsuario(
            Long idCompra, Long idUsuario);

    Optional<Compra> findByClaveIdempotencia(String claveIdempotencia);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM Compra c WHERE c.idCompra = :idCompra")
    Optional<Compra> findByIdCompraForUpdate(@Param("idCompra") Long idCompra);

    @Query("""
        SELECT COALESCE(SUM(g.subtotal), 0)
        FROM CompraGrupo g
        WHERE g.compra.cliente.idUsuario = :idUsuario
          AND g.estado = :estado
        """)
    BigDecimal sumarPendienteGrupos(
            @Param("idUsuario") Long idUsuario,
            @Param("estado") EstadoCompraGrupo estado);
}
