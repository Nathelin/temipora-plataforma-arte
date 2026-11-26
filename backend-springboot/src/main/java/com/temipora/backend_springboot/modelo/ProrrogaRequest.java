package com.temipora.backend_springboot.modelo;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProrrogaRequest {

    @NotNull(message = "Indicá cuántos días necesitás.")
    @Min(value = 1, message = "La prórroga debe ser de al menos 1 día.")
    @Max(value = 365, message = "La prórroga no puede superar los 365 días.")
    private Integer dias;

    @NotBlank(message = "Contale al cliente por qué necesitás más plazo.")
    @Size(max = 500, message = "El motivo no puede superar los 500 caracteres.")
    private String motivo;
}
