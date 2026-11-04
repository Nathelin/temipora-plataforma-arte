package com.temipora.backend_springboot.servicios;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.temipora.backend_springboot.config.AuditoriaRequestFilter;
import com.temipora.backend_springboot.modelo.CambioAuditoriaDTO;
import com.temipora.backend_springboot.modelo.CategoriaAuditoriaDTO;
import com.temipora.backend_springboot.modelo.EventoAuditoria;
import com.temipora.backend_springboot.modelo.EventoAuditoriaDTO;
import com.temipora.backend_springboot.modelo.FiltroAuditoria;
import com.temipora.backend_springboot.modelo.OpcionesAuditoriaDTO;
import com.temipora.backend_springboot.modelo.PaginaDTO;
import com.temipora.backend_springboot.modelo.ResumenAuditoriaDTO;
import com.temipora.backend_springboot.repositorios.EventoAuditoriaRepository;
import com.temipora.backend_springboot.repositorios.UsuarioRepository;

import jakarta.persistence.criteria.Predicate;
import jakarta.servlet.http.HttpServletRequest;
import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

@Service
public class AuditoriaService {

    private static final Logger log = LoggerFactory.getLogger(AuditoriaService.class);
    private static final int TAMANIO_MAXIMO = 100;
    private static final int RESUMEN_MAXIMO = 500;
    private static final int MOTIVO_MAXIMO = 500;

    private final EventoAuditoriaRepository eventoRepository;
    private final UsuarioRepository usuarioRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public AuditoriaService(
            EventoAuditoriaRepository eventoRepository,
            UsuarioRepository usuarioRepository) {
        this.eventoRepository = eventoRepository;
        this.usuarioRepository = usuarioRepository;
    }

