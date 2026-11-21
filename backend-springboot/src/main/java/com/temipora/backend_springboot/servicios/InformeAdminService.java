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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.temipora.backend_springboot.modelo.InformeAdminComparacionDTO;
import com.temipora.backend_springboot.modelo.InformeAdminDTO;
import com.temipora.backend_springboot.modelo.InformeAdminItemDTO;
import com.temipora.backend_springboot.modelo.InformeAdminSerieDTO;
import com.temipora.backend_springboot.modelo.InformeAdminTipoDTO;
import com.temipora.backend_springboot.modelo.InformeAdminTotalesDTO;
import com.temipora.backend_springboot.modelo.InformeAdminVendedorDTO;
import com.temipora.backend_springboot.modelo.InformePeriodoDTO;
import com.temipora.backend_springboot.repositorios.CompraGrupoRepository;
import com.temipora.backend_springboot.repositorios.CompraItemRepository;
import com.temipora.backend_springboot.repositorios.PagoRepository;

/**
 * Arma el informe de ventas y comisiones de toda la plataforma. Suma servicios
 * (pagos aprobados) y productos (grupos pagados), y expone los cortes por
 * tipo, por vendedor y por ítem del catálogo.
 *
 * <p>La comisión es el ingreso de Temipora. El neto percibido es el importe
 * que el medio de pago liquidó realmente a los vendedores; si falta ese dato
 * se usa el neto del artista y la operación se marca como estimada.</p>
 */
@Service
public class InformeAdminService {

    private static final int DIAS_POR_DEFECTO = 30;
    private static final int MAX_DIAS = 3660;
    private static final int MAX_DIAS_DIARIO = 366;
    private static final String MONEDA_COBRO = "ARS";
    private static final String SIN_NOMBRE = "Sin nombre";
    private static final String SERVICIO = "SERVICIO";
    private static final String PRODUCTO = "PRODUCTO";
    private static final DateTimeFormatter MES =
        DateTimeFormatter.ofPattern("yyyy-MM");

    private final PagoRepository pagoRepository;
    private final CompraGrupoRepository compraGrupoRepository;
    private final CompraItemRepository compraItemRepository;

    public InformeAdminService(
            PagoRepository pagoRepository,
            CompraGrupoRepository compraGrupoRepository,
            CompraItemRepository compraItemRepository) {
        this.pagoRepository = pagoRepository;
        this.compraGrupoRepository = compraGrupoRepository;
        this.compraItemRepository = compraItemRepository;
    }

    @Transactional(readOnly = true)
    public InformeAdminDTO generar(
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

        Calculo actual = calcular(desde, hasta, tipo, true);
        LocalDate hastaAnterior = desde.minusDays(1);
        LocalDate desdeAnterior = hastaAnterior.minusDays(dias - 1);
        Calculo anterior = calcular(
            desdeAnterior, hastaAnterior, tipo, false);

        return new InformeAdminDTO(
            new InformePeriodoDTO(desde, hasta, dias, agrupacion.name()),
            MONEDA_COBRO,
            tipo.name(),
            LocalDateTime.now(),
            actual.totales.dto(),
            new InformeAdminComparacionDTO(
                desdeAnterior,
                hastaAnterior,
                dinero(anterior.totales.volumen),
                dinero(anterior.totales.comision),
                variacion(actual.totales.volumen, anterior.totales.volumen),
                variacion(actual.totales.comision, anterior.totales.comision)),
            actual.sinNetoReal.size(),
            serie(actual.serie, desde, hasta, agrupacion),
            tipos(actual),
            vendedores(actual),
            items(actual));
    }

    private Calculo calcular(
            LocalDate desde,
            LocalDate hasta,
            Tipo tipo,
            boolean conDetalle) {
        Calculo calculo = new Calculo();
        LocalDateTime inicio = desde.atStartOfDay();
        LocalDateTime fin = hasta.plusDays(1).atStartOfDay();

        if (tipo.incluyeServicios()) {
            cargarServicios(calculo,
                pagoRepository.filasInformeAdminServicios(inicio, fin));
        }
        if (tipo.incluyeProductos()) {
            cargarProductos(calculo, inicio, fin, conDetalle);
        }
        return calculo;
    }

