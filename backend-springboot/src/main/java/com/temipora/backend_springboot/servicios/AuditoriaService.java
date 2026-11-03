package com.temipora.backend_springboot.servicios;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

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
import com.temipora.backend_springboot.modelo.EventoAuditoria;
import com.temipora.backend_springboot.modelo.EventoAuditoriaDTO;
import com.temipora.backend_springboot.modelo.OpcionesAuditoriaDTO;
import com.temipora.backend_springboot.modelo.PaginaDTO;
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

    @Transactional(readOnly = true)
    public PaginaDTO<EventoAuditoriaDTO> listar(
            String actor,
            String entidad,
            String accion,
            LocalDateTime desde,
            LocalDateTime hasta,
            int pagina,
            int tamanio) {
        Pageable pageable = PageRequest.of(
            Math.max(0, pagina),
            Math.min(Math.max(1, tamanio), TAMANIO_MAXIMO),
            Sort.by(Sort.Direction.DESC, "ocurridoEn").and(Sort.by(Sort.Direction.DESC, "idEventoAuditoria")));

        Specification<EventoAuditoria> filtro = (root, query, cb) -> {
            List<Predicate> predicados = new ArrayList<>();
            if (actor != null && !actor.isBlank()) {
                predicados.add(cb.like(cb.lower(root.<String>get("actorNombre")),
                    "%" + actor.trim().toLowerCase() + "%"));
            }
            if (entidad != null && !entidad.isBlank()) {
                predicados.add(cb.equal(root.get("entidad"), entidad.trim()));
            }
            if (accion != null && !accion.isBlank()) {
                predicados.add(cb.equal(root.get("accion"), accion.trim()));
            }
            if (desde != null) {
                predicados.add(cb.greaterThanOrEqualTo(root.<LocalDateTime>get("ocurridoEn"), desde));
            }
            if (hasta != null) {
                predicados.add(cb.lessThanOrEqualTo(root.<LocalDateTime>get("ocurridoEn"), hasta));
            }
            return cb.and(predicados.toArray(Predicate[]::new));
        };

        Page<EventoAuditoria> page = eventoRepository.findAll(filtro, pageable);
        return PaginaDTO.<EventoAuditoriaDTO>builder()
            .contenido(page.getContent().stream().map(this::aDto).toList())
            .pagina(page.getNumber())
            .tamanio(page.getSize())
            .totalElementos(page.getTotalElements())
            .totalPaginas(page.getTotalPages())
            .hayMas(page.hasNext())
            .build();
    }

    @Transactional(readOnly = true)
    public OpcionesAuditoriaDTO opciones() {
        return new OpcionesAuditoriaDTO(
            eventoRepository.entidadesRegistradas(),
            eventoRepository.accionesRegistradas());
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

    private EventoAuditoriaDTO aDto(EventoAuditoria evento) {
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
            evento.getResultado());
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
