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
public class ConversacionResumenDTO {

    private Long idConversacion;
    private EstadoConversacion estado;
    private String motivoContacto;
    private UsuarioResumenDTO contraparte;
    private boolean soyIniciador;
    private LocalDateTime fechaSolicitud;
    private LocalDateTime fechaUltimoMensaje;
    private String ultimoMensajePreview;
    private long noLeidos;
    private boolean archivada;
}
