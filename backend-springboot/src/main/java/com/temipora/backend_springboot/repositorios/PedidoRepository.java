package com.temipora.backend_springboot.repositorios;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.temipora.backend_springboot.modelo.EstadoEntregaHito;
import com.temipora.backend_springboot.modelo.EstadoPagoHito;
import com.temipora.backend_springboot.modelo.EstadoPedido;
import com.temipora.backend_springboot.modelo.Pedido;
import com.temipora.backend_springboot.modelo.TipoHito;

import jakarta.persistence.LockModeType;

public interface PedidoRepository extends JpaRepository<Pedido, Long> {

    Optional<Pedido> findBySolicitudIdSolicitud(Long idSolicitud);

    boolean existsBySolicitudIdSolicitud(Long idSolicitud);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Pedido p where p.idPedido = :id")
    Optional<Pedido> findByIdPedidoForUpdate(@Param("id") Long id);

    List<Pedido> findByArtistaIdUsuarioAndEstadoInOrderByFechaCreacionDesc(
        Long idArtista, Collection<EstadoPedido> estados, Pageable pageable);

    List<Pedido> findByClienteIdUsuarioAndEstadoInOrderByFechaCreacionDesc(
        Long idCliente, Collection<EstadoPedido> estados, Pageable pageable);

    @Query("""
        select p from Pedido p
        join fetch p.artista
        join fetch p.servicio
        where p.cliente.idUsuario = :idCliente
          and p.estado in :estados
          and p.finalizadoEn is not null
          and p.finalizadoEn >= :desde
          and not exists (
              select r from Resenia r
              where r.pedido = p
                and r.autor.idUsuario = :idCliente)
        order by p.finalizadoEn desc
        """)
    List<Pedido> findFinalizadosSinReseniaCliente(
        @Param("idCliente") Long idCliente,
        @Param("estados") Collection<EstadoPedido> estados,
        @Param("desde") LocalDateTime desde);

    @Query("""
        select distinct p from Pedido p
        left join fetch p.hitos h
        left join fetch h.hitoPresupuesto
        join fetch p.artista
        join fetch p.cliente
        left join fetch p.servicio
        where p.estado = :estado
        """)
    List<Pedido> findConHitosByEstado(@Param("estado") EstadoPedido estado);

    /**
     * Pedidos en curso que todavía no registraron mora y fueron creados después
     * del corte de sanciones automáticas.
     */
    @Query("""
        select distinct p from Pedido p
        left join fetch p.hitos h
        left join fetch h.hitoPresupuesto
        join fetch p.artista
        join fetch p.cliente
        left join fetch p.servicio
        where p.estado = :estado
          and p.moraDesde is null
          and p.fechaCreacion >= :corte
        """)
    List<Pedido> findCandidatosMora(
        @Param("estado") EstadoPedido estado,
        @Param("corte") LocalDateTime corte);

    @Query("""
        select distinct p from Pedido p
        left join fetch p.hitos h
        left join fetch h.hitoPresupuesto
        join fetch p.artista
        join fetch p.cliente
        left join fetch p.servicio
        where p.estado = :estado
          and p.moraDesde is not null
        """)
    List<Pedido> findEnMora(@Param("estado") EstadoPedido estado);

    @Query("""
        select distinct p from Pedido p
        left join fetch p.hitos h
        left join fetch h.hitoPresupuesto
        join fetch p.cliente
        left join fetch p.servicio
        where p.estado = :estado
          and p.moraDesde is not null
          and p.artista.idUsuario = :idArtista
        """)
    List<Pedido> findEnMoraDeArtista(
        @Param("idArtista") Long idArtista,
        @Param("estado") EstadoPedido estado);

    /**
     * Pedidos en curso con la entrega final ya subida y su pago vencido, que
     * todavía no registraron la mora del cliente y fueron creados después del
     * corte de sanciones automáticas.
     */
    @Query("""
        select distinct p from Pedido p
        left join fetch p.hitos h
        left join fetch h.hitoPresupuesto
        join fetch p.artista
        join fetch p.cliente
        left join fetch p.servicio
        where p.estado = :estado
          and p.impagoDesde is null
          and p.fechaCreacion >= :corte
          and exists (
              select h2 from HitoPedido h2
              where h2.pedido = p
                and h2.hitoPresupuesto.tipo = :tipo
                and h2.estadoPago = :pagoPendiente
                and h2.estadoEntrega = :entregaEntregada
                and h2.fechaLimitePago is not null
                and h2.fechaLimitePago < :ahora)
        """)
    List<Pedido> findCandidatosImpagoCliente(
        @Param("estado") EstadoPedido estado,
        @Param("corte") LocalDateTime corte,
        @Param("tipo") TipoHito tipo,
        @Param("pagoPendiente") EstadoPagoHito pagoPendiente,
        @Param("entregaEntregada") EstadoEntregaHito entregaEntregada,
        @Param("ahora") LocalDateTime ahora);

