package com.temipora.backend_springboot.servicios;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import com.temipora.backend_springboot.modelo.AccionReporteResenia;
import com.temipora.backend_springboot.modelo.ERol;
import com.temipora.backend_springboot.modelo.EstadoPedido;
import com.temipora.backend_springboot.modelo.EstadoReporteResenia;
import com.temipora.backend_springboot.modelo.EstadoResenia;
import com.temipora.backend_springboot.modelo.MotivoReporteResenia;
import com.temipora.backend_springboot.modelo.PaginaDTO;
import com.temipora.backend_springboot.modelo.Pedido;
import com.temipora.backend_springboot.modelo.ReporteResenia;
import com.temipora.backend_springboot.modelo.ReporteReseniaDTO;
import com.temipora.backend_springboot.modelo.ReporteReseniaHistorialDTO;
import com.temipora.backend_springboot.modelo.ReporteReseniaRequest;
import com.temipora.backend_springboot.modelo.Resenia;
import com.temipora.backend_springboot.modelo.ReseniaOpcion;
import com.temipora.backend_springboot.modelo.ReseniaOpcionDTO;
import com.temipora.backend_springboot.modelo.ReseniaReportadaDTO;
import com.temipora.backend_springboot.modelo.ReseniaValor;
import com.temipora.backend_springboot.modelo.ReseniaValorDTO;
import com.temipora.backend_springboot.modelo.ReseniaVersion;
import com.temipora.backend_springboot.modelo.Usuario;
import com.temipora.backend_springboot.modelo.UsuarioResumenDTO;
import com.temipora.backend_springboot.repositorios.ReporteReseniaRepository;
import com.temipora.backend_springboot.repositorios.ReseniaOpcionRepository;
import com.temipora.backend_springboot.repositorios.ReseniaRepository;
import com.temipora.backend_springboot.repositorios.ReseniaValorRepository;
import com.temipora.backend_springboot.repositorios.ReseniaVersionRepository;
import com.temipora.backend_springboot.repositorios.UsuarioRepository;

@Service
@Validated
public class ReseniaModeracionService {

    private static final int TAMANIO_MAXIMO = 50;
    private static final String MOTIVO_POR_DEFECTO = "No especificado";
    private static final String NOTIFICACION_REPORTADA = "RESENIA_REPORTADA";
    private static final String NOTIFICACION_OCULTA = "RESENIA_OCULTA";
    private static final String NOTIFICACION_RESTAURADA = "RESENIA_RESTAURADA";
    private static final String REFERENCIA_PEDIDO = "PEDIDO";
    private static final String REFERENCIA_RESENIA = "RESENIA";

    private final ReseniaRepository reseniaRepository;
    private final ReporteReseniaRepository reporteRepository;
    private final UsuarioRepository usuarioRepository;
    private final ReseniaVersionRepository versionRepository;
    private final ReseniaValorRepository valorRepository;
    private final ReseniaOpcionRepository opcionRepository;
    private final NotificacionService notificacionService;

    public ReseniaModeracionService(
            ReseniaRepository reseniaRepository,
            ReporteReseniaRepository reporteRepository,
            UsuarioRepository usuarioRepository,
            ReseniaVersionRepository versionRepository,
            ReseniaValorRepository valorRepository,
            ReseniaOpcionRepository opcionRepository,
            NotificacionService notificacionService) {
        this.reseniaRepository = reseniaRepository;
        this.reporteRepository = reporteRepository;
        this.usuarioRepository = usuarioRepository;
        this.versionRepository = versionRepository;
        this.valorRepository = valorRepository;
        this.opcionRepository = opcionRepository;
        this.notificacionService = notificacionService;
    }

    // -----------------------------------------------------------------
    // Reporte de usuarios
    // -----------------------------------------------------------------

