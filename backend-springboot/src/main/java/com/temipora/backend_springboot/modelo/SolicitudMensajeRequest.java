package com.temipora.backend_springboot.modelo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class SolicitudMensajeRequest {

    @NotBlank(message = "Indicá el usuario al que querés enviarle la solicitud.")
    @Size(max = 25, message = "El nombre de usuario no es válido.")
    private String nombreUsuarioDestinatario;

    @NotBlank(message = "El motivo de contacto es obligatorio.")
    @Size(max = 200, message = "El motivo de contacto no puede superar los 200 caracteres.")
    private String motivoContacto;

    @NotBlank(message = "El primer mensaje es obligatorio.")
    @Size(max = 4000, message = "El mensaje no puede superar los 4000 caracteres.")
    private String primerMensaje;
}
