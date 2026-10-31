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
public class ConversacionDTO {

    private Long idConversacion;
    private EstadoConversacion estado;
    private String motivoContacto;
    private UsuarioResumenDTO iniciador;
    private UsuarioResumenDTO destinatario;
    private UsuarioResumenDTO contraparte;
    private boolean soyIniciador;
    private LocalDateTime fechaSolicitud;
    private LocalDateTime fechaRespuesta;
    private LocalDateTime fechaUltimoMensaje;
    private String motivoRechazo;
    private LocalDateTime fechaRechazo;
    private long noLeidos;
    private boolean archivada;
    private boolean puedeAceptar;
    private boolean puedeRechazar;
    private boolean puedeCancelar;
    private boolean puedeEnviar;
    private String motivoNoEnviable;
    private Integer longitudMaximaMensaje;
    private Integer longitudMaximaMotivo;
    private LocalDateTime fechaProximoEnvio;
    private LocalDateTime contraparteUltimoLeidoEn;
}
