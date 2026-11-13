package com.temipora.backend_springboot.modelo;

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
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "resenia")
@Getter
@Setter
@NoArgsConstructor
public class Resenia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idResenia;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AmbitoResenia ambito = AmbitoResenia.SERVICIO;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_pedido")
    private Pedido pedido;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_producto")
    private Producto producto;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_compra_item")
    private CompraItem compraItem;

    @Size(max = 500, message = "Las variantes compradas no pueden superar los 500 caracteres")
    @Column(name = "variantes_compradas", length = 500)
    private String variantesCompradas;

    @Column(name = "cantidad_variantes_compradas", nullable = false)
    private int cantidadVariantesCompradas = 0;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_autor", nullable = false)
    private Usuario autor;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoResenia estado = EstadoResenia.PUBLICADA;

    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private LocalDateTime fechaCreacion = LocalDateTime.now();

    @Column(name = "fecha_actualizacion")
    private LocalDateTime fechaActualizacion;

    @Column(name = "eliminada_en")
    private LocalDateTime eliminadaEn;

    @Size(max = 500, message = "El motivo no puede superar los 500 caracteres")
    @Column(name = "motivo_baja", length = 500)
    private String motivoBaja;

    @Column(name = "mostrar_nombre", nullable = false)
    private boolean mostrarNombre = true;

    @Column(name = "moderada_en")
    private LocalDateTime moderadaEn;

    @Size(max = 500, message = "El motivo de moderación no puede superar los 500 caracteres")
    @Column(name = "motivo_moderacion", length = 500)
    private String motivoModeracion;

    @OneToMany(mappedBy = "resenia", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("numero ASC")
    private List<ReseniaVersion> versiones = new ArrayList<>();

    public void agregarVersion(ReseniaVersion version) {
        version.setResenia(this);
        this.versiones.add(version);
    }

    public ReseniaVersion versionActual() {
        return versiones.stream()
            .max((a, b) -> Integer.compare(a.getNumero(), b.getNumero()))
            .orElse(null);
    }

    public boolean estaEditada() {
        return versiones.size() > 1;
    }

    public boolean esDeProducto() {
        return producto != null;
    }
}
