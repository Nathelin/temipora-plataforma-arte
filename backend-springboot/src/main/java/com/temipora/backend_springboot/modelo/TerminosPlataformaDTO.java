package com.temipora.backend_springboot.modelo;

import java.math.BigDecimal;
import java.time.LocalDateTime;

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
public class TerminosPlataformaDTO {

    private Long idTerminosPlataforma;
    private Integer version;
    private String titulo;
    private String contenido;
    private BigDecimal comisionServicio;
    private BigDecimal comisionProducto;
    private LocalDateTime fechaPublicacion;
    private boolean vigente;

    public static TerminosPlataformaDTO desdeTerminos(TerminosPlataforma terminos) {
        return TerminosPlataformaDTO.builder()
            .idTerminosPlataforma(terminos.getIdTerminosPlataforma())
            .version(terminos.getVersion())
            .titulo(terminos.getTitulo())
            .contenido(terminos.getContenido())
            .comisionServicio(terminos.getComisionServicio())
            .comisionProducto(terminos.getComisionProducto())
            .fechaPublicacion(terminos.getFechaPublicacion())
            .vigente(terminos.isVigente())
            .build();
    }
}
