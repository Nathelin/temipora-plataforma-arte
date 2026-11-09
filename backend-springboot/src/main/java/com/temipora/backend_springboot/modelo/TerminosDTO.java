package com.temipora.backend_springboot.modelo;

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
public class TerminosDTO {

    private Long idTerminos;
    private Long idServicio;
    private boolean propio;
    private Integer version;
    private String informacionGeneral;
    private String politicaRevisiones;
    private String politicaCancelaciones;
    private String usoArtista;
    private String usoCliente;
    private String pagoExtra;
    private String queDibuja;
    private String queNoDibuja;
    private List<TerminoDerechoDTO> derechos;

    public static TerminosDTO desdeTerminos(Terminos terminos) {
        return desdeTerminos(terminos, false);
    }

    public static TerminosDTO desdeTerminos(Terminos terminos, boolean propio) {
        List<TerminoDerechoDTO> derechos = terminos.getTerminoDerechos().stream()
            .sorted(Comparator.comparing(d -> d.getDerecho() != null ? d.getDerecho().getOrden() : 0))
            .map(TerminoDerechoDTO::desdeTerminoDerecho)
            .toList();

        return TerminosDTO.builder()
            .idTerminos(terminos.getIdTerminos())
            .idServicio(terminos.getServicio() != null
                ? terminos.getServicio().getIdServicio()
                : null)
            .propio(propio)
            .version(terminos.getVersion())
            .informacionGeneral(terminos.getInformacionGeneral())
            .politicaRevisiones(terminos.getPoliticaRevisiones())
            .politicaCancelaciones(terminos.getPoliticaCancelaciones())
            .usoArtista(terminos.getUsoArtista())
            .usoCliente(terminos.getUsoCliente())
            .pagoExtra(terminos.getPagoExtra())
            .queDibuja(terminos.getQueDibuja())
            .queNoDibuja(terminos.getQueNoDibuja())
            .derechos(derechos)
            .build();
    }

    public static TerminosDTO vacio() {
        return TerminosDTO.builder()
            .version(0)
            .derechos(List.of())
            .build();
    }
}
