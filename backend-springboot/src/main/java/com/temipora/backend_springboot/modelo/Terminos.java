package com.temipora.backend_springboot.modelo;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "terminos")
@Getter
@Setter
@NoArgsConstructor
public class Terminos {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idTerminos;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_artista")
    private Usuario artista;

    @Column(name = "id_producto")
    private Long idProducto;

    @Column(name = "id_servicio")
    private Long idServicio;

    @Column(nullable = false)
    private Integer version = 1;

    @Column(name = "informacion_general", columnDefinition = "TEXT")
    private String informacionGeneral;

    @Column(name = "politica_revisiones", columnDefinition = "TEXT")
    private String politicaRevisiones;

    @Column(name = "politica_cancelaciones", columnDefinition = "TEXT")
    private String politicaCancelaciones;

    @Column(name = "uso_artista", columnDefinition = "TEXT")
    private String usoArtista;

    @Column(name = "uso_cliente", columnDefinition = "TEXT")
    private String usoCliente;

    @Column(name = "pago_extra", length = 250)
    private String pagoExtra;

    @Column(name = "tiempo_entrega", length = 120)
    private String tiempoEntrega;

    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private LocalDateTime fechaCreacion = LocalDateTime.now();

    @Column(name = "fecha_actualizacion")
    private LocalDateTime fechaActualizacion;

    @OneToMany(mappedBy = "terminos", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TerminoTema> terminoTemas = new ArrayList<>();

    @OneToMany(mappedBy = "terminos", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TerminoDerecho> terminoDerechos = new ArrayList<>();

    public void reemplazarTemas(List<TerminoTema> nuevos) {
        this.terminoTemas.clear();
        for (TerminoTema t : nuevos) {
            t.setTerminos(this);
            this.terminoTemas.add(t);
        }
    }

    public void reemplazarDerechos(List<TerminoDerecho> nuevos) {
        this.terminoDerechos.clear();
        for (TerminoDerecho d : nuevos) {
            d.setTerminos(this);
            this.terminoDerechos.add(d);
        }
    }
}
