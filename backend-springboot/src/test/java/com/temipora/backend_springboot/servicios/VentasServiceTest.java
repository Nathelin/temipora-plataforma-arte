package com.temipora.backend_springboot.servicios;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import com.temipora.backend_springboot.modelo.EstadoPago;
import com.temipora.backend_springboot.modelo.HitoPedido;
import com.temipora.backend_springboot.modelo.Moneda;
import com.temipora.backend_springboot.modelo.PaginaDTO;
import com.temipora.backend_springboot.modelo.Pago;
import com.temipora.backend_springboot.modelo.Pedido;
import com.temipora.backend_springboot.modelo.Servicio;
import com.temipora.backend_springboot.modelo.TipoHito;
import com.temipora.backend_springboot.modelo.Usuario;
import com.temipora.backend_springboot.modelo.VentaDTO;
import com.temipora.backend_springboot.repositorios.PagoRepository;

@ExtendWith(MockitoExtension.class)
class VentasServiceTest {

    private static final Long ARTISTA = 2L;

    @Mock
    private PagoRepository pagoRepository;
    @Mock
    private MonedaVistaService monedaVistaService;

    @InjectMocks
    private VentasService ventasService;

    @Test
    void listaSinFiltrosYMapeaLosDatosDelPago() {
        Pago pago = pago(30L, "cliente1", "Ilustración");
        when(pagoRepository.findAll(
            ArgumentMatchers.<Specification<Pago>>any(),
            any(Pageable.class)))
            .thenReturn(new PageImpl<>(List.of(pago),
                PageRequest.of(0, 10), 1));

        PaginaDTO<VentaDTO> pagina = ventasService.listar(
            ARTISTA, "TODOS", null, null, 0, 10);

        assertThat(pagina.getContenido()).hasSize(1);
        assertThat(pagina.getTotalElementos()).isEqualTo(1);
        VentaDTO venta = pagina.getContenido().get(0);
        assertThat(venta.getIdPago()).isEqualTo(30L);
        assertThat(venta.getNombreUsuarioCliente()).isEqualTo("cliente1");
        assertThat(venta.getNombreServicio()).isEqualTo("Ilustración");
        assertThat(venta.getNombreHito()).isEqualTo("Seña");
        assertThat(venta.getTipoHito()).isEqualTo(TipoHito.SENA.name());
        assertThat(venta.getEstado()).isEqualTo(EstadoPago.APROBADO.name());
        assertThat(venta.getNetoMostrado())
            .isEqualByComparingTo("100.00");
    }

    @Test
    void calculaElRangoDelMes() {
        VentasService.RangoMes rango = ventasService.rangoDe("2026-11");

        assertThat(rango).isNotNull();
        assertThat(rango.desde())
            .isEqualTo(LocalDateTime.of(2026, 11, 1, 0, 0));
        assertThat(rango.hasta())
            .isEqualTo(LocalDateTime.of(2026, 12, 1, 0, 0));
    }

    @Test
    void mesInvalidoOVacioNoGeneraRango() {
        assertThat(ventasService.rangoDe("no-es-un-mes")).isNull();
        assertThat(ventasService.rangoDe(null)).isNull();
        assertThat(ventasService.rangoDe("  ")).isNull();
    }

    @Test
    void convierteElNetoALaMonedaPreferida() {
        Pago pago = pago(30L, "cliente1", "Ilustración");
        Moneda usd = moneda("USD");
        when(pagoRepository.findAll(
            ArgumentMatchers.<Specification<Pago>>any(),
            any(Pageable.class)))
            .thenReturn(new PageImpl<>(List.of(pago),
                PageRequest.of(0, 10), 1));
        when(monedaVistaService.preferidaDe(ARTISTA)).thenReturn(usd);
        when(monedaVistaService.factorCobroAMostrada(eq(usd), any()))
            .thenReturn(new BigDecimal("1000"));

        VentaDTO venta = ventasService.listar(
            ARTISTA, null, null, null, 0, 10).getContenido().get(0);

        assertThat(venta.getMontoNetoCobro())
            .isEqualByComparingTo("100.00");
        assertThat(venta.getNetoMostrado())
            .isEqualByComparingTo("100000.00");
        assertThat(venta.getMonedaMostrada()).isEqualTo("USD");
    }

    @Test
    void estadoInvalidoFalla() {
        assertThatThrownBy(() -> ventasService.listar(
            ARTISTA, "INVALIDO", null, null, 0, 10))
            .isInstanceOf(IllegalArgumentException.class);
    }

    private Pago pago(Long id, String usuarioCliente, String servicio) {
        Servicio entidadServicio = new Servicio();
        entidadServicio.setNombre(servicio);

        Usuario cliente = new Usuario();
        cliente.setNombreUsuario(usuarioCliente);

        Pedido pedido = new Pedido();
        pedido.setIdPedido(10L);
        pedido.setCliente(cliente);
        pedido.setServicio(entidadServicio);

        HitoPedido hito = new HitoPedido();
        hito.setOrden(0);
        hito.setNombre("Seña");
        hito.setTipo(TipoHito.SENA);
        hito.setPedido(pedido);

        Pago pago = new Pago();
        pago.setIdPago(id);
        pago.setHitoPedido(hito);
        pago.setMonto(new BigDecimal("100.00"));
        pago.setMoneda(moneda("ARS"));
        pago.setMontoCobro(new BigDecimal("100.00"));
        pago.setMonedaCobro(moneda("ARS"));
        pago.setComisionMontoCobro(new BigDecimal("10.00"));
        pago.setMontoNetoCobro(new BigDecimal("100.00"));
        pago.setEstado(EstadoPago.APROBADO);
        pago.setProveedor("MERCADOPAGO");
        pago.setFechaCreacion(LocalDateTime.of(2026, 11, 2, 10, 0));
        return pago;
    }

    private Moneda moneda(String codigo) {
        Moneda moneda = new Moneda();
        moneda.setCodigo(codigo);
        return moneda;
    }
}
