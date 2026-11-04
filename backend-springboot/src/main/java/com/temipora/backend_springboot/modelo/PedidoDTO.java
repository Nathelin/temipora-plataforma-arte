package com.temipora.backend_springboot.modelo;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
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
public class PedidoDTO {

    private Long idPedido;
    private Long idSolicitud;
    private Long idServicio;
    private String nombreServicio;
    private UsuarioResumenDTO cliente;
    private UsuarioResumenDTO artista;
    private String estado;
    private LocalDateTime fechaCreacion;
    private LocalDate fechaInicio;
    private LocalDate limiteEntrega;
    private LocalDateTime finalizadoEn;
    private BigDecimal precioFinal;
    private BigDecimal precioFinalCobro;
    private BigDecimal precioFinalMostrado;
    private BigDecimal descuentoPorcentaje;
    private BigDecimal descuentoMonto;
    private BigDecimal descuentoMontoCobro;
    private BigDecimal descuentoMostrado;
    private BigDecimal precioFinalCliente;
    private BigDecimal precioFinalClienteCobro;
    private BigDecimal precioFinalClienteMostrado;
    private String moneda;
    private String monedaCobro;
    private String monedaMostrada;
    private BigDecimal cotizacionCobro;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private BigDecimal comisionPorcentaje;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private BigDecimal comisionMonto;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private BigDecimal comisionMontoCobro;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private BigDecimal comisionMontoMostrado;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private BigDecimal montoNetoArtista;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private BigDecimal montoNetoArtistaCobro;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private BigDecimal montoNetoArtistaMostrado;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private BigDecimal arancelMpPorcentaje;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private BigDecimal arancelMpEstimado;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private BigDecimal arancelMpEstimadoCobro;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private BigDecimal arancelMpEstimadoMostrado;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private BigDecimal montoNetoArtistaConArancel;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private BigDecimal montoNetoArtistaConArancelCobro;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private BigDecimal montoNetoArtistaConArancelMostrado;

    private Integer terminosPlataformaVersion;
    private LocalDateTime terminosPlataformaAceptadosEn;
    private List<HitoPedidoDTO> hitos;
    private List<DibujoResumenDTO> dibujos;
    private boolean puedeCancelar;
    private String rolVista;
    private Long idConversacion;
    private String estadoConversacion;
    private long noLeidos;

    private boolean publicacionAutorizada;
    private LocalDateTime publicacionAutorizadaEn;
    private boolean publicacionOcultadaCliente;
    private LocalDateTime publicacionOcultadaEn;
    private boolean galeriaClientePublica;
    private boolean publicacionVisible;
    private boolean puedeAutorizarPublicacion;
    private boolean puedeOcultarPublicacion;

    public void aplicarArancelMp(BigDecimal porcentajeTotal) {
        if (porcentajeTotal == null || porcentajeTotal.signum() < 0) {
            return;
        }

        BigDecimal base = precioFinalCliente != null
            ? precioFinalCliente
            : precioFinal;
        BigDecimal baseCobro = precioFinalClienteCobro != null
            ? precioFinalClienteCobro
            : precioFinalCobro;
        BigDecimal baseMostrado = precioFinalClienteMostrado != null
            ? precioFinalClienteMostrado
            : precioFinalMostrado;

        this.arancelMpPorcentaje = porcentajeTotal.setScale(4, RoundingMode.HALF_UP);
        this.arancelMpEstimado = PresupuestoDTO.parte(base, porcentajeTotal);
        this.arancelMpEstimadoCobro = PresupuestoDTO.parte(
            baseCobro, porcentajeTotal);
        this.arancelMpEstimadoMostrado = PresupuestoDTO.parte(
            baseMostrado, porcentajeTotal);
        this.montoNetoArtistaConArancel = PresupuestoDTO.restar(
            montoNetoArtista, arancelMpEstimado);
        this.montoNetoArtistaConArancelCobro = PresupuestoDTO.restar(
            montoNetoArtistaCobro, arancelMpEstimadoCobro);
        this.montoNetoArtistaConArancelMostrado = PresupuestoDTO.restar(
            montoNetoArtistaMostrado, arancelMpEstimadoMostrado);

        if (hitos != null) {
            for (HitoPedidoDTO hito : hitos) {
                hito.aplicarArancelMp(porcentajeTotal);
            }
        }
    }
}
