package com.temipora.backend_springboot.modelo;

import java.time.LocalDateTime;

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
public class MensajeDTO {

    private Long idMensaje;
    private String contenido;
    private LocalDateTime fechaEnvio;
    private boolean esPropio;
    private boolean leido;
    private EstadoMensaje estado;

    public static MensajeDTO desdeMensaje(
            Mensaje mensaje,
            Long idUsuario,
            LocalDateTime ultimoLeidoContraparte) {
        boolean propio = mensaje.getRemitente().getIdUsuario().equals(idUsuario);
        boolean leido = propio
            && ultimoLeidoContraparte != null
            && !mensaje.getFechaEnvio().isAfter(ultimoLeidoContraparte);
        return MensajeDTO.builder()
            .idMensaje(mensaje.getIdMensaje())
            .contenido(mensaje.getContenido())
            .fechaEnvio(mensaje.getFechaEnvio())
            .esPropio(propio)
            .leido(leido)
            .estado(mensaje.getEstado())
            .build();
    }
}
