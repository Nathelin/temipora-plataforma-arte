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
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "sustitucion")
@Getter
@Setter
@NoArgsConstructor
public class Sustitucion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idSustitucion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_cliente")
    private Usuario cliente;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_servicio_origen", nullable = false)
    private Servicio servicioOrigen;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_artista_origen", nullable = false)
    private Usuario artistaOrigen;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private MotivoSustitucion motivo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private DisparadorSustitucion disparador;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private EstadoSustitucion estado = EstadoSustitucion.ABIERTA;

    @Column(name = "cantidad_opciones", nullable = false)
    private int cantidadOpciones = 0;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_servicio_elegido")
    private Servicio servicioElegido;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_solicitud")
    private Solicitud solicitud;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_pedido")
    private Pedido pedido;

    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private LocalDateTime fechaCreacion = LocalDateTime.now();

    @Column(name = "fecha_resolucion")
    private LocalDateTime fechaResolucion;

    @OneToMany(mappedBy = "sustitucion", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("orden ASC")
    private List<SustitucionOpcion> opciones = new ArrayList<>();

    public void agregarOpcion(SustitucionOpcion opcion) {
        opcion.setSustitucion(this);
        this.opciones.add(opcion);
    }
}
