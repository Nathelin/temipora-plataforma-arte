package com.temipora.backend_springboot.modelo;

import java.math.BigDecimal;
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
public class RecomendacionArtistaDTO {

    private Long idArtista;
    private String nombreUsuario;
    private String nombreDisplay;
    private String urlFotoPerfil;
    private String imagenRepresentativa;
    private String nombreServicio;
    private Long idServicio;
    private BigDecimal precioDesde;
    private String moneda;
    private BigDecimal calificacionPromedio;
    private Long cantidadResenias;
    private List<String> etiquetasDestacadas;
    private String motivo;
    private Double score;
}
