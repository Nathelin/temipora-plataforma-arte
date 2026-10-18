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
public class TipoCampoDTO {

    private Long idTipoCampo;
    private String codigo;
    private String nombre;

    public static TipoCampoDTO desdeTipoCampo(TipoCampo tipo) {
        return TipoCampoDTO.builder()
            .idTipoCampo(tipo.getIdTipoCampo())
            .codigo(tipo.getCodigo())
            .nombre(tipo.getNombre())
            .build();
    }
}
