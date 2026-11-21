package com.temipora.backend_springboot.modelo;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import lombok.Builder;
import lombok.Getter;

/** Aceptación de términos de un pedido, para la página pública de verificación. */
@Getter
@Builder
public class TerminosAceptadosDTO {

    private String codigo;
    private String urlVerificacion;
    private Long idPedido;
    private String cliente;
    private String artista;
    private String servicio;
    private Integer version;
    private boolean delServicio;
    private LocalDateTime aceptadosEn;
    private String huella;
    private Integer plataformaVersion;
    private String plataformaTitulo;
    private LocalDateTime plataformaAceptadosEn;
    private String plataformaHuella;
    private Contenido contenido;

    /** Texto aceptado, tal como se guardó en el snapshot de la solicitud. */
    public record Contenido(
            Long idTerminos,
            Long idServicio,
            Integer version,
            String informacionGeneral,
            String politicaRevisiones,
            String politicaCancelaciones,
            String usoArtista,
            String usoCliente,
            String pagoExtra,
            String queDibuja,
            String queNoDibuja,
            List<Derecho> derechos) {
    }

    public record Derecho(
            Long idDerechoTermino,
            String codigo,
            String nombre,
            String descripcion,
            boolean requierePiezaUnica,
            boolean disponible,
            BigDecimal porcentajeExtra) {
    }
}
