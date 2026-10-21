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
    private String tiempoEntrega;
    private List<TerminoTemaDTO> temas;
    private List<TerminoDerechoDTO> derechos;

    public static TerminosDTO desdeTerminos(Terminos terminos) {
        return desdeTerminos(terminos, false);
    }

    public static TerminosDTO desdeTerminos(Terminos terminos, boolean propio) {
        List<TerminoTemaDTO> temas = terminos.getTerminoTemas().stream()
            .sorted(Comparator.comparing(t -> t.getTema() != null ? t.getTema().getOrden() : Integer.MAX_VALUE))
            .map(TerminoTemaDTO::desdeTerminoTema)
            .toList();

        List<TerminoDerechoDTO> derechos = terminos.getTerminoDerechos().stream()
            .sorted(Comparator.comparing(d -> d.getDerecho() != null ? d.getDerecho().getOrden() : 0))
            .map(TerminoDerechoDTO::desdeTerminoDerecho)
            .toList();

        return TerminosDTO.builder()
            .idTerminos(terminos.getIdTerminos())
            .idServicio(terminos.getIdServicio())
            .propio(propio)
            .version(terminos.getVersion())
            .informacionGeneral(terminos.getInformacionGeneral())
            .politicaRevisiones(terminos.getPoliticaRevisiones())
            .politicaCancelaciones(terminos.getPoliticaCancelaciones())
            .usoArtista(terminos.getUsoArtista())
            .usoCliente(terminos.getUsoCliente())
            .pagoExtra(terminos.getPagoExtra())
            .tiempoEntrega(terminos.getTiempoEntrega())
            .temas(temas)
            .derechos(derechos)
            .build();
    }

    public static TerminosDTO vacio() {
        return TerminosDTO.builder()
            .version(0)
            .temas(List.of())
            .derechos(List.of())
            .build();
    }
}
