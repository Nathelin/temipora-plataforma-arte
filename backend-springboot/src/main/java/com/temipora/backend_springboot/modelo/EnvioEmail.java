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

@Entity
@Table(name = "envio_email")
@Getter
@Setter
@NoArgsConstructor
public class EnvioEmail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idEnvio;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_usuario")
    private Usuario usuario;

    @Column(nullable = false, length = 50)
    private String tipo;

    @Column(name = "id_referencia")
    private Long idReferencia;

    @Column(name = "tipo_referencia", length = 30)
    private String tipoReferencia;

    @Column(nullable = false, length = 120)
    private String destinatario;

    @Column(nullable = false, length = 200)
    private String asunto;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoEnvioEmail estado = EstadoEnvioEmail.PENDIENTE;

    @Column(length = 30)
    private String proveedor;

    @Column(name = "id_externo", length = 100)
    private String idExterno;

    @Column(length = 500)
    private String error;

    @Column(name = "clave_idempotencia", nullable = false, unique = true, length = 120)
    private String claveIdempotencia;

    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private LocalDateTime fechaCreacion = LocalDateTime.now();

    @Column(name = "fecha_envio")
    private LocalDateTime fechaEnvio;
}
