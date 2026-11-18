package com.temipora.backend_springboot.repositorios;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.temipora.backend_springboot.modelo.AnclajeLedgerEvento;

public interface AnclajeLedgerEventoRepository
        extends JpaRepository<AnclajeLedgerEvento, Long> {

    List<AnclajeLedgerEvento> findByIdAnclajeOrderByPosicionAsc(Long idAnclaje);

    Optional<AnclajeLedgerEvento> findFirstByIdEventoLedgerOrderByIdAnclajeDesc(
        Long idEventoLedger);

    List<AnclajeLedgerEvento> findByIdEventoLedgerIn(
        Collection<Long> idsEventoLedger);
}
