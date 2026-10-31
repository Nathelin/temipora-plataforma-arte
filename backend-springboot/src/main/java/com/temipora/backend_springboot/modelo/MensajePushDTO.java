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
public class MensajePushDTO {

    private Long idMensaje;
    private String contenido;
    private String fechaEnvio;
    private boolean esPropio;
    private boolean leido;

    public static MensajePushDTO desde(MensajeDTO mensaje) {
        if (mensaje == null) {
            return null;
        }
        return MensajePushDTO.builder()
            .idMensaje(mensaje.getIdMensaje())
            .contenido(mensaje.getContenido())
            .fechaEnvio(mensaje.getFechaEnvio() != null
                ? mensaje.getFechaEnvio().toString()
                : null)
            .esPropio(mensaje.isEsPropio())
            .leido(mensaje.isLeido())
            .build();
    }
}
