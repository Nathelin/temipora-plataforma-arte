package com.temipora.backend_springboot.repositorios;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.temipora.backend_springboot.modelo.CompraGrupo;
import com.temipora.backend_springboot.modelo.EstadoCompraGrupo;

import jakarta.persistence.LockModeType;

public interface CompraGrupoRepository extends JpaRepository<CompraGrupo, Long> {

    List<CompraGrupo> findByCompraIdCompraOrderByFechaCreacionAsc(Long idCompra);

    Optional<CompraGrupo> findByIdCompraGrupoAndCompraIdCompra(
            Long idCompraGrupo, Long idCompra);

    Optional<CompraGrupo> findByIdCompraGrupoAndCompraClienteIdUsuario(
            Long idCompraGrupo, Long idUsuario);

    long countByCompraIdCompraAndEstado(
            Long idCompra, EstadoCompraGrupo estado);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT g FROM CompraGrupo g WHERE g.idCompraGrupo = :idCompraGrupo")
    Optional<CompraGrupo> findByIdCompraGrupoForUpdate(
            @Param("idCompraGrupo") Long idCompraGrupo);
}