    @Transactional
    public ReporteReseniaDTO reportar(
            Long idResenia, Long idUsuario, ReporteReseniaRequest request) {
        Resenia resenia = reseniaRepository.findById(idResenia)
            .orElseThrow(() -> new NoSuchElementException(
                "Reseña no encontrada: " + idResenia));

        if (resenia.getEstado() != EstadoResenia.PUBLICADA) {
            throw new IllegalArgumentException(
                "Solo podés reportar reseñas publicadas.");
        }
        if (resenia.getAutor().getIdUsuario().equals(idUsuario)) {
            throw new IllegalArgumentException(
                "No podés reportar tu propia reseña.");
        }
        if (!resenia.getAutor().getIdUsuario()
                .equals(resenia.getPedido().getCliente().getIdUsuario())
                || resenia.getPedido().getEstado() != EstadoPedido.FINALIZADO) {
            throw new IllegalArgumentException(
                "Solo podés reportar reseñas públicas de pedidos finalizados.");
        }
        if (reporteRepository
                .existsByReseniaIdReseniaAndReportanteIdUsuario(idResenia, idUsuario)) {
            throw new ConflictoException("Ya reportaste esta reseña.");
        }

        Usuario reportante = usuarioRepository.findByIdUsuario(idUsuario)
            .orElseThrow(() -> new NoSuchElementException(
                "Usuario no encontrado: " + idUsuario));

        long pendientesPrevios =
            reporteRepository.countByReseniaIdReseniaAndEstado(
                idResenia, EstadoReporteResenia.PENDIENTE);

        ReporteResenia reporte = new ReporteResenia();
        reporte.setResenia(resenia);
        reporte.setReportante(reportante);
        reporte.setMotivo(request.getMotivo());
        reporte.setComentario(normalizarTexto(request.getComentario()));
        reporte.setEstado(EstadoReporteResenia.PENDIENTE);
        reporte.setFechaCreacion(LocalDateTime.now());

        ReporteResenia guardado = reporteRepository.save(reporte);

        if (pendientesPrevios == 0) {
            notificarAdministradores(resenia, request.getMotivo());
        }

        return ReporteReseniaDTO.desde(guardado);
    }

    @Transactional(readOnly = true)
    public List<Long> reportesDelUsuario(Long idUsuario) {
        return reporteRepository.findIdsReseniasReportadasPorUsuario(idUsuario);
    }

    // -----------------------------------------------------------------
    // Cola de moderación
    // -----------------------------------------------------------------

    @Transactional(readOnly = true)
    public PaginaDTO<ReseniaReportadaDTO> listarReportadas(
            int pagina, int tamanio) {
        Page<Resenia> page = reseniaRepository.findConReportesPendientes(
            EstadoReporteResenia.PENDIENTE, pagina(pagina, tamanio));
        return aPagina(page);
    }

    @Transactional(readOnly = true)
    public PaginaDTO<ReseniaReportadaDTO> listarOcultas(
            int pagina, int tamanio) {
        Page<Resenia> page = reseniaRepository.findPorEstado(
            EstadoResenia.OCULTA, pagina(pagina, tamanio));
        return aPagina(page);
    }

    @Transactional(readOnly = true)
    public PaginaDTO<ReporteReseniaHistorialDTO> listarHistorial(
            EstadoReporteResenia estado, int pagina, int tamanio) {
        Page<ReporteResenia> page = reporteRepository.findHistorial(
            estado, pagina(pagina, tamanio));
        return PaginaDTO.<ReporteReseniaHistorialDTO>builder()
            .contenido(page.getContent().stream()
                .map(this::aHistorial)
                .toList())
            .pagina(page.getNumber())
            .tamanio(page.getSize())
            .totalElementos(page.getTotalElements())
            .totalPaginas(page.getTotalPages())
            .hayMas(page.hasNext())
            .build();
    }

    private ReporteReseniaHistorialDTO aHistorial(ReporteResenia reporte) {
        Resenia resenia = reporte.getResenia();
        return ReporteReseniaHistorialDTO.builder()
            .idReporte(reporte.getIdReporte())
            .idResenia(resenia.getIdResenia())
            .idPedido(resenia.getPedido().getIdPedido())
            .nombreServicio(resenia.getPedido().getServicio() != null
                ? resenia.getPedido().getServicio().getNombre()
                : null)
            .autorResenia(resenia.getAutor() != null
                ? resenia.getAutor().getNombreUsuario()
                : null)
            .reseniaAnonima(!resenia.isMostrarNombre())
            .estadoResenia(resenia.getEstado())
            .motivo(reporte.getMotivo())
            .etiquetaMotivo(reporte.getMotivo() != null
                ? reporte.getMotivo().getEtiqueta()
                : null)
            .comentario(reporte.getComentario())
            .estado(reporte.getEstado())
            .accion(reporte.getAccion())
            .fechaCreacion(reporte.getFechaCreacion())
            .fechaResolucion(reporte.getFechaResolucion())
            .reportante(UsuarioResumenDTO.desdeUsuario(reporte.getReportante()))
            .resueltoPor(UsuarioResumenDTO.desdeUsuario(reporte.getResueltoPor()))
            .build();
    }

