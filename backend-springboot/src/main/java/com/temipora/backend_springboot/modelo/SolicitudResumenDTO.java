package com.temipora.backend_springboot.modelo;

import java.math.BigDecimal;
import java.time.LocalDateTime;

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
public class SolicitudResumenDTO {

    private Long idSolicitud;
    private Long idServicio;
    private String nombreServicio;
    private EstadoServicio estadoServicio;
    private UsuarioResumenDTO cliente;
    private UsuarioResumenDTO artista;
    private EstadoSolicitud estado;
    private Integer cantidadDibujos;
    private BigDecimal precioBase;
    private String moneda;
    // Importes listos para mostrar según la preferencia del usuario.
    private BigDecimal precioBaseMostrado;
    private BigDecimal precioFinalMostrado;
    private BigDecimal precioFinalClienteMostrado;
    private BigDecimal descuentoMostrado;
    private String monedaMostrada;
    private boolean prioridadDefinidaPorCliente;
    private LocalDateTime fechaCreacion;
    private LocalDateTime fechaActualizacion;
    private BigDecimal precioFinal;
    private LocalDateTime fechaVencimiento;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private ReputacionClienteDTO reputacionCliente;

    public static SolicitudResumenDTO desdeSolicitud(Solicitud solicitud) {
        return desdeSolicitud(solicitud, null, null, null);
    }

    public static SolicitudResumenDTO desdeSolicitud(
            Solicitud solicitud,
            Moneda monedaDestino,
            BigDecimal factorOrigenDestino,
            BigDecimal factorCobroDestino) {
        Moneda origen = solicitud.getMoneda();
        Moneda destino = monedaDestino != null ? monedaDestino : origen;
        boolean muestraOrigen = PresupuestoDTO.mismaMoneda(destino, origen);

        BigDecimal precioBaseMostrado = solicitud.getPrecioBase();
        String codigoMostrado = origen != null ? origen.getCodigo() : null;
        if (!muestraOrigen && destino != null && factorOrigenDestino != null) {
            precioBaseMostrado = PresupuestoDTO.convertir(
                solicitud.getPrecioBase(), factorOrigenDestino);
            codigoMostrado = destino.getCodigo();
        }

        PresupuestoDTO presupuesto = solicitud.getPresupuesto() != null
            ? PresupuestoDTO.desdePresupuesto(
                solicitud.getPresupuesto(),
                monedaDestino,
                factorOrigenDestino,
                factorCobroDestino)
            : null;

        return SolicitudResumenDTO.builder()
            .idSolicitud(solicitud.getIdSolicitud())
            .idServicio(solicitud.getServicio() != null
                ? solicitud.getServicio().getIdServicio()
                : null)
            .nombreServicio(solicitud.getServicio() != null
                ? solicitud.getServicio().getNombre()
                : null)
            .estadoServicio(solicitud.getServicio() != null
                ? solicitud.getServicio().getEstado()
                : null)
            .cliente(UsuarioResumenDTO.desdeUsuario(solicitud.getCliente()))
            .artista(UsuarioResumenDTO.desdeUsuario(solicitud.getArtista()))
            .estado(solicitud.getEstado())
            .cantidadDibujos(solicitud.getCantidadDibujos())
            .precioBase(solicitud.getPrecioBase())
            .moneda(origen != null ? origen.getCodigo() : null)
            .precioBaseMostrado(precioBaseMostrado)
            .precioFinalMostrado(presupuesto != null
                ? presupuesto.getPrecioFinalMostrado()
                : null)
            .precioFinalClienteMostrado(presupuesto != null
                ? presupuesto.getPrecioFinalClienteMostrado()
                : null)
            .descuentoMostrado(presupuesto != null
                ? presupuesto.getDescuentoMostrado()
                : null)
            .monedaMostrada(presupuesto != null
                ? presupuesto.getMonedaMostrada()
                : codigoMostrado)
            .prioridadDefinidaPorCliente(solicitud.isPrioridadDefinidaPorCliente())
            .fechaCreacion(solicitud.getFechaCreacion())
            .fechaActualizacion(solicitud.getFechaActualizacion())
            .precioFinal(solicitud.getPresupuesto() != null
                ? solicitud.getPresupuesto().getPrecioFinal()
                : null)
            .fechaVencimiento(solicitud.getPresupuesto() != null
                ? solicitud.getPresupuesto().getFechaVencimiento()
                : null)
            .build();
    }
}
