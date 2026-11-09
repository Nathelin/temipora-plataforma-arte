package com.temipora.backend_springboot.modelo;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.persistence.CascadeType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
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
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "producto")
@Getter
@Setter
@NoArgsConstructor
public class Producto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idProducto;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_artista", nullable = false)
    private Usuario artista;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "id_tipo_producto", nullable = false)
    private TipoProducto tipoProducto;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "id_medio", nullable = false)
    private Medio medio;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "id_moneda", nullable = false)
    private Moneda moneda;

    @NotBlank
    @Size(max = 100)
    @Column(nullable = false, length = 100)
    private String titulo;

    @NotBlank
    @Size(max = 500)
    @Column(nullable = false, length = 500)
    private String descripcion;

    @Column(name = "contenido_sensible", nullable = false)
    private boolean contenidoSensible = false;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "producto_contenido_sensible",
        joinColumns = @JoinColumn(name = "id_producto"))
    @Enumerated(EnumType.STRING)
    @Column(name = "contenido", nullable = false, length = 20)
    private Set<ContenidoSensible> contenidosSensibles = new HashSet<>();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoProducto estado = EstadoProducto.DISPONIBLE;

    @Column(name = "pausado_en")
    private LocalDateTime pausadoEn;

    @Column(name = "archivado_en")
    private LocalDateTime archivadoEn;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_archivado_por")
    private Usuario archivadoPor;

    @Column(name = "imagen_portada", length = 255)
    private String imagenPortada;

    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private LocalDateTime fechaCreacion = LocalDateTime.now();

    @Column(name = "fecha_actualizacion")
    private LocalDateTime fechaActualizacion;

    @OneToMany(mappedBy = "producto", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("orden ASC")
    private Set<ProductoVariante> variantes = new LinkedHashSet<>();

    @OneToMany(mappedBy = "producto", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("orden ASC")
    private Set<ProductoImagen> imagenes = new LinkedHashSet<>();

    @OneToMany(mappedBy = "producto", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<ProductoEtiqueta> etiquetas = new LinkedHashSet<>();

    @OneToMany(mappedBy = "producto", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("version ASC")
    private Set<LicenciaProducto> licencias = new LinkedHashSet<>();

    public void agregarVariante(ProductoVariante variante) {
        variante.setProducto(this);
        this.variantes.add(variante);
    }

    public void quitarVariante(ProductoVariante variante) {
        this.variantes.remove(variante);
    }

    public void reemplazarImagenes(List<ProductoImagen> nuevas) {
        this.imagenes.clear();
        for (ProductoImagen imagen : nuevas) {
            imagen.setProducto(this);
            this.imagenes.add(imagen);
        }
    }

    public void reemplazarEtiquetas(Set<Etiqueta> nuevas) {
        Set<Etiqueta> deseadas = nuevas != null ? nuevas : Set.of();
        Set<Long> idsDeseados = deseadas.stream()
            .map(Etiqueta::getIdEtiqueta)
            .collect(Collectors.toSet());
        this.etiquetas.removeIf(asignacion ->
            !idsDeseados.contains(asignacion.getEtiqueta().getIdEtiqueta()));

        Set<Long> idsActuales = this.etiquetas.stream()
            .map(asignacion -> asignacion.getEtiqueta().getIdEtiqueta())
            .collect(Collectors.toSet());
        for (Etiqueta etiqueta : deseadas) {
            if (idsActuales.add(etiqueta.getIdEtiqueta())) {
                ProductoEtiqueta asignacion = new ProductoEtiqueta();
                asignacion.setId(new ProductoEtiquetaId());
                asignacion.setProducto(this);
                asignacion.setEtiqueta(etiqueta);
                this.etiquetas.add(asignacion);
            }
        }
    }

    public void reemplazarContenidosSensibles(Set<ContenidoSensible> nuevos) {
        this.contenidosSensibles.clear();
        if (nuevos != null) {
            this.contenidosSensibles.addAll(nuevos);
        }
    }

    public void agregarLicencia(LicenciaProducto licencia) {
        licencia.setProducto(this);
        this.licencias.add(licencia);
    }

    public LicenciaProducto licenciaVigente() {
        return licencias.stream()
            .filter(LicenciaProducto::isVigente)
            .findFirst()
            .orElse(null);
    }
}
