package com.temipora.backend_springboot.modelo;

import java.time.LocalDateTime;
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
public class HistorialMensajesDTO {

    private List<MensajeDTO> mensajes;
    private Long idCursorAnterior;
    private boolean hayMas;
    private LocalDateTime contraparteUltimoLeidoEn;
}
