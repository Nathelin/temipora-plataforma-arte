package com.temipora.backend_springboot.modelo;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "portfolio_entrega_verificada")
@Getter
@Setter
@NoArgsConstructor
public class PortfolioEntregaVerificada {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idVerificacion;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_imagen", nullable = false, unique = true)
    private Imagen imagen;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_archivo_entrega", nullable = false)
    private EntregaArchivo archivoEntrega;

    @Column(name = "fecha_vinculacion", nullable = false)
    private LocalDateTime fechaVinculacion = LocalDateTime.now();

    @Column(nullable = false)
    private boolean vigente = true;
}
