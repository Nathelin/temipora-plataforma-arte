package com.temipora.backend_springboot.modelo;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "tipo_campo")
@Getter
@Setter
@NoArgsConstructor
public class TipoCampo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idTipoCampo;

    @NotBlank
    @Column(nullable = false, unique = true, length = 30)
    private String codigo;

    @NotBlank
    @Size(max = 60)
    @Column(nullable = false, length = 60)
    private String nombre;

    @Column(nullable = false)
    private boolean activo = true;
}
