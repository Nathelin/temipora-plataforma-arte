package com.temipora.backend_springboot.servicios;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.temipora.backend_springboot.modelo.InformeComparacionDTO;
import com.temipora.backend_springboot.modelo.InformeItemDTO;
import com.temipora.backend_springboot.modelo.InformePeriodoDTO;
import com.temipora.backend_springboot.modelo.InformeSerieDTO;
import com.temipora.backend_springboot.modelo.InformeTotalesDTO;
import com.temipora.backend_springboot.modelo.InformeVentasDTO;
import com.temipora.backend_springboot.repositorios.CompraGrupoRepository;
import com.temipora.backend_springboot.repositorios.CompraItemRepository;
import com.temipora.backend_springboot.repositorios.PagoRepository;

/**
 * Arma el informe de ventas de un artista para un período.
 *
 * <p>El neto del informe es el importe que el medio de pago liquidó realmente
 * ({@code netoRecibidoMp}). Cuando ese dato no existe, se usa el neto del
 * artista como estimación. El informe siempre se expresa en ARS, la moneda en
 * la que se guardan los importes de cobro.</p>
 */
@Service
public class InformeVentasService {

    private static final int DIAS_POR_DEFECTO = 30;
    private static final int MAX_DIAS = 3660;
    private static final int MAX_DIAS_DIARIO = 366;
    private static final String MONEDA_COBRO = "ARS";
    private static final String SIN_NOMBRE = "Sin nombre";
    private static final DateTimeFormatter MES =
        DateTimeFormatter.ofPattern("yyyy-MM");

    private final PagoRepository pagoRepository;
    private final CompraGrupoRepository compraGrupoRepository;
    private final CompraItemRepository compraItemRepository;

    public InformeVentasService(
            PagoRepository pagoRepository,
            CompraGrupoRepository compraGrupoRepository,
            CompraItemRepository compraItemRepository) {
        this.pagoRepository = pagoRepository;
        this.compraGrupoRepository = compraGrupoRepository;
        this.compraItemRepository = compraItemRepository;
    }

    @Transactional(readOnly = true)
    public InformeVentasDTO generar(
            Long idArtista,
            String desdeTexto,
            String hastaTexto,
            String tipoTexto,
            String agrupacionTexto) {
        LocalDate hasta = fechaDe(hastaTexto, LocalDate.now());
        LocalDate desde = fechaDe(desdeTexto,
            hasta.minusDays(DIAS_POR_DEFECTO - 1L));
        long dias = validarPeriodo(desde, hasta);
        Tipo tipo = Tipo.de(tipoTexto);
        Agrupacion agrupacion = Agrupacion.de(agrupacionTexto);

        if (agrupacion == Agrupacion.DIA && dias > MAX_DIAS_DIARIO) {
            throw new IllegalArgumentException(
                "El período máximo para la agrupación diaria es de "
                    + MAX_DIAS_DIARIO + " días.");
        }

        Calculo actual = calcular(idArtista, desde, hasta, tipo, true);
        LocalDate hastaAnterior = desde.minusDays(1);
        LocalDate desdeAnterior = hastaAnterior.minusDays(dias - 1);
        Calculo anterior = calcular(
            idArtista, desdeAnterior, hastaAnterior, tipo, false);

        return new InformeVentasDTO(
            new InformePeriodoDTO(desde, hasta, dias, agrupacion.name()),
            MONEDA_COBRO,
            LocalDateTime.now(),
            actual.totalesDto(),
            new InformeComparacionDTO(
                desdeAnterior,
                hastaAnterior,
                dinero(anterior.totales.bruto),
                dinero(anterior.totales.neto),
                variacion(actual.totales.neto, anterior.totales.neto)),
            actual.operacionesSinNetoReal.size(),
            serie(actual.serie, desde, hasta, agrupacion),
            items(actual));
    }

