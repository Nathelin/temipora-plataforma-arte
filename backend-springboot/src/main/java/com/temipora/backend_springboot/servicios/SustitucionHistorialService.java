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
import java.util.NoSuchElementException;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.temipora.backend_springboot.modelo.DisparadorSustitucion;
import com.temipora.backend_springboot.modelo.EstadoSustitucion;
import com.temipora.backend_springboot.modelo.InformeSustitucionesHistorialDTO;
import com.temipora.backend_springboot.modelo.MotivoSustitucion;
import com.temipora.backend_springboot.modelo.ResumenSustitucionesHistorialDTO;
import com.temipora.backend_springboot.modelo.Sustitucion;
import com.temipora.backend_springboot.modelo.SustitucionConteoDTO;
import com.temipora.backend_springboot.modelo.SustitucionDetalleHistorialDTO;
import com.temipora.backend_springboot.modelo.SustitucionHistorialDTO;
import com.temipora.backend_springboot.modelo.SustitucionOpcion;
import com.temipora.backend_springboot.modelo.SustitucionOpcionHistorialDTO;
import com.temipora.backend_springboot.repositorios.SustitucionOpcionRepository;
import com.temipora.backend_springboot.repositorios.SustitucionRepository;

/**
 * Historial administrativo de sustituciones. Cada fila es un evento de
 * búsqueda de alternativas; el detalle conserva las opciones mostradas con el
 * precio y el motivo con los que se ofrecieron.
 */
@Service
public class SustitucionHistorialService {

    public static final int FILAS_PDF = 2000;

    private static final int DIAS_POR_DEFECTO = 30;
    private static final int MAX_DIAS = 3660;

    private final SustitucionRepository sustitucionRepository;
    private final SustitucionOpcionRepository opcionRepository;

    public SustitucionHistorialService(
            SustitucionRepository sustitucionRepository,
            SustitucionOpcionRepository opcionRepository) {
        this.sustitucionRepository = sustitucionRepository;
        this.opcionRepository = opcionRepository;
    }

    @Transactional(readOnly = true)
    public InformeSustitucionesHistorialDTO generar(
            String desdeTexto,
            String hastaTexto,
            String busqueda,
            String estadoTexto,
            String motivoTexto,
            String disparadorTexto,
            int pagina,
            int tamanio) {
        LocalDate hasta = fechaDe(hastaTexto, LocalDate.now());
        LocalDate desde = fechaDe(desdeTexto,
            hasta.minusDays(DIAS_POR_DEFECTO - 1L));
        long dias = validarPeriodo(desde, hasta);

        EstadoSustitucion estado = estadoDe(estadoTexto);
        MotivoSustitucion motivo = motivoDe(motivoTexto);
        DisparadorSustitucion disparador = disparadorDe(disparadorTexto);
        String patron = busqueda == null
            ? "" : busqueda.trim().toLowerCase(Locale.ROOT);

        int paginaSegura = Math.max(0, pagina);
        int tamanioSeguro = Math.min(Math.max(1, tamanio), FILAS_PDF);
        Pageable pageable = PageRequest.of(paginaSegura, tamanioSeguro,
            Sort.by(Sort.Direction.DESC, "fechaCreacion")
                .and(Sort.by(Sort.Direction.DESC, "idSustitucion")));

        LocalDateTime inicio = desde.atStartOfDay();
        LocalDateTime fin = hasta.plusDays(1).atStartOfDay();

        Page<Sustitucion> page = sustitucionRepository.buscarHistorial(
            inicio, fin, estado, motivo, disparador, patron, pageable);
        List<SustitucionHistorialDTO> eventos = page.getContent().stream()
            .map(this::aDto)
            .toList();

        return new InformeSustitucionesHistorialDTO(
            desde,
            hasta,
            dias,
            filtro(patron, estado, motivo, disparador),
            LocalDateTime.now(),
            resumen(inicio, fin, estado, motivo, disparador, patron),
            eventos,
            page.getNumber(),
            page.getSize(),
            page.getTotalElements(),
            page.getTotalPages(),
            page.hasNext());
    }

    @Transactional(readOnly = true)
    public SustitucionDetalleHistorialDTO detalle(Long idSustitucion) {
        Sustitucion sustitucion = sustitucionRepository
            .findByIdConDetalle(idSustitucion)
            .orElseThrow(() -> new NoSuchElementException(
                "Evento de sustitución no encontrado: " + idSustitucion));
        List<SustitucionOpcion> opciones = opcionRepository
            .findHistorialPorSustitucion(idSustitucion);
        return new SustitucionDetalleHistorialDTO(
            aDto(sustitucion),
            opciones.stream().map(this::aDtoOpcion).toList());
    }

