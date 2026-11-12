package com.temipora.backend_springboot.modelo;

import java.math.BigDecimal;
import java.time.LocalDateTime;
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
public class CompraGrupoDTO {

    private Long idCompraGrupo;
    private Long idArtista;
    private String nombreArtista;
    private String nombreVendedor;
    private boolean esReventa;
    private String urlFotoArtista;
    private BigDecimal subtotal;
    private String moneda;
    private EstadoCompraGrupo estado;
    private LocalDateTime fechaPago;
    private boolean puedePagar;
    private String mensajePago;
    @Builder.Default
    private List<CompraItemDTO> items = new ArrayList<>();
}
