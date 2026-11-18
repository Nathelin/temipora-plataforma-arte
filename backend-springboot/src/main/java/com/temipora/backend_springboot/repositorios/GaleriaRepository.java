package com.temipora.backend_springboot.repositorios;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.temipora.backend_springboot.modelo.EstadoPedido;
import com.temipora.backend_springboot.modelo.ItemSolicitud;
import com.temipora.backend_springboot.modelo.Servicio;
import com.temipora.backend_springboot.modelo.Usuario;

public interface GaleriaRepository
        extends JpaRepository<ItemSolicitud, Long> {

    @Query(value = """
        select i, p from Solicitud s
        join s.items i
        join s.pedido p
        join fetch p.servicio
        join fetch p.artista
        where p.cliente.idUsuario = :idCliente
          and p.estado = :estado
          and p.artista.estadoCuenta = true
          and p.publicacionAutorizada = true
          and (:esDuenio = true
               or (p.cliente.pedidosPublicos = true
                   and p.publicacionOcultadaCliente = false))
          and (:idServicio is null
               or p.servicio.idServicio = :idServicio)
          and (:idArtista is null
               or p.artista.idUsuario = :idArtista)
          and (:ocultarSensibles = false
               or p.servicio.contenidoSensible = false
               or p.cliente.idUsuario = :viewerId)
          and exists (
              select 1 from EntregaArchivo a
              join a.entrega e
              where a.itemSolicitud = i
                and e.esFinal = true
                and e.vigente = true
                and e.retirada = false
                and a.retirado = false
                and a.mime like 'image/%'
          )
        order by p.finalizadoEn desc, i.orden asc
        """, countQuery = """
        select count(i) from Solicitud s
        join s.items i
        join s.pedido p
        where p.cliente.idUsuario = :idCliente
          and p.estado = :estado
          and p.artista.estadoCuenta = true
          and p.publicacionAutorizada = true
          and (:esDuenio = true
               or (p.cliente.pedidosPublicos = true
                   and p.publicacionOcultadaCliente = false))
          and (:idServicio is null
               or p.servicio.idServicio = :idServicio)
          and (:idArtista is null
               or p.artista.idUsuario = :idArtista)
          and (:ocultarSensibles = false
               or p.servicio.contenidoSensible = false
               or p.cliente.idUsuario = :viewerId)
          and exists (
              select 1 from EntregaArchivo a
              join a.entrega e
              where a.itemSolicitud = i
                and e.esFinal = true
                and e.vigente = true
                and e.retirada = false
                and a.retirado = false
                and a.mime like 'image/%'
          )
        """)
    Page<Object[]> findPaginaGaleria(
        @Param("idCliente") Long idCliente,
        @Param("estado") EstadoPedido estado,
        @Param("esDuenio") boolean esDuenio,
        @Param("viewerId") Long viewerId,
        @Param("ocultarSensibles") boolean ocultarSensibles,
        @Param("idServicio") Long idServicio,
        @Param("idArtista") Long idArtista,
        Pageable pageable);

    @Query("""
        select distinct p.servicio from Solicitud s
        join s.pedido p
        where p.cliente.idUsuario = :idCliente
          and p.estado = :estado
          and p.artista.estadoCuenta = true
          and p.publicacionAutorizada = true
          and (:esDuenio = true
               or (p.cliente.pedidosPublicos = true
                   and p.publicacionOcultadaCliente = false))
          and (:ocultarSensibles = false
               or p.servicio.contenidoSensible = false
               or p.cliente.idUsuario = :viewerId)
          and exists (
              select 1 from EntregaArchivo a
              join a.entrega e
              join e.hitoPedido h
              where h.pedido = p
                and e.esFinal = true
                and e.vigente = true
                and e.retirada = false
                and a.retirado = false
                and a.mime like 'image/%'
          )
        order by p.servicio.nombre
        """)
    List<Servicio> serviciosDisponibles(
        @Param("idCliente") Long idCliente,
        @Param("estado") EstadoPedido estado,
        @Param("esDuenio") boolean esDuenio,
        @Param("viewerId") Long viewerId,
        @Param("ocultarSensibles") boolean ocultarSensibles);

    @Query("""
        select distinct p.artista from Solicitud s
        join s.pedido p
        where p.cliente.idUsuario = :idCliente
          and p.estado = :estado
          and p.artista.estadoCuenta = true
          and p.publicacionAutorizada = true
          and (:esDuenio = true
               or (p.cliente.pedidosPublicos = true
                   and p.publicacionOcultadaCliente = false))
          and (:ocultarSensibles = false
               or p.servicio.contenidoSensible = false
               or p.cliente.idUsuario = :viewerId)
          and exists (
              select 1 from EntregaArchivo a
              join a.entrega e
              join e.hitoPedido h
              where h.pedido = p
                and e.esFinal = true
                and e.vigente = true
                and e.retirada = false
                and a.retirado = false
                and a.mime like 'image/%'
          )
        order by p.artista.nombreUsuario
        """)
    List<Usuario> artistasDisponibles(
        @Param("idCliente") Long idCliente,
        @Param("estado") EstadoPedido estado,
        @Param("esDuenio") boolean esDuenio,
        @Param("viewerId") Long viewerId,
        @Param("ocultarSensibles") boolean ocultarSensibles);
}