    private ResumenSustitucionesHistorialDTO resumen(
            LocalDateTime inicio,
            LocalDateTime fin,
            EstadoSustitucion estado,
            MotivoSustitucion motivo,
            DisparadorSustitucion disparador,
            String patron) {
        List<Object[]> filas = sustitucionRepository.agruparHistorial(
            inicio, fin, estado, motivo, disparador, patron);

        Map<EstadoSustitucion, Long> porEstado =
            new EnumMap<>(EstadoSustitucion.class);
        Map<MotivoSustitucion, Long> porMotivo =
            new EnumMap<>(MotivoSustitucion.class);
        long total = 0;
        long opciones = 0;
        long anonimas = 0;

        for (Object[] fila : filas) {
            if (fila == null || fila.length < 5) {
                continue;
            }
            EstadoSustitucion est = (EstadoSustitucion) fila[0];
            MotivoSustitucion mot = (MotivoSustitucion) fila[1];
            long cantidad = entero(fila, 2);
            porEstado.merge(est, cantidad, Long::sum);
            if (mot != null) {
                porMotivo.merge(mot, cantidad, Long::sum);
            }
            total += cantidad;
            opciones += entero(fila, 3);
            anonimas += entero(fila, 4);
        }

        long alternativa = porEstado.getOrDefault(
            EstadoSustitucion.RESUELTA_ALTERNATIVA, 0L);
        long cola = porEstado.getOrDefault(
            EstadoSustitucion.RESUELTA_COLA, 0L);
        long resueltas = alternativa + cola;
        BigDecimal tasa = resueltas == 0
            ? BigDecimal.ZERO.setScale(2)
            : BigDecimal.valueOf(alternativa * 100.0 / resueltas)
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal promedio = total == 0
            ? BigDecimal.ZERO.setScale(2)
            : BigDecimal.valueOf(opciones)
                .divide(BigDecimal.valueOf(total), 2, RoundingMode.HALF_UP);

        List<Map.Entry<MotivoSustitucion, Long>> ordenados =
            new ArrayList<>(porMotivo.entrySet());
        ordenados.sort((a, b) -> Long.compare(b.getValue(), a.getValue()));
        List<SustitucionConteoDTO> motivos = ordenados.stream()
            .map(entry -> new SustitucionConteoDTO(
                entry.getKey().name(),
                etiquetaMotivo(entry.getKey()),
                entry.getValue()))
            .toList();

        return new ResumenSustitucionesHistorialDTO(
            total,
            alternativa,
            cola,
            porEstado.getOrDefault(
                EstadoSustitucion.ALTERNATIVA_ELEGIDA, 0L),
            porEstado.getOrDefault(EstadoSustitucion.DESCARTADA, 0L),
            porEstado.getOrDefault(EstadoSustitucion.ABIERTA, 0L),
            porEstado.getOrDefault(EstadoSustitucion.EXPIRADA, 0L),
            anonimas,
            tasa,
            promedio,
            motivos);
    }

    private SustitucionHistorialDTO aDto(Sustitucion sustitucion) {
        EstadoSustitucion estado = sustitucion.getEstado();
        MotivoSustitucion motivo = sustitucion.getMotivo();
        DisparadorSustitucion disparador = sustitucion.getDisparador();
        var cliente = sustitucion.getCliente();
        var origen = sustitucion.getServicioOrigen();
        var artistaOrigen = sustitucion.getArtistaOrigen();
        var elegido = sustitucion.getServicioElegido();

        return new SustitucionHistorialDTO(
            sustitucion.getIdSustitucion(),
            sustitucion.getFechaCreacion(),
            sustitucion.getFechaResolucion(),
            estado != null ? estado.name() : null,
            etiquetaEstado(estado),
            motivo != null ? motivo.name() : null,
            etiquetaMotivo(motivo),
            disparador != null ? disparador.name() : null,
            etiquetaDisparador(disparador),
            cliente != null ? cliente.getIdUsuario() : null,
            cliente != null ? cliente.getNombreUsuario() : null,
            cliente != null ? cliente.getNombreDisplay() : null,
            origen != null ? origen.getIdServicio() : null,
            origen != null ? origen.getNombre() : null,
            artistaOrigen != null ? artistaOrigen.getIdUsuario() : null,
            artistaOrigen != null ? artistaOrigen.getNombreUsuario() : null,
            sustitucion.getCantidadOpciones(),
            elegido != null ? elegido.getIdServicio() : null,
            elegido != null ? elegido.getNombre() : null,
            sustitucion.getSolicitud() != null
                ? sustitucion.getSolicitud().getIdSolicitud() : null,
            sustitucion.getPedido() != null
                ? sustitucion.getPedido().getIdPedido() : null,
            resultado(sustitucion));
    }

