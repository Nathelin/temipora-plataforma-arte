package com.temipora.backend_springboot.modelo;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Comparator;
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
public class CampanaReactivacionDTO {

    private Long idCampana;
    private String token;
    private String estado;
    private LocalDateTime fechaVencimiento;
    private boolean vigente;
    private List<OfertaReactivacionDTO> ofertas;

    public static CampanaReactivacionDTO desdeEntidad(
            CampanaReactivacion campana,
            LocalDateTime ahora) {
        return desdeEntidad(campana, ahora, null);
    }

    public static CampanaReactivacionDTO desdeEntidad(
            CampanaReactivacion campana,
            LocalDateTime ahora,
            BigDecimal comisionVigente) {
        boolean vigente = campana.getEstado() == EstadoCampanaReactivacion.ACTIVA
            && campana.getFechaVencimiento() != null
            && campana.getFechaVencimiento().isAfter(ahora);

        List<OfertaReactivacionDTO> ofertas = campana.getOfertas() == null
            ? List.of()
            : campana.getOfertas().stream()
                .sorted(Comparator.comparing(OfertaReactivacion::getIdOferta))
                .map(oferta -> OfertaReactivacionDTO.desdeEntidad(
                    oferta, ahora, comisionVigente))
                .toList();

        return CampanaReactivacionDTO.builder()
            .idCampana(campana.getIdCampana())
            .token(campana.getToken())
            .estado(campana.getEstado() != null ? campana.getEstado().name() : null)
            .fechaVencimiento(campana.getFechaVencimiento())
            .vigente(vigente)
            .ofertas(ofertas)
            .build();
    }
}
