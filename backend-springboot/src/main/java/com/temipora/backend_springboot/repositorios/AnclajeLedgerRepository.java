package com.temipora.backend_springboot.repositorios;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import com.temipora.backend_springboot.modelo.AnclajeLedger;
import com.temipora.backend_springboot.modelo.EstadoAnclaje;

import jakarta.persistence.LockModeType;

public interface AnclajeLedgerRepository extends JpaRepository<AnclajeLedger, Long> {

    Optional<AnclajeLedger> findTopByOrderByIdAnclajeDesc();

    List<AnclajeLedger> findByEstadoOrderByIdAnclajeAsc(EstadoAnclaje estado);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<AnclajeLedger> findByIdAnclajeAndEstado(Long idAnclaje, EstadoAnclaje estado);
}
