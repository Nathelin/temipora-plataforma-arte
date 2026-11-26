package com.temipora.backend_springboot.modelo;

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

/**
 * Recordatorio pendiente de enviar por mail. Cada fila se envía en su propio
 * correo: no se agrupan etapas distintas del mismo pedido.
 */
@Entity
@Table(name = "email_recordatorio_pendiente")
@Getter
@Setter
@NoArgsConstructor
public class EmailRecordatorioPendiente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idEmailRecordatorio;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_usuario", nullable = false)
    private Usuario usuario;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_pedido", nullable = false)
    private Pedido pedido;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_hito_pedido")
    private HitoPedido hitoPedido;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private MotivoRecordatorio motivo;

    @Column(nullable = false, length = 160)
    private String clave;

    @Column(name = "fecha_referencia")
    private LocalDateTime fechaReferencia;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoEmailRecordatorio estado = EstadoEmailRecordatorio.PENDIENTE;

    @Column(name = "id_lote", length = 36)
    private String idLote;

    @Column(nullable = false)
    private int intentos = 0;

    @Column(name = "creado_en", nullable = false, updatable = false)
    private LocalDateTime creadoEn = LocalDateTime.now();

    @Column(name = "enviado_en")
    private LocalDateTime enviadoEn;
}
