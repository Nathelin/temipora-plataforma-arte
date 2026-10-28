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
}
