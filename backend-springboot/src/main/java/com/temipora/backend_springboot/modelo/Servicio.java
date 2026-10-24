package com.temipora.backend_springboot.modelo;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
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
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "Servicio")
@Getter
@Setter
@NoArgsConstructor
public class Servicio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idServicio;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_artista", nullable = false)
    private Usuario artista;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "id_tipo_servicio", nullable = false)
    private TipoServicio tipoServicio;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "id_medio", nullable = false)
    private Medio medio;

    @NotBlank
    @Size(max = 50)
    @Column(nullable = false, length = 50)
    private String nombre;

    @NotBlank
    @Size(max = 250)
    @Column(nullable = false, length = 250)
    private String descripcion;

    @NotNull
    @Column(name = "precio_base", nullable = false, precision = 12, scale = 2)
    private BigDecimal precioBase;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "id_moneda", nullable = false)
    private Moneda moneda;

    @Column(name = "contenido_sensible", nullable = false)
    private boolean contenidoSensible = false;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "servicio_contenido_sensible",
        joinColumns = @JoinColumn(name = "id_servicio"))
    @Enumerated(EnumType.STRING)
    @Column(name = "contenido", nullable = false, length = 20)
    private Set<ContenidoSensible> contenidosSensibles = new HashSet<>();

    @Column(name = "tiempo_entrega_valor")
    private Integer tiempoEntregaValor;

    @Enumerated(EnumType.STRING)
    @Column(name = "tiempo_entrega_unidad", length = 10)
    private ETiempoEntregaUnidad tiempoEntregaUnidad;

    @Column(name = "tiempo_entrega_dias")
    private Integer tiempoEntregaDias;

    @Column(nullable = false)
    private Integer cupos = 0;

    @Enumerated(EnumType.STRING)
    @Column(name = "modo_recepcion", nullable = false, length = 20)
    private ModoRecepcionSolicitud modoRecepcion = ModoRecepcionSolicitud.ABIERTA;

    @Column(name = "max_dibujos_por_solicitud", nullable = false)
    private Integer maxDibujosPorSolicitud = 3;

    @Column(name = "permite_lista_espera", nullable = false)
    private boolean permiteListaEspera = false;

    @Column(name = "habilitar_cupos_tras_entrega", nullable = false)
    private boolean habilitarCuposTrasEntrega = false;

    @Column(name = "imagen_portada", length = 255)
    private String imagenPortada;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoServicio estado = EstadoServicio.PUBLICADO;

    @Column(name = "pausado_en")
    private LocalDateTime pausadoEn;

    @Column(name = "archivado_en")
    private LocalDateTime archivadoEn;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_archivado_por")
    private Usuario archivadoPor;

    @OneToMany(mappedBy = "servicio", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("orden ASC")
    private List<ServicioImagen> imagenes = new ArrayList<>();

    @OneToMany(mappedBy = "servicio", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<ServicioEtiqueta> etiquetas = new LinkedHashSet<>();

    public void reemplazarImagenes(List<ServicioImagen> nuevas) {
        this.imagenes.clear();
        for (ServicioImagen img : nuevas) {
            img.setServicio(this);
            this.imagenes.add(img);
        }
    }

    public void reemplazarEtiquetas(Set<Etiqueta> nuevas) {
        Set<Etiqueta> deseadas = nuevas != null ? nuevas : Set.of();
        Set<Long> idsDeseados = deseadas.stream()
            .map(Etiqueta::getIdEtiqueta)
            .collect(Collectors.toSet());
        this.etiquetas.removeIf(se -> !idsDeseados.contains(se.getEtiqueta().getIdEtiqueta()));

        Set<Long> idsActuales = this.etiquetas.stream()
            .map(se -> se.getEtiqueta().getIdEtiqueta())
            .collect(Collectors.toSet());
        for (Etiqueta etiqueta : deseadas) {
            if (idsActuales.add(etiqueta.getIdEtiqueta())) {
                ServicioEtiqueta asignacion = new ServicioEtiqueta();
                asignacion.setId(new ServicioEtiquetaId());
                asignacion.setServicio(this);
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
}
