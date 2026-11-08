package com.temipora.backend_springboot.servicios;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.temipora.backend_springboot.modelo.EstadoSancion;
import com.temipora.backend_springboot.modelo.EstadoSancionDTO;
import com.temipora.backend_springboot.modelo.ERol;
import com.temipora.backend_springboot.modelo.PaginaDTO;
import com.temipora.backend_springboot.modelo.SancionCuenta;
import com.temipora.backend_springboot.modelo.SancionCuentaDTO;
import com.temipora.backend_springboot.modelo.SancionRequest;
import com.temipora.backend_springboot.modelo.TipoSancion;
import com.temipora.backend_springboot.modelo.Usuario;
import com.temipora.backend_springboot.repositorios.SancionCuentaRepository;
import com.temipora.backend_springboot.repositorios.UsuarioRepository;

import jakarta.persistence.criteria.Predicate;

@Service
public class SancionService {

    private static final String ENTIDAD = "SancionCuenta";
    private static final int TAMANIO_MAXIMO = 100;
    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private static final List<CampoAuditado> CAMPOS_APLICAR = List.of(
        CampoAuditado.de("tipo", "Tipo"),
        CampoAuditado.de("alcance", "Alcance"),
        CampoAuditado.de("motivo", "Motivo"),
        CampoAuditado.de("estado", "Estado"));

    private static final List<CampoAuditado> CAMPOS_REVERSION = List.of(
        CampoAuditado.de("estado", "Estado"),
        CampoAuditado.de("observacionReversion", "Observación"));

    private final SancionCuentaRepository sancionRepository;
    private final UsuarioRepository usuarioRepository;
    private final AuditoriaService auditoria;

    public SancionService(
            SancionCuentaRepository sancionRepository,
            UsuarioRepository usuarioRepository,
            AuditoriaService auditoria) {
        this.sancionRepository = sancionRepository;
        this.usuarioRepository = usuarioRepository;
        this.auditoria = auditoria;
    }

    @Transactional
    public SancionCuentaDTO aplicar(Long idAdmin, SancionRequest request) {
        Usuario admin = usuarioRepository.findByIdUsuario(idAdmin)
            .orElseThrow(() -> new NoSuchElementException("Administrador no encontrado: " + idAdmin));
        Usuario usuario = usuarioRepository.findByIdUsuario(request.getIdUsuario())
            .orElseThrow(() -> new NoSuchElementException("Usuario no encontrado: " + request.getIdUsuario()));

        if (usuario.getRol() != null && usuario.getRol().getNombre() == ERol.ADMIN) {
            throw new IllegalArgumentException("No se puede inhabilitar una cuenta de administrador.");
        }
        if (usuario.getIdUsuario().equals(admin.getIdUsuario())) {
            throw new IllegalArgumentException("No podés inhabilitar tu propia cuenta.");
        }

        LocalDateTime ahora = LocalDateTime.now();
        if (!sancionRepository.findVigentesByUsuarioId(
                usuario.getIdUsuario(), EstadoSancion.ACTIVA, ahora).isEmpty()) {
            throw new ConflictoException("La cuenta ya tiene una sanción activa.");
        }

        validarFechas(request, ahora);

        String motivo = request.getMotivo().trim();
        SancionCuenta sancion = SancionCuenta.builder()
            .usuario(usuario)
            .tipo(request.getTipo())
            .alcance(com.temipora.backend_springboot.modelo.AlcanceSancion.ACCESO)
            .motivo(motivo)
            .fechaInicio(ahora)
            .fechaFin(request.getFechaFin())
            .estado(EstadoSancion.ACTIVA)
            .adminAplico(admin)
            .build();

        SancionCuenta guardada = sancionRepository.save(sancion);

        auditoria.registrar(
            AccionesAdmin.CUENTA_SANCIONAR, ENTIDAD, guardada.getIdSancion(),
            resumenAplicacion(usuario.getNombreUsuario(), request.getTipo(), request.getFechaFin(), motivo),
            null, guardada, CAMPOS_APLICAR);

        return SancionCuentaDTO.desde(guardada, ahora);
    }

    @Transactional
    public SancionCuentaDTO revertir(Long idAdmin, Long idSancion, String observacion) {
        Usuario admin = usuarioRepository.findByIdUsuario(idAdmin)
            .orElseThrow(() -> new NoSuchElementException("Administrador no encontrado: " + idAdmin));
        SancionCuenta sancion = sancionRepository.findById(idSancion)
            .orElseThrow(() -> new NoSuchElementException("Sanción no encontrada: " + idSancion));

        LocalDateTime ahora = LocalDateTime.now();
        if (sancion.getEstado() != EstadoSancion.ACTIVA) {
            throw new IllegalArgumentException("La sanción no está activa.");
        }
        if (!sancion.estaVigente(ahora)) {
            throw new IllegalArgumentException("La sanción ya venció; la cuenta puede volver a iniciar sesión.");
        }

        Snapshot antes = Snapshot.de(sancion, CAMPOS_REVERSION);
        sancion.setEstado(EstadoSancion.REVERTIDA);
        sancion.setAdminRevertio(admin);
        sancion.setFechaReversion(ahora);
        sancion.setObservacionReversion(
            observacion != null && !observacion.isBlank() ? observacion.trim() : null);

        SancionCuenta guardada = sancionRepository.save(sancion);

        String resumen = "Revirtió la sanción de @"
            + (guardada.getUsuario() != null ? guardada.getUsuario().getNombreUsuario() : "usuario");
        auditoria.registrar(
            AccionesAdmin.CUENTA_REVERTIR_SANCION, ENTIDAD, guardada.getIdSancion(),
            resumen, antes, guardada, CAMPOS_REVERSION);

        return SancionCuentaDTO.desde(guardada, ahora);
    }

