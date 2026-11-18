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
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
    name = "compra_grupo",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_compra_grupo_artista",
        columnNames = { "id_compra", "id_artista" }))
@Getter
@Setter
@NoArgsConstructor
public class CompraGrupo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_compra_grupo")
    private Long idCompraGrupo;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_compra", nullable = false)
    private Compra compra;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_artista", nullable = false)
    private Usuario artista;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "id_moneda", nullable = false)
    private Moneda moneda;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal subtotal = BigDecimal.ZERO;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal comision = BigDecimal.ZERO;

    @Column(name = "neto_artista", nullable = false, precision = 12, scale = 2)
    private BigDecimal netoArtista = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private EstadoCompraGrupo estado = EstadoCompraGrupo.PENDIENTE;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_cuenta_pago")
    private CuentaPagoUsuario cuentaPago;

    @Column(name = "cuenta_externa_id", length = 100)
    private String cuentaExternaId;

    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private LocalDateTime fechaCreacion = LocalDateTime.now();

    @Column(name = "fecha_actualizacion")
    private LocalDateTime fechaActualizacion;

    @Column(name = "fecha_pago")
    private LocalDateTime fechaPago;

    @OneToMany(mappedBy = "grupo", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CompraItem> items = new ArrayList<>();

    public void agregarItem(CompraItem item) {
        item.setGrupo(this);
        this.items.add(item);
    }
}
