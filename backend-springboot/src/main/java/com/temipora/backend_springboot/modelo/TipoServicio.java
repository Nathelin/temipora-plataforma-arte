package com.temipora.backend_springboot.modelo;


import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name="TipoServicio")
@Setter
@Getter
@NoArgsConstructor
public class TipoServicio {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long tipoServicio;

    @NotBlank
    private String nombre;

    @NotBlank
    private String descripcion;

    @NotNull
    private boolean estado = true;

}
