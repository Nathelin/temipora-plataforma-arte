package com.temipora.backend_springboot.modelo;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.Set;

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
@Table(name = "licencia_producto")
@Getter
@Setter
@NoArgsConstructor
public class LicenciaProducto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idLicenciaProducto;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_producto", nullable = false)
    private Producto producto;

    @Column(name = "permite_modificaciones", nullable = false)
    private boolean permiteModificaciones = false;

    @Column(name = "requiere_credito", nullable = false)
    private boolean requiereCredito = false;

    @Column(name = "porcentaje_regalia", nullable = false, precision = 5, scale = 2)
    private BigDecimal porcentajeRegalia = BigDecimal.ZERO;

    @Column(name = "texto_adicional", length = 2000)
    private String textoAdicional;

    @Column(nullable = false)
    private Integer version = 1;

    @Column(nullable = false)
    private boolean vigente = true;

    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private LocalDateTime fechaCreacion = LocalDateTime.now();

    @OneToMany(mappedBy = "licencia", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<LicenciaProductoDerecho> derechos = new LinkedHashSet<>();

    public void reemplazarDerechos(Set<DerechoTermino> nuevos) {
        this.derechos.clear();
        if (nuevos == null) {
            return;
        }
        for (DerechoTermino derecho : nuevos) {
            LicenciaProductoDerecho asignacion = new LicenciaProductoDerecho();
            asignacion.setId(new LicenciaProductoDerechoId());
            asignacion.setLicencia(this);
            asignacion.setDerecho(derecho);
            this.derechos.add(asignacion);
        }
    }

    public boolean permiteReventa() {
        return derechos.stream()
            .map(LicenciaProductoDerecho::getDerecho)
            .anyMatch(derecho -> derecho != null
                && "REVENTA".equalsIgnoreCase(derecho.getCodigo()));
    }
}
