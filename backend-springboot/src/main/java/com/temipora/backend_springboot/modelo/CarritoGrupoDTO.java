package com.temipora.backend_springboot.modelo;

import java.math.BigDecimal;
import java.util.ArrayList;
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
public class CarritoGrupoDTO {

    private Long idArtista;
    private String nombreArtista;
    private String urlFotoArtista;
    private boolean puedeCobrar;
    private String motivoNoCobro;
    private boolean esReventa;
    @Builder.Default
    private List<CarritoItemDTO> items = new ArrayList<>();
    private BigDecimal subtotal;
    private BigDecimal total;
}
