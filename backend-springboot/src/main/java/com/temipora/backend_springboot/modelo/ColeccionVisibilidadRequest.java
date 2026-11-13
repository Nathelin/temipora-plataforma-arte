package com.temipora.backend_springboot.modelo;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ColeccionVisibilidadRequest {

    @NotNull(message = "Indicá si querés ocultar o mostrar la compra.")
    private Boolean ocultar;
}
