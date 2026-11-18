package com.temipora.backend_springboot.servicios;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.temipora.backend_springboot.modelo.ERol;
import com.temipora.backend_springboot.modelo.EstadoCompraGrupo;
import com.temipora.backend_springboot.modelo.EstadoEntregaHito;
import com.temipora.backend_springboot.modelo.EstadoPago;
import com.temipora.backend_springboot.modelo.EstadoPedido;
import com.temipora.backend_springboot.modelo.ResumenComunidadDTO;
import com.temipora.backend_springboot.modelo.ResumenNegocioDTO;
import com.temipora.backend_springboot.modelo.ResumenOperacionDTO;
import com.temipora.backend_springboot.repositorios.CompraGrupoRepository;
import com.temipora.backend_springboot.repositorios.HitoPedidoRepository;
import com.temipora.backend_springboot.repositorios.PagoRepository;
import com.temipora.backend_springboot.repositorios.PedidoRepository;
import com.temipora.backend_springboot.repositorios.UsuarioRepository;

@ExtendWith(MockitoExtension.class)
class ResumenAdminServiceTest {

    private static final LocalDate HOY = LocalDate.of(2026, 11, 15);

    @Mock
    private PagoRepository pagoRepository;

    @Mock
    private CompraGrupoRepository compraGrupoRepository;

    @Mock
    private PedidoRepository pedidoRepository;

    @Mock
    private HitoPedidoRepository hitoPedidoRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @InjectMocks
    private ResumenAdminService resumenService;

    @Test
    void negocio_calculaMesActualYMesAnterior() {
        LocalDateTime inicioActual = LocalDateTime.of(2026, 11, 1, 0, 0);
        LocalDateTime finActual = LocalDateTime.of(2026, 11, 16, 0, 0);
        LocalDateTime inicioAnterior = LocalDateTime.of(2026, 10, 1, 0, 0);
        LocalDateTime finAnterior = LocalDateTime.of(2026, 11, 1, 0, 0);

        when(pagoRepository.sumarMontoCobro(
            EstadoPago.APROBADO, inicioActual, finActual))
            .thenReturn(new BigDecimal("1500.00"));
        when(pagoRepository.sumarMontoCobro(
            EstadoPago.APROBADO, inicioAnterior, finAnterior))
            .thenReturn(new BigDecimal("1000.00"));
        when(pagoRepository.sumarComisionCobro(
            EstadoPago.APROBADO, inicioActual, finActual))
            .thenReturn(new BigDecimal("150.00"));
        when(pagoRepository.sumarComisionCobro(
            EstadoPago.APROBADO, inicioAnterior, finAnterior))
            .thenReturn(new BigDecimal("100.00"));
        when(pagoRepository.volumenPorPedido(
            EstadoPago.APROBADO, inicioActual, finActual))
            .thenReturn(List.<Object[]>of(
                new Object[] { 1L, new BigDecimal("500.00") },
                new Object[] { 2L, new BigDecimal("1000.00") }));
        when(pagoRepository.volumenPorPedido(
            EstadoPago.APROBADO, inicioAnterior, finAnterior))
            .thenReturn(List.<Object[]>of(
                new Object[] { 3L, new BigDecimal("1000.00") }));
        when(compraGrupoRepository.sumarSubtotal(
            EstadoCompraGrupo.PAGADO, inicioActual, finActual))
            .thenReturn(new BigDecimal("500.00"));
        when(compraGrupoRepository.sumarComision(
            EstadoCompraGrupo.PAGADO, inicioActual, finActual))
            .thenReturn(new BigDecimal("50.00"));
        when(compraGrupoRepository.contarPagados(
            EstadoCompraGrupo.PAGADO, inicioActual, finActual))
            .thenReturn(2L);

        ResumenNegocioDTO resumen = resumenService.negocio(HOY);

        assertThat(resumen.periodo()).isEqualTo("2026-11");
        assertThat(resumen.moneda()).isEqualTo("ARS");
        assertThat(resumen.volumen()).isEqualByComparingTo("2000.00");
        assertThat(resumen.volumenAnterior()).isEqualByComparingTo("1000.00");
        assertThat(resumen.comision()).isEqualByComparingTo("200.00");
        assertThat(resumen.comisionAnterior()).isEqualByComparingTo("100.00");
        assertThat(resumen.operaciones()).isEqualTo(4);
        assertThat(resumen.operacionesAnterior()).isEqualTo(1);
        assertThat(resumen.ticketPromedio()).isEqualByComparingTo("500.00");
        assertThat(resumen.ticketPromedioAnterior()).isEqualByComparingTo("1000.00");
    }

    @Test
    void negocio_sinPagos_devuelveCeros() {
        when(pagoRepository.sumarMontoCobro(
            eq(EstadoPago.APROBADO), org.mockito.ArgumentMatchers.any(),
            org.mockito.ArgumentMatchers.any())).thenReturn(null);
        when(pagoRepository.sumarComisionCobro(
            eq(EstadoPago.APROBADO), org.mockito.ArgumentMatchers.any(),
            org.mockito.ArgumentMatchers.any())).thenReturn(null);
        when(pagoRepository.volumenPorPedido(
            eq(EstadoPago.APROBADO), org.mockito.ArgumentMatchers.any(),
            org.mockito.ArgumentMatchers.any())).thenReturn(List.of());

        ResumenNegocioDTO resumen = resumenService.negocio(HOY);

        assertThat(resumen.volumen()).isEqualByComparingTo("0.00");
        assertThat(resumen.comision()).isEqualByComparingTo("0.00");
        assertThat(resumen.operaciones()).isZero();
        assertThat(resumen.ticketPromedio()).isEqualByComparingTo("0.00");
    }

    @Test
    void operacion_cuentaEntregasAtrasadasYAprobacionesPendientes() {
        when(pedidoRepository.contarEntregasAtrasadas(
            EstadoPedido.EN_CURSO, HOY)).thenReturn(3L);
        when(hitoPedidoRepository.contarAprobacionesPendientes(
            EstadoEntregaHito.ENTREGADO)).thenReturn(4L);

        ResumenOperacionDTO resumen = resumenService.operacion(HOY);

        assertThat(resumen.entregasAtrasadas()).isEqualTo(3);
        assertThat(resumen.aprobacionesPendientes()).isEqualTo(4);
    }

    @Test
    void comunidad_devuelveTotalDesgloseYNuevosDeLaSemana() {
        LocalDateTime ahora = LocalDateTime.of(2026, 11, 15, 12, 0);
        when(usuarioRepository.count()).thenReturn(10L);
        when(usuarioRepository.countByRolNombre(ERol.ARTISTA)).thenReturn(3L);
        when(usuarioRepository.countByRolNombre(ERol.CLIENTE)).thenReturn(6L);
        when(usuarioRepository.countByRolNombre(ERol.ADMIN)).thenReturn(1L);
        when(usuarioRepository.countByFechaCreacionAfter(
            ahora.minusDays(7))).thenReturn(2L);

        ResumenComunidadDTO resumen = resumenService.comunidad(ahora);

        assertThat(resumen.total()).isEqualTo(10);
        assertThat(resumen.artistas()).isEqualTo(3);
        assertThat(resumen.clientes()).isEqualTo(6);
        assertThat(resumen.administradores()).isEqualTo(1);
        assertThat(resumen.nuevosSemana()).isEqualTo(2);
    }
}
