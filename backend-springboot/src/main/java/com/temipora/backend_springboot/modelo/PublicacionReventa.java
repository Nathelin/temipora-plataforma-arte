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
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "publicacion_reventa")
@Getter
@Setter
@NoArgsConstructor
public class PublicacionReventa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_publicacion_reventa")
    private Long idPublicacionReventa;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_variante", nullable = false)
    private ProductoVariante variante;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_vendedor", nullable = false)
    private Usuario vendedor;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_compra_item_origen", nullable = false)
    private CompraItem compraItemOrigen;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "id_moneda", nullable = false)
    private Moneda moneda;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal precio;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoPublicacionReventa estado = EstadoPublicacionReventa.PUBLICADA;

    @Version
    @Column(nullable = false)
    private Long version = 0L;

    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private LocalDateTime fechaCreacion = LocalDateTime.now();

    @Column(name = "fecha_actualizacion")
    private LocalDateTime fechaActualizacion;

    @Column(name = "fecha_publicacion", nullable = false)
    private LocalDateTime fechaPublicacion = LocalDateTime.now();

    @Column(name = "fecha_cierre")
    private LocalDateTime fechaCierre;
}
