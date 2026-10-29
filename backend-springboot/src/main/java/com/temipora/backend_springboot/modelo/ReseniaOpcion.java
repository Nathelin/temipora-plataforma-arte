package com.temipora.backend_springboot.modelo;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "resenia_opcion")
@Getter
@Setter
@NoArgsConstructor
public class ReseniaOpcion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idOpcionResenia;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_version", nullable = false)
    private ReseniaVersion version;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_opcion", nullable = false)
    private OpcionCriterio opcion;
}
