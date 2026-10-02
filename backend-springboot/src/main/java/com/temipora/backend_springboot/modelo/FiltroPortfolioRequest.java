package com.temipora.backend_springboot.modelo;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class FiltroPortfolioRequest {

    private String titulo;
    private Long idEtiqueta;
    private String medio;
    private String sensible;
    private String orden;
}