    private void cargarServicios(Calculo calculo, List<Object[]> filas) {
        Bucket tipo = calculo.tipos.computeIfAbsent(
            SERVICIO, k -> new Bucket(SERVICIO, null, "Servicios"));

        for (Object[] fila : filas) {
            if (fila == null || fila.length < 12) {
                continue;
            }
            FilaServicio servicio = filaServicio(fila);
            String operacion = "PEDIDO:" + servicio.idPedido();

            calculo.totales.sumar(servicio.volumen(), servicio.comision(),
                servicio.neto(), servicio.arancel(),
                servicio.netoPercibido());
            calculo.totales.unidades++;
            calculo.totales.operaciones.add(operacion);
            calculo.punto(servicio.fecha())
                .sumar(servicio.volumen(), servicio.comision());
            if (!servicio.netoReal()) {
                calculo.sinNetoReal.add(operacion);
            }

            tipo.sumar(servicio.volumen(), servicio.comision(),
                servicio.neto(), servicio.arancel());
            tipo.unidades++;
            tipo.operaciones.add(operacion);

            Bucket vendedor = calculo.vendedores.computeIfAbsent(
                "U:" + servicio.idArtista(),
                k -> new Bucket(null, servicio.idArtista(),
                    servicio.nombreArtista()));
            vendedor.sumar(servicio.volumen(), servicio.comision(),
                servicio.neto(), servicio.arancel());
            vendedor.unidades++;
            vendedor.operaciones.add(operacion);

            Bucket item = calculo.items.computeIfAbsent(
                "S:" + servicio.idServicio(),
                k -> new Bucket(SERVICIO, servicio.idServicio(),
                    servicio.nombreServicio()));
            item.sumar(servicio.volumen(), servicio.comision(),
                servicio.neto(), servicio.arancel());
            item.unidades++;
            item.operaciones.add(operacion);
        }
    }

    private void cargarProductos(
            Calculo calculo,
            LocalDateTime desde,
            LocalDateTime hasta,
            boolean conDetalle) {
        Bucket tipo = calculo.tipos.computeIfAbsent(
            PRODUCTO, k -> new Bucket(PRODUCTO, null, "Productos"));

        for (Object[] fila : compraGrupoRepository
                .filasInformeAdminGrupos(desde, hasta)) {
            if (fila == null || fila.length < 11) {
                continue;
            }
            FilaGrupo grupo = filaGrupo(fila);
            String operacion = "GRUPO:" + grupo.idGrupo();
            calculo.grupos.put(grupo.idGrupo(), grupo);

            calculo.totales.sumar(grupo.volumen(), grupo.comision(),
                grupo.neto(), grupo.arancel(), grupo.netoPercibido());
            calculo.totales.unidades += grupo.unidades();
            calculo.totales.operaciones.add(operacion);
            calculo.punto(grupo.fecha())
                .sumar(grupo.volumen(), grupo.comision());
            if (!grupo.netoReal()) {
                calculo.sinNetoReal.add(operacion);
            }

            tipo.sumar(grupo.volumen(), grupo.comision(),
                grupo.neto(), grupo.arancel());
            tipo.unidades += grupo.unidades();
            tipo.operaciones.add(operacion);

            Bucket vendedor = calculo.vendedores.computeIfAbsent(
                "U:" + grupo.idArtista(),
                k -> new Bucket(null, grupo.idArtista(),
                    grupo.nombreArtista()));
            vendedor.sumar(grupo.volumen(), grupo.comision(),
                grupo.neto(), grupo.arancel());
            vendedor.unidades += grupo.unidades();
            vendedor.operaciones.add(operacion);
        }

        if (!conDetalle || calculo.grupos.isEmpty()) {
            return;
        }

        Map<Long, List<FilaItem>> itemsPorGrupo = new HashMap<>();
        for (Object[] fila : compraItemRepository
                .filasInformeAdminProductos(desde, hasta)) {
            if (fila == null || fila.length < 6) {
                continue;
            }
            FilaItem item = filaItem(fila);
            itemsPorGrupo.computeIfAbsent(item.idGrupo(),
                k -> new ArrayList<>()).add(item);
        }
        itemsPorGrupo.forEach((idGrupo, items) ->
            acumularItems(calculo, calculo.grupos.get(idGrupo), items));
    }

