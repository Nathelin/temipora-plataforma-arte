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
public class CarritoDTO {

    private Long idCarrito;
    @Builder.Default
    private List<CarritoGrupoDTO> grupos = new ArrayList<>();
    private BigDecimal subtotal;
    private BigDecimal comisionTotal;
    private BigDecimal total;
    private String moneda;
    private int cantidadItems;
    private boolean puedeCheckout;
    @Builder.Default
    private List<String> avisos = new ArrayList<>();
}
