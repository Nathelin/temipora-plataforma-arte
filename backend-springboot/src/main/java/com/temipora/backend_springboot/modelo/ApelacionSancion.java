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
 * Pedido de revisión de una sanción automática. El artista puede reintentar
 * pasado un plazo; el historial completo se conserva.
 */
@Entity
@Table(name = "apelacion_sancion")
@Getter
@Setter
@NoArgsConstructor
public class ApelacionSancion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idApelacion;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_sancion", nullable = false)
    private SancionCuenta sancion;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_usuario", nullable = false)
    private Usuario usuario;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String mensaje;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoApelacion estado = EstadoApelacion.PENDIENTE;

    @Column(name = "creada_en", nullable = false, updatable = false)
    private LocalDateTime creadaEn = LocalDateTime.now();

    @Column(name = "resuelta_en")
    private LocalDateTime resueltaEn;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_admin_resolvio")
    private Usuario adminResolvio;

    @Column(columnDefinition = "TEXT")
    private String respuesta;
}
