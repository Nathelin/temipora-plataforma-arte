package com.temipora.backend_springboot.servicios;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.temipora.backend_springboot.modelo.CampanaReactivacion;
import com.temipora.backend_springboot.modelo.DescuentoPedidoDTO;
import com.temipora.backend_springboot.modelo.EstadoOfertaReactivacion;
import com.temipora.backend_springboot.modelo.InformeDescuentosDTO;
import com.temipora.backend_springboot.modelo.OfertaReactivacion;
import com.temipora.backend_springboot.modelo.OfertasReactivacionResumenDTO;
import com.temipora.backend_springboot.modelo.Pedido;
import com.temipora.backend_springboot.modelo.Presupuesto;
import com.temipora.backend_springboot.modelo.ResumenDescuentosDTO;
import com.temipora.backend_springboot.repositorios.CampanaReactivacionRepository;
import com.temipora.backend_springboot.repositorios.OfertaReactivacionRepository;
import com.temipora.backend_springboot.repositorios.PedidoRepository;

/**
 * Informe de descuentos de reactivación. Cada fila es un pedido: el descuento
 * se toma del presupuesto congelado al emitirlo. Las ofertas se resumen aparte
 * para no repetir la misma promoción en varias filas.
 */
@Service
public class PromocionDescuentoService {

    public static final int FILAS_PDF = 2000;

    private static final int DIAS_POR_DEFECTO = 30;
    private static final int MAX_DIAS = 3660;
    private static final String MONEDA = "ARS";

    private final PedidoRepository pedidoRepository;
    private final OfertaReactivacionRepository ofertaRepository;
    private final CampanaReactivacionRepository campanaRepository;

    public PromocionDescuentoService(
            PedidoRepository pedidoRepository,
            OfertaReactivacionRepository ofertaRepository,
            CampanaReactivacionRepository campanaRepository) {
        this.pedidoRepository = pedidoRepository;
        this.ofertaRepository = ofertaRepository;
        this.campanaRepository = campanaRepository;
    }

    @Transactional(readOnly = true)
    public InformeDescuentosDTO generar(
            String desdeTexto,
            String hastaTexto,
            String busqueda,
            boolean comisionCero,
            int pagina,
            int tamanio) {
        LocalDate hasta = fechaDe(hastaTexto, LocalDate.now());
        LocalDate desde = fechaDe(desdeTexto,
            hasta.minusDays(DIAS_POR_DEFECTO - 1L));
        long dias = validarPeriodo(desde, hasta);

        String patron = busqueda == null
            ? "" : busqueda.trim().toLowerCase(Locale.ROOT);
        int paginaSegura = Math.max(0, pagina);
        int tamanioSeguro = Math.min(Math.max(1, tamanio), FILAS_PDF);
        Pageable pageable = PageRequest.of(paginaSegura, tamanioSeguro,
            Sort.by(Sort.Direction.DESC, "fechaCreacion")
                .and(Sort.by(Sort.Direction.DESC, "idPedido")));

        LocalDateTime inicio = desde.atStartOfDay();
        LocalDateTime fin = hasta.plusDays(1).atStartOfDay();

        Page<Pedido> page = pedidoRepository.buscarConDescuento(
            inicio, fin, patron, comisionCero, pageable);
        List<DescuentoPedidoDTO> pedidos = page.getContent().stream()
            .map(this::aDto)
            .toList();

        return new InformeDescuentosDTO(
            desde,
            hasta,
            dias,
            MONEDA,
            filtro(patron, comisionCero),
            LocalDateTime.now(),
            resumen(inicio, fin, patron, comisionCero),
            ofertas(inicio, fin),
            pedidos,
            page.getNumber(),
            page.getSize(),
            page.getTotalElements(),
            page.getTotalPages(),
            page.hasNext());
    }

    private ResumenDescuentosDTO resumen(
            LocalDateTime inicio,
            LocalDateTime fin,
            String patron,
            boolean comisionCero) {
        List<Object[]> filas = pedidoRepository.resumirConDescuento(
            inicio, fin, patron, comisionCero);
        Object[] fila = filas.isEmpty() ? null : filas.get(0);

        long pedidos = entero(fila, 0);
        BigDecimal descuento = decimal(fila, 1);
        long conComisionCero = entero(fila, 2);
        BigDecimal descuentoCero = decimal(fila, 3);
        BigDecimal volumenCliente = decimal(fila, 4);
        BigDecimal volumenSinDescuento = decimal(fila, 5);
        BigDecimal comisionEfectiva = decimal(fila, 6);
        BigDecimal comisionNominal = comisionEfectiva.add(descuento);

        long totalPeriodo = pedidoRepository.contarPedidosPeriodo(
            inicio, fin, patron);
        BigDecimal porcentaje = totalPeriodo == 0
            ? BigDecimal.ZERO.setScale(2)
            : BigDecimal.valueOf(pedidos * 100.0 / totalPeriodo)
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal ticket = pedidos == 0
            ? BigDecimal.ZERO.setScale(2)
            : volumenCliente.divide(
                BigDecimal.valueOf(pedidos), 2, RoundingMode.HALF_UP);

        return new ResumenDescuentosDTO(
            pedidos,
            conComisionCero,
            totalPeriodo,
            porcentaje,
            dinero(descuento),
            dinero(descuentoCero),
            dinero(volumenCliente),
            dinero(volumenSinDescuento),
            dinero(comisionEfectiva),
            dinero(comisionNominal),
            ticket);
    }

