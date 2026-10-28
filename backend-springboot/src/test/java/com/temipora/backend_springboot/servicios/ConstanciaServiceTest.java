package com.temipora.backend_springboot.servicios;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.temipora.backend_springboot.modelo.HitoPedidoDTO;
import com.temipora.backend_springboot.modelo.PagoDTO;
import com.temipora.backend_springboot.modelo.PedidoDTO;
import com.temipora.backend_springboot.modelo.UsuarioResumenDTO;

class ConstanciaServiceTest {

    private final ConstanciaService constanciaService = new ConstanciaService();

    @Test
    void generaUnPdfConLosDatosDelPedido() {
        PedidoDTO pedido = PedidoDTO.builder()
            .idPedido(12L)
            .cliente(usuario(1L, "cliente"))
            .artista(usuario(2L, "artista"))
            .nombreServicio("Ilustración")
            .estado("EN_CURSO")
            .precioFinalMostrado(new BigDecimal("100.00"))
            .monedaMostrada("ARS")
            .hitos(List.of(HitoPedidoDTO.builder()
                .idHitoPedido(1L)
                .orden(0)
                .nombre("Seña")
                .descripcion("Bocetos de los 2 dibujos")
                .tipo("SENA")
                .montoMostrado(new BigDecimal("50.00"))
                .estadoPago("PAGADO")
                .estadoEntrega("APROBADO")
                .pagos(List.of(PagoDTO.builder()
                    .idPago(1L)
                    .estado("APROBADO")
                    .monto(new BigDecimal("50.00"))
                    .moneda("ARS")
                    .fechaCreacion(LocalDateTime.now())
                    .build()))
                .build()))
            .build();

        byte[] pdf = constanciaService.generar(pedido);

        assertThat(pdf).isNotEmpty();
        assertThat(new String(pdf, 0, 4, StandardCharsets.US_ASCII))
            .isEqualTo("%PDF");
    }

    @Test
    void generaUnPdfSinComisionParaLaVistaDelCliente() {
        PedidoDTO pedido = PedidoDTO.builder()
            .idPedido(13L)
            .cliente(usuario(1L, "cliente"))
            .artista(usuario(2L, "artista"))
            .nombreServicio("Retrato")
            .estado("FINALIZADO")
            .precioFinalMostrado(new BigDecimal("200.00"))
            .monedaMostrada("ARS")
            .hitos(List.of())
            .build();

        byte[] pdf = constanciaService.generar(pedido);

        assertThat(pdf).isNotEmpty();
        assertThat(new String(pdf, 0, 4, StandardCharsets.US_ASCII))
            .isEqualTo("%PDF");
    }

    private UsuarioResumenDTO usuario(Long id, String nombre) {
        return UsuarioResumenDTO.builder()
            .idUsuario(id)
            .nombreUsuario(nombre)
            .nombreDisplay(nombre)
            .build();
    }
}
