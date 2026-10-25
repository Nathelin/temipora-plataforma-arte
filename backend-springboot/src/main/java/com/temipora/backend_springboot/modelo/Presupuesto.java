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
@Table(name = "presupuesto")
@Getter
@Setter
@NoArgsConstructor
public class Presupuesto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idPresupuesto;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_solicitud", nullable = false, unique = true)
    private Solicitud solicitud;

    @Column(name = "precio_base_total", nullable = false, precision = 12, scale = 2)
    private BigDecimal precioBaseTotal;

    @Column(name = "precio_final", nullable = false, precision = 12, scale = 2)
    private BigDecimal precioFinal;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "id_moneda", nullable = false)
    private Moneda moneda;

    @Column(name = "tiempo_entrega_valor", nullable = false)
    private Integer tiempoEntregaValor;

    @Enumerated(EnumType.STRING)
    @Column(name = "tiempo_entrega_unidad", nullable = false, length = 10)
    private ETiempoEntregaUnidad tiempoEntregaUnidad;

    @Column(name = "tiempo_entrega_dias", nullable = false)
    private Integer tiempoEntregaDias;

    @Column(length = 500)
    private String mensaje;

    @Column(name = "fecha_emision", nullable = false, updatable = false)
    private LocalDateTime fechaEmision = LocalDateTime.now();

    @Column(name = "fecha_vencimiento", nullable = false)
    private LocalDateTime fechaVencimiento;

    @OneToMany(mappedBy = "presupuesto", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("orden ASC")
    private List<ItemPresupuesto> items = new ArrayList<>();

    public void agregarItem(ItemPresupuesto item) {
        item.setPresupuesto(this);
        this.items.add(item);
    }
}
