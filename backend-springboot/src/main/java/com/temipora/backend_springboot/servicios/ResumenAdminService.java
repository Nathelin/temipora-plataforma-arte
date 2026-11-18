package com.temipora.backend_springboot.servicios;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

@Service
public class ResumenAdminService {

    private final PagoRepository pagoRepository;
    private final CompraGrupoRepository compraGrupoRepository;
    private final PedidoRepository pedidoRepository;
    private final HitoPedidoRepository hitoPedidoRepository;
    private final UsuarioRepository usuarioRepository;

    public ResumenAdminService(
            PagoRepository pagoRepository,
            CompraGrupoRepository compraGrupoRepository,
            PedidoRepository pedidoRepository,
            HitoPedidoRepository hitoPedidoRepository,
            UsuarioRepository usuarioRepository) {
        this.pagoRepository = pagoRepository;
        this.compraGrupoRepository = compraGrupoRepository;
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
            actual.operaciones,
            anterior.operaciones,
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
        LocalDateTime fin = hasta.plusDays(1).atStartOfDay();

        BigDecimal volumenServicios = ceroSiNulo(
            pagoRepository.sumarMontoCobro(
                EstadoPago.APROBADO, inicio, fin));
        BigDecimal comisionServicios = ceroSiNulo(
            pagoRepository.sumarComisionCobro(
                EstadoPago.APROBADO, inicio, fin));
        long pedidos = pagoRepository.volumenPorPedido(
            EstadoPago.APROBADO, inicio, fin).size();

        BigDecimal volumenProductos = ceroSiNulo(
            compraGrupoRepository.sumarSubtotal(
                EstadoCompraGrupo.PAGADO, inicio, fin));
        BigDecimal comisionProductos = ceroSiNulo(
            compraGrupoRepository.sumarComision(
                EstadoCompraGrupo.PAGADO, inicio, fin));
        long grupos = compraGrupoRepository.contarPagados(
            EstadoCompraGrupo.PAGADO, inicio, fin);

        BigDecimal volumen = volumenServicios.add(volumenProductos);
        BigDecimal comision = comisionServicios.add(comisionProductos);
        long operaciones = pedidos + grupos;
        BigDecimal ticket = operaciones > 0
            ? volumen.divide(
                BigDecimal.valueOf(operaciones), 2, RoundingMode.HALF_UP)
            : BigDecimal.ZERO.setScale(2);
        return new DatosPeriodo(volumen, comision, operaciones, ticket);
    }

    private BigDecimal ceroSiNulo(BigDecimal valor) {
        return valor != null ? valor : BigDecimal.ZERO.setScale(2);
    }

    private record DatosPeriodo(
            BigDecimal volumen,
            BigDecimal comision,
            long operaciones,
            BigDecimal ticket) {
    }
}
