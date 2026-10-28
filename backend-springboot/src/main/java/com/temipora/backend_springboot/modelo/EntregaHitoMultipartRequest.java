package com.temipora.backend_springboot.modelo;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EntregaHitoMultipartRequest {

    @Size(max = 500, message = "La descripción no puede superar los 500 caracteres.")
    private String descripcion;

    @NotEmpty(message = "Tenés que subir al menos un archivo.")
    @Size(max = 20, message = "Podés subir hasta 20 archivos por entrega.")
    @Valid
    private List<ArchivoEntregaRequest> archivos;
}