    private Calculo calcular(
            Long idArtista,
            LocalDate desde,
            LocalDate hasta,
            Tipo tipo,
            boolean conDetalle) {
        Calculo calculo = new Calculo();
        LocalDateTime inicio = desde.atStartOfDay();
        LocalDateTime fin = hasta.plusDays(1).atStartOfDay();

        if (tipo.incluyeServicios()) {
            cargarServicios(calculo,
                pagoRepository.filasInformeServicios(idArtista, inicio, fin));
        }
        if (tipo.incluyeProductos()) {
            cargarProductos(calculo, idArtista, inicio, fin, conDetalle);
        }
        return calculo;
    }

    private void cargarServicios(Calculo calculo, List<Object[]> filas) {
        Set<Long> pedidos = new HashSet<>();
        for (Object[] fila : filas) {
            if (fila == null || fila.length < 9) {
                continue;
            }
            FilaServicio servicio = filaServicio(fila);
            pedidos.add(servicio.idPedido());
            calculo.totales.sumar(servicio.bruto(), servicio.comision(),
                servicio.arancel(), servicio.neto());
            calculo.totales.unidades++;
            calculo.punto(servicio.fecha())
                .sumar(servicio.bruto(), servicio.neto());
            if (!servicio.netoReal()) {
                calculo.operacionesSinNetoReal.add(
                    "PEDIDO:" + servicio.idPedido());
            }

            String clave = clave("SERVICIO", servicio.idServicio());
            ItemAcum item = calculo.items.computeIfAbsent(clave,
                k -> new ItemAcum("SERVICIO", servicio.idServicio(),
                    servicio.nombre()));
            item.sumar(servicio.bruto(), servicio.comision(),
                servicio.arancel(), servicio.neto());
            item.unidades++;
            calculo.operacionesPorItem
                .computeIfAbsent(clave, k -> new HashSet<>())
                .add(servicio.idPedido());
        }
        calculo.totales.operaciones += pedidos.size();
    }

    private void cargarProductos(
            Calculo calculo,
            Long idArtista,
            LocalDateTime desde,
            LocalDateTime hasta,
            boolean conDetalle) {
        for (Object[] fila : compraGrupoRepository
                .filasInformeGrupos(idArtista, desde, hasta)) {
            if (fila == null || fila.length < 8) {
                continue;
            }
            FilaGrupo grupo = filaGrupo(fila);
            calculo.grupos.put(grupo.idGrupo(), grupo);
            calculo.totales.sumar(grupo.bruto(), grupo.comision(),
                grupo.arancel(), grupo.neto());
            calculo.totales.unidades += grupo.unidades();
            calculo.punto(grupo.fecha()).sumar(grupo.bruto(), grupo.neto());
            if (!grupo.netoReal()) {
                calculo.operacionesSinNetoReal.add(
                    "GRUPO:" + grupo.idGrupo());
            }
        }
        calculo.totales.operaciones += calculo.grupos.size();

        if (!conDetalle || calculo.grupos.isEmpty()) {
            return;
        }

        Map<Long, List<FilaItemProducto>> itemsPorGrupo = new HashMap<>();
        for (Object[] fila : compraItemRepository
                .filasInformeProductos(idArtista, desde, hasta)) {
            if (fila == null || fila.length < 6) {
                continue;
            }
            FilaItemProducto item = filaItem(fila);
            itemsPorGrupo.computeIfAbsent(item.idGrupo(),
                k -> new ArrayList<>()).add(item);
        }
        itemsPorGrupo.forEach((idGrupo, items) ->
            acumularItems(calculo, calculo.grupos.get(idGrupo), items));
    }

