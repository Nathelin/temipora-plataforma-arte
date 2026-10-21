package com.temipora.backend_springboot.modelo;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TerminosRequest {

    private String informacionGeneral;
    private String politicaRevisiones;
    private String politicaCancelaciones;
    private String usoArtista;
    private String usoCliente;

    @Size(max = 250, message = "El pago extra no puede superar los 250 caracteres")
    private String pagoExtra;

    @Size(max = 2000, message = "Qué dibujás no puede superar los 2000 caracteres")
    private String queDibuja;

    @Size(max = 2000, message = "Qué no dibujás no puede superar los 2000 caracteres")
    private String queNoDibuja;

    private List<@Valid TerminoDerechoRequest> derechos;
}
