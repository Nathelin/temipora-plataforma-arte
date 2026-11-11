package com.temipora.backend_springboot.modelo;

import java.io.Serializable;
import java.time.LocalDate;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class VisitaProductoId implements Serializable {

    private Long idUsuario;
    private Long idProducto;
    private LocalDate fecha;
}
