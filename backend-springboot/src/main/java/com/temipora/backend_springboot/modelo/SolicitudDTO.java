package com.temipora.backend_springboot.modelo;

import java.math.BigDecimal;
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
public class SolicitudDTO {

    private Long idSolicitud;
    private Long idServicio;
    private String nombreServicio;
    private EstadoServicio estadoServicio;
    private UsuarioResumenDTO cliente;
    private UsuarioResumenDTO artista;
    private Long idTerminos;
    private Integer terminosVersion;
    private LocalDateTime terminosAceptadosEn;
    private boolean prioridadDefinidaPorCliente;
    private EstadoSolicitud estado;
    private Integer cantidadDibujos;
    private BigDecimal precioBase;
    private String moneda;
    // Precio base listo para mostrar según la preferencia del usuario.
    private BigDecimal precioBaseMostrado;
    private String monedaMostrada;
    private LocalDateTime fechaCreacion;
    private Integer posicionListaEspera;
    private String motivoRechazo;
    private LocalDateTime fechaRechazo;
    private String motivoCancelacion;
    private LocalDateTime fechaCancelacion;
    private List<ItemSolicitudDTO> items;
    private PresupuestoDTO presupuesto;
    private Long idPedido;
    private EstadoPedido estadoPedido;
    private Long idConversacion;
    private String estadoConversacion;
    private Integer posicionCola;
    private boolean puedePresupuestar;
    private String motivoNoPresupuestable;
    private boolean puedeCancelar;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private ReputacionClienteDTO reputacionCliente;

    public static SolicitudDTO desdeSolicitud(Solicitud solicitud) {
        return desdeSolicitud(solicitud, null, null, null, false);
    }

    public static SolicitudDTO desdeSolicitud(
            Solicitud solicitud,
            Moneda monedaDestino,
            BigDecimal factorOrigenDestino,
            BigDecimal factorCobroDestino) {
        return desdeSolicitud(
            solicitud, monedaDestino, factorOrigenDestino, factorCobroDestino, false);
    }

    public static SolicitudDTO desdeSolicitud(
            Solicitud solicitud,
            Moneda monedaDestino,
            BigDecimal factorOrigenDestino,
            BigDecimal factorCobroDestino,
            boolean incluirComision) {
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

        return SolicitudDTO.builder()
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
            .idTerminos(solicitud.getTerminos() != null
                ? solicitud.getTerminos().getIdTerminos()
                : null)
            .terminosVersion(solicitud.getTerminosVersion())
            .terminosAceptadosEn(solicitud.getTerminosAceptadosEn())
            .prioridadDefinidaPorCliente(solicitud.isPrioridadDefinidaPorCliente())
            .estado(solicitud.getEstado())
            .cantidadDibujos(solicitud.getCantidadDibujos())
            .precioBase(solicitud.getPrecioBase())
            .moneda(origen != null ? origen.getCodigo() : null)
            .precioBaseMostrado(precioBaseMostrado)
            .monedaMostrada(codigoMostrado)
            .fechaCreacion(solicitud.getFechaCreacion())
            .posicionListaEspera(solicitud.getListaEspera() != null
                && "ESPERANDO".equals(solicitud.getListaEspera().getEstado())
                ? solicitud.getListaEspera().getPosicion()
                : null)
            .items(solicitud.getItems().stream()
                .map(ItemSolicitudDTO::desdeItem)
                .toList())
            .presupuesto(solicitud.getPresupuesto() != null
                ? PresupuestoDTO.desdePresupuesto(
                    solicitud.getPresupuesto(),
                    monedaDestino,
                    factorOrigenDestino,
                    factorCobroDestino,
                    incluirComision)
                : null)
            .idPedido(solicitud.getPedido() != null
                ? solicitud.getPedido().getIdPedido()
                : null)
            .estadoPedido(solicitud.getPedido() != null
                ? solicitud.getPedido().getEstado()
                : null)
            .build();
    }
}
