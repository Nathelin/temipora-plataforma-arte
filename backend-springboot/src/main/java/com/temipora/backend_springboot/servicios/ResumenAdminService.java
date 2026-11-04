package com.temipora.backend_springboot.servicios;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.temipora.backend_springboot.modelo.ERol;
import com.temipora.backend_springboot.modelo.EstadoEntregaHito;
import com.temipora.backend_springboot.modelo.EstadoPago;
import com.temipora.backend_springboot.modelo.EstadoPedido;
import com.temipora.backend_springboot.modelo.ResumenComunidadDTO;
import com.temipora.backend_springboot.modelo.ResumenNegocioDTO;
import com.temipora.backend_springboot.modelo.ResumenOperacionDTO;
import com.temipora.backend_springboot.repositorios.HitoPedidoRepository;
import com.temipora.backend_springboot.repositorios.PagoRepository;
import com.temipora.backend_springboot.repositorios.PedidoRepository;
import com.temipora.backend_springboot.repositorios.UsuarioRepository;

@Service
public class ResumenAdminService {

    private final PagoRepository pagoRepository;
    private final PedidoRepository pedidoRepository;
    private final HitoPedidoRepository hitoPedidoRepository;
    private final UsuarioRepository usuarioRepository;

    public ResumenAdminService(
            PagoRepository pagoRepository,
            PedidoRepository pedidoRepository,
            HitoPedidoRepository hitoPedidoRepository,
            UsuarioRepository usuarioRepository) {
        this.pagoRepository = pagoRepository;
        this.pedidoRepository = pedidoRepository;
        this.hitoPedidoRepository = hitoPedidoRepository;
        this.usuarioRepository = usuarioRepository;
    }

    @Transactional(readOnly = true)
    public ResumenNegocioDTO negocio() {
        return negocio(LocalDate.now());
    }

    ResumenNegocioDTO negocio(LocalDate hoy) {
        LocalDate inicioActual = hoy.withDayOfMonth(1);
        LocalDate inicioAnterior = inicioActual.minusMonths(1);
        DatosPeriodo actual = datos(inicioActual, hoy);
        DatosPeriodo anterior = datos(
            inicioAnterior, inicioActual.minusDays(1));

        return new ResumenNegocioDTO(
            inicioActual.toString().substring(0, 7),
            MonedaVistaService.CODIGO_COBRO,
            actual.volumen,
            anterior.volumen,
            actual.comision,
            anterior.comision,
            actual.pedidos,
            anterior.pedidos,
            actual.ticket,
            anterior.ticket);
    }

    @Transactional(readOnly = true)
    public ResumenOperacionDTO operacion() {
        return operacion(LocalDate.now());
    }

    ResumenOperacionDTO operacion(LocalDate hoy) {
        return new ResumenOperacionDTO(
            pedidoRepository.contarEntregasAtrasadas(
                EstadoPedido.EN_CURSO, hoy),
            hitoPedidoRepository.contarAprobacionesPendientes(
                EstadoEntregaHito.ENTREGADO));
    }

    @Transactional(readOnly = true)
    public ResumenComunidadDTO comunidad() {
        return comunidad(LocalDateTime.now());
    }

    ResumenComunidadDTO comunidad(LocalDateTime ahora) {
        return new ResumenComunidadDTO(
            usuarioRepository.count(),
            usuarioRepository.countByRolNombre(ERol.ARTISTA),
            usuarioRepository.countByRolNombre(ERol.CLIENTE),
            usuarioRepository.countByRolNombre(ERol.ADMIN),
            usuarioRepository.countByFechaCreacionAfter(
                ahora.minusDays(7)));
    }

    private DatosPeriodo datos(LocalDate desde, LocalDate hasta) {
        LocalDateTime inicio = desde.atStartOfDay();
        LocalDateTime fin = hasta.atTime(23, 59, 59);
        BigDecimal volumen = ceroSiNulo(pagoRepository.sumarMontoCobro(
            EstadoPago.APROBADO, inicio, fin));
        BigDecimal comision = ceroSiNulo(pagoRepository.sumarComisionCobro(
            EstadoPago.APROBADO, inicio, fin));
        List<Object[]> porPedido = pagoRepository.volumenPorPedido(
            EstadoPago.APROBADO, inicio, fin);
        long pedidos = porPedido.size();
        BigDecimal ticket = pedidos > 0
            ? volumen.divide(
                BigDecimal.valueOf(pedidos), 2, RoundingMode.HALF_UP)
            : BigDecimal.ZERO.setScale(2);
        return new DatosPeriodo(volumen, comision, pedidos, ticket);
    }

    private BigDecimal ceroSiNulo(BigDecimal valor) {
        return valor != null ? valor : BigDecimal.ZERO.setScale(2);
    }

    private record DatosPeriodo(
            BigDecimal volumen,
            BigDecimal comision,
            long pedidos,
            BigDecimal ticket) {
    }
}
