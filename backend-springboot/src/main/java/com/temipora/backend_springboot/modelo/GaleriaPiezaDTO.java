package com.temipora.backend_springboot.modelo;

import java.time.LocalDateTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

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
public class GaleriaPiezaDTO {

    private Long idPieza;
    private String nombreDibujo;
    private Long idServicio;
    private String nombreServicio;
    private boolean servicioDisponible;
    private UsuarioResumenDTO artista;
    private LocalDateTime fechaEntrega;
    private boolean contenidoSensible;
    private List<String> contenidosSensibles;
    private boolean requiereAdvertencia;
    private List<GaleriaArchivoDTO> archivos;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Long idPedido;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Boolean ocultoPorCliente;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private ReseniaPublicaResumenDTO resenia;
}
