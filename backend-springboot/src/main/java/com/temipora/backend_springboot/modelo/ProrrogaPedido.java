package com.temipora.backend_springboot.modelo;

import java.time.LocalDate;
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
 * Propuesta del artista para correr el plazo de entrega de un pedido. No
 * modifica {@code pedido.limiteEntrega}: la fecha prometida queda congelada y
 * los días aceptados se suman a la entrega prevista.
 */
@Entity
@Table(name = "prorroga_pedido")
@Getter
@Setter
@NoArgsConstructor
public class ProrrogaPedido {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idProrroga;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_pedido", nullable = false)
    private Pedido pedido;

    @Column(name = "dias_solicitados", nullable = false)
    private Integer diasSolicitados;

    @Column(nullable = false, length = 500)
    private String motivo;

    @Column(name = "motivo_respuesta", length = 500)
    private String motivoRespuesta;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoProrroga estado = EstadoProrroga.PENDIENTE;

    @Column(name = "fecha_solicitud", nullable = false, updatable = false)
    private LocalDateTime fechaSolicitud = LocalDateTime.now();

    @Column(name = "fecha_limite_respuesta", nullable = false)
    private LocalDateTime fechaLimiteRespuesta;

    @Column(name = "fecha_respuesta")
    private LocalDateTime fechaRespuesta;

    /**
     * Entrega prevista en el momento de aceptar. Se guarda como constancia del
     * corrimiento efectivo, aunque después se acepten otras prórrogas.
     */
    @Column(name = "entrega_prevista_antes")
    private LocalDate entregaPrevistaAntes;

    @Column(name = "entrega_prevista_despues")
    private LocalDate entregaPrevistaDespues;
}
