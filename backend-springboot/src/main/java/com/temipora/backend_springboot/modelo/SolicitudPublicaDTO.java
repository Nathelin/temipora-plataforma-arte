package com.temipora.backend_springboot.modelo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SolicitudPublicaDTO {

    private ServicioDTO servicio;
    private TerminosDTO terminos;
    private FormularioDTO formulario;
    private DisponibilidadServicioDTO disponibilidad;
    private boolean terminosValidos;
}
