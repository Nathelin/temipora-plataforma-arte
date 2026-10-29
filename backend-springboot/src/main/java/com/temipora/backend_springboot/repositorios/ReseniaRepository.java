package com.temipora.backend_springboot.repositorios;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.temipora.backend_springboot.modelo.EstadoResenia;
import com.temipora.backend_springboot.modelo.Resenia;

import jakarta.persistence.LockModeType;

public interface ReseniaRepository extends JpaRepository<Resenia, Long> {

    Optional<Resenia> findByPedidoIdPedidoAndAutorIdUsuario(
        Long idPedido, Long idAutor);

    boolean existsByPedidoIdPedidoAndAutorIdUsuario(Long idPedido, Long idAutor);

    List<Resenia> findByPedidoIdPedidoAndEstadoOrderByFechaCreacionAsc(
        Long idPedido, EstadoResenia estado);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT r FROM Resenia r
        WHERE r.pedido.idPedido = :idPedido AND r.autor.idUsuario = :idAutor
        """)
    Optional<Resenia> findForUpdateByPedidoAndAutor(
        @Param("idPedido") Long idPedido,
        @Param("idAutor") Long idAutor);

    @Query("""
        SELECT COUNT(r) FROM Resenia r
        WHERE r.pedido.cliente.idUsuario = :idCliente
          AND r.autor.idUsuario = r.pedido.artista.idUsuario
          AND r.estado = :estado
        """)
    long contarReseniasDeCliente(
        @Param("idCliente") Long idCliente,
        @Param("estado") EstadoResenia estado);

    @Query("""
        SELECT AVG(v.calificacionGeneral) FROM Resenia r
        JOIN r.versiones v
        WHERE r.pedido.cliente.idUsuario = :idCliente
          AND r.autor.idUsuario = r.pedido.artista.idUsuario
          AND r.estado = :estado
          AND v.numero = (
              SELECT MAX(v2.numero) FROM ReseniaVersion v2 WHERE v2.resenia = r
          )
        """)
    Double promedioReseniasDeCliente(
        @Param("idCliente") Long idCliente,
        @Param("estado") EstadoResenia estado);
}
