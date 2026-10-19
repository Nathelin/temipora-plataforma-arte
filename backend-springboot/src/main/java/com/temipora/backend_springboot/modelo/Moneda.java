package com.temipora.backend_springboot.modelo;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "moneda")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Moneda {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_moneda")
    private Long idMoneda;

    @NotBlank(message = "El código es obligatorio.")
    @Size(min = 3, max = 3, message = "El código debe tener 3 letras")
    @Column(nullable = false, unique = true, length = 3)
    private String codigo;

    @NotBlank(message = "El nombre es obligatorio.")
    @Size(max = 60, message = "El nombre no puede superar los 60 caracteres")
    @Column(nullable = false, length = 60)
    private String nombre;

    @Size(max = 6, message = "El símbolo no puede superar los 6 caracteres")
    @Column(length = 6)
    private String simbolo;

    @Builder.Default
    @Column(nullable = false)
    private boolean predeterminada = false;

    @Builder.Default
    @Column(nullable = false)
    private boolean estado = true;
}
