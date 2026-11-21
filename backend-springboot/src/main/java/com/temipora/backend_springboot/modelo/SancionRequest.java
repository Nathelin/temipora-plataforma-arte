package com.temipora.backend_springboot.modelo;

import java.time.LocalDateTime;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class SancionRequest {

    @NotNull(message = "Debés indicar el usuario a inhabilitar.")
    private Long idUsuario;

    @NotNull(message = "Debés indicar el tipo de sanción.")
    private TipoSancion tipo;

    /** Si no se indica, se bloquea el acceso completo. */
    private AlcanceSancion alcance;

    @NotBlank(message = "El motivo es obligatorio.")
    @Size(max = 500, message = "El motivo no puede superar los 500 caracteres.")
    private String motivo;

    private LocalDateTime fechaFin;
}