    /**
     * El neto real se conoce por grupo, no por ítem: se reparte según la
     * participación de cada ítem en el bruto del grupo. El último ítem absorbe
     * el residuo de redondeo para que la tabla sume exactamente el total.
     */
    private void acumularItems(
            Calculo calculo, FilaGrupo grupo, List<FilaItemProducto> items) {
        if (grupo == null || items == null || items.isEmpty()) {
            return;
        }
        BigDecimal sumaBruto = items.stream()
            .map(FilaItemProducto::bruto)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal base = grupo.bruto().signum() > 0
            ? grupo.bruto() : sumaBruto;
        BigDecimal netoAsignado = BigDecimal.ZERO;
        BigDecimal arancelAsignado = BigDecimal.ZERO;

        for (int indice = 0; indice < items.size(); indice++) {
            FilaItemProducto item = items.get(indice);
            boolean ultimo = indice == items.size() - 1;
            BigDecimal neto;
            BigDecimal arancel;

            if (ultimo) {
                neto = grupo.neto().subtract(netoAsignado);
                arancel = grupo.arancel().subtract(arancelAsignado);
            } else {
                BigDecimal parte = base.signum() == 0
                    ? BigDecimal.ZERO
                    : item.bruto().divide(base, 8, RoundingMode.HALF_UP);
                neto = grupo.neto().multiply(parte)
                    .setScale(2, RoundingMode.HALF_UP);
                arancel = grupo.arancel().multiply(parte)
                    .setScale(2, RoundingMode.HALF_UP);
                netoAsignado = netoAsignado.add(neto);
                arancelAsignado = arancelAsignado.add(arancel);
            }

            String clave = clave("PRODUCTO", item.idProducto());
            ItemAcum acumulado = calculo.items.computeIfAbsent(clave,
                k -> new ItemAcum("PRODUCTO", item.idProducto(),
                    item.titulo()));
            acumulado.sumar(item.bruto(), item.comision(), arancel, neto);
            acumulado.unidades += item.cantidad();
            calculo.operacionesPorItem
                .computeIfAbsent(clave, k -> new HashSet<>())
                .add(grupo.idGrupo());
        }
    }

    private List<InformeItemDTO> items(Calculo calculo) {
        BigDecimal totalNeto = calculo.totales.neto;
        Comparator<ItemAcum> orden = Comparator
            .comparing((ItemAcum item) -> item.neto,
                Comparator.reverseOrder())
            .thenComparing(item -> item.nombre == null ? "" : item.nombre,
                String.CASE_INSENSITIVE_ORDER);

        return calculo.items.values().stream()
            .sorted(orden)
            .map(item -> new InformeItemDTO(
                item.tipo,
                item.id,
                item.nombre == null || item.nombre.isBlank()
                    ? SIN_NOMBRE : item.nombre,
                item.unidades,
                calculo.operacionesPorItem
                    .getOrDefault(item.clave, Set.of()).size(),
                dinero(item.bruto),
                dinero(item.comision),
                dinero(item.arancel),
                dinero(item.neto),
                participacion(item.neto, totalNeto)))
            .toList();
    }

    private List<InformeSerieDTO> serie(
            Map<LocalDate, Punto> acumulada,
            LocalDate desde,
            LocalDate hasta,
            Agrupacion agrupacion) {
        Map<LocalDate, Punto> buckets = new TreeMap<>();
        acumulada.forEach((fecha, punto) -> buckets
            .computeIfAbsent(inicio(fecha, agrupacion), k -> new Punto())
            .sumar(punto));

        List<InformeSerieDTO> salida = new ArrayList<>();
        LocalDate limite = inicio(hasta, agrupacion);
        for (LocalDate cursor = inicio(desde, agrupacion);
                !cursor.isAfter(limite);
                cursor = siguiente(cursor, agrupacion)) {
            Punto punto = buckets.getOrDefault(cursor, new Punto());
            salida.add(new InformeSerieDTO(
                etiqueta(cursor, agrupacion),
                dinero(punto.bruto),
                dinero(punto.neto),
                punto.cobros));
        }
        return salida;
    }

