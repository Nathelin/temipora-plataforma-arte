package com.temipora.backend_springboot.modelo;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Vincula un evento del ledger con el ancla que lo sella (orden de Merkle). */
@Entity
@Table(name = "anclaje_ledger_evento")
@Getter
@Setter
@NoArgsConstructor
public class AnclajeLedgerEvento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_anclaje_evento")
    private Long idAnclajeEvento;

    @Column(name = "id_anclaje", nullable = false)
    private Long idAnclaje;

    @Column(name = "id_evento_ledger", nullable = false)
    private Long idEventoLedger;

    @Column(name = "posicion", nullable = false)
    private int posicion;
}
