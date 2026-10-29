package com.temipora.backend_springboot.modelo;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ReporteReseniaRequest {

    @NotNull(message = "Seleccioná un motivo para el reporte.")
    private MotivoReporteResenia motivo;

    @Size(max = 500, message = "El comentario no puede superar los 500 caracteres")
    private String comentario;
}
