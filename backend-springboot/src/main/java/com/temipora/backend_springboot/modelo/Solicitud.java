package com.temipora.backend_springboot.modelo;

import java.math.BigDecimal;
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

@Entity
@Table(name = "solicitud")
@Getter
@Setter
@NoArgsConstructor
public class Solicitud {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idSolicitud;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_servicio", nullable = false)
    private Servicio servicio;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_cliente", nullable = false)
    private Usuario cliente;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_artista", nullable = false)
    private Usuario artista;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_formulario", nullable = false)
    private Formulario formulario;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_terminos")
    private Terminos terminos;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_oferta_reactivacion")
    private OfertaReactivacion ofertaReactivacion;

    @Column(name = "terminos_version", nullable = false)
    private Integer terminosVersion;

    @Column(name = "terminos_aceptados_en", nullable = false)
    private LocalDateTime terminosAceptadosEn;

    @Column(name = "terminos_snapshot", nullable = false, columnDefinition = "TEXT")
    private String terminosSnapshot;

    @Column(name = "prioridad_definida_por_cliente", nullable = false)
    private boolean prioridadDefinidaPorCliente;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private EstadoSolicitud estado;

    @Column(name = "cantidad_dibujos", nullable = false)
    private Integer cantidadDibujos;

    @Column(name = "precio_base", precision = 12, scale = 2)
    private BigDecimal precioBase;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_moneda")
    private Moneda moneda;

    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private LocalDateTime fechaCreacion = LocalDateTime.now();

    @Column(name = "fecha_actualizacion")
    private LocalDateTime fechaActualizacion;

    @OneToMany(mappedBy = "solicitud", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("orden ASC")
    private List<ItemSolicitud> items = new ArrayList<>();

    @OneToOne(mappedBy = "solicitud", fetch = FetchType.LAZY)
    private ListaEspera listaEspera;

    @OneToOne(mappedBy = "solicitud", fetch = FetchType.LAZY)
    private Presupuesto presupuesto;

    @OneToOne(mappedBy = "solicitud", fetch = FetchType.LAZY)
    private Pedido pedido;

    public void agregarItem(ItemSolicitud item) {
        item.setSolicitud(this);
        this.items.add(item);
    }
}
