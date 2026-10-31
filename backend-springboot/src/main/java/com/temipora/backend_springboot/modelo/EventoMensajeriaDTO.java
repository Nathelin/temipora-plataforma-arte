package com.temipora.backend_springboot.modelo;

import com.fasterxml.jackson.annotation.JsonInclude;

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
@JsonInclude(JsonInclude.Include.NON_NULL)
public class EventoMensajeriaDTO {

    public static final String TIPO_MENSAJE = "MENSAJE";
    public static final String TIPO_ESCRITURA = "ESCRITURA";
    public static final String TIPO_LEIDO = "LEIDO";
    public static final String TIPO_CONVERSACION = "CONVERSACION";

    private String tipo;
    private Long idConversacion;
    private MensajePushDTO mensaje;
    private String fechaLectura;
    private EstadoConversacion estado;
}
