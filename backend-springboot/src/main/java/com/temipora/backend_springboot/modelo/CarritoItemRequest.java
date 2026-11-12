package com.temipora.backend_springboot.modelo;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class CarritoItemRequest {

    @NotNull(message = "La variante es obligatoria.")
    private Long idVariante;

    private Long idPublicacionReventa;

    @Min(value = 1, message = "La cantidad debe ser al menos 1.")
    private Integer cantidad = 1;
}