    /**
     * Fail-closed: se ejecuta dentro de la transacción del llamador, así que si el
     * registro falla también se revierte la acción auditada.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void registrar(
            String accion,
            String entidad,
            Object entidadId,
            String resumen,
            Snapshot antes,
            Object despues,
            List<CampoAuditado> campos) {
        List<CambioAuditoriaDTO> cambios = new ArrayList<>();
        if (campos != null) {
            for (CampoAuditado campo : campos) {
                Object anterior = antes != null ? antes.get(campo.campo()) : null;
                Object nuevo = despues != null ? Snapshot.valorLegible(Snapshot.valorDe(despues, campo.campo())) : null;
                if (Objects.equals(anterior, nuevo)) {
                    continue;
                }
                cambios.add(new CambioAuditoriaDTO(
                    campo.campo(), campo.etiqueta(), texto(anterior), texto(nuevo)));
            }
        }
        guardar(accion, entidad, entidadId, resumen, cambios, null);
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void registrar(String accion, String entidad, Object entidadId, String resumen, String motivo) {
        guardar(accion, entidad, entidadId, resumen, List.of(), motivo);
    }

    /**
     * Para tareas de sistema que no corren dentro de una transacción de negocio.
     * Fail-open: si la auditoría falla, la tarea ya ejecutada no se revierte.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void registrarSistema(String accion, String entidad, Object entidadId, String resumen) {
        try {
            guardar(accion, entidad, entidadId, resumen, List.of(), null);
        } catch (RuntimeException e) {
            log.warn("No se pudo registrar la auditoría de {}: {}", accion, e.getMessage());
        }
    }

    /**
     * Login fallido: no hay sesión, pero guardamos el usuario intentado en
     * actorNombre para poder agrupar intentos por cuenta.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void registrarIntentoFallido(String usuario) {
        try {
            String intento = usuario != null && !usuario.isBlank()
                ? usuario.trim()
                : null;

            LocalDateTime ocurridoEn = LocalDateTime.now();
            EventoAuditoria evento = new EventoAuditoria();
            evento.setOcurridoEn(ocurridoEn);
            evento.setAccion(AccionesAdmin.LOGIN_FALLIDO);
            evento.setEntidad("Usuario");
            evento.setResumen(recortar(intento == null
                ? "Intento de inicio de sesión fallido (sin usuario)"
                : "Intento de inicio de sesión fallido para '" + intento + "'",
                RESUMEN_MAXIMO));
            evento.setCambios(serializar(List.of()));
            evento.setResultado("OK");
            evento.setOrigen("SISTEMA");
            evento.setActorNombre(intento != null
                ? recortar(intento, 60)
                : null);
            completarRequest(evento);
            eventoRepository.save(evento);
        } catch (RuntimeException e) {
            log.warn("No se pudo registrar el login fallido: {}", e.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public PaginaDTO<EventoAuditoriaDTO> listar(
            FiltroAuditoria filtro,
            int pagina,
            int tamanio) {
        Pageable pageable = PageRequest.of(
            Math.max(0, pagina),
            Math.min(Math.max(1, tamanio), TAMANIO_MAXIMO),
            Sort.by(Sort.Direction.DESC, "ocurridoEn").and(Sort.by(Sort.Direction.DESC, "idEventoAuditoria")));

        Specification<EventoAuditoria> especificacion = (root, query, cb) -> {
            List<Predicate> predicados = new ArrayList<>();
            if (texto(filtro.actor())) {
                predicados.add(cb.like(cb.lower(root.<String>get("actorNombre")),
                    "%" + filtro.actor().trim().toLowerCase() + "%"));
            }
            if (texto(filtro.origen())) {
                predicados.add(cb.equal(root.get("origen"),
                    filtro.origen().trim().toUpperCase()));
            }
            if (texto(filtro.entidad())) {
                predicados.add(cb.equal(root.get("entidad"), filtro.entidad().trim()));
            }
            if (texto(filtro.entidadId())) {
                predicados.add(cb.like(cb.lower(root.<String>get("entidadId")),
                    "%" + filtro.entidadId().trim().toLowerCase() + "%"));
            }
            if (texto(filtro.accion())) {
                predicados.add(cb.equal(root.get("accion"), filtro.accion().trim()));
            }
            if (texto(filtro.requestId())) {
                predicados.add(cb.equal(root.get("requestId"), filtro.requestId().trim()));
            }
            if (texto(filtro.texto())) {
                String patron = "%" + filtro.texto().trim().toLowerCase() + "%";
                predicados.add(cb.or(
                    cb.like(cb.lower(root.<String>get("actorNombre")), patron),
                    cb.like(cb.lower(root.<String>get("resumen")), patron),
                    cb.like(cb.lower(root.<String>get("entidadId")), patron)));
            }
            if (texto(filtro.categoria())) {
                var acciones = CatalogoAuditoria.accionesDe(filtro.categoria());
                predicados.add(acciones.isEmpty()
                    ? cb.disjunction()
                    : root.get("accion").in(acciones));
            }
            if (Boolean.TRUE.equals(filtro.critica())) {
                predicados.add(root.get("accion")
                    .in(CatalogoAuditoria.accionesCriticas()));
            }
            if (filtro.desde() != null) {
                predicados.add(cb.greaterThanOrEqualTo(
                    root.<LocalDateTime>get("ocurridoEn"), filtro.desde()));
            }
            if (filtro.hasta() != null) {
                predicados.add(cb.lessThanOrEqualTo(
                    root.<LocalDateTime>get("ocurridoEn"), filtro.hasta()));
            }
            return cb.and(predicados.toArray(Predicate[]::new));
        };

        Page<EventoAuditoria> page = eventoRepository.findAll(especificacion, pageable);
        Map<String, Long> operaciones = contarOperaciones(page.getContent());
        return PaginaDTO.<EventoAuditoriaDTO>builder()
            .contenido(page.getContent().stream()
                .map(evento -> aDto(evento, operacionDe(evento, operaciones)))
                .toList())
            .pagina(page.getNumber())
            .tamanio(page.getSize())
            .totalElementos(page.getTotalElements())
            .totalPaginas(page.getTotalPages())
            .hayMas(page.hasNext())
            .build();
    }

    @Transactional(readOnly = true)
    public EventoAuditoriaDTO obtener(Long idEvento) {
        EventoAuditoria evento = eventoRepository.findById(idEvento)
            .orElseThrow(() -> new NoSuchElementException(
                "Evento de auditoría no encontrado: " + idEvento));
        long operacion = evento.getRequestId() != null
            ? eventoRepository.countByRequestId(evento.getRequestId())
            : 1L;
        return aDto(evento, operacion);
    }

    private Map<String, Long> contarOperaciones(List<EventoAuditoria> eventos) {
        Set<String> requestIds = eventos.stream()
            .map(EventoAuditoria::getRequestId)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());
        if (requestIds.isEmpty()) {
            return Map.of();
        }
        return eventoRepository.contarPorRequestIds(requestIds).stream()
            .collect(Collectors.toMap(
                fila -> (String) fila[0],
                fila -> ((Number) fila[1]).longValue()));
    }

    private long operacionDe(
            EventoAuditoria evento, Map<String, Long> operaciones) {
        return evento.getRequestId() != null
            ? operaciones.getOrDefault(evento.getRequestId(), 1L)
            : 1L;
    }

    @Transactional(readOnly = true)
    public OpcionesAuditoriaDTO opciones() {
        return new OpcionesAuditoriaDTO(
            eventoRepository.entidadesRegistradas(),
            eventoRepository.accionesRegistradas(),
            CatalogoAuditoria.categorias().stream()
                .map(categoria -> new CategoriaAuditoriaDTO(
                    categoria.name(), categoria.getEtiqueta()))
                .toList());
    }

    @Transactional(readOnly = true)
    public ResumenAuditoriaDTO resumen(
            LocalDateTime desde, LocalDateTime hasta) {
        return new ResumenAuditoriaDTO(
            eventoRepository.countByOcurridoEnBetween(desde, hasta),
            eventoRepository.countByAccionInAndOcurridoEnBetween(
                CatalogoAuditoria.accionesCriticas(), desde, hasta),
            eventoRepository.countByAccionAndOcurridoEnBetween(
                AccionesAdmin.LOGIN_FALLIDO, desde, hasta));
    }

    private boolean texto(String valor) {
        return valor != null && !valor.isBlank();
    }

    private void guardar(
            String accion,
            String entidad,
            Object entidadId,
            String resumen,
            List<CambioAuditoriaDTO> cambios,
            String motivo) {
        EventoAuditoria evento = new EventoAuditoria();
        evento.setOcurridoEn(LocalDateTime.now());
        evento.setAccion(accion);
        evento.setEntidad(entidad);
        evento.setEntidadId(entidadId != null ? String.valueOf(entidadId) : null);
        evento.setResumen(recortar(resumen, RESUMEN_MAXIMO));
        evento.setCambios(serializar(cambios));
        evento.setMotivo(recortar(motivo, MOTIVO_MAXIMO));
        evento.setResultado("OK");
        completarActor(evento);
        completarRequest(evento);
        eventoRepository.save(evento);
    }

    private void completarActor(EventoAuditoria evento) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getName())) {
            evento.setOrigen("SISTEMA");
            return;
        }
        evento.setActorNombre(recortar(auth.getName(), 60));
        String rol = auth.getAuthorities().stream()
            .map(a -> a.getAuthority())
            .filter(a -> a.startsWith("ROLE_"))
            .map(a -> a.substring("ROLE_".length()))
            .findFirst()
            .orElse(null);
        evento.setActorRol(rol);
        evento.setOrigen(rol != null ? rol : "USUARIO");
        usuarioRepository.findByNombreUsuarioIgnoreCase(auth.getName())
            .ifPresent(usuario -> evento.setActorId(usuario.getIdUsuario()));
    }

    private void completarRequest(EventoAuditoria evento) {
        if (!(RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes atributos)) {
            return;
        }
        HttpServletRequest request = atributos.getRequest();
        Object requestId = request.getAttribute(AuditoriaRequestFilter.REQUEST_ID_ATTR);
        evento.setRequestId(requestId != null ? String.valueOf(requestId) : null);
    }

    private EventoAuditoriaDTO aDto(
            EventoAuditoria evento, long eventosOperacion) {
        return new EventoAuditoriaDTO(
            evento.getIdEventoAuditoria(),
            evento.getOcurridoEn(),
            evento.getActorId(),
            evento.getActorRol(),
            evento.getActorNombre(),
            evento.getAccion(),
            evento.getEntidad(),
            evento.getEntidadId(),
            evento.getResumen(),
            deserializar(evento.getCambios()),
            evento.getMotivo(),
            evento.getRequestId(),
            evento.getOrigen(),
            evento.getResultado(),
            CatalogoAuditoria.categoriaDe(evento.getAccion()).name(),
            CatalogoAuditoria.esCritica(evento.getAccion()),
            eventosOperacion);
    }

    private String serializar(List<CambioAuditoriaDTO> cambios) {
        try {
            return objectMapper.writeValueAsString(cambios);
        } catch (JacksonException e) {
            throw new IllegalStateException("No se pudo registrar la auditoría.", e);
        }
    }

    private List<CambioAuditoriaDTO> deserializar(String cambios) {
        if (cambios == null || cambios.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(cambios, new TypeReference<List<CambioAuditoriaDTO>>() { });
        } catch (JacksonException e) {
            return List.of();
        }
    }

    private String texto(Object valor) {
        return valor != null ? String.valueOf(valor) : null;
    }

    private String recortar(String valor, int maximo) {
        if (valor == null || valor.length() <= maximo) {
            return valor;
        }
        return valor.substring(0, maximo);
    }
}
