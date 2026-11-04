package com.temipora.backend_springboot.servicios;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.NoSuchElementException;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.criteria.Predicate;

import com.temipora.backend_springboot.modelo.EstadoPago;
import com.temipora.backend_springboot.modelo.HitoPedido;
import com.temipora.backend_springboot.modelo.Moneda;
import com.temipora.backend_springboot.modelo.PaginaDTO;
import com.temipora.backend_springboot.modelo.Pago;
import com.temipora.backend_springboot.modelo.Pedido;
import com.temipora.backend_springboot.modelo.VentaDTO;
import com.temipora.backend_springboot.repositorios.PagoRepository;

@Service
public class VentasService {

    private static final int TAMANIO_MAXIMO = 50;

    private final PagoRepository pagoRepository;
    private final PagoService pagoService;
    private final MonedaVistaService monedaVistaService;

    public VentasService(
            PagoRepository pagoRepository,
            PagoService pagoService,
            MonedaVistaService monedaVistaService) {
        this.pagoRepository = pagoRepository;
        this.pagoService = pagoService;
        this.monedaVistaService = monedaVistaService;
    }

    @Transactional(readOnly = true)
    public PaginaDTO<VentaDTO> listar(
            Long idArtista,
            String estado,
            String mes,
            Long idServicio,
            int pagina,
            int tamanio) {
        RangoMes rango = rangoDe(mes);
        Page<Pago> page = pagoRepository.findAll(
            filtro(idArtista, estadoDe(estado), rango, idServicio),
            pageRequest(pagina, tamanio));

        Moneda mostrada = monedaVistaService.preferidaDe(idArtista);

        return PaginaDTO.<VentaDTO>builder()
            .contenido(page.getContent().stream()
                .map(pago -> dto(pago, mostrada))
                .toList())
            .pagina(page.getNumber())
            .tamanio(page.getSize())
            .totalElementos(page.getTotalElements())
            .totalPaginas(page.getTotalPages())
            .hayMas(page.hasNext())
            .build();
    }

    @Transactional
    public VentaDTO sincronizar(Long idArtista, Long idPago) {
        Pago pago = pagoRepository.findByIdPagoConPedido(idPago)
            .orElseThrow(() -> new NoSuchElementException(
                "Pago no encontrado: " + idPago));
        Pedido pedido = pago.getHitoPedido().getPedido();
        if (pedido.getArtista() == null
                || !idArtista.equals(pedido.getArtista().getIdUsuario())) {
            throw new AccesoDenegadoException(
                "No podés sincronizar esta venta.");
        }
        pagoService.sincronizar(idPago);
        Pago actualizado = pagoRepository.findByIdPagoConPedido(idPago)
            .orElseThrow(() -> new NoSuchElementException(
                "Pago no encontrado: " + idPago));
        return dto(actualizado, monedaVistaService.preferidaDe(idArtista));
    }

    private Specification<Pago> filtro(
            Long idArtista,
            EstadoPago estado,
            RangoMes rango,
            Long idServicio) {
        return (root, query, cb) -> {
            List<Predicate> predicados = new ArrayList<>();
            predicados.add(cb.equal(
                root.get("hitoPedido").get("pedido")
                    .get("artista").get("idUsuario"),
                idArtista));
            if (estado != null) {
                predicados.add(cb.equal(root.get("estado"), estado));
            }
            if (rango != null) {
                predicados.add(cb.greaterThanOrEqualTo(
                    root.<LocalDateTime>get("fechaCreacion"), rango.desde()));
                predicados.add(cb.lessThan(
                    root.<LocalDateTime>get("fechaCreacion"), rango.hasta()));
            }
            if (idServicio != null) {
                predicados.add(cb.equal(
                    root.get("hitoPedido").get("pedido")
                        .get("servicio").get("idServicio"),
                    idServicio));
            }
            return cb.and(predicados.toArray(new Predicate[0]));
        };
    }

    private VentaDTO dto(Pago pago, Moneda mostrada) {
        HitoPedido hito = pago.getHitoPedido();
        Pedido pedido = hito.getPedido();
        BigDecimal netoMostrado = convertir(
            pago.getMontoNetoCobro(), mostrada, pago.getFechaCreacion());

        return VentaDTO.builder()
            .idPago(pago.getIdPago())
            .fecha(pago.getFechaCreacion())
            .fechaAcreditacion(pago.getFechaAcreditacion())
            .idPedido(pedido.getIdPedido())
            .nombreServicio(pedido.getServicio() != null
                ? pedido.getServicio().getNombre()
                : null)
            .nombreUsuarioCliente(pedido.getCliente() != null
                ? pedido.getCliente().getNombreUsuario()
                : null)
            .nombreHito(hito.getNombre())
            .ordenHito(hito.getOrden())
            .tipoHito(hito.getTipo() != null ? hito.getTipo().name() : null)
            .estado(pago.getEstado() != null ? pago.getEstado().name() : null)
            .monto(pago.getMonto())
            .moneda(pago.getMoneda() != null
                ? pago.getMoneda().getCodigo()
                : null)
            .montoCobro(pago.getMontoCobro())
            .monedaCobro(pago.getMonedaCobro() != null
                ? pago.getMonedaCobro().getCodigo()
                : null)
            .comisionMontoCobro(pago.getComisionMontoCobro())
            .montoNetoCobro(pago.getMontoNetoCobro())
            .netoMostrado(netoMostrado)
            .monedaMostrada(mostrada != null ? mostrada.getCodigo() : null)
            .arancelMp(pago.getArancelMp())
            .netoRecibidoMp(pago.getNetoRecibidoMp())
            .fechaAprobacion(pago.getFechaAprobacion())
            .fechaLiberacion(pago.getFechaLiberacion())
            .medioPago(pago.getMedioPago())
            .tipoMedioPago(pago.getTipoMedioPago())
            .cuotas(pago.getCuotas())
            .ultimosCuatro(pago.getUltimosCuatro())
            .puedeSincronizar(pago.getIdPagoExterno() != null)
            .proveedor(pago.getProveedor())
            .build();
    }

    private BigDecimal convertir(
            BigDecimal monto, Moneda mostrada, LocalDateTime fecha) {
        if (monto == null || mostrada == null) {
            return monto;
        }
        BigDecimal factor = monedaVistaService
            .factorCobroAMostrada(mostrada, fecha);
        if (factor == null) {
            return monto;
        }
        return monto.multiply(factor).setScale(2, RoundingMode.HALF_UP);
    }

    private EstadoPago estadoDe(String estado) {
        if (estado == null || estado.isBlank()
                || "TODOS".equalsIgnoreCase(estado)) {
            return null;
        }
        try {
            return EstadoPago.valueOf(
                estado.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                "Estado de venta inválido: " + estado);
        }
    }

    RangoMes rangoDe(String mes) {
        if (mes == null || mes.isBlank()) {
            return null;
        }
        try {
            YearMonth periodo = YearMonth.parse(mes.trim());
            LocalDateTime desde = periodo.atDay(1).atStartOfDay();
            return new RangoMes(desde, desde.plusMonths(1));
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    private Pageable pageRequest(int pagina, int tamanio) {
        int paginaSegura = Math.max(pagina, 0);
        int tamanioSeguro = Math.min(Math.max(tamanio, 1), TAMANIO_MAXIMO);
        return PageRequest.of(paginaSegura, tamanioSeguro,
            Sort.by(Sort.Direction.DESC, "fechaCreacion"));
    }

    record RangoMes(LocalDateTime desde, LocalDateTime hasta) {
    }
}
