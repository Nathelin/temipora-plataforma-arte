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
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "producto_variante")
@Getter
@Setter
@NoArgsConstructor
public class ProductoVariante {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idVariante;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_producto", nullable = false)
    private Producto producto;

    @NotBlank
    @Size(max = 100)
    @Column(nullable = false, length = 100)
    private String nombre;

    @Size(max = 500)
    @Column(length = 500)
    private String descripcion;

    /**
     * En un borrador puede quedar vacío; al publicar se exige mayor a cero.
     */
    @DecimalMin(value = "0")
    @Column(precision = 12, scale = 2)
    private BigDecimal precio;

    /**
     * NULL = stock ilimitado; 1 = pieza única; > 1 = edición limitada.
     */
    @Min(0)
    @Column(name = "stock_disponible")
    private Integer stockDisponible;

    @NotNull
    @Min(0)
    @Column(nullable = false)
    private Integer orden;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoVariante estado = EstadoVariante.DISPONIBLE;

    @Column(name = "imagen_url", length = 255)
    private String imagenUrl;

    @Column(length = 64)
    private String sha256;

    @Column(name = "clave_archivo_original", length = 255)
    private String claveArchivoOriginal;

    @Column(name = "numero_edicion")
    private Integer numeroEdicion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_propietario_actual")
    private Usuario propietarioActual;

    @Column(name = "reservado_hasta")
    private LocalDateTime reservadoHasta;

    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private LocalDateTime fechaCreacion = LocalDateTime.now();

    @Column(name = "fecha_actualizacion")
    private LocalDateTime fechaActualizacion;

    public boolean esUnica() {
        return stockDisponible != null && stockDisponible == 1;
    }

    public boolean estaDisponible() {
        return estado == EstadoVariante.DISPONIBLE
            && (stockDisponible == null || stockDisponible > 0);
    }
}
