package com.temipora.backend_springboot.modelo;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ConfiguracionMensajeriaRequest {

    @NotNull(message = "La longitud máxima del mensaje es obligatoria.")
    @Min(value = 1, message = "La longitud máxima del mensaje debe ser mayor a 0.")
    @Max(value = 4000, message = "La longitud máxima del mensaje no puede superar los 4000 caracteres.")
    private Integer longitudMaximaMensaje;

    @NotNull(message = "La longitud máxima del motivo es obligatoria.")
    @Min(value = 1, message = "La longitud máxima del motivo debe ser mayor a 0.")
    @Max(value = 200, message = "La longitud máxima del motivo no puede superar los 200 caracteres.")
    private Integer longitudMaximaMotivo;

    @NotNull(message = "Las horas de espera para reenviar son obligatorias.")
    @Min(value = 0, message = "Las horas de espera no pueden ser negativas.")
    @Max(value = 8760, message = "Las horas de espera no pueden superar un año.")
    private Integer horasEsperaReenvio;

    @NotNull(message = "El máximo de solicitudes pendientes es obligatorio.")
    @Min(value = 1, message = "El máximo de solicitudes pendientes debe ser al menos 1.")
    @Max(value = 100, message = "El máximo de solicitudes pendientes no puede superar 100.")
    private Integer maxSolicitudesPendientes;

    @NotNull(message = "El máximo de solicitudes por día es obligatorio.")
    @Min(value = 1, message = "El máximo de solicitudes por día debe ser al menos 1.")
    @Max(value = 1000, message = "El máximo de solicitudes por día no puede superar 1000.")
    private Integer maxSolicitudesPorDia;

    @NotNull(message = "El máximo de mensajes por día es obligatorio.")
    @Min(value = 1, message = "El máximo de mensajes por día debe ser al menos 1.")
    @Max(value = 10000, message = "El máximo de mensajes por día no puede superar 10000.")
    private Integer maxMensajesPorDia;
}
