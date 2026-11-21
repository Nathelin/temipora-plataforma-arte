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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_servicio")
    private Servicio servicio;

    @Column(nullable = false)
    private Integer version = 1;

    /**
     * Sólo la última versión de cada alcance (global o servicio) queda vigente.
     * Las versiones anteriores se conservan para respetar lo aceptado por los
     * usuarios y para auditoría.
     */
    @Column(nullable = false)
    private boolean vigente = true;

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

    @Column(name = "que_dibuja", columnDefinition = "TEXT")
    private String queDibuja;

    @Column(name = "que_no_dibuja", columnDefinition = "TEXT")
    private String queNoDibuja;

    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private LocalDateTime fechaCreacion = LocalDateTime.now();

    @Column(name = "fecha_actualizacion")
    private LocalDateTime fechaActualizacion;

    /**
     * El artista declara que acepta la preeminencia de los Términos de la
     * Plataforma sobre los suyos. Se registra la versión concreta aceptada.
     */
    @Column(name = "acepta_preeminencia", nullable = false)
    private boolean aceptaPreeminencia = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_terminos_plataforma_aceptados")
    private TerminosPlataforma terminosPlataformaAceptados;

    @Column(name = "preeminencia_aceptada_en")
    private LocalDateTime preeminenciaAceptadaEn;

    @OneToMany(mappedBy = "terminos", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TerminoDerecho> terminoDerechos = new ArrayList<>();

    public void reemplazarDerechos(List<TerminoDerecho> nuevos) {
        this.terminoDerechos.clear();
        for (TerminoDerecho d : nuevos) {
            d.setTerminos(this);
            this.terminoDerechos.add(d);
        }
    }
}
