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
public class SancionCuentaDTO {

    private Long idSancion;
    private Long idUsuario;
    private String nombreUsuario;
    private String correo;
    private TipoSancion tipo;
    private AlcanceSancion alcance;
    private OrigenSancion origen;
    private String motivo;
    private LocalDateTime fechaInicio;
    private LocalDateTime fechaFin;
    private Long idPedido;
    private Long idHitoPedido;
    private LocalDateTime fechaObjetivo;
    private EstadoSancion estado;
    private String estadoEfectivo;
    private boolean vigente;
    private String adminAplico;
    private LocalDateTime fechaReversion;
    private String adminRevertio;
    private String observacionReversion;

    public static SancionCuentaDTO desde(SancionCuenta sancion, LocalDateTime ahora) {
        return SancionCuentaDTO.builder()
            .idSancion(sancion.getIdSancion())
            .idUsuario(sancion.getUsuario() != null ? sancion.getUsuario().getIdUsuario() : null)
            .nombreUsuario(sancion.getUsuario() != null ? sancion.getUsuario().getNombreUsuario() : null)
            .correo(sancion.getUsuario() != null ? sancion.getUsuario().getCorreo() : null)
            .tipo(sancion.getTipo())
            .alcance(sancion.getAlcance())
            .origen(sancion.getOrigen())
            .motivo(sancion.getMotivo())
            .fechaInicio(sancion.getFechaInicio())
            .fechaFin(sancion.getFechaFin())
            .idPedido(sancion.getPedido() != null
                ? sancion.getPedido().getIdPedido()
                : null)
            .idHitoPedido(sancion.getHitoPedido() != null
                ? sancion.getHitoPedido().getIdHitoPedido()
                : null)
            .fechaObjetivo(sancion.getFechaObjetivo())
            .estado(sancion.getEstado())
            .estadoEfectivo(estadoEfectivo(sancion, ahora))
            .vigente(sancion.estaVigente(ahora))
            .adminAplico(sancion.getAdminAplico() != null ? sancion.getAdminAplico().getNombreUsuario() : null)
            .fechaReversion(sancion.getFechaReversion())
            .adminRevertio(sancion.getAdminRevertio() != null ? sancion.getAdminRevertio().getNombreUsuario() : null)
            .observacionReversion(sancion.getObservacionReversion())
            .build();
    }

    private static String estadoEfectivo(SancionCuenta sancion, LocalDateTime ahora) {
        if (sancion.getEstado() == EstadoSancion.REVERTIDA) {
            return "REVERTIDA";
        }
        if (sancion.getFechaFin() != null && !sancion.getFechaFin().isAfter(ahora)) {
            return "VENCIDA";
        }
        return "ACTIVA";
    }
}
