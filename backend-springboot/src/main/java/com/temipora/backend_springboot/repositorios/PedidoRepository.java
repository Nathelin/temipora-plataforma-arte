package com.temipora.backend_springboot.repositorios;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.temipora.backend_springboot.modelo.EstadoPedido;
import com.temipora.backend_springboot.modelo.Pedido;

import jakarta.persistence.LockModeType;

public interface PedidoRepository extends JpaRepository<Pedido, Long> {

    Optional<Pedido> findBySolicitudIdSolicitud(Long idSolicitud);

    boolean existsBySolicitudIdSolicitud(Long idSolicitud);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Pedido p where p.idPedido = :id")
    Optional<Pedido> findByIdPedidoForUpdate(@Param("id") Long id);

    Page<Pedido> findByClienteIdUsuario(Long idCliente, Pageable pageable);

    Page<Pedido> findByArtistaIdUsuario(Long idArtista, Pageable pageable);

    Page<Pedido> findByClienteIdUsuarioAndEstado(
        Long idCliente, EstadoPedido estado, Pageable pageable);

    Page<Pedido> findByArtistaIdUsuarioAndEstado(
        Long idArtista, EstadoPedido estado, Pageable pageable);

    boolean existsByArtistaIdUsuarioAndClienteIdUsuario(
        Long idArtista, Long idCliente);
}
