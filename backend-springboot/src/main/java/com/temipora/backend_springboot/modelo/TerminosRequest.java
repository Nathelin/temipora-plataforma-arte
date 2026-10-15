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

    @Size(max = 120, message = "El tiempo de entrega no puede superar los 120 caracteres")
    private String tiempoEntrega;

    private List<@Valid TerminoTemaRequest> temas;

    private List<@Valid TerminoDerechoRequest> derechos;
}
