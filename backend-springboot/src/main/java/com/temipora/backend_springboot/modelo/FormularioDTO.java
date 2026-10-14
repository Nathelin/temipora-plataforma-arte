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
public class FormularioDTO {

    private Long idFormulario;
    private boolean esSistema;
    private List<PreguntaDTO> preguntas;

    public static FormularioDTO desdeFormulario(Formulario formulario) {
        return FormularioDTO.builder()
            .idFormulario(formulario.getIdFormulario())
            .esSistema(formulario.isEsSistema())
            .preguntas(formulario.getPreguntas().stream()
                .map(PreguntaDTO::desdePregunta)
                .toList())
            .build();
    }
}
