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
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
    name = "cuenta_pago_usuario",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_cuenta_pago_usuario",
        columnNames = { "id_usuario", "proveedor" }))
@Getter
@Setter
@NoArgsConstructor
public class CuentaPagoUsuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_cuenta_pago")
    private Long idCuentaPago;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_usuario", nullable = false)
    private Usuario usuario;

    @Column(nullable = false, length = 30)
    private String proveedor;

    @Column(name = "id_cuenta_externa", length = 100)
    private String idCuentaExterna;

    @Column(name = "access_token", columnDefinition = "TEXT")
    private String accessToken;

    @Column(name = "refresh_token", columnDefinition = "TEXT")
    private String refreshToken;

    @Column(name = "token_expira_en")
    private LocalDateTime tokenExpiraEn;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoCuentaPago estado = EstadoCuentaPago.PENDIENTE;

    @Column(name = "fecha_vinculacion", nullable = false, updatable = false)
    private LocalDateTime fechaVinculacion = LocalDateTime.now();

    @Column(name = "fecha_actualizacion")
    private LocalDateTime fechaActualizacion;
}
