package com.temipora.backend_springboot.modelo;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity 
@Table 
@Getter 
@Setter 
@NoArgsConstructor 
public class Pais {
    @Id 
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idPais;

    @NotNull 
    private String nombre;

    @NotNull
    private boolean estado = true;
}
