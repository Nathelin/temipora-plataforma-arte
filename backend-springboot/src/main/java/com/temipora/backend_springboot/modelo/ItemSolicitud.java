package com.temipora.backend_springboot.modelo;

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
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "item_solicitud")
@Getter
@Setter
@NoArgsConstructor
public class ItemSolicitud {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idItemSolicitud;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_solicitud", nullable = false)
    private Solicitud solicitud;

    @Column(nullable = false, length = 100)
    private String nombre;

    @Column(nullable = false)
    private Integer orden = 0;

    @Column(name = "orden_prioridad")
    private Integer ordenPrioridad;

    @Column(nullable = false, length = 30)
    private String estado = "PENDIENTE";

    @OneToMany(mappedBy = "item", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("orden ASC, suborden ASC")
    private List<Respuesta> respuestas = new ArrayList<>();

    public void agregarRespuesta(Respuesta respuesta) {
        respuesta.setItem(this);
        this.respuestas.add(respuesta);
    }
}
