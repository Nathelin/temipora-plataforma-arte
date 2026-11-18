package com.temipora.backend_springboot.servicios;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.temipora.backend_springboot.modelo.CompraGrupo;
import com.temipora.backend_springboot.modelo.CompraItem;
import com.temipora.backend_springboot.modelo.EstadoCompraGrupo;
import com.temipora.backend_springboot.modelo.EstadoIntentoCobro;
import com.temipora.backend_springboot.modelo.IntentoCobro;
import com.temipora.backend_springboot.modelo.IntentoCobroGrupo;
import com.temipora.backend_springboot.modelo.ItemVentaProductoDTO;
import com.temipora.backend_springboot.modelo.Moneda;
import com.temipora.backend_springboot.modelo.PaginaDTO;
import com.temipora.backend_springboot.modelo.VentaProductoDTO;
import com.temipora.backend_springboot.repositorios.CompraGrupoRepository;
import com.temipora.backend_springboot.repositorios.CompraItemRepository;
import com.temipora.backend_springboot.repositorios.IntentoCobroGrupoRepository;

import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Subquery;

@Service
public class VentaProductoService {

    private static final String ARS = "ARS";
    private static final int TAMANIO_MAXIMO = 50;

    private final CompraGrupoRepository compraGrupoRepository;
    private final CompraItemRepository compraItemRepository;
    private final IntentoCobroGrupoRepository intentoCobroGrupoRepository;
    private final MonedaVistaService monedaVistaService;
    private final ImagenVentaProductoService imagenVentaProductoService;

    public VentaProductoService(
            CompraGrupoRepository compraGrupoRepository,
            CompraItemRepository compraItemRepository,
            IntentoCobroGrupoRepository intentoCobroGrupoRepository,
            MonedaVistaService monedaVistaService,
            ImagenVentaProductoService imagenVentaProductoService) {
        this.compraGrupoRepository = compraGrupoRepository;
        this.compraItemRepository = compraItemRepository;
        this.intentoCobroGrupoRepository = intentoCobroGrupoRepository;
        this.monedaVistaService = monedaVistaService;
        this.imagenVentaProductoService = imagenVentaProductoService;
    }

    @Transactional(readOnly = true)
    public PaginaDTO<VentaProductoDTO> listar(
            Long idArtista,
            String mes,
            Long idProducto,
            int pagina,
            int tamanio) {
        RangoMes rango = rangoDe(mes);
        Page<CompraGrupo> page = compraGrupoRepository.findAll(
            filtro(idArtista, rango, idProducto),
            pageRequest(pagina, tamanio));

        Moneda mostrada = monedaVistaService.preferidaDe(idArtista);
        List<VentaProductoDTO> contenido = page.isEmpty()
            ? List.of()
            : dtoDePagina(page.getContent(), mostrada);

        return PaginaDTO.<VentaProductoDTO>builder()
            .contenido(contenido)
            .pagina(page.getNumber())
            .tamanio(page.getSize())
            .totalElementos(page.getTotalElements())
            .totalPaginas(page.getTotalPages())
            .hayMas(page.hasNext())
            .build();
    }

    private Specification<CompraGrupo> filtro(
            Long idArtista, RangoMes rango, Long idProducto) {
        return (root, query, cb) -> {
            List<Predicate> predicados = new ArrayList<>();
            predicados.add(cb.equal(
                root.get("artista").get("idUsuario"), idArtista));
            predicados.add(cb.equal(
                root.get("estado"), EstadoCompraGrupo.PAGADO));
            if (rango != null) {
                predicados.add(cb.greaterThanOrEqualTo(
                    root.get("fechaPago"), rango.desde()));
                predicados.add(cb.lessThan(
                    root.get("fechaPago"), rango.hasta()));
            }
            if (idProducto != null) {
                Subquery<Long> sub = query.subquery(Long.class);
                var item = sub.from(CompraItem.class);
                sub.select(item.get("idCompraItem"));
                sub.where(cb.and(
                    cb.equal(item.get("grupo").get("idCompraGrupo"),
                        root.get("idCompraGrupo")),
                    cb.equal(
                        item.get("producto").get("idProducto"), idProducto)));
                predicados.add(cb.exists(sub));
            }
            return cb.and(predicados.toArray(new Predicate[0]));
        };
    }

