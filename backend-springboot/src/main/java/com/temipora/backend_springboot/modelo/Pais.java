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
@Table(name = "Pais")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Pais {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idPais;

    @NotBlank(message = "El nombre es obligatorio.")
    @Size(max = 60, message = "El nombre no puede superar los 60 caracteres")
    @Column(nullable = false, unique = true, length = 60)
    private String nombre;

    @NotBlank(message = "El código ISO es obligatorio.")
    @Size(min = 2, max = 2, message = "El código ISO debe tener 2 letras")
    @Column(name = "codigo_iso", unique = true, length = 2)
    private String codigoIso;

    @NotBlank(message = "El prefijo telefónico es obligatorio.")
    @Size(max = 6, message = "El prefijo telefónico no puede superar los 6 caracteres")
    @Column(name = "prefijo_telefonico", length = 6)
    private String prefijoTelefonico;

    @Builder.Default
    @Column(nullable = false)
    private boolean estado = true;
}