    private OfertasReactivacionResumenDTO ofertas(
            LocalDateTime inicio, LocalDateTime fin) {
        Map<EstadoOfertaReactivacion, Long> conteo =
            new EnumMap<>(EstadoOfertaReactivacion.class);
        for (Object[] fila : ofertaRepository
                .contarPorEstadoEntre(inicio, fin)) {
            if (fila == null || fila.length < 2) {
                continue;
            }
            EstadoOfertaReactivacion estado =
                (EstadoOfertaReactivacion) fila[0];
            conteo.put(estado, entero(fila, 1));
        }

        long campanas = campanaRepository.contarEntre(inicio, fin);
        long ofertas = conteo.values().stream()
            .mapToLong(Long::longValue)
            .sum();
        long aceptadas = ofertaRepository.contarAceptadasEntre(inicio, fin);
        long utilizadas = conteo.getOrDefault(
            EstadoOfertaReactivacion.UTILIZADA, 0L);
        BigDecimal tasa = ofertas == 0
            ? BigDecimal.ZERO.setScale(2)
            : BigDecimal.valueOf(utilizadas * 100.0 / ofertas)
                .setScale(2, RoundingMode.HALF_UP);

        return new OfertasReactivacionResumenDTO(
            campanas,
            ofertas,
            conteo.getOrDefault(EstadoOfertaReactivacion.OFRECIDA, 0L),
            aceptadas,
            utilizadas,
            conteo.getOrDefault(EstadoOfertaReactivacion.VENCIDA, 0L),
            conteo.getOrDefault(EstadoOfertaReactivacion.CANCELADA, 0L),
            tasa);
    }

    private DescuentoPedidoDTO aDto(Pedido pedido) {
        Presupuesto presupuesto = pedido.getPresupuesto();
        OfertaReactivacion oferta = presupuesto != null
            ? presupuesto.getOfertaReactivacion() : null;
        CampanaReactivacion campana = oferta != null
            ? oferta.getCampana() : null;

        return new DescuentoPedidoDTO(
            pedido.getIdPedido(),
            pedido.getFechaCreacion(),
            pedido.getEstado() != null ? pedido.getEstado().name() : null,
            pedido.getCliente().getIdUsuario(),
            pedido.getCliente().getNombreUsuario(),
            pedido.getCliente().getNombreDisplay(),
            pedido.getArtista().getIdUsuario(),
            pedido.getArtista().getNombreUsuario(),
            pedido.getServicio().getIdServicio(),
            pedido.getServicio().getNombre(),
            campana != null ? campana.getIdCampana() : null,
            campana != null ? campana.getFechaCreacion() : null,
            oferta != null ? oferta.getIdOferta() : null,
            oferta != null ? oferta.getPorcentajeDescuento() : null,
            oferta != null ? oferta.getMotivo() : null,
            oferta != null && oferta.getEstado() != null
                ? oferta.getEstado().name() : null,
            oferta != null ? oferta.getFechaReserva() : null,
            oferta != null ? oferta.getFechaUtilizada() : null,
            presupuesto.getDescuentoPorcentaje(),
            presupuesto.getDescuentoMontoCobro(),
            presupuesto.getPrecioFinalCobro(),
            presupuesto.getPrecioFinalClienteCobro(),
            presupuesto.getComisionPorcentaje(),
            presupuesto.getComisionMontoCobro(),
            comisionNominal(presupuesto),
            presupuesto.getMonedaCobro() != null
                ? presupuesto.getMonedaCobro().getCodigo() : MONEDA,
            presupuesto.getComisionPorcentaje() != null
                && presupuesto.getComisionPorcentaje().signum() == 0);
    }

    private static BigDecimal comisionNominal(Presupuesto presupuesto) {
        return nulo(presupuesto.getComisionMontoCobro())
            .add(nulo(presupuesto.getDescuentoMontoCobro()))
            .setScale(2, RoundingMode.HALF_UP);
    }

    private static String filtro(String patron, boolean comisionCero) {
        List<String> partes = new ArrayList<>();
        if (!patron.isBlank()) {
            partes.add("Búsqueda: \"" + patron + "\"");
        }
        if (comisionCero) {
            partes.add("Solo comisión 0");
        }
        return partes.isEmpty()
            ? "Todos los pedidos con descuento"
            : String.join(" · ", partes);
    }

    private static LocalDate fechaDe(String valor, LocalDate porDefecto) {
        if (valor == null || valor.isBlank()) {
            return porDefecto;
        }
        try {
            return LocalDate.parse(valor.trim());
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Fecha inválida: " + valor);
        }
    }

    private static long validarPeriodo(LocalDate desde, LocalDate hasta) {
        if (desde.isAfter(hasta)) {
            throw new IllegalArgumentException(
                "El período debe empezar antes de terminar.");
        }
        long dias = ChronoUnit.DAYS.between(desde, hasta) + 1;
        if (dias > MAX_DIAS) {
            throw new IllegalArgumentException(
                "El período no puede superar los " + MAX_DIAS + " días.");
        }
        return dias;
    }

    private static BigDecimal decimal(Object[] fila, int indice) {
        if (fila == null || indice >= fila.length || fila[indice] == null) {
            return BigDecimal.ZERO;
        }
        Object valor = fila[indice];
        if (valor instanceof BigDecimal decimal) {
            return decimal;
        }
        if (valor instanceof Number numero) {
            return new BigDecimal(numero.toString());
        }
        return new BigDecimal(valor.toString());
    }

    private static long entero(Object[] fila, int indice) {
        if (fila == null || indice >= fila.length || fila[indice] == null) {
            return 0L;
        }
        return fila[indice] instanceof Number numero
            ? numero.longValue() : 0L;
    }

    private static BigDecimal dinero(BigDecimal valor) {
        return valor == null
            ? BigDecimal.ZERO.setScale(2)
            : valor.setScale(2, RoundingMode.HALF_UP);
    }

    private static BigDecimal nulo(BigDecimal valor) {
        return valor == null ? BigDecimal.ZERO : valor;
    }
}