    private List<VentaProductoDTO> dtoDePagina(
            List<CompraGrupo> grupos, Moneda mostrada) {
        List<Long> ids = grupos.stream()
            .map(CompraGrupo::getIdCompraGrupo)
            .toList();

        // Inicializa compra y cliente en un solo query (evita N+1).
        compraGrupoRepository.cargarDetalle(ids);

        Map<Long, List<CompraItem>> itemsPorGrupo = compraItemRepository
            .buscarPorGrupos(ids).stream()
            .collect(Collectors.groupingBy(
                item -> item.getGrupo().getIdCompraGrupo()));

        Map<Long, IntentoCobroGrupo> intentoPorGrupo = new HashMap<>();
        for (IntentoCobroGrupo asignacion : intentoCobroGrupoRepository
                .buscarPorGrupos(ids, EstadoIntentoCobro.APROBADO)) {
            Long idGrupo = asignacion.getGrupo().getIdCompraGrupo();
            intentoPorGrupo.merge(
                idGrupo, asignacion, this::intentoMasReciente);
        }

        return grupos.stream()
            .map(grupo -> dto(
                grupo,
                itemsPorGrupo.getOrDefault(
                    grupo.getIdCompraGrupo(), List.of()),
                intentoPorGrupo.get(grupo.getIdCompraGrupo()),
                mostrada))
            .toList();
    }

    private IntentoCobroGrupo intentoMasReciente(
            IntentoCobroGrupo actual, IntentoCobroGrupo candidato) {
        LocalDateTime fechaActual = fechaAprobacion(actual);
        LocalDateTime fechaCandidato = fechaAprobacion(candidato);
        if (fechaActual == null) {
            return candidato;
        }
        if (fechaCandidato == null) {
            return actual;
        }
        return fechaCandidato.isAfter(fechaActual) ? candidato : actual;
    }

    private LocalDateTime fechaAprobacion(IntentoCobroGrupo asignacion) {
        IntentoCobro intento = asignacion.getIntentoCobro();
        return intento != null ? intento.getFechaAprobacion() : null;
    }

    private VentaProductoDTO dto(
            CompraGrupo grupo,
            List<CompraItem> items,
            IntentoCobroGrupo asignacion,
            Moneda mostrada) {
        LocalDateTime fecha = grupo.getFechaPago() != null
            ? grupo.getFechaPago()
            : grupo.getFechaCreacion();
        IntentoCobro intento = asignacion != null
            ? asignacion.getIntentoCobro()
            : null;
        BigDecimal neto = netoDe(grupo);

        List<ItemVentaProductoDTO> itemsDto = items.stream()
            .map(this::itemDto)
            .toList();
        int cantidadItems = items.stream()
            .mapToInt(CompraItem::getCantidad)
            .sum();

        return VentaProductoDTO.builder()
            .idCompraGrupo(grupo.getIdCompraGrupo())
            .idCompra(grupo.getCompra() != null
                ? grupo.getCompra().getIdCompra() : null)
            .fecha(fecha)
            .nombreUsuarioCliente(grupo.getCompra() != null
                    && grupo.getCompra().getCliente() != null
                ? grupo.getCompra().getCliente().getNombreUsuario()
                : null)
            .cantidadItems(cantidadItems)
            .items(itemsDto)
            .montoCobro(grupo.getSubtotal())
            .monedaCobro(grupo.getMoneda() != null
                ? grupo.getMoneda().getCodigo() : ARS)
            .comisionMontoCobro(grupo.getComision())
            .montoNetoCobro(neto)
            .netoMostrado(convertir(neto, mostrada, fecha))
            .monedaMostrada(mostrada != null ? mostrada.getCodigo() : null)
            .arancelMp(intento != null ? intento.getArancelMp() : null)
            .netoRecibidoMp(intento != null ? intento.getNetoRecibidoMp() : null)
            .estado(grupo.getEstado() != null ? grupo.getEstado().name() : null)
            .proveedor(intento != null ? intento.getProveedor() : null)
            .build();
    }

    private ItemVentaProductoDTO itemDto(CompraItem item) {
        BigDecimal total = item.getPrecioCobro() != null
            ? item.getPrecioCobro()
                .multiply(BigDecimal.valueOf(item.getCantidad()))
                .setScale(2, RoundingMode.HALF_UP)
            : null;
        return ItemVentaProductoDTO.builder()
            .idCompraItem(item.getIdCompraItem())
            .idProducto(item.getProducto() != null
                ? item.getProducto().getIdProducto() : null)
            .idVariante(item.getVariante() != null
                ? item.getVariante().getIdVariante() : null)
            .tituloProducto(item.getTituloProducto())
            .nombreVariante(item.getNombreVariante())
            .tipoEntrega(item.getTipoEntrega())
            .cantidad(item.getCantidad())
            .precioCobro(item.getPrecioCobro())
            .precioTotalCobro(total)
            .imagenUrl(imagenVentaProductoService.url(item))
            .build();
    }

    private BigDecimal netoDe(CompraGrupo grupo) {
        if (grupo.getNetoArtista() != null) {
            return grupo.getNetoArtista();
        }
        if (grupo.getSubtotal() == null) {
            return null;
        }
        BigDecimal comision = grupo.getComision() != null
            ? grupo.getComision() : BigDecimal.ZERO;
        return grupo.getSubtotal().subtract(comision);
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
            Sort.by(Sort.Direction.DESC, "fechaPago")
                .and(Sort.by(Sort.Direction.DESC, "idCompraGrupo")));
    }

    record RangoMes(LocalDateTime desde, LocalDateTime hasta) {
    }
}
