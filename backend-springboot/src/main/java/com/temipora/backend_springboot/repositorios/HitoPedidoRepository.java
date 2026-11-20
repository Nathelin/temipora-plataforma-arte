package com.temipora.backend_springboot.repositorios;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.temipora.backend_springboot.modelo.EstadoEntregaHito;
import com.temipora.backend_springboot.modelo.EstadoPagoHito;
import com.temipora.backend_springboot.modelo.EstadoPedido;
import com.temipora.backend_springboot.modelo.HitoPedido;
import com.temipora.backend_springboot.modelo.TipoHito;

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

    @Query("""
        select count(h) from HitoPedido h
        where h.estadoEntrega = :estado
          and h.fechaLimiteAprobacion is not null
        """)
    long contarAprobacionesPendientes(
        @Param("estado") EstadoEntregaHito estado);

    @Query("""
        select h from HitoPedido h
        join fetch h.pedido p
        where p.artista.idUsuario = :idArtista and h.estadoPago = :pagado
        order by h.fechaPago desc
        """)
    List<HitoPedido> findPagadosDeArtista(
        @Param("idArtista") Long idArtista,
        @Param("pagado") EstadoPagoHito pagado);

    @Query("""
        select h from HitoPedido h
        join fetch h.pedido p
        join fetch h.hitoPresupuesto hp
        join fetch p.cliente
        join fetch p.artista
        left join fetch p.servicio
        where h.estadoPago = :pendiente
          and hp.tipo = :tipo
          and p.estado = :estadoPedido
          and p.fechaCreacion <= :limite
        """)
    List<HitoPedido> findSenasPendientes(
        @Param("pendiente") EstadoPagoHito pendiente,
        @Param("tipo") TipoHito tipo,
        @Param("estadoPedido") EstadoPedido estadoPedido,
        @Param("limite") LocalDateTime limite);

    @Query("""
        select h from HitoPedido h
        join fetch h.pedido p
        join fetch h.hitoPresupuesto hp
        join fetch p.cliente
        join fetch p.artista
        left join fetch p.servicio
        where h.estadoPago = :pendiente
          and h.fechaLimitePago between :desde and :hasta
          and hp.tipo in :tipos
          and p.estado in :estados
        """)
    List<HitoPedido> findPagosPorVencer(
        @Param("pendiente") EstadoPagoHito pendiente,
        @Param("tipos") Collection<TipoHito> tipos,
        @Param("estados") Collection<EstadoPedido> estados,
        @Param("desde") LocalDateTime desde,
        @Param("hasta") LocalDateTime hasta);
}