    @Query("""
        select distinct p from Pedido p
        left join fetch p.hitos h
        left join fetch h.hitoPresupuesto
        join fetch p.artista
        join fetch p.cliente
        left join fetch p.servicio
        where p.estado = :estado
          and p.impagoDesde is not null
        """)
    List<Pedido> findConImpago(@Param("estado") EstadoPedido estado);

    @Query("""
        select distinct p from Pedido p
        left join fetch p.hitos h
        left join fetch h.hitoPresupuesto
        join fetch p.artista
        join fetch p.cliente
        left join fetch p.servicio
        where p.estado = :estado
          and p.impagoDesde is not null
          and p.cliente.idUsuario = :idCliente
        """)
    List<Pedido> findConImpagoDeCliente(
        @Param("idCliente") Long idCliente,
        @Param("estado") EstadoPedido estado);

    boolean existsByArtistaIdUsuarioAndClienteIdUsuario(
        Long idArtista, Long idCliente);

    long countByArtistaIdUsuario(Long idArtista);

    long countByClienteIdUsuario(Long idCliente);

    long countByArtistaIdUsuarioAndEstadoIn(
        Long idArtista, Collection<EstadoPedido> estados);

    long countByClienteIdUsuarioAndEstadoIn(
        Long idCliente, Collection<EstadoPedido> estados);

    @Query("""
        select count(p) from Pedido p
        where p.estado = :estado
          and p.limiteEntrega is not null
          and p.limiteEntrega < :hoy
        """)
    long contarEntregasAtrasadas(
        @Param("estado") EstadoPedido estado,
        @Param("hoy") LocalDate hoy);

    @Query("""
        select p from Pedido p
        left join p.servicio s
        where p.artista.idUsuario = :idArtista
          and p.estado in :estados
          and (:idServicio is null or s.idServicio = :idServicio)
          and (:patron = ''
               or lower(p.cliente.nombreUsuario)
                   like concat('%', :patron, '%')
               or lower(p.cliente.nombreDisplay)
                   like concat('%', :patron, '%')
               or lower(p.cliente.nombre)
                   like concat('%', :patron, '%')
               or lower(p.cliente.apellido)
                   like concat('%', :patron, '%')
               or lower(s.nombre) like concat('%', :patron, '%'))
        """)
    Page<Pedido> buscarRecibidos(
        @Param("idArtista") Long idArtista,
        @Param("estados") Collection<EstadoPedido> estados,
        @Param("idServicio") Long idServicio,
        @Param("patron") String patron,
        Pageable pageable);

    @Query("""
        select p from Pedido p
        left join p.servicio s
        where p.cliente.idUsuario = :idCliente
          and p.estado in :estados
          and (:idServicio is null or s.idServicio = :idServicio)
          and (:patron = ''
               or lower(p.artista.nombreUsuario)
                   like concat('%', :patron, '%')
               or lower(p.artista.nombreDisplay)
                   like concat('%', :patron, '%')
               or lower(p.artista.nombre)
                   like concat('%', :patron, '%')
               or lower(p.artista.apellido)
                   like concat('%', :patron, '%')
               or lower(s.nombre) like concat('%', :patron, '%'))
        """)
    Page<Pedido> buscarEnviados(
        @Param("idCliente") Long idCliente,
        @Param("estados") Collection<EstadoPedido> estados,
        @Param("idServicio") Long idServicio,
        @Param("patron") String patron,
        Pageable pageable);

    @Query("""
        select p from Pedido p
        left join p.servicio s
        where p.artista.idUsuario = :idArtista
          and p.estado = :estado
          and exists (
              select h from HitoPedido h
              where h.pedido = p
                and h.hitoPresupuesto.requiereEntrega = true
                and h.estadoEntrega in :estadosEntrega)
          and (:idServicio is null or s.idServicio = :idServicio)
          and (:patron = ''
               or lower(p.cliente.nombreUsuario)
                   like concat('%', :patron, '%')
               or lower(p.cliente.nombreDisplay)
                   like concat('%', :patron, '%')
               or lower(p.cliente.nombre)
                   like concat('%', :patron, '%')
               or lower(p.cliente.apellido)
                   like concat('%', :patron, '%')
               or lower(s.nombre) like concat('%', :patron, '%'))
        """)
    List<Pedido> candidatosAccionRecibidos(
        @Param("idArtista") Long idArtista,
        @Param("estado") EstadoPedido estado,
        @Param("estadosEntrega") Collection<EstadoEntregaHito> estadosEntrega,
        @Param("idServicio") Long idServicio,
        @Param("patron") String patron);

    @Query("""
        select p from Pedido p
        left join p.servicio s
        where p.cliente.idUsuario = :idCliente
          and p.estado in :estados
          and exists (
              select h from HitoPedido h
              where h.pedido = p
                and (h.estadoPago = :pagoPendiente
                     or h.estadoEntrega = :entregaEntregada))
          and (:idServicio is null or s.idServicio = :idServicio)
          and (:patron = ''
               or lower(p.artista.nombreUsuario)
                   like concat('%', :patron, '%')
               or lower(p.artista.nombreDisplay)
                   like concat('%', :patron, '%')
               or lower(p.artista.nombre)
                   like concat('%', :patron, '%')
               or lower(p.artista.apellido)
                   like concat('%', :patron, '%')
               or lower(s.nombre) like concat('%', :patron, '%'))
        """)
    List<Pedido> candidatosAccionEnviados(
        @Param("idCliente") Long idCliente,
        @Param("estados") Collection<EstadoPedido> estados,
        @Param("pagoPendiente") EstadoPagoHito pagoPendiente,
        @Param("entregaEntregada") EstadoEntregaHito entregaEntregada,
        @Param("idServicio") Long idServicio,
        @Param("patron") String patron);

