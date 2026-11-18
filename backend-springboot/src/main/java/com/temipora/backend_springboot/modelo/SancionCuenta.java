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
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "sancion_cuenta")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SancionCuenta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_sancion")
    private Long idSancion;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_usuario", nullable = false)
    private Usuario usuario;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", nullable = false, length = 20)
    private TipoSancion tipo;

    @Enumerated(EnumType.STRING)
    @Column(name = "alcance", nullable = false, length = 20)
    @Builder.Default
    private AlcanceSancion alcance = AlcanceSancion.ACCESO;

    @Column(name = "motivo", nullable = false, length = 500)
    private String motivo;

    @Column(name = "fecha_inicio", nullable = false)
    @Builder.Default
    private LocalDateTime fechaInicio = LocalDateTime.now();

    @Column(name = "fecha_fin")
    private LocalDateTime fechaFin;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 20)
    @Builder.Default
    private EstadoSancion estado = EstadoSancion.ACTIVA;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_admin_aplico", nullable = false)
    private Usuario adminAplico;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_admin_revertio")
    private Usuario adminRevertio;

    @Column(name = "fecha_reversion")
    private LocalDateTime fechaReversion;

    @Column(name = "observacion_reversion", length = 500)
    private String observacionReversion;

    public boolean estaVigente(LocalDateTime ahora) {
        return estado == EstadoSancion.ACTIVA
            && (fechaFin == null || fechaFin.isAfter(ahora));
    }
}
