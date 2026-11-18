package com.temipora.backend_springboot.modelo;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class RevertirSancionRequest {

    @Size(max = 500, message = "La observación no puede superar los 500 caracteres.")
    private String observacion;
}
