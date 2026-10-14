package com.temipora.backend_springboot.modelo;

import java.util.List;

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
public class PreguntaDTO {

    private Long idPregunta;
    private Long idTipoCampo;
    private String codigoTipo;
    private String enunciado;
    private String ayuda;
    private boolean obligatoria;
    private List<String> opciones;

    public static PreguntaDTO desdePregunta(Pregunta pregunta) {
        return PreguntaDTO.builder()
            .idPregunta(pregunta.getIdPregunta())
            .idTipoCampo(pregunta.getTipoCampo() != null ? pregunta.getTipoCampo().getIdTipoCampo() : null)
            .codigoTipo(pregunta.getTipoCampo() != null ? pregunta.getTipoCampo().getCodigo() : null)
            .enunciado(pregunta.getEnunciado())
            .ayuda(pregunta.getAyuda())
            .obligatoria(pregunta.isObligatoria())
            .opciones(pregunta.getOpciones() != null
                ? pregunta.getOpciones().stream().map(OpcionPregunta::getTexto).toList()
                : List.of())
            .build();
    }
}