    @Transactional(readOnly = true)
    public PaginaDTO<SancionCuentaDTO> listar(
            Long idUsuario,
            String texto,
            TipoSancion tipo,
            String estadoEfectivo,
            int pagina,
            int tamanio) {

        LocalDateTime ahora = LocalDateTime.now();
        Pageable pageable = PageRequest.of(
            Math.max(0, pagina),
            Math.min(Math.max(1, tamanio), TAMANIO_MAXIMO),
            Sort.by(Sort.Direction.DESC, "fechaInicio")
                .and(Sort.by(Sort.Direction.DESC, "idSancion")));

        Specification<SancionCuenta> especificacion = (root, query, cb) -> {
            List<Predicate> predicados = new ArrayList<>();
            if (idUsuario != null) {
                predicados.add(cb.equal(root.get("usuario").get("idUsuario"), idUsuario));
            }
            if (texto != null && !texto.isBlank()) {
                String patron = "%" + texto.trim().toLowerCase() + "%";
                predicados.add(cb.or(
                    cb.like(cb.lower(root.get("usuario").<String>get("nombreUsuario")), patron),
                    cb.like(cb.lower(root.get("usuario").<String>get("correo")), patron)));
            }
            if (tipo != null) {
                predicados.add(cb.equal(root.get("tipo"), tipo));
            }
            if (estadoEfectivo != null && !estadoEfectivo.isBlank()) {
                switch (estadoEfectivo.trim().toUpperCase()) {
                    case "ACTIVA" -> predicados.add(cb.and(
                        cb.equal(root.get("estado"), EstadoSancion.ACTIVA),
                        cb.or(
                            cb.isNull(root.get("fechaFin")),
                            cb.greaterThan(root.<LocalDateTime>get("fechaFin"), ahora))));
                    case "VENCIDA" -> predicados.add(cb.and(
                        cb.equal(root.get("estado"), EstadoSancion.ACTIVA),
                        cb.isNotNull(root.get("fechaFin")),
                        cb.lessThanOrEqualTo(root.<LocalDateTime>get("fechaFin"), ahora)));
                    case "REVERTIDA" -> predicados.add(
                        cb.equal(root.get("estado"), EstadoSancion.REVERTIDA));
                    default -> { }
                }
            }
            return cb.and(predicados.toArray(Predicate[]::new));
        };

        Page<SancionCuenta> page = sancionRepository.findAll(especificacion, pageable);
        return PaginaDTO.<SancionCuentaDTO>builder()
            .contenido(page.getContent().stream()
                .map(sancion -> SancionCuentaDTO.desde(sancion, ahora))
                .toList())
            .pagina(page.getNumber())
            .tamanio(page.getSize())
            .totalElementos(page.getTotalElements())
            .totalPaginas(page.getTotalPages())
            .hayMas(page.hasNext())
            .build();
    }

    @Transactional(readOnly = true)
    public List<SancionCuentaDTO> historial(Long idUsuario) {
        LocalDateTime ahora = LocalDateTime.now();
        return sancionRepository.findByUsuarioIdUsuarioOrderByFechaInicioDesc(idUsuario).stream()
            .map(sancion -> SancionCuentaDTO.desde(sancion, ahora))
            .toList();
    }

    @Transactional(readOnly = true)
    public EstadoSancionDTO estadoUsuario(Long idUsuario) {
        LocalDateTime ahora = LocalDateTime.now();
        SancionCuentaDTO actual = obtenerVigente(idUsuario)
            .map(sancion -> SancionCuentaDTO.desde(sancion, ahora))
            .orElse(null);
        return EstadoSancionDTO.builder()
            .sancionado(actual != null)
            .sancionActual(actual)
            .build();
    }

    @Transactional(readOnly = true)
    public java.util.Optional<SancionCuenta> obtenerVigente(Long idUsuario) {
        return sancionRepository.findVigentesByUsuarioId(
                idUsuario, EstadoSancion.ACTIVA, LocalDateTime.now())
            .stream()
            .findFirst();
    }

    /**
     * Mapa de sanciones vigentes por usuario, para enriquecer listados sin N+1.
     */
    @Transactional(readOnly = true)
    public Map<Long, SancionCuenta> vigentesDe(java.util.Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Map.of();
        }
        return sancionRepository.findVigentesByUsuarioIds(
                ids, EstadoSancion.ACTIVA, LocalDateTime.now())
            .stream()
            .collect(Collectors.toMap(
                sancion -> sancion.getUsuario().getIdUsuario(),
                sancion -> sancion,
                (primera, segunda) -> primera));
    }

    private void validarFechas(SancionRequest request, LocalDateTime ahora) {
        if (request.getTipo() == TipoSancion.SUSPENSION) {
            if (request.getFechaFin() == null) {
                throw new IllegalArgumentException("Una suspensión temporal necesita una fecha de fin.");
            }
            if (!request.getFechaFin().isAfter(ahora)) {
                throw new IllegalArgumentException("La fecha de fin debe ser posterior a la actual.");
            }
        } else if (request.getFechaFin() != null) {
            throw new IllegalArgumentException("Una inhabilitación permanente no lleva fecha de fin.");
        }
    }

    private String resumenAplicacion(
            String nombreUsuario, TipoSancion tipo, LocalDateTime fechaFin, String motivo) {
        StringBuilder resumen = new StringBuilder("Inhabilitó la cuenta de @")
            .append(nombreUsuario);
        if (tipo == TipoSancion.SUSPENSION && fechaFin != null) {
            resumen.append(" hasta el ").append(fechaFin.format(FECHA));
        } else {
            resumen.append(" de forma permanente");
        }
        resumen.append(". Motivo: ").append(motivo);
        return resumen.toString();
    }
}
