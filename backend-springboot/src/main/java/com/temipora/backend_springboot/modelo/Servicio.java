package com.temipora.backend_springboot.modelo;


import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;


@Entity
@Table(name="Servicio")
@Getter
@Setter
@NoArgsConstructor

public class Servicio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idServicio;

    @NotBlank(message = "Por favor, ingrese un nombre para el servicio.")
    @Size(min = 2, max = 25, message = "El nombre ingresado debe tener entre 2 y 25 caracteres")
    private String nombre;

    @NotBlank(message="Por favor, ingrese una descripción del servicio.")
    @Size(min=2, max=250, message = "El nombre ingresado debe tener entre 2 y 250 caracteres")
    private String descripcion;

    @NotNull
    @Column(name="precio base")
    private BigDecimal precioBase;

    @NotNull
    private boolean estado = true;

}