    /**
     * El neto y el arancel se conocen por grupo, no por ítem: se reparten
     * según la participación de cada ítem en el volumen del grupo. El último
     * ítem absorbe el residuo de redondeo.
     */
    private void acumularItems(
            Calculo calculo, FilaGrupo grupo, List<FilaItem> items) {
        if (grupo == null || items == null || items.isEmpty()) {
            return;
        }
        BigDecimal sumaVolumen = items.stream()
            .map(FilaItem::volumen)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal base = grupo.volumen().signum() > 0
            ? grupo.volumen() : sumaVolumen;
        BigDecimal netoAsignado = BigDecimal.ZERO;
        BigDecimal arancelAsignado = BigDecimal.ZERO;

        for (int indice = 0; indice < items.size(); indice++) {
            FilaItem item = items.get(indice);
            boolean ultimo = indice == items.size() - 1;
            BigDecimal neto;
            BigDecimal arancel;

            if (ultimo) {
                neto = grupo.neto().subtract(netoAsignado);
                arancel = grupo.arancel().subtract(arancelAsignado);
            } else {
                BigDecimal parte = base.signum() == 0
                    ? BigDecimal.ZERO
                    : item.volumen().divide(
                        base, 8, RoundingMode.HALF_UP);
                neto = grupo.neto().multiply(parte)
                    .setScale(2, RoundingMode.HALF_UP);
                arancel = grupo.arancel().multiply(parte)
                    .setScale(2, RoundingMode.HALF_UP);
                netoAsignado = netoAsignado.add(neto);
                arancelAsignado = arancelAsignado.add(arancel);
            }

            Bucket bucket = calculo.items.computeIfAbsent(
                "P:" + item.idProducto(),
                k -> new Bucket(PRODUCTO, item.idProducto(),
                    item.titulo()));
            bucket.sumar(item.volumen(), item.comision(), neto, arancel);
            bucket.unidades += item.cantidad();
            bucket.operaciones.add("GRUPO:" + grupo.idGrupo());
        }
    }

    private List<InformeAdminTipoDTO> tipos(Calculo calculo) {
        return calculo.tipos.values().stream()
            .sorted(Comparator.comparing(bucket -> bucket.tipo))
            .map(bucket -> new InformeAdminTipoDTO(
                bucket.tipo,
                bucket.operaciones.size(),
                bucket.unidades,
                dinero(bucket.volumen),
                dinero(bucket.comision),
                dinero(bucket.neto),
                dinero(bucket.arancel)))
            .toList();
    }

    private List<InformeAdminVendedorDTO> vendedores(Calculo calculo) {
        Comparator<Bucket> orden = Comparator
            .comparing((Bucket bucket) -> bucket.comision,
                Comparator.reverseOrder())
            .thenComparing(
                bucket -> bucket.nombre == null ? "" : bucket.nombre,
                String.CASE_INSENSITIVE_ORDER);

        return calculo.vendedores.values().stream()
            .sorted(orden)
            .map(bucket -> new InformeAdminVendedorDTO(
                bucket.id,
                nombre(bucket.nombre),
                bucket.operaciones.size(),
                bucket.unidades,
                dinero(bucket.volumen),
                dinero(bucket.comision),
                dinero(bucket.neto),
                dinero(bucket.arancel)))
            .toList();
    }

