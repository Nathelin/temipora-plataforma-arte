package com.temipora.backend_springboot.modelo;

import java.time.LocalDate;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "visita_servicio")
@IdClass(VisitaServicioId.class)
@Getter
@Setter
@NoArgsConstructor
public class VisitaServicio {

    @Id
    @Column(name = "id_usuario", nullable = false)
    private Long idUsuario;

    @Id
    @Column(name = "id_servicio", nullable = false)
    private Long idServicio;

    @Id
    @Column(name = "fecha", nullable = false)
    private LocalDate fecha;

    @Column(name = "cantidad", nullable = false)
    private int cantidad = 1;

    @Column(name = "ultima_visita", nullable = false)
    private LocalDateTime ultimaVisita = LocalDateTime.now();
}