    @Transactional
    public ReseniaReportadaDTO ocultar(
            Long idResenia, Long idAdmin, String motivo) {
        Resenia resenia = reseniaRepository.findForUpdateById(idResenia)
            .orElseThrow(() -> new NoSuchElementException(
                "Reseña no encontrada: " + idResenia));
        if (resenia.getEstado() == EstadoResenia.ELIMINADA) {
            throw new IllegalArgumentException(
                "No se puede ocultar una reseña eliminada.");
        }

        Usuario admin = obtenerAdmin(idAdmin);
        boolean yaEstabaOculta = resenia.getEstado() == EstadoResenia.OCULTA;
        LocalDateTime ahora = LocalDateTime.now();

        resenia.setEstado(EstadoResenia.OCULTA);
        resenia.setModeradaEn(ahora);
        resenia.setMotivoModeracion(normalizarMotivo(motivo));
        reseniaRepository.save(resenia);

        resolverPendientes(
            resenia, admin, EstadoReporteResenia.RESUELTO,
            AccionReporteResenia.OCULTAR, ahora);

        if (!yaEstabaOculta) {
            notificarAutor(resenia, NOTIFICACION_OCULTA,
                "Tu reseña fue oculta",
                "Tu reseña de " + nombreServicio(resenia)
                    + " fue oculta por moderación. Motivo: "
                    + resenia.getMotivoModeracion() + ".");
        }

        return aDTOs(List.of(resenia)).get(0);
    }

    @Transactional
    public ReseniaReportadaDTO descartarReportes(
            Long idResenia, Long idAdmin) {
        Resenia resenia = reseniaRepository.findForUpdateById(idResenia)
            .orElseThrow(() -> new NoSuchElementException(
                "Reseña no encontrada: " + idResenia));

        List<ReporteResenia> pendientes = reporteRepository
            .findByReseniaIdReseniaAndEstadoOrderByFechaCreacionAsc(
                idResenia, EstadoReporteResenia.PENDIENTE);
        if (pendientes.isEmpty()) {
            throw new IllegalArgumentException(
                "No hay reportes pendientes para esta reseña.");
        }

        Usuario admin = obtenerAdmin(idAdmin);
        LocalDateTime ahora = LocalDateTime.now();
        for (ReporteResenia reporte : pendientes) {
            reporte.setEstado(EstadoReporteResenia.DESCARTADO);
            reporte.setAccion(AccionReporteResenia.DESCARTAR);
            reporte.setResueltoPor(admin);
            reporte.setFechaResolucion(ahora);
        }
        reporteRepository.saveAll(pendientes);

        return aDTOs(List.of(resenia)).get(0);
    }

    @Transactional
    public ReseniaReportadaDTO restaurar(Long idResenia, Long idAdmin) {
        Resenia resenia = reseniaRepository.findForUpdateById(idResenia)
            .orElseThrow(() -> new NoSuchElementException(
                "Reseña no encontrada: " + idResenia));
        if (resenia.getEstado() != EstadoResenia.OCULTA) {
            throw new IllegalArgumentException("La reseña no está oculta.");
        }

        obtenerAdmin(idAdmin);
        resenia.setEstado(EstadoResenia.PUBLICADA);
        resenia.setModeradaEn(null);
        resenia.setMotivoModeracion(null);
        reseniaRepository.save(resenia);

        notificarAutor(resenia, NOTIFICACION_RESTAURADA,
            "Tu reseña volvió a publicarse",
            "Tu reseña de " + nombreServicio(resenia)
                + " volvió a publicarse.");

        return aDTOs(List.of(resenia)).get(0);
    }

