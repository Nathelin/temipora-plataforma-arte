package com.temipora.backend_springboot.modelo;

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
import jakarta.persistence.Table;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "termino_tema")
@Getter
@Setter
@NoArgsConstructor
public class TerminoTema {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idTerminoTema;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_terminos", nullable = false)
    private Terminos terminos;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_tema_termino")
    private TemaTermino tema;

    @Size(max = 120)
    @Column(name = "nombre_personalizado", length = 120)
    private String nombrePersonalizado;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private EEstadoTema estado;
}
