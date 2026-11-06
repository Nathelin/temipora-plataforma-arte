package com.temipora.backend_springboot.modelo;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SolicitudServicioRequest {

    @NotNull(message = "El servicio es obligatorio.")
    private Long idServicio;

    /**
     * Oferta de reactivación elegida por el cliente, si la solicitud viene
     * desde una campaña. Es opcional.
     */
    private Long idOferta;

    /**
     * Búsqueda de alternativas desde la que el cliente llegó a este servicio.
     * Es opcional y sirve para cerrar el registro del proceso de sustitución.
     */
    private Long idSustitucion;

    @NotNull(message = "Los términos aceptados son obligatorios.")
    private Long idTerminos;

    @NotNull(message = "La versión de los términos es obligatoria.")
    private Integer terminosVersion;

    private boolean aceptaTerminos;

    private boolean aceptaListaEspera;

    private boolean prioridadDefinida;

    @NotEmpty(message = "Debés agregar al menos un dibujo.")
    @Size(max = 50, message = "Una solicitud puede tener como máximo 50 dibujos.")
    @Valid
    private List<ItemSolicitudRequest> items;
}
