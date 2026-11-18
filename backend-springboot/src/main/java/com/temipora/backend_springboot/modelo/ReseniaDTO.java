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
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ReseniaDTO {

    private Long idResenia;
    private Long idPedido;
    private String nombreServicio;
    private Long idProducto;
    private String nombreProducto;
    private String nombreVariante;
    private boolean compraVerificada;
    private String variantesCompradas;
    private int cantidadVariantesCompradas;
    private AmbitoResenia direccion;
    private UsuarioResumenDTO autor;
    private EstadoResenia estado;
    private boolean editada;
    private int ediciones;
    private LocalDateTime fechaCreacion;
    private LocalDateTime fechaActualizacion;
    private int numeroVersion;
    private LocalDateTime fechaVersion;
    private String descripcion;
    private int calificacionGeneral;
    private boolean mostrarNombre;
    private String motivoModeracion;
    private List<ReseniaValorDTO> valores;
    private List<ReseniaOpcionDTO> opciones;
    private List<ReseniaVersionDTO> historial;

    public static ReseniaDTO desde(Resenia resenia, boolean incluirHistorial) {
        ReseniaVersion actual = resenia.versionActual();
        Pedido pedido = resenia.getPedido();
        Producto producto = resenia.getProducto();
        CompraItem compra = resenia.getCompraItem();

        ReseniaDTOBuilder builder = ReseniaDTO.builder()
            .idResenia(resenia.getIdResenia())
            .idPedido(pedido != null ? pedido.getIdPedido() : null)
            .nombreServicio(pedido != null && pedido.getServicio() != null
                ? pedido.getServicio().getNombre()
                : null)
            .idProducto(producto != null ? producto.getIdProducto() : null)
            .nombreProducto(producto != null ? producto.getTitulo() : null)
            .nombreVariante(compra != null ? compra.getNombreVariante() : null)
            .compraVerificada(producto != null)
            .variantesCompradas(resenia.getVariantesCompradas())
            .cantidadVariantesCompradas(resenia.getCantidadVariantesCompradas())
            .direccion(resenia.getAmbito())
            .autor(UsuarioResumenDTO.desdeUsuario(resenia.getAutor()))
            .estado(resenia.getEstado())
            .mostrarNombre(resenia.isMostrarNombre())
            .motivoModeracion(resenia.getMotivoModeracion())
            .editada(resenia.estaEditada())
            .ediciones(Math.max(0, resenia.getVersiones().size() - 1))
            .fechaCreacion(resenia.getFechaCreacion())
            .fechaActualizacion(resenia.getFechaActualizacion());

        if (actual != null) {
            builder.numeroVersion(actual.getNumero())
                .fechaVersion(actual.getFechaVersion())
                .descripcion(actual.getDescripcion())
                .calificacionGeneral(actual.getCalificacionGeneral())
                .valores(actual.getValores().stream()
                    .map(ReseniaValorDTO::desdeValor)
                    .toList())
                .opciones(actual.getOpciones().stream()
                    .map(ReseniaOpcionDTO::desdeOpcion)
                    .toList());
        }

        if (incluirHistorial) {
            builder.historial(resenia.getVersiones().stream()
                .map(version -> ReseniaVersionDTO.desdeVersion(
                    version, actual != null && version.getNumero() == actual.getNumero()))
                .toList());
        }

        return builder.build();
    }
}
