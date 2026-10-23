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
public class NotificacionDTO {

    private Long idNotificacionUsuario;
    private Long idNotificacion;
    private String nombre;
    private String descripcion;
    private String estado;
    private String codigoTipo;
    private Long idReferencia;
    private String tipoReferencia;
    private boolean leida;
    private LocalDateTime fechaCreacion;
    private LocalDateTime fechaLectura;

    public static NotificacionDTO desdeNotificacionUsuario(NotificacionUsuario destinatario) {
        Notificacion notificacion = destinatario.getNotificacion();
        return NotificacionDTO.builder()
            .idNotificacionUsuario(destinatario.getIdNotificacionUsuario())
            .idNotificacion(notificacion != null ? notificacion.getIdNotificacion() : null)
            .nombre(notificacion != null ? notificacion.getNombre() : null)
            .descripcion(notificacion != null ? notificacion.getDescripcion() : null)
            .estado(notificacion != null ? notificacion.getEstado() : null)
            .codigoTipo(notificacion != null && notificacion.getTipoNotificacion() != null
                ? notificacion.getTipoNotificacion().getCodigo()
                : null)
            .idReferencia(notificacion != null ? notificacion.getIdReferencia() : null)
            .tipoReferencia(notificacion != null ? notificacion.getTipoReferencia() : null)
            .leida(destinatario.isLeida())
            .fechaCreacion(notificacion != null ? notificacion.getFechaCreacion() : null)
            .fechaLectura(destinatario.getFechaLectura())
            .build();
    }
}
