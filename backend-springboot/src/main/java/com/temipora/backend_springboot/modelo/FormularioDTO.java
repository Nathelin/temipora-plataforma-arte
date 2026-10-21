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
    private Long idServicio;
    private boolean esSistema;
    private boolean propio;
    private List<PreguntaDTO> preguntas;

    public static FormularioDTO desdeFormulario(Formulario formulario) {
        return desdeFormulario(formulario, false);
    }

    public static FormularioDTO desdeFormulario(Formulario formulario, boolean propio) {
        return FormularioDTO.builder()
            .idFormulario(formulario.getIdFormulario())
            .idServicio(formulario.getIdServicio())
            .esSistema(formulario.isEsSistema())
            .propio(propio)
            .preguntas(formulario.getPreguntas().stream()
                .map(PreguntaDTO::desdePregunta)
                .toList())
            .build();
    }
}
