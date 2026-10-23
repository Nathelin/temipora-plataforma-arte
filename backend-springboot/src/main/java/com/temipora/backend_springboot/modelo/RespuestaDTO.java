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
public class RespuestaDTO {

    private Long idRespuesta;
    private Long idPregunta;
    private Long idTipoCampo;
    private String codigoTipo;
    private String enunciado;
    private String valor;
    private Integer orden;
    private Integer suborden;

    public static RespuestaDTO desdeRespuesta(Respuesta respuesta) {
        return RespuestaDTO.builder()
            .idRespuesta(respuesta.getIdRespuesta())
            .idPregunta(respuesta.getPregunta() != null ? respuesta.getPregunta().getIdPregunta() : null)
            .idTipoCampo(respuesta.getTipoCampo() != null ? respuesta.getTipoCampo().getIdTipoCampo() : null)
            .codigoTipo(respuesta.getTipoCampo() != null ? respuesta.getTipoCampo().getCodigo() : null)
            .enunciado(respuesta.getEnunciado())
            .valor(respuesta.getRespuesta())
            .orden(respuesta.getOrden())
            .suborden(respuesta.getSuborden())
            .build();
    }
}