    @Query(value = """
        select p from Pedido p
        join fetch p.presupuesto pr
        join fetch p.cliente c
        join fetch p.artista a
        join fetch p.servicio s
        left join fetch pr.ofertaReactivacion o
        left join fetch o.campana
        where pr.descuentoMonto > 0
          and p.fechaCreacion >= :desde
          and p.fechaCreacion < :hasta
          and (:patron = ''
               or lower(c.nombreUsuario) like concat('%', :patron, '%')
               or lower(c.nombreDisplay) like concat('%', :patron, '%')
               or lower(c.nombre) like concat('%', :patron, '%')
               or lower(c.apellido) like concat('%', :patron, '%')
               or lower(a.nombreUsuario) like concat('%', :patron, '%')
               or lower(a.nombreDisplay) like concat('%', :patron, '%')
               or lower(s.nombre) like concat('%', :patron, '%'))
          and (:comisionCero = false or pr.comisionPorcentaje = 0)
        """,
        countQuery = """
        select count(p) from Pedido p
        join p.presupuesto pr
        join p.cliente c
        join p.artista a
        join p.servicio s
        where pr.descuentoMonto > 0
          and p.fechaCreacion >= :desde
          and p.fechaCreacion < :hasta
          and (:patron = ''
               or lower(c.nombreUsuario) like concat('%', :patron, '%')
               or lower(c.nombreDisplay) like concat('%', :patron, '%')
               or lower(c.nombre) like concat('%', :patron, '%')
               or lower(c.apellido) like concat('%', :patron, '%')
               or lower(a.nombreUsuario) like concat('%', :patron, '%')
               or lower(a.nombreDisplay) like concat('%', :patron, '%')
               or lower(s.nombre) like concat('%', :patron, '%'))
          and (:comisionCero = false or pr.comisionPorcentaje = 0)
        """)
    Page<Pedido> buscarConDescuento(
        @Param("desde") LocalDateTime desde,
        @Param("hasta") LocalDateTime hasta,
        @Param("patron") String patron,
        @Param("comisionCero") boolean comisionCero,
        Pageable pageable);

    @Query("""
        select count(p),
               coalesce(sum(pr.descuentoMontoCobro), 0),
               coalesce(sum(case when pr.comisionPorcentaje = 0 then 1 else 0 end), 0),
               coalesce(sum(case when pr.comisionPorcentaje = 0 then pr.descuentoMontoCobro else 0 end), 0),
               coalesce(sum(pr.precioFinalClienteCobro), 0),
               coalesce(sum(pr.precioFinalCobro), 0),
               coalesce(sum(pr.comisionMontoCobro), 0)
        from Pedido p
        join p.presupuesto pr
        join p.cliente c
        join p.artista a
        join p.servicio s
        where pr.descuentoMonto > 0
          and p.fechaCreacion >= :desde
          and p.fechaCreacion < :hasta
          and (:patron = ''
               or lower(c.nombreUsuario) like concat('%', :patron, '%')
               or lower(c.nombreDisplay) like concat('%', :patron, '%')
               or lower(c.nombre) like concat('%', :patron, '%')
               or lower(c.apellido) like concat('%', :patron, '%')
               or lower(a.nombreUsuario) like concat('%', :patron, '%')
               or lower(a.nombreDisplay) like concat('%', :patron, '%')
               or lower(s.nombre) like concat('%', :patron, '%'))
          and (:comisionCero = false or pr.comisionPorcentaje = 0)
        """)
    List<Object[]> resumirConDescuento(
        @Param("desde") LocalDateTime desde,
        @Param("hasta") LocalDateTime hasta,
        @Param("patron") String patron,
        @Param("comisionCero") boolean comisionCero);

    @Query("""
        select count(p) from Pedido p
        join p.cliente c
        join p.artista a
        join p.servicio s
        where p.fechaCreacion >= :desde
          and p.fechaCreacion < :hasta
          and (:patron = ''
               or lower(c.nombreUsuario) like concat('%', :patron, '%')
               or lower(c.nombreDisplay) like concat('%', :patron, '%')
               or lower(c.nombre) like concat('%', :patron, '%')
               or lower(c.apellido) like concat('%', :patron, '%')
               or lower(a.nombreUsuario) like concat('%', :patron, '%')
               or lower(a.nombreDisplay) like concat('%', :patron, '%')
               or lower(s.nombre) like concat('%', :patron, '%'))
        """)
    long contarPedidosPeriodo(
        @Param("desde") LocalDateTime desde,
        @Param("hasta") LocalDateTime hasta,
        @Param("patron") String patron);
}
