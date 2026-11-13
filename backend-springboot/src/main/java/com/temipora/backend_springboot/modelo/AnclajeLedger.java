package com.temipora.backend_springboot.modelo;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Sellado externo (OpenTimestamps) de un lote de eventos de {@code evento_ledger}.
 * Es append-only: nunca se borra ni se reescribe un evento del ledger; el ancla
 * referencia sus ids y guarda la prueba.
 */
@Entity
@Table(name = "anclaje_ledger")
@Getter
@Setter
@NoArgsConstructor
public class AnclajeLedger {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_anclaje")
    private Long idAnclaje;

    /** Raiz de Merkle (hex) de los hashes de evento cubiertos. */
    @Column(name = "hash_raiz", nullable = false, length = 64)
    private String hashRaiz;

    @Column(name = "cantidad_eventos", nullable = false)
    private int cantidadEventos;

    @Column(name = "id_evento_desde")
    private Long idEventoDesde;

    @Column(name = "id_evento_hasta")
    private Long idEventoHasta;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 20)
    private EstadoAnclaje estado = EstadoAnclaje.PENDIENTE;

    /** Archivo .ots (prueba de OpenTimestamps) tal como lo devuelve el calendario. */
    @Column(name = "prueba_ots")
    private byte[] pruebaOts;

    /** Prueba con la atestacion de Bitcoin ya incorporada (tras el upgrade). */
    @Column(name = "prueba_confirmada")
    private byte[] pruebaConfirmada;

    @Column(name = "calendario_url", length = 255)
    private String calendarioUrl;

    /** Raiz de Merkle del bloque de Bitcoin que contiene el sello (hex). */
    @Column(name = "hash_bitcoin", length = 64)
    private String hashBitcoin;

    @Column(name = "bitcoin_altura")
    private Long bitcoinAltura;

    @Column(name = "fecha_solicitud", nullable = false)
    private LocalDateTime fechaSolicitud = LocalDateTime.now();

    @Column(name = "fecha_confirmacion")
    private LocalDateTime fechaConfirmacion;

    @Column(name = "fecha_actualizacion")
    private LocalDateTime fechaActualizacion;

    @Column(name = "id_evento_auditoria")
    private Long idEventoAuditoria;

    public boolean estaConfirmado() {
        return estado == EstadoAnclaje.CONFIRMADO;
    }
}