    private List<InformeAdminItemDTO> items(Calculo calculo) {
        Comparator<Bucket> orden = Comparator
            .comparing((Bucket bucket) -> bucket.comision,
                Comparator.reverseOrder())
            .thenComparing(
                bucket -> bucket.nombre == null ? "" : bucket.nombre,
                String.CASE_INSENSITIVE_ORDER);

        return calculo.items.values().stream()
            .sorted(orden)
            .map(bucket -> new InformeAdminItemDTO(
                bucket.tipo,
                bucket.id,
                nombre(bucket.nombre),
                bucket.operaciones.size(),
                bucket.unidades,
                dinero(bucket.volumen),
                dinero(bucket.comision),
                dinero(bucket.neto),
                dinero(bucket.arancel)))
            .toList();
    }

    private List<InformeAdminSerieDTO> serie(
            Map<LocalDate, Punto> acumulada,
            LocalDate desde,
            LocalDate hasta,
            Agrupacion agrupacion) {
        Map<LocalDate, Punto> buckets = new TreeMap<>();
        acumulada.forEach((fecha, punto) -> buckets
            .computeIfAbsent(inicio(fecha, agrupacion), k -> new Punto())
            .sumar(punto));

        List<InformeAdminSerieDTO> salida = new ArrayList<>();
        LocalDate limite = inicio(hasta, agrupacion);
        for (LocalDate cursor = inicio(desde, agrupacion);
                !cursor.isAfter(limite);
                cursor = siguiente(cursor, agrupacion)) {
            Punto punto = buckets.getOrDefault(cursor, new Punto());
            salida.add(new InformeAdminSerieDTO(
                etiqueta(cursor, agrupacion),
                dinero(punto.volumen),
                dinero(punto.comision),
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
        BigDecimal volumen = decimal(fila[5]);
        BigDecimal comision = decimal(fila[6]);
        BigDecimal arancel = decimal(fila[8]);
        return new FilaServicio(
            fecha(fila[0]),
            numero(fila[1]),
            texto(fila[2]),
            numero(fila[3]),
            texto(fila[4]),
            volumen,
            comision,
            decimal(fila[7]),
            arancel,
            CalculoLiquidacion.netoRecibido(volumen, comision, arancel),
            entero(fila[10]) == 0,
            numero(fila[11]));
    }

    private static FilaGrupo filaGrupo(Object[] fila) {
        BigDecimal volumen = decimal(fila[3]);
        BigDecimal comision = decimal(fila[4]);
        BigDecimal arancel = decimal(fila[6]);
        return new FilaGrupo(
            fecha(fila[0]),
            numero(fila[1]),
            texto(fila[2]),
            volumen,
            comision,
            decimal(fila[5]),
            arancel,
            CalculoLiquidacion.netoRecibido(volumen, comision, arancel),
            entero(fila[8]) == 0,
            entero(fila[9]),
            numero(fila[10]));
    }

    private static FilaItem filaItem(Object[] fila) {
        BigDecimal precio = decimal(fila[4]);
        long cantidad = entero(fila[3]);
        return new FilaItem(
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

    private static BigDecimal dinero(BigDecimal valor) {
        return valor == null
            ? BigDecimal.ZERO.setScale(2)
            : valor.setScale(2, RoundingMode.HALF_UP);
    }

    private static String nombre(String valor) {
        return valor == null || valor.isBlank() ? SIN_NOMBRE : valor;
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
            Long idArtista,
            String nombreArtista,
            Long idServicio,
            String nombreServicio,
            BigDecimal volumen,
            BigDecimal comision,
            BigDecimal neto,
            BigDecimal arancel,
            BigDecimal netoPercibido,
            boolean netoReal,
            Long idPedido) {
    }

    private record FilaGrupo(
            LocalDate fecha,
            Long idArtista,
            String nombreArtista,
            BigDecimal volumen,
            BigDecimal comision,
            BigDecimal neto,
            BigDecimal arancel,
            BigDecimal netoPercibido,
            boolean netoReal,
            long unidades,
            Long idGrupo) {
    }

    private record FilaItem(
            Long idGrupo,
            Long idProducto,
            String titulo,
            long cantidad,
            BigDecimal volumen,
            BigDecimal comision) {
    }

    private static final class Punto {
        private BigDecimal volumen = BigDecimal.ZERO;
        private BigDecimal comision = BigDecimal.ZERO;
        private long cobros;

        void sumar(BigDecimal volumen, BigDecimal comision) {
            this.volumen = this.volumen.add(volumen);
            this.comision = this.comision.add(comision);
            this.cobros++;
        }

        void sumar(Punto otro) {
            this.volumen = this.volumen.add(otro.volumen);
            this.comision = this.comision.add(otro.comision);
            this.cobros += otro.cobros;
        }
    }

    private static final class Totales {
        private BigDecimal volumen = BigDecimal.ZERO;
        private BigDecimal comision = BigDecimal.ZERO;
        private BigDecimal neto = BigDecimal.ZERO;
        private BigDecimal arancel = BigDecimal.ZERO;
        private BigDecimal netoPercibido = BigDecimal.ZERO;
        private long unidades;
        private final Set<String> operaciones = new HashSet<>();

        void sumar(
                BigDecimal volumen,
                BigDecimal comision,
                BigDecimal neto,
                BigDecimal arancel,
                BigDecimal netoPercibido) {
            this.volumen = this.volumen.add(volumen);
            this.comision = this.comision.add(comision);
            this.neto = this.neto.add(neto);
            this.arancel = this.arancel.add(arancel);
            this.netoPercibido = this.netoPercibido.add(netoPercibido);
        }

        InformeAdminTotalesDTO dto() {
            long operaciones = this.operaciones.size();
            BigDecimal ticket = operaciones == 0
                ? BigDecimal.ZERO.setScale(2)
                : volumen.divide(BigDecimal.valueOf(operaciones),
                    2, RoundingMode.HALF_UP);
            return new InformeAdminTotalesDTO(
                dinero(volumen),
                dinero(comision),
                dinero(neto),
                dinero(arancel),
                dinero(netoPercibido),
                operaciones,
                unidades,
                dinero(ticket));
        }
    }

    private static final class Bucket {
        private final String tipo;
        private final Long id;
        private final String nombre;
        private BigDecimal volumen = BigDecimal.ZERO;
        private BigDecimal comision = BigDecimal.ZERO;
        private BigDecimal neto = BigDecimal.ZERO;
        private BigDecimal arancel = BigDecimal.ZERO;
        private long unidades;
        private final Set<String> operaciones = new HashSet<>();

        Bucket(String tipo, Long id, String nombre) {
            this.tipo = tipo;
            this.id = id;
            this.nombre = nombre;
        }

        void sumar(
                BigDecimal volumen,
                BigDecimal comision,
                BigDecimal neto,
                BigDecimal arancel) {
            this.volumen = this.volumen.add(volumen);
            this.comision = this.comision.add(comision);
            this.neto = this.neto.add(neto);
            this.arancel = this.arancel.add(arancel);
        }
    }

    private static final class Calculo {
        private final Totales totales = new Totales();
        private final Map<LocalDate, Punto> serie = new HashMap<>();
        private final Map<String, Bucket> tipos = new LinkedHashMap<>();
        private final Map<String, Bucket> vendedores = new LinkedHashMap<>();
        private final Map<String, Bucket> items = new LinkedHashMap<>();
        private final Map<Long, FilaGrupo> grupos = new HashMap<>();
        private final Set<String> sinNetoReal = new HashSet<>();

        Punto punto(LocalDate fecha) {
            return serie.computeIfAbsent(fecha, k -> new Punto());
        }
    }
}
