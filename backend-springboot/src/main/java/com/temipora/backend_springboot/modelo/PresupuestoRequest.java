package com.temipora.backend_springboot.modelo;

import java.time.LocalDate;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PresupuestoRequest {

    @NotNull(message = "El tiempo máximo de entrega es obligatorio.")
    @Min(value = 1, message = "El tiempo máximo de entrega debe ser al menos 1.")
    private Integer tiempoEntregaValor;

    @NotNull(message = "La unidad del tiempo de entrega es obligatoria.")
    private ETiempoEntregaUnidad tiempoEntregaUnidad;

    private LocalDate fechaVencimiento;

    @Size(max = 500, message = "El mensaje no puede superar los 500 caracteres.")
    private String mensaje;

    @NotEmpty(message = "Tenés que incluir todos los dibujos de la solicitud.")
    @Size(max = 50, message = "Una solicitud puede tener como máximo 50 dibujos.")
    private List<@Valid ItemPresupuestoRequest> items;
}
