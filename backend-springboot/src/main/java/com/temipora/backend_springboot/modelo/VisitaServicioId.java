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
public class VisitaServicioId implements Serializable {

    private Long idUsuario;
    private Long idServicio;
    private LocalDate fecha;
}
