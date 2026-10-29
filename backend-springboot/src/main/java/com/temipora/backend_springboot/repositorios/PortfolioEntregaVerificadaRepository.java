package com.temipora.backend_springboot.repositorios;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.temipora.backend_springboot.modelo.PortfolioEntregaVerificada;

public interface PortfolioEntregaVerificadaRepository
        extends JpaRepository<PortfolioEntregaVerificada, Long> {

    Optional<PortfolioEntregaVerificada>
        findByImagenIdImagenAndVigenteTrue(Long idImagen);

    boolean existsByArchivoEntregaIdArchivoAndVigenteTrue(Long idArchivo);

    @Query("""
        select v from PortfolioEntregaVerificada v
        join fetch v.imagen i
        join fetch v.archivoEntrega a
        join fetch a.entrega e
        join fetch e.hitoPedido h
        join fetch h.pedido p
        join fetch p.servicio
        where i.idImagen in :ids
          and v.vigente = true
          and i.estado = true
        """)
    List<PortfolioEntregaVerificada> listarActivasPorImagenes(
        @Param("ids") Collection<Long> ids);

    @Query("""
        select v from PortfolioEntregaVerificada v
        join fetch v.imagen i
        join fetch v.archivoEntrega a
        join fetch a.entrega e
        join fetch e.hitoPedido h
        join fetch h.pedido p
        join fetch p.servicio
        where i.usuario.idUsuario = :idUsuario
          and v.vigente = true
          and i.estado = true
        order by v.fechaVinculacion desc
        """)
    List<PortfolioEntregaVerificada> listarActivasPorUsuario(
        @Param("idUsuario") Long idUsuario);

    @Query("""
        select v from PortfolioEntregaVerificada v
        join fetch v.imagen i
        join fetch v.archivoEntrega a
        join fetch a.entrega e
        join fetch e.hitoPedido h
        join fetch h.pedido p
        join fetch p.artista
        join fetch p.cliente
        join fetch p.servicio
        left join fetch a.itemSolicitud
        where a.idArchivo = :idArchivo
          and v.vigente = true
          and i.estado = true
        """)
    Optional<PortfolioEntregaVerificada> buscarActivaPorArchivo(
        @Param("idArchivo") Long idArchivo);
}
