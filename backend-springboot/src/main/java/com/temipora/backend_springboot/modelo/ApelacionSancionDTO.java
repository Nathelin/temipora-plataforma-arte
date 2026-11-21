package com.temipora.backend_springboot.modelo;

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
public class ApelacionSancionDTO {

    private Long idApelacion;
    private Long idSancion;
    private Long idUsuario;
    private String nombreUsuario;
    private String mensaje;
    private EstadoApelacion estado;
    private LocalDateTime creadaEn;
    private LocalDateTime resueltaEn;
    private String adminResolvio;
    private String respuesta;

    private TipoSancion tipoSancion;
    private AlcanceSancion alcanceSancion;
    private String motivoSancion;
    private LocalDateTime fechaInicioSancion;
    private LocalDateTime fechaFinSancion;
    private Long idPedido;
    private Long idHitoPedido;
    private LocalDateTime fechaObjetivo;
    private boolean sancionVigente;

    public static ApelacionSancionDTO desde(
            ApelacionSancion apelacion, LocalDateTime ahora) {
        SancionCuenta sancion = apelacion.getSancion();
        return ApelacionSancionDTO.builder()
            .idApelacion(apelacion.getIdApelacion())
            .idSancion(sancion != null ? sancion.getIdSancion() : null)
            .idUsuario(apelacion.getUsuario() != null
                ? apelacion.getUsuario().getIdUsuario()
                : null)
            .nombreUsuario(apelacion.getUsuario() != null
                ? apelacion.getUsuario().getNombreUsuario()
                : null)
            .mensaje(apelacion.getMensaje())
            .estado(apelacion.getEstado())
            .creadaEn(apelacion.getCreadaEn())
            .resueltaEn(apelacion.getResueltaEn())
            .adminResolvio(apelacion.getAdminResolvio() != null
                ? apelacion.getAdminResolvio().getNombreUsuario()
                : null)
            .respuesta(apelacion.getRespuesta())
            .tipoSancion(sancion != null ? sancion.getTipo() : null)
            .alcanceSancion(sancion != null ? sancion.getAlcance() : null)
            .motivoSancion(sancion != null ? sancion.getMotivo() : null)
            .fechaInicioSancion(sancion != null ? sancion.getFechaInicio() : null)
            .fechaFinSancion(sancion != null ? sancion.getFechaFin() : null)
            .idPedido(sancion != null && sancion.getPedido() != null
                ? sancion.getPedido().getIdPedido()
                : null)
            .idHitoPedido(sancion != null && sancion.getHitoPedido() != null
                ? sancion.getHitoPedido().getIdHitoPedido()
                : null)
            .fechaObjetivo(sancion != null ? sancion.getFechaObjetivo() : null)
            .sancionVigente(sancion != null && sancion.estaVigente(ahora))
            .build();
    }
}