    // -----------------------------------------------------------------
    // armado de la respuesta
    // -----------------------------------------------------------------

    private PaginaDTO<ReseniaReportadaDTO> aPagina(Page<Resenia> page) {
        return PaginaDTO.<ReseniaReportadaDTO>builder()
            .contenido(aDTOs(page.getContent()))
            .pagina(page.getNumber())
            .tamanio(page.getSize())
            .totalElementos(page.getTotalElements())
            .totalPaginas(page.getTotalPages())
            .hayMas(page.hasNext())
            .build();
    }

    private List<ReseniaReportadaDTO> aDTOs(List<Resenia> resenias) {
        if (resenias.isEmpty()) {
            return List.of();
        }

        List<Long> ids = resenias.stream()
            .map(Resenia::getIdResenia)
            .toList();
        Map<Long, ReseniaVersion> versiones = versionesActuales(resenias);
        Map<Long, List<ReseniaValor>> valores = valoresPorVersion(
            versiones.values());
        Map<Long, List<ReseniaOpcion>> opciones = opcionesPorVersion(
            versiones.values());
        Map<Long, List<ReporteResenia>> reportes = reporteRepository
            .findByReseniaIdReseniaInOrderByFechaCreacionAsc(ids).stream()
            .collect(Collectors.groupingBy(
                reporte -> reporte.getResenia().getIdResenia()));

        return resenias.stream()
            .map(resenia -> aDTO(
                resenia,
                versiones.get(resenia.getIdResenia()),
                valores,
                opciones,
                reportes.getOrDefault(resenia.getIdResenia(), List.of())))
            .toList();
    }

    private ReseniaReportadaDTO aDTO(
            Resenia resenia,
            ReseniaVersion version,
            Map<Long, List<ReseniaValor>> valores,
            Map<Long, List<ReseniaOpcion>> opciones,
            List<ReporteResenia> reportes) {
        Long idVersion = version != null ? version.getIdVersion() : null;
        List<ReporteResenia> reportesResenia = reportes != null
            ? reportes
            : List.of();
        long pendientes = reportesResenia.stream()
            .filter(reporte -> reporte.getEstado() == EstadoReporteResenia.PENDIENTE)
            .count();

        return ReseniaReportadaDTO.builder()
            .idResenia(resenia.getIdResenia())
            .idPedido(resenia.getPedido().getIdPedido())
            .nombreServicio(resenia.getPedido().getServicio() != null
                ? resenia.getPedido().getServicio().getNombre()
                : null)
            .autor(UsuarioResumenDTO.desdeUsuario(resenia.getAutor()))
            .mostrarNombre(resenia.isMostrarNombre())
            .estado(resenia.getEstado())
            .fechaCreacion(resenia.getFechaCreacion())
            .fechaVersion(version != null ? version.getFechaVersion() : null)
            .calificacionGeneral(version != null
                ? version.getCalificacionGeneral()
                : null)
            .descripcion(version != null ? version.getDescripcion() : null)
            .valores(idVersion != null
                ? valores.getOrDefault(idVersion, List.of()).stream()
                    .map(ReseniaValorDTO::desdeValor).toList()
                : List.of())
            .opciones(idVersion != null
                ? opciones.getOrDefault(idVersion, List.of()).stream()
                    .map(ReseniaOpcionDTO::desdeOpcion).toList()
                : List.of())
            .moderadaEn(resenia.getModeradaEn())
            .motivoModeracion(resenia.getMotivoModeracion())
            .reportesTotales(reportesResenia.size())
            .reportesPendientes(pendientes)
            .reportes(reportesResenia.stream()
                .map(ReporteReseniaDTO::desde)
                .toList())
            .build();
    }

    private Map<Long, ReseniaVersion> versionesActuales(
            Collection<Resenia> resenias) {
        if (resenias.isEmpty()) {
            return Map.of();
        }
        List<Long> ids = resenias.stream()
            .map(Resenia::getIdResenia)
            .toList();
        return versionRepository.findActualesPorReseniaIds(ids).stream()
            .collect(Collectors.toMap(
                version -> version.getResenia().getIdResenia(),
                version -> version));
    }

