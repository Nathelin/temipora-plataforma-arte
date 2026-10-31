package com.temipora.backend_springboot.repositorios;

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

import jakarta.persistence.LockModeType;

public interface PedidoRepository extends JpaRepository<Pedido, Long> {

    Optional<Pedido> findBySolicitudIdSolicitud(Long idSolicitud);

    boolean existsBySolicitudIdSolicitud(Long idSolicitud);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Pedido p where p.idPedido = :id")
    Optional<Pedido> findByIdPedidoForUpdate(@Param("id") Long id);

    List<Pedido> findByArtistaIdUsuarioAndEstadoInOrderByFechaCreacionDesc(
        Long idArtista, Collection<EstadoPedido> estados, Pageable pageable);

    boolean existsByArtistaIdUsuarioAndClienteIdUsuario(
        Long idArtista, Long idCliente);

    long countByArtistaIdUsuario(Long idArtista);

    long countByClienteIdUsuario(Long idCliente);

    long countByArtistaIdUsuarioAndEstadoIn(
        Long idArtista, Collection<EstadoPedido> estados);

    long countByClienteIdUsuarioAndEstadoIn(
        Long idCliente, Collection<EstadoPedido> estados);

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
                and h.requiereEntrega = true
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
}
