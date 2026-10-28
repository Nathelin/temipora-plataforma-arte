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
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "entrega_hito")
@Getter
@Setter
@NoArgsConstructor
public class EntregaHito {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idEntrega;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_hito_pedido", nullable = false)
    private HitoPedido hitoPedido;

    @Column(nullable = false)
    private Integer numero;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_autor", nullable = false)
    private Usuario autor;

    @Column(length = 500)
    private String descripcion;

    @Column(name = "fecha_entrega", nullable = false)
    private LocalDateTime fechaEntrega = LocalDateTime.now();

    @Column(nullable = false)
    private boolean vigente = true;

    @Column(name = "revision_motivo", length = 500)
    private String revisionMotivo;

    @Column(name = "revision_fecha")
    private LocalDateTime revisionFecha;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "revision_por")
    private Usuario revisionPor;

    @Column(nullable = false)
    private boolean retirada = false;

    @Column(name = "motivo_retiro", length = 250)
    private String motivoRetiro;

    @Column(name = "fecha_retiro")
    private LocalDateTime fechaRetiro;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "retirada_por")
    private Usuario retiradaPor;

    @OneToMany(mappedBy = "entrega", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("orden ASC")
    private List<EntregaArchivo> archivos = new ArrayList<>();

    public void agregarArchivo(EntregaArchivo archivo) {
        archivo.setEntrega(this);
        this.archivos.add(archivo);
    }
}
