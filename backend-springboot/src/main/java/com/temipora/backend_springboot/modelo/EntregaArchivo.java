package com.temipora.backend_springboot.modelo;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
@Table(name = "entrega_archivo")
@Getter
@Setter
@NoArgsConstructor
public class EntregaArchivo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idArchivo;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_entrega", nullable = false)
    private EntregaHito entrega;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_item_solicitud")
    private ItemSolicitud itemSolicitud;

    @Column(name = "nombre_original", nullable = false, length = 255)
    private String nombreOriginal;

    @Column(name = "clave_original", nullable = false, length = 255)
    private String claveOriginal;

    @Column(name = "clave_preview", length = 255)
    private String clavePreview;

    @Column(name = "clave_limpia", length = 255)
    private String claveLimpia;

    @Column(nullable = false, length = 64)
    private String sha256;

    @Column(nullable = false, length = 50)
    private String mime;

    @Column(nullable = false)
    private Integer ancho;

    @Column(nullable = false)
    private Integer alto;

    @Column(name = "tamano_bytes", nullable = false)
    private Long tamanoBytes;

    @Column(nullable = false)
    private Integer orden = 0;

    @Column(name = "fecha_creacion", nullable = false)
    private LocalDateTime fechaCreacion = LocalDateTime.now();

    @Column(nullable = false)
    private boolean retirado = false;

    @Column(name = "motivo_retiro", length = 250)
    private String motivoRetiro;

    @Column(name = "fecha_retiro")
    private LocalDateTime fechaRetiro;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "retirado_por")
    private Usuario retiradoPor;
}
