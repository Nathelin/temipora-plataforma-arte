package com.temipora.backend_springboot.modelo;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
@Table(name = "item_presupuesto")
@Getter
@Setter
@NoArgsConstructor
public class ItemPresupuesto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idItemPresupuesto;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_presupuesto", nullable = false)
    private Presupuesto presupuesto;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_item_solicitud", nullable = false, unique = true)
    private ItemSolicitud itemSolicitud;

    @Column(name = "precio_base", nullable = false, precision = 12, scale = 2)
    private BigDecimal precioBase;

    @Column(name = "precio_final", nullable = false, precision = 12, scale = 2)
    private BigDecimal precioFinal;

    @Column(length = 250)
    private String nota;

    @Column(nullable = false)
    private Integer orden = 0;

    @OneToMany(mappedBy = "itemPresupuesto", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("orden ASC")
    private List<RecargoItemPresupuesto> recargos = new ArrayList<>();

    public void agregarRecargo(RecargoItemPresupuesto recargo) {
        recargo.setItemPresupuesto(this);
        this.recargos.add(recargo);
    }
}
