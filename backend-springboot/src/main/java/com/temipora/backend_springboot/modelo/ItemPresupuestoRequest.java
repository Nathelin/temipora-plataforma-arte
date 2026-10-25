package com.temipora.backend_springboot.modelo;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ItemPresupuestoRequest {

    @NotNull(message = "Cada ítem del presupuesto debe indicar su dibujo.")
    private Long idItemSolicitud;

    @Size(max = 250, message = "La nota del dibujo no puede superar los 250 caracteres.")
    private String nota;

    @Size(max = 20, message = "Un dibujo puede tener como máximo 20 recargos.")
    private List<@Valid RecargoPresupuestoRequest> recargos;
}
