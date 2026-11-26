package com.temipora.backend_springboot.modelo;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
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
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.BatchSize;

@Entity
@Table(name = "pedido")
@Getter
@Setter
@NoArgsConstructor
public class Pedido {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idPedido;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_solicitud", nullable = false, unique = true)
    private Solicitud solicitud;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_presupuesto", nullable = false, unique = true)
    private Presupuesto presupuesto;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_cliente", nullable = false)
    private Usuario cliente;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_artista", nullable = false)
    private Usuario artista;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_servicio", nullable = false)
    private Servicio servicio;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoPedido estado = EstadoPedido.PENDIENTE_PAGO;

    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private LocalDateTime fechaCreacion = LocalDateTime.now();

    @Column(name = "fecha_inicio")
    private LocalDate fechaInicio;

    @Column(name = "limite_entrega")
    private LocalDate limiteEntrega;

    @Column(name = "finalizado_en")
    private LocalDateTime finalizadoEn;

    /**
     * Instante del plazo de entrega que el artista incumplió. Se guarda una vez
     * y no se recalcula: {@code entregaPrevista} se corre hacia adelante si el
     * cliente paga tarde.
     */
    @Column(name = "mora_desde")
    private LocalDateTime moraDesde;

    /**
     * Instante del plazo de pago de la entrega final que el cliente incumplió.
     * Se guarda una vez y no se recalcula: el plazo se corre si el cliente pide
     * una revisión o si el artista reentrega.
     */
    @Column(name = "impago_desde")
    private LocalDateTime impagoDesde;

    @Column(name = "incumplimiento_en")
    private LocalDateTime incumplimientoEn;

    @Column(name = "cierre_por_incumplimiento", nullable = false)
    private boolean cierrePorIncumplimiento = false;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_aceptacion_terminos_plataforma", nullable = false)
    private AceptacionTerminosPlataforma aceptacionTerminosPlataforma;

    @Column(name = "publicacion_autorizada", nullable = false)
    private boolean publicacionAutorizada = false;

    @Column(name = "publicacion_autorizada_en")
    private LocalDateTime publicacionAutorizadaEn;

    @Column(name = "publicacion_ocultada_cliente", nullable = false)
    private boolean publicacionOcultadaCliente = false;

    @Column(name = "publicacion_ocultada_en")
    private LocalDateTime publicacionOcultadaEn;

    @OneToMany(mappedBy = "pedido", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("orden ASC")
    private List<HitoPedido> hitos = new ArrayList<>();

    /**
     * Propuestas de prórroga del plazo. Se cargan en lote para que proyectar
     * las fechas de una lista de pedidos no dispare una consulta por fila.
     */
    @OneToMany(mappedBy = "pedido", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("fechaSolicitud DESC")
    @BatchSize(size = 50)
    private List<ProrrogaPedido> prorrogas = new ArrayList<>();

    public void agregarHito(HitoPedido hito) {
        hito.setPedido(this);
        this.hitos.add(hito);
    }

    public void agregarProrroga(ProrrogaPedido prorroga) {
        prorroga.setPedido(this);
        this.prorrogas.add(prorroga);
    }
}
