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
public class EstadoRelacionMensajeDTO {

    private EstadoRelacionMensaje estadoRelacion;
    private UsuarioResumenDTO contraparte;
    private Long idConversacion;
    private boolean puedeSolicitar;
    private String motivoNoSolicitable;
    private LocalDateTime fechaProximoEnvio;
}