    private SustitucionOpcionHistorialDTO aDtoOpcion(
            SustitucionOpcion opcion) {
        return new SustitucionOpcionHistorialDTO(
            opcion.getIdSustitucionOpcion(),
            opcion.getOrden(),
            opcion.getServicio().getIdServicio(),
            opcion.getServicio().getNombre(),
            opcion.getArtista().getIdUsuario(),
            opcion.getArtista().getNombreUsuario(),
            opcion.getScore(),
            opcion.getMotivo(),
            opcion.getPrecioMomento(),
            opcion.getMoneda() != null
                ? opcion.getMoneda().getCodigo() : null,
            opcion.isElegida());
    }

    private static String resultado(Sustitucion sustitucion) {
        if (sustitucion.getEstado() == null) {
            return "Sin estado";
        }
        return switch (sustitucion.getEstado()) {
            case ABIERTA -> "Abierta, sin resolver";
            case ALTERNATIVA_ELEGIDA -> "Alternativa elegida";
            case RESUELTA_ALTERNATIVA -> sustitucion.getServicioElegido() != null
                ? "Alternativa: " + sustitucion.getServicioElegido().getNombre()
                : "Resuelta con alternativa";
            case RESUELTA_COLA -> "Quedó en lista de espera";
            case DESCARTADA -> "Descartada";
            case EXPIRADA -> "Expirada";
        };
    }

    private static String filtro(
            String patron,
            EstadoSustitucion estado,
            MotivoSustitucion motivo,
            DisparadorSustitucion disparador) {
        List<String> partes = new ArrayList<>();
        if (!patron.isBlank()) {
            partes.add("Búsqueda: \"" + patron + "\"");
        }
        if (estado != null) {
            partes.add("Estado: " + etiquetaEstado(estado));
        }
        if (motivo != null) {
            partes.add("Motivo: " + etiquetaMotivo(motivo));
        }
        if (disparador != null) {
            partes.add("Disparador: " + etiquetaDisparador(disparador));
        }
        return partes.isEmpty()
            ? "Todos los eventos"
            : String.join(" · ", partes);
    }

    private static String etiquetaEstado(EstadoSustitucion estado) {
        if (estado == null) {
            return "—";
        }
        return switch (estado) {
            case ABIERTA -> "Abierta";
            case ALTERNATIVA_ELEGIDA -> "Alternativa elegida";
            case RESUELTA_ALTERNATIVA -> "Resuelta con alternativa";
            case RESUELTA_COLA -> "Resuelta en cola";
            case DESCARTADA -> "Descartada";
            case EXPIRADA -> "Expirada";
        };
    }

    private static String etiquetaMotivo(MotivoSustitucion motivo) {
        if (motivo == null) {
            return "—";
        }
        return switch (motivo) {
            case SIN_CUPOS -> "Sin cupos";
            case SOLO_ESPERA -> "Sólo lista de espera";
            case CERRADA -> "Recepción cerrada";
            case PAUSADO -> "Servicio pausado";
            case ARCHIVADO -> "Servicio archivado";
            case SOLICITUD_RECHAZADA -> "Solicitud rechazada";
            case PRESUPUESTO_RETIRADO -> "Presupuesto retirado";
            case PRESUPUESTO_VENCIDO -> "Presupuesto vencido";
            case SERVICIO_DADO_DE_BAJA -> "Servicio dado de baja";
            case PEDIDO_IMPAGO -> "Pedido impago";
        };
    }

    private static String etiquetaDisparador(DisparadorSustitucion disparador) {
        if (disparador == null) {
            return "—";
        }
        return switch (disparador) {
            case DETALLE -> "Desde el detalle";
            case EXPLORAR -> "Desde Explorar";
            case PERFIL -> "Desde el perfil";
            case FAVORITOS -> "Desde favoritos";
            case INTENTO_SOLICITUD -> "Al intentar solicitar";
            case CIERRE -> "Al cerrarse la solicitud o el pedido";
        };
    }

    private static EstadoSustitucion estadoDe(String valor) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        try {
            return EstadoSustitucion.valueOf(
                valor.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Estado inválido: " + valor);
        }
    }

    private static MotivoSustitucion motivoDe(String valor) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        try {
            return MotivoSustitucion.valueOf(
                valor.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Motivo inválido: " + valor);
        }
    }

    private static DisparadorSustitucion disparadorDe(String valor) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        try {
            return DisparadorSustitucion.valueOf(
                valor.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                "Disparador inválido: " + valor);
        }
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

    private static long entero(Object[] fila, int indice) {
        if (fila == null || indice >= fila.length || fila[indice] == null) {
            return 0L;
        }
        return fila[indice] instanceof Number numero
            ? numero.longValue() : 0L;
    }
}
