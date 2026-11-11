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
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "intento_cobro")
@Getter
@Setter
@NoArgsConstructor
public class IntentoCobro {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_intento_cobro")
    private Long idIntentoCobro;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_compra", nullable = false)
    private Compra compra;

    @Column(nullable = false, length = 30)
    private String proveedor;

    @Column(name = "external_reference", nullable = false, unique = true, length = 100)
    private String externalReference;

    @Column(name = "id_preferencia_externa", length = 100)
    private String idPreferenciaExterna;

    @Column(name = "id_pago_externo", length = 100)
    private String idPagoExterno;

    @Column(columnDefinition = "TEXT")
    private String instruccion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private EstadoIntentoCobro estado = EstadoIntentoCobro.CREADO;

    @Column(name = "estado_externo", length = 50)
    private String estadoExterno;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal monto;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "id_moneda", nullable = false)
    private Moneda moneda;

    @Column(name = "clave_idempotencia", unique = true, length = 100)
    private String claveIdempotencia;

    @Column(columnDefinition = "TEXT")
    private String payload;

    @Column(name = "arancel_mp", precision = 12, scale = 2)
    private BigDecimal arancelMp;

    @Column(name = "neto_recibido_mp", precision = 12, scale = 2)
    private BigDecimal netoRecibidoMp;

    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private LocalDateTime fechaCreacion = LocalDateTime.now();

    @Column(name = "fecha_actualizacion")
    private LocalDateTime fechaActualizacion;

    @Column(name = "fecha_aprobacion")
    private LocalDateTime fechaAprobacion;

    @OneToMany(mappedBy = "intentoCobro", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<IntentoCobroGrupo> grupos = new ArrayList<>();

    public void agregarGrupo(IntentoCobroGrupo grupo) {
        grupo.setIntentoCobro(this);
        this.grupos.add(grupo);
    }
}
