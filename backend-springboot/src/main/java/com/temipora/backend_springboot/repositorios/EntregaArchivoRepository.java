package com.temipora.backend_springboot.repositorios;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.temipora.backend_springboot.modelo.EntregaArchivo;

public interface EntregaArchivoRepository extends JpaRepository<EntregaArchivo, Long> {

    @Query("""
        select a from EntregaArchivo a
        left join fetch a.itemSolicitud
        where a.entrega.idEntrega in :ids
        order by a.orden asc
        """)
    List<EntregaArchivo> listarPorEntregas(@Param("ids") Collection<Long> ids);

    @Query("""
        select a from EntregaArchivo a
        join fetch a.entrega e
        join fetch e.hitoPedido h
        join fetch h.pedido p
        join fetch p.artista
        join fetch p.cliente
        left join fetch a.itemSolicitud
        where a.idArchivo = :idArchivo
          and e.idEntrega = :idEntrega
          and p.idPedido = :idPedido
        """)
    Optional<EntregaArchivo> buscarDelPedido(
        @Param("idArchivo") Long idArchivo,
        @Param("idEntrega") Long idEntrega,
        @Param("idPedido") Long idPedido);

    @Query("""
        select a from EntregaArchivo a
        join fetch a.entrega e
        join fetch e.hitoPedido h
        join fetch h.pedido p
        join fetch p.servicio
        join fetch p.artista
        join fetch a.itemSolicitud i
        where i.idItemSolicitud in :ids
          and e.esFinal = true
          and e.vigente = true
          and e.retirada = false
          and a.retirado = false
          and a.mime like 'image/%'
        order by i.orden asc, a.orden asc
        """)
    List<EntregaArchivo> listarGaleriaPorItems(
        @Param("ids") Collection<Long> ids);

    @Query("""
        select a from EntregaArchivo a
        join fetch a.entrega e
        join fetch e.hitoPedido h
        where h.pedido.idPedido = :idPedido
          and e.esFinal = true
          and e.vigente = true
          and e.retirada = false
          and a.retirado = false
          and a.mime like 'image/%'
        order by a.orden asc
        """)
    List<EntregaArchivo> listarFinalesDelPedido(
        @Param("idPedido") Long idPedido);

    @Query("""
        select a from EntregaArchivo a
        join fetch a.entrega e
        join fetch e.hitoPedido h
        join fetch h.pedido p
        join fetch p.cliente
        join fetch p.artista
        join fetch p.servicio
        left join fetch a.itemSolicitud
        where a.idArchivo = :idArchivo
        """)
    Optional<EntregaArchivo> buscarParaGaleria(
        @Param("idArchivo") Long idArchivo);
}
