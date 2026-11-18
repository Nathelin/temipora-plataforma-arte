package com.temipora.backend_springboot.repositorios;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.temipora.backend_springboot.modelo.EstadoReserva;
import com.temipora.backend_springboot.modelo.ReservaStock;

public interface ReservaStockRepository extends JpaRepository<ReservaStock, Long> {

    Optional<ReservaStock> findByCompraItemIdCompraItem(Long idCompraItem);

    List<ReservaStock> findByVarianteIdVarianteAndEstado(
            Long idVariante, EstadoReserva estado);

    List<ReservaStock> findByEstadoAndExpiraEnBefore(
            EstadoReserva estado, LocalDateTime limite);

    List<ReservaStock> findByCompraItemGrupoIdCompraGrupoAndEstado(
            Long idCompraGrupo, EstadoReserva estado);

    @Query("""
        SELECT COALESCE(SUM(r.cantidad), 0)
          FROM ReservaStock r
         WHERE r.variante.idVariante = :idVariante
           AND r.estado = :estado
        """)
    long sumarCantidad(
            @Param("idVariante") Long idVariante,
            @Param("estado") EstadoReserva estado);
}
