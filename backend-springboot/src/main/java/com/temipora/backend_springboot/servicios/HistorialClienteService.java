package com.temipora.backend_springboot.servicios;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.temipora.backend_springboot.modelo.HistorialClienteDTO;
import com.temipora.backend_springboot.modelo.HistorialItemDTO;
import com.temipora.backend_springboot.modelo.HistorialResumenDTO;
import com.temipora.backend_springboot.repositorios.HistorialClienteRepository;

/**
 * Historial de sólo lectura del comprador. Una fila por pedido de servicio o
 * compra de producto; los importes están en la moneda de cobro (ARS).
 */
@Service
public class HistorialClienteService {

    public static final int FILAS_PDF = 2000;

    private static final int TAMANIO_MAX = 2000;
    private static final LocalDateTime DESDE_MIN =
        LocalDateTime.of(1970, 1, 1, 0, 0);
    private static final LocalDateTime HASTA_MAX =
        LocalDateTime.of(9999, 12, 31, 23, 59, 59);
    private static final DateTimeFormatter FECHA =
        DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final HistorialClienteRepository repository;

    public HistorialClienteService(HistorialClienteRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public HistorialClienteDTO listar(
            Long idUsuario,
            String desdeTexto,
            String hastaTexto,
            String tipoTexto,
            String estadoTexto,
            String busqueda,
            int pagina,
            int tamanio) {
        LocalDate desdeFecha = fechaDe(desdeTexto);
        LocalDate hastaFecha = fechaDe(hastaTexto);
        if (desdeFecha != null && hastaFecha != null
                && desdeFecha.isAfter(hastaFecha)) {
            throw new IllegalArgumentException(
                "La fecha de inicio debe ser anterior a la de fin.");
        }

        String tipo = tipoDe(tipoTexto);
        String estado = estadoDe(estadoTexto);
        String patron = busqueda == null
            ? "" : busqueda.trim().toLowerCase(Locale.ROOT);
        int paginaSegura = Math.max(0, pagina);
        int tamanioSeguro = Math.min(Math.max(1, tamanio), TAMANIO_MAX);
        LocalDateTime desde = desdeFecha != null
            ? desdeFecha.atStartOfDay() : DESDE_MIN;
        LocalDateTime hasta = hastaFecha != null
            ? hastaFecha.plusDays(1).atStartOfDay() : HASTA_MAX;

        List<Object[]> filas = repository.buscar(
            idUsuario, desde, hasta, tipo, estado, patron,
            paginaSegura, tamanioSeguro);
        long total = repository.contar(
            idUsuario, desde, hasta, tipo, estado, patron);
        List<Object[]> grupos = repository.resumir(
            idUsuario, desde, hasta, tipo, estado, patron);

        List<HistorialItemDTO> contenido = filas.stream()
            .map(this::aDto)
            .toList();
        int totalPaginas = (int) Math.ceil((double) total / tamanioSeguro);
        boolean hayMas = (paginaSegura + 1L) * tamanioSeguro < total;

        return new HistorialClienteDTO(
            desdeFecha,
            hastaFecha,
            periodoTexto(desdeFecha, hastaFecha),
            filtroTexto(tipo, estado, patron),
            LocalDateTime.now(),
            resumen(grupos),
            contenido,
            paginaSegura,
            tamanioSeguro,
            total,
            totalPaginas,
            hayMas);
    }

    private HistorialResumenDTO resumen(List<Object[]> grupos) {
        long operaciones = 0;
        long pedidos = 0;
        long compras = 0;
        long completadas = 0;
        long enCurso = 0;
        long pendientes = 0;
        long canceladas = 0;
        BigDecimal importeTotal = BigDecimal.ZERO;
        BigDecimal importeCompletado = BigDecimal.ZERO;
        BigDecimal descuentos = BigDecimal.ZERO;

        for (Object[] fila : grupos) {
            if (fila == null || fila.length < 6) {
                continue;
            }
            String tipo = texto(fila[0]);
            String estadoGrupo = texto(fila[1]);
            long cantidad = entero(fila[3]);
            BigDecimal monto = decimal(fila[4]);
            BigDecimal descuento = decimal(fila[5]);

            operaciones += cantidad;
            if ("PEDIDO".equals(tipo)) {
                pedidos += cantidad;
            } else if ("COMPRA".equals(tipo)) {
                compras += cantidad;
            }
            switch (estadoGrupo) {
                case "FINALIZADO" -> completadas += cantidad;
                case "EN_CURSO" -> enCurso += cantidad;
                case "PENDIENTE" -> pendientes += cantidad;
                case "CANCELADO" -> canceladas += cantidad;
                default -> {
                }
            }
            importeTotal = importeTotal.add(monto);
            if ("FINALIZADO".equals(estadoGrupo)) {
                importeCompletado = importeCompletado.add(monto);
            }
            descuentos = descuentos.add(descuento);
        }

        return new HistorialResumenDTO(
            operaciones,
            pedidos,
            compras,
            completadas,
            enCurso,
            pendientes,
            canceladas,
            dinero(importeTotal),
            dinero(importeCompletado),
            dinero(descuentos),
            "ARS");
    }

    private HistorialItemDTO aDto(Object[] fila) {
        String tipo = texto(fila[0]);
        String estado = texto(fila[7]);
        return new HistorialItemDTO(
            tipo,
            "PEDIDO".equals(tipo) ? "Servicio" : "Producto",
            numero(fila[1]),
            fecha(fila[2]),
            texto(fila[3]),
            texto(fila[4]),
            decimal(fila[5]),
            texto(fila[6]),
            estado,
            etiquetaEstado(tipo, estado),
            texto(fila[8]),
            decimal(fila[9]),
            decimal(fila[10]),
            entero(fila[11]),
            entero(fila[12]));
    }

    private static String periodoTexto(LocalDate desde, LocalDate hasta) {
        if (desde == null && hasta == null) {
            return "Todo el historial";
        }
        if (desde != null && hasta != null) {
            return FECHA.format(desde) + " al " + FECHA.format(hasta);
        }
        if (desde != null) {
            return "Desde el " + FECHA.format(desde);
        }
        return "Hasta el " + FECHA.format(hasta);
    }

    private static String filtroTexto(
            String tipo, String estado, String patron) {
        List<String> partes = new ArrayList<>();
        partes.add(switch (tipo) {
            case "PEDIDO" -> "Servicios";
            case "COMPRA" -> "Productos";
            default -> "Servicios y productos";
        });
        partes.add(switch (estado) {
            case "FINALIZADO" -> "Completados";
            case "ABIERTO" -> "En curso y pendientes";
            case "CANCELADO" -> "Cancelados";
            default -> "Todos los estados";
        });
        if (!patron.isBlank()) {
            partes.add("Búsqueda: \"" + patron + "\"");
        }
        return String.join(" · ", partes);
    }

    private static String etiquetaEstado(String tipo, String estado) {
        if (estado == null || estado.isBlank()) {
            return "—";
        }
        if ("PEDIDO".equals(tipo)) {
            return switch (estado) {
                case "PENDIENTE_PAGO" -> "Pendiente de pago";
                case "EN_CURSO" -> "En curso";
                case "FINALIZADO" -> "Finalizado";
                case "CANCELADO" -> "Cancelado";
                case "IMPAGO" -> "Pago vencido";
                case "INCUMPLIMIENTO" -> "Cerrado por incumplimiento";
                default -> estado;
            };
        }
        return switch (estado) {
            case "PENDIENTE_PAGO" -> "Pendiente de pago";
            case "PARCIALMENTE_PAGA" -> "Parcialmente paga";
            case "PAGA" -> "Pagada";
            case "VENCIDA" -> "Vencida";
            case "CANCELADA" -> "Cancelada";
            default -> estado;
        };
    }

    private static String tipoDe(String valor) {
        if (valor == null || valor.isBlank()
                || "TODOS".equalsIgnoreCase(valor.trim())) {
            return "";
        }
        String tipo = valor.trim().toUpperCase(Locale.ROOT);
        if (!"PEDIDO".equals(tipo) && !"COMPRA".equals(tipo)) {
            throw new IllegalArgumentException(
                "Tipo de historial inválido: " + valor);
        }
        return tipo;
    }

    private static String estadoDe(String valor) {
        if (valor == null || valor.isBlank()) {
            return "FINALIZADO";
        }
        String estado = valor.trim().toUpperCase(Locale.ROOT);
        if ("TODOS".equals(estado)) {
            return "";
        }
        if ("FINALIZADO".equals(estado)
                || "ABIERTO".equals(estado)
                || "CANCELADO".equals(estado)) {
            return estado;
        }
        throw new IllegalArgumentException(
            "Estado de historial inválido: " + valor);
    }

    private static LocalDate fechaDe(String valor) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(valor.trim());
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Fecha inválida: " + valor);
        }
    }

    private static LocalDateTime fecha(Object valor) {
        if (valor == null) {
            return null;
        }
        if (valor instanceof LocalDateTime fechaHora) {
            return fechaHora;
        }
        if (valor instanceof java.sql.Timestamp timestamp) {
            return timestamp.toLocalDateTime();
        }
        if (valor instanceof java.util.Date date) {
            return Instant.ofEpochMilli(date.getTime())
                .atZone(ZoneId.systemDefault())
                .toLocalDateTime();
        }
        if (valor instanceof String texto) {
            return LocalDateTime.parse(texto);
        }
        throw new IllegalArgumentException(
            "Fecha inválida en el historial: " + valor);
    }

    private static BigDecimal decimal(Object valor) {
        if (valor == null) {
            return BigDecimal.ZERO.setScale(2);
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
        return valor == null ? "" : valor.toString();
    }

    private static BigDecimal dinero(BigDecimal valor) {
        return valor.setScale(2, RoundingMode.HALF_UP);
    }
}
