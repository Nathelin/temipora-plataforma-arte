package com.temipora.backend_springboot.modelo;

import java.time.LocalDate;
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
public class ProrrogaDTO {

    private Long idProrroga;
    private int diasSolicitados;
    private String motivo;
    private String motivoRespuesta;
    private String estado;
    private LocalDateTime fechaSolicitud;
    private LocalDateTime fechaLimiteRespuesta;
    private LocalDateTime fechaRespuesta;
    private LocalDate entregaPrevistaAntes;
    private LocalDate entregaPrevistaDespues;

    public static ProrrogaDTO desde(ProrrogaPedido prorroga) {
        if (prorroga == null) {
            return null;
        }
        return ProrrogaDTO.builder()
            .idProrroga(prorroga.getIdProrroga())
            .diasSolicitados(prorroga.getDiasSolicitados() != null
                ? prorroga.getDiasSolicitados()
                : 0)
            .motivo(prorroga.getMotivo())
            .motivoRespuesta(prorroga.getMotivoRespuesta())
            .estado(prorroga.getEstado() != null
                ? prorroga.getEstado().name()
                : null)
            .fechaSolicitud(prorroga.getFechaSolicitud())
            .fechaLimiteRespuesta(prorroga.getFechaLimiteRespuesta())
            .fechaRespuesta(prorroga.getFechaRespuesta())
            .entregaPrevistaAntes(prorroga.getEntregaPrevistaAntes())
            .entregaPrevistaDespues(prorroga.getEntregaPrevistaDespues())
            .build();
    }
}