    private static LocalDate inicio(LocalDate fecha, Agrupacion agrupacion) {
        return switch (agrupacion) {
            case DIA -> fecha;
            case SEMANA -> fecha.with(
                TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
            case MES -> fecha.withDayOfMonth(1);
        };
    }

    private static LocalDate siguiente(LocalDate fecha, Agrupacion agrupacion) {
        return switch (agrupacion) {
            case DIA -> fecha.plusDays(1);
            case SEMANA -> fecha.plusWeeks(1);
            case MES -> fecha.plusMonths(1);
        };
    }

    private static String etiqueta(LocalDate fecha, Agrupacion agrupacion) {
        return agrupacion == Agrupacion.MES
            ? fecha.format(MES)
            : fecha.format(DateTimeFormatter.ISO_LOCAL_DATE);
    }

    private static FilaServicio filaServicio(Object[] fila) {
        return new FilaServicio(
            fecha(fila[0]),
            numero(fila[1]),
            texto(fila[2]),
            decimal(fila[3]),
            decimal(fila[4]),
            decimal(fila[5]),
            decimal(fila[6]),
            entero(fila[7]) == 0,
            numero(fila[8]));
    }

    private static FilaGrupo filaGrupo(Object[] fila) {
        return new FilaGrupo(
            fecha(fila[0]),
            numero(fila[1]),
            decimal(fila[2]),
            decimal(fila[3]),
            decimal(fila[4]),
            decimal(fila[5]),
            entero(fila[6]) == 0,
            entero(fila[7]));
    }

    private static FilaItemProducto filaItem(Object[] fila) {
        BigDecimal precio = decimal(fila[4]);
        long cantidad = entero(fila[3]);
        return new FilaItemProducto(
            numero(fila[0]),
            numero(fila[1]),
            texto(fila[2]),
            cantidad,
            precio.multiply(BigDecimal.valueOf(cantidad)),
            decimal(fila[5]));
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

    private static BigDecimal variacion(
            BigDecimal actual, BigDecimal anterior) {
        if (anterior == null || anterior.signum() == 0) {
            return null;
        }
        return actual.subtract(anterior)
            .multiply(BigDecimal.valueOf(100))
            .divide(anterior, 2, RoundingMode.HALF_UP);
    }

    private static BigDecimal participacion(
            BigDecimal neto, BigDecimal total) {
        if (total == null || total.signum() == 0) {
            return dinero(BigDecimal.ZERO);
        }
        return neto.multiply(BigDecimal.valueOf(100))
            .divide(total, 2, RoundingMode.HALF_UP);
    }

    private static BigDecimal dinero(BigDecimal valor) {
        return valor == null
            ? BigDecimal.ZERO.setScale(2)
            : valor.setScale(2, RoundingMode.HALF_UP);
    }

    private static String clave(String tipo, Long id) {
        return tipo + ":" + id;
    }

    private static BigDecimal decimal(Object valor) {
        if (valor == null) {
            return BigDecimal.ZERO;
        }
        if (valor instanceof BigDecimal decimal) {
            return decimal;
        }
        if (valor instanceof Number numero) {
            return new BigDecimal(numero.toString());
        }
        return new BigDecimal(valor.toString());
    }

    private static long entero(Object valor) {
        return valor instanceof Number numero ? numero.longValue() : 0L;
    }

    private static Long numero(Object valor) {
        return valor instanceof Number numero ? numero.longValue() : null;
    }

    private static String texto(Object valor) {
        return valor == null ? null : valor.toString();
    }

    private static LocalDate fecha(Object valor) {
        if (valor instanceof java.sql.Date fechaSql) {
            return fechaSql.toLocalDate();
        }
        if (valor instanceof LocalDate fecha) {
            return fecha;
        }
        if (valor instanceof LocalDateTime fechaHora) {
            return fechaHora.toLocalDate();
        }
        if (valor instanceof java.util.Date fecha) {
            return fecha.toInstant()
                .atZone(ZoneId.systemDefault())
                .toLocalDate();
        }
        if (valor instanceof String texto) {
            return LocalDate.parse(texto);
        }
        throw new IllegalArgumentException(
            "Fecha inválida en el informe: " + valor);
    }

    enum Tipo {
        TODOS,
        PRODUCTOS,
        SERVICIOS;

        static Tipo de(String valor) {
            if (valor == null || valor.isBlank()) {
                return TODOS;
            }
            try {
                return valueOf(valor.trim().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException(
                    "Tipo de informe inválido: " + valor);
            }
        }

        boolean incluyeServicios() {
            return this != PRODUCTOS;
        }

        boolean incluyeProductos() {
            return this != SERVICIOS;
        }
    }

    enum Agrupacion {
        DIA,
        SEMANA,
        MES;

        static Agrupacion de(String valor) {
            if (valor == null || valor.isBlank()) {
                return DIA;
            }
            try {
                return valueOf(valor.trim().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException(
                    "Agrupación inválida: " + valor);
            }
        }
    }

    private record FilaServicio(
            LocalDate fecha,
            Long idServicio,
            String nombre,
            BigDecimal bruto,
            BigDecimal comision,
            BigDecimal arancel,
            BigDecimal neto,
            boolean netoReal,
            Long idPedido) {
    }

    private record FilaGrupo(
            LocalDate fecha,
            Long idGrupo,
            BigDecimal bruto,
            BigDecimal comision,
            BigDecimal arancel,
            BigDecimal neto,
            boolean netoReal,
            long unidades) {
    }

    private record FilaItemProducto(
            Long idGrupo,
            Long idProducto,
            String titulo,
            long cantidad,
            BigDecimal bruto,
            BigDecimal comision) {
    }

    private static final class Punto {
        private BigDecimal bruto = BigDecimal.ZERO;
        private BigDecimal neto = BigDecimal.ZERO;
        private long cobros;

        void sumar(BigDecimal bruto, BigDecimal neto) {
            this.bruto = this.bruto.add(bruto);
            this.neto = this.neto.add(neto);
            this.cobros++;
        }

        void sumar(Punto otro) {
            this.bruto = this.bruto.add(otro.bruto);
            this.neto = this.neto.add(otro.neto);
            this.cobros += otro.cobros;
        }
    }

    private static final class ItemAcum {
        private final String clave;
        private final String tipo;
        private final Long id;
        private final String nombre;
        private long unidades;
        private BigDecimal bruto = BigDecimal.ZERO;
        private BigDecimal comision = BigDecimal.ZERO;
        private BigDecimal arancel = BigDecimal.ZERO;
        private BigDecimal neto = BigDecimal.ZERO;

        ItemAcum(String tipo, Long id, String nombre) {
            this.tipo = tipo;
            this.id = id;
            this.nombre = nombre;
            this.clave = clave(tipo, id);
        }

        void sumar(
                BigDecimal bruto,
                BigDecimal comision,
                BigDecimal arancel,
                BigDecimal neto) {
            this.bruto = this.bruto.add(bruto);
            this.comision = this.comision.add(comision);
            this.arancel = this.arancel.add(arancel);
            this.neto = this.neto.add(neto);
        }
    }

    private static final class Acumulado {
        private BigDecimal bruto = BigDecimal.ZERO;
        private BigDecimal comision = BigDecimal.ZERO;
        private BigDecimal arancel = BigDecimal.ZERO;
        private BigDecimal neto = BigDecimal.ZERO;
        private long operaciones;
        private long unidades;

        void sumar(
                BigDecimal bruto,
                BigDecimal comision,
                BigDecimal arancel,
                BigDecimal neto) {
            this.bruto = this.bruto.add(bruto);
            this.comision = this.comision.add(comision);
            this.arancel = this.arancel.add(arancel);
            this.neto = this.neto.add(neto);
        }
    }

    private static final class Calculo {
        private final Acumulado totales = new Acumulado();
        private final Map<LocalDate, Punto> serie = new HashMap<>();
        private final Map<String, ItemAcum> items = new HashMap<>();
        private final Map<String, Set<Long>> operacionesPorItem =
            new HashMap<>();
        private final Map<Long, FilaGrupo> grupos = new HashMap<>();
        private final Set<String> operacionesSinNetoReal = new HashSet<>();

        Punto punto(LocalDate fecha) {
            return serie.computeIfAbsent(fecha, k -> new Punto());
        }

        InformeTotalesDTO totalesDto() {
            return new InformeTotalesDTO(
                dinero(totales.bruto),
                dinero(totales.neto),
                dinero(totales.bruto.subtract(totales.neto)),
                dinero(totales.comision),
                dinero(totales.arancel),
                totales.operaciones,
                totales.unidades,
                totales.operaciones == 0
                    ? dinero(BigDecimal.ZERO)
                    : dinero(totales.neto.divide(
                        BigDecimal.valueOf(totales.operaciones),
                        2, RoundingMode.HALF_UP)));
        }
    }
}
