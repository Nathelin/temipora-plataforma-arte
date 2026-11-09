package com.temipora.backend_springboot.modelo;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "evento_ledger")
@Getter
@Setter
@NoArgsConstructor
public class EventoLedger {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idEventoLedger;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_variante", nullable = false)
    private ProductoVariante variante;

    @Column(nullable = false)
    private Long secuencia;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TipoEventoLedger tipo;

    @Column(name = "id_venta")
    private Long idVenta;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_actor_origen")
    private Usuario actorOrigen;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_actor_destino")
    private Usuario actorDestino;

    @Column(precision = 12, scale = 2)
    private BigDecimal monto;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_moneda")
    private Moneda moneda;

    @Column(name = "ocurrido_en", nullable = false)
    private LocalDateTime ocurridoEn;

    @Column(columnDefinition = "TEXT")
    private String datos;

    @Column(name = "hash_previo", nullable = false, length = 64)
    private String hashPrevio;

    @Column(name = "hash_imagen", length = 64)
    private String hashImagen;

    @Column(name = "hash_evento", nullable = false, unique = true, length = 64)
    private String hashEvento;

    @Column(name = "id_evento_auditoria")
    private Long idEventoAuditoria;
}