    private Map<Long, List<ReseniaValor>> valoresPorVersion(
            Collection<ReseniaVersion> versiones) {
        if (versiones.isEmpty()) {
            return Map.of();
        }
        List<Long> ids = versiones.stream()
            .map(ReseniaVersion::getIdVersion)
            .toList();
        return valorRepository.findByVersionIdVersionIn(ids).stream()
            .collect(Collectors.groupingBy(
                valor -> valor.getVersion().getIdVersion()));
    }

    private Map<Long, List<ReseniaOpcion>> opcionesPorVersion(
            Collection<ReseniaVersion> versiones) {
        if (versiones.isEmpty()) {
            return Map.of();
        }
        List<Long> ids = versiones.stream()
            .map(ReseniaVersion::getIdVersion)
            .toList();
        return opcionRepository.findByVersionIdVersionIn(ids).stream()
            .collect(Collectors.groupingBy(
                opcion -> opcion.getVersion().getIdVersion()));
    }

    // -----------------------------------------------------------------
    // notificaciones y helpers
    // -----------------------------------------------------------------

    private void notificarAdministradores(
            Resenia resenia, MotivoReporteResenia motivo) {
        String etiquetaMotivo = motivo != null
            ? motivo.getEtiqueta()
            : "otro motivo";
        List<Usuario> admins = usuarioRepository.findByRol_Nombre(ERol.ADMIN);
        for (Usuario admin : admins) {
            if (admin.getIdUsuario().equals(resenia.getAutor().getIdUsuario())) {
                continue;
            }
            notificacionService.crearParaUsuario(
                admin.getIdUsuario(),
                NOTIFICACION_REPORTADA,
                "Nueva reseña reportada",
                "Se reportó una reseña de " + nombreServicio(resenia)
                    + ". Motivo: " + etiquetaMotivo + ".",
                resenia.getIdResenia(),
                REFERENCIA_RESENIA);
        }
    }

    private void notificarAutor(
            Resenia resenia, String codigo, String nombre, String descripcion) {
        notificacionService.crearParaUsuario(
            resenia.getAutor().getIdUsuario(),
            codigo,
            nombre,
            descripcion,
            resenia.getPedido().getIdPedido(),
            REFERENCIA_PEDIDO);
    }

    private void resolverPendientes(
            Resenia resenia,
            Usuario admin,
            EstadoReporteResenia estado,
            AccionReporteResenia accion,
            LocalDateTime ahora) {
        List<ReporteResenia> pendientes = reporteRepository
            .findByReseniaIdReseniaAndEstadoOrderByFechaCreacionAsc(
                resenia.getIdResenia(), EstadoReporteResenia.PENDIENTE);
        for (ReporteResenia reporte : pendientes) {
            reporte.setEstado(estado);
            reporte.setAccion(accion);
            reporte.setResueltoPor(admin);
            reporte.setFechaResolucion(ahora);
        }
        if (!pendientes.isEmpty()) {
            reporteRepository.saveAll(pendientes);
        }
    }

    private Usuario obtenerAdmin(Long idAdmin) {
        return usuarioRepository.findByIdUsuario(idAdmin)
            .orElseThrow(() -> new NoSuchElementException(
                "Administrador no encontrado: " + idAdmin));
    }

    private String nombreServicio(Resenia resenia) {
        Pedido pedido = resenia.getPedido();
        if (pedido == null
                || pedido.getServicio() == null
                || pedido.getServicio().getNombre() == null
                || pedido.getServicio().getNombre().isBlank()) {
            return "un servicio";
        }
        return "'" + pedido.getServicio().getNombre() + "'";
    }

    private String normalizarMotivo(String motivo) {
        String limpio = normalizarTexto(motivo);
        return limpio != null ? limpio : MOTIVO_POR_DEFECTO;
    }

    private String normalizarTexto(String valor) {
        if (valor == null) {
            return null;
        }
        String limpio = valor.trim();
        return limpio.isEmpty() ? null : limpio;
    }

    private Pageable pagina(int pagina, int tamanio) {
        return PageRequest.of(
            Math.max(0, pagina),
            Math.min(Math.max(1, tamanio), TAMANIO_MAXIMO));
    }
}
