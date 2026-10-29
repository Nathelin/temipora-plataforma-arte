package com.temipora.backend_springboot.repositorios;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.temipora.backend_springboot.modelo.EntregaHito;
import com.temipora.backend_springboot.modelo.EstadoEntregaHito;

public interface EntregaHitoRepository extends JpaRepository<EntregaHito, Long> {

    Optional<EntregaHito> findFirstByHitoPedidoIdHitoPedidoAndVigenteTrue(
        Long idHitoPedido);

    Optional<EntregaHito> findFirstByHitoPedidoIdHitoPedidoOrderByNumeroDesc(
        Long idHitoPedido);

    @Query("""
        select e from EntregaHito e
        join fetch e.hitoPedido h
        join fetch e.autor
        left join fetch e.revisionPor
        where h.pedido.idPedido = :idPedido
        order by h.orden asc, e.numero asc
        """)
    List<EntregaHito> listarPorPedido(@Param("idPedido") Long idPedido);

    @Query("""
        select e from EntregaHito e
        join fetch e.hitoPedido h
        join fetch h.pedido p
        join fetch p.artista
        join fetch p.cliente
        where e.idEntrega = :idEntrega and p.idPedido = :idPedido
        """)
    Optional<EntregaHito> buscarDelPedido(
        @Param("idEntrega") Long idEntrega,
        @Param("idPedido") Long idPedido);

    @Query("""
        select e from EntregaHito e
        join e.hitoPedido h
        where h.pedido.idPedido = :idPedido
          and e.vigente = true
          and e.retirada = false
          and h.estadoEntrega in :estados
        order by h.orden desc, e.numero desc
        """)
    List<EntregaHito> listarVigentesAprobadasDelPedido(
        @Param("idPedido") Long idPedido,
        @Param("estados") Collection<EstadoEntregaHito> estados);
}
