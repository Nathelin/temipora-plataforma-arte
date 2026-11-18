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

/**
 * Versión de la imagen de una variante. Cada cambio de imagen crea una fila
 * nueva y marca la anterior como no vigente, de modo que la entrega queda
 * trazada: se puede saber qué archivo recibía el comprador en cada momento.
 * Las variantes de stock 1 no rotan: el ledger fija su imagen.
 */
@Entity
@Table(name = "producto_variante_imagen")
@Getter
@Setter
@NoArgsConstructor
public class ProductoVarianteImagen {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idImagenVariante;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_variante", nullable = false)
    private ProductoVariante variante;

    @Column(nullable = false)
    private Integer numero;

    @Column(nullable = false, length = 255)
    private String url;

    @Column(nullable = false, length = 64)
    private String sha256;

    @Column(name = "clave_archivo_original", length = 255)
    private String claveArchivoOriginal;

    @Column(nullable = false)
    private boolean vigente = true;

    @Column(name = "fecha_desde", nullable = false, updatable = false)
    private LocalDateTime fechaDesde = LocalDateTime.now();

    @Column(name = "fecha_hasta")
    private LocalDateTime fechaHasta;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_usuario_cambio")
    private Usuario usuarioCambio;
}
