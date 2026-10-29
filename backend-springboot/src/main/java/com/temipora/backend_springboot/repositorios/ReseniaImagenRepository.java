package com.temipora.backend_springboot.repositorios;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.temipora.backend_springboot.modelo.EstadoPedido;
import com.temipora.backend_springboot.modelo.EstadoResenia;
import com.temipora.backend_springboot.modelo.ReseniaImagen;

public interface ReseniaImagenRepository
        extends JpaRepository<ReseniaImagen, Long> {

    @EntityGraph(attributePaths = "archivo")
    List<ReseniaImagen> findByVersionIdVersionIn(
        Collection<Long> idsVersion);

    @Query("""
        select ri from ReseniaImagen ri
        join fetch ri.archivo a
        join fetch a.entrega e
        join fetch e.hitoPedido h
        join fetch h.pedido p
        join fetch p.servicio
        join fetch ri.version v
        join fetch v.resenia r
        where a.idArchivo = :idArchivo
          and r.estado = :estado
          and r.autor.idUsuario = p.cliente.idUsuario
          and p.estado = :estadoPedido
          and p.publicacionAutorizada = true
          and p.publicacionOcultadaCliente = false
          and v.numero = (
              select max(v2.numero) from ReseniaVersion v2
              where v2.resenia = r
          )
        """)
    List<ReseniaImagen> buscarPublicasPorArchivo(
        @Param("idArchivo") Long idArchivo,
        @Param("estado") EstadoResenia estado,
        @Param("estadoPedido") EstadoPedido estadoPedido);
}
