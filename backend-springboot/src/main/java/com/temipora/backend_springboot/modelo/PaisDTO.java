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
public class PaisDTO {

    private Long idPais;
    private String nombre;
    private String codigoIso;
    private String prefijoTelefonico;
    private boolean activo;

    public static PaisDTO desdePais(Pais pais) {
        return PaisDTO.builder()
            .idPais(pais.getIdPais())
            .nombre(pais.getNombre())
            .codigoIso(pais.getCodigoIso())
            .prefijoTelefonico(pais.getPrefijoTelefonico())
            .activo(pais.isEstado())
            .build();
    }
}
