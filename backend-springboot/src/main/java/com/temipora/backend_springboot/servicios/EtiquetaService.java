package com.temipora.backend_springboot.servicios;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.temipora.backend_springboot.modelo.CategoriaEtiqueta;
import com.temipora.backend_springboot.modelo.CategoriaEtiquetaDTO;
import com.temipora.backend_springboot.modelo.Etiqueta;
import com.temipora.backend_springboot.modelo.EtiquetaDTO;
import com.temipora.backend_springboot.modelo.EtiquetaEstado;
import com.temipora.backend_springboot.modelo.EtiquetaRequest;
import com.temipora.backend_springboot.modelo.PaginaDTO;
import com.temipora.backend_springboot.modelo.Usuario;
import com.temipora.backend_springboot.repositorios.CategoriaEtiquetaRepository;
import com.temipora.backend_springboot.repositorios.EtiquetaRepository;
import com.temipora.backend_springboot.repositorios.ImagenEtiquetaRepository;

@Service
public class EtiquetaService {

    private static final int TAMANIO_MAXIMO = 100;
    private static final String ENTIDAD = "Etiqueta";
    private static final List<CampoAuditado> CAMPOS = List.of(
        CampoAuditado.de("nombre", "Nombre"),
        CampoAuditado.de("descripcion", "Descripción"),
        CampoAuditado.de("observaciones", "Observaciones"),
        CampoAuditado.de("categoria", "Categoría"),
        CampoAuditado.de("estado", "Estado"));

    private final EtiquetaRepository etiquetaRepository;
    private final CategoriaEtiquetaRepository categoriaEtiquetaRepository;
    private final ImagenEtiquetaRepository imagenEtiquetaRepository;
    private final AuditoriaService auditoria;

    public EtiquetaService(
            EtiquetaRepository etiquetaRepository,
            CategoriaEtiquetaRepository categoriaEtiquetaRepository,
            ImagenEtiquetaRepository imagenEtiquetaRepository,
            AuditoriaService auditoria) {
        this.etiquetaRepository = etiquetaRepository;
        this.categoriaEtiquetaRepository = categoriaEtiquetaRepository;
        this.imagenEtiquetaRepository = imagenEtiquetaRepository;
        this.auditoria = auditoria;
    }

    // ---------------------------------------------------------------
    // Administración (UC-54, UC-55, UC-56, UC-57)
    // ---------------------------------------------------------------

    @Transactional(readOnly = true)
    public PaginaDTO<EtiquetaDTO> listarAdmin(
            EtiquetaEstado estado, Long idCategoria, int pagina, int tamanio) {
        Pageable pageable = pagina(pagina, tamanio, ordenDe(estado));

        Page<Etiqueta> page;
        if (estado != null && idCategoria != null) {
            page = etiquetaRepository.findByEstadoAndCategoriaIdCategoriaEtiqueta(estado, idCategoria, pageable);
        } else if (estado != null) {
            page = etiquetaRepository.findByEstado(estado, pageable);
        } else if (idCategoria != null) {
            page = etiquetaRepository.findByCategoriaIdCategoriaEtiqueta(idCategoria, pageable);
        } else {
            page = etiquetaRepository.findAll(pageable);
        }

        return PaginaDTO.<EtiquetaDTO>builder()
            .contenido(page.getContent().stream().map(EtiquetaDTO::desdeEtiqueta).toList())
            .pagina(page.getNumber())
            .tamanio(page.getSize())
            .totalElementos(page.getTotalElements())
            .totalPaginas(page.getTotalPages())
            .hayMas(page.hasNext())
            .build();
    }

    @Transactional(readOnly = true)
    public long contarPendientes() {
        return etiquetaRepository.countByEstado(EtiquetaEstado.PENDIENTE);
    }

    @Transactional
    public EtiquetaDTO crearDesdeAdmin(Usuario admin, EtiquetaRequest request) {
        String nombre = request.getNombre().trim();
        validarNombreDisponible(nombre, null);
        CategoriaEtiqueta categoria = categoriaActiva(request.getIdCategoria());

        Etiqueta etiqueta = Etiqueta.builder()
            .nombre(nombre)
            .descripcion(request.getDescripcion().trim())
            .observaciones(request.getObservaciones() != null ? request.getObservaciones().trim() : null)
            .categoria(categoria)
            .creadoPor(admin)
            .estado(EtiquetaEstado.VALIDA)
            .vecesSolicitada(0)
            .fechaCreacion(LocalDateTime.now())
            .build();

        Etiqueta guardada = etiquetaRepository.save(etiqueta);
        auditoria.registrar(
            AccionesAdmin.ETIQUETA_CREAR, ENTIDAD, guardada.getIdEtiqueta(),
            "Creó la etiqueta “" + guardada.getNombre() + "”", null, guardada, CAMPOS);
        return EtiquetaDTO.desdeEtiqueta(guardada);
    }

    @Transactional
    public EtiquetaDTO modificar(Long id, EtiquetaRequest request) {
        Etiqueta etiqueta = obtener(id);
        Snapshot antes = Snapshot.de(etiqueta, CAMPOS);
        String nombre = request.getNombre().trim();
        validarNombreDisponible(nombre, id);

        CategoriaEtiqueta categoria = categoriaEtiquetaRepository
            .findByIdCategoriaEtiqueta(request.getIdCategoria())
            .orElseThrow(() -> new NoSuchElementException("Categoría de etiqueta no encontrada: " + request.getIdCategoria()));

        boolean categoriaSinCambiar = categoria.getIdCategoriaEtiqueta()
            .equals(etiqueta.getCategoria().getIdCategoriaEtiqueta());
        if (!categoria.isActivo() && !categoriaSinCambiar) {
            throw new IllegalArgumentException("La categoría seleccionada está inactiva y no puede ofrecerse para editar etiquetas.");
        }

        etiqueta.setNombre(nombre);
        etiqueta.setDescripcion(request.getDescripcion().trim());
        etiqueta.setObservaciones(request.getObservaciones() != null ? request.getObservaciones().trim() : null);
        etiqueta.setCategoria(categoria);

        Etiqueta guardada = etiquetaRepository.save(etiqueta);
        auditoria.registrar(
            AccionesAdmin.ETIQUETA_MODIFICAR, ENTIDAD, guardada.getIdEtiqueta(),
            "Modificó la etiqueta “" + guardada.getNombre() + "”", antes, guardada, CAMPOS);
        return EtiquetaDTO.desdeEtiqueta(guardada);
    }

    @Transactional
    public EtiquetaDTO aprobar(Usuario admin, Long id) {
        Etiqueta etiqueta = obtener(id);

        if (etiqueta.getEstado() == EtiquetaEstado.VALIDA) {
            throw new IllegalArgumentException("La etiqueta ya está aprobada.");
        }
        if (etiqueta.getEstado() == EtiquetaEstado.INACTIVA) {
            throw new IllegalArgumentException("No se puede aprobar una etiqueta dada de baja.");
        }
        if (etiquetaRepository.existsByNombreIgnoreCaseAndEstado(etiqueta.getNombre(), EtiquetaEstado.VALIDA)) {
            throw new IllegalArgumentException("Ya existe una etiqueta válida con ese nombre; modifícala o eliminala primero.");
        }

        Snapshot antes = Snapshot.de(etiqueta, CAMPOS);
        etiqueta.setEstado(EtiquetaEstado.VALIDA);
        etiqueta.setEvaluadoPor(admin);
        etiqueta.setFechaEvaluacion(LocalDateTime.now());

        Etiqueta guardada = etiquetaRepository.save(etiqueta);
        auditoria.registrar(
            AccionesAdmin.ETIQUETA_APROBAR, ENTIDAD, guardada.getIdEtiqueta(),
            "Aprobó la etiqueta “" + guardada.getNombre() + "”", antes, guardada, CAMPOS);
        return EtiquetaDTO.desdeEtiqueta(guardada);
    }

    @Transactional
    public EtiquetaDTO rechazar(Usuario admin, Long id) {
        Etiqueta etiqueta = obtener(id);

        if (etiqueta.getEstado() != EtiquetaEstado.PENDIENTE) {
            throw new IllegalArgumentException("Solo se pueden rechazar etiquetas pendientes de evaluación.");
        }

        Snapshot antes = Snapshot.de(etiqueta, CAMPOS);
        etiqueta.setEstado(EtiquetaEstado.RECHAZADA);
        etiqueta.setEvaluadoPor(admin);
        etiqueta.setFechaEvaluacion(LocalDateTime.now());
        Etiqueta guardada = etiquetaRepository.save(etiqueta);

        // UC-57: se quita de las imágenes a las que se asignó mientras estaba PENDIENTE
        imagenEtiquetaRepository.deleteByEtiquetaIdEtiqueta(id);

        auditoria.registrar(
            AccionesAdmin.ETIQUETA_RECHAZAR, ENTIDAD, guardada.getIdEtiqueta(),
            "Rechazó la etiqueta “" + guardada.getNombre() + "”", antes, guardada, CAMPOS);
        return EtiquetaDTO.desdeEtiqueta(guardada);
    }

    @Transactional
    public EtiquetaDTO darDeBaja(Long id) {
        Etiqueta etiqueta = obtener(id);

        if (etiqueta.getEstado() != EtiquetaEstado.VALIDA) {
            throw new IllegalArgumentException("Solo puede darse de baja una etiqueta en estado VALIDA.");
        }

        Snapshot antes = Snapshot.de(etiqueta, CAMPOS);
        etiqueta.setEstado(EtiquetaEstado.INACTIVA);
        Etiqueta guardada = etiquetaRepository.save(etiqueta);
        auditoria.registrar(
            AccionesAdmin.ETIQUETA_BAJA, ENTIDAD, guardada.getIdEtiqueta(),
            "Dio de baja la etiqueta “" + guardada.getNombre() + "”", antes, guardada, CAMPOS);
        return EtiquetaDTO.desdeEtiqueta(guardada);
    }

    @Transactional
    public EtiquetaDTO reactivar(Usuario admin, Long id) {
        Etiqueta etiqueta = obtener(id);

        if (etiqueta.getEstado() != EtiquetaEstado.INACTIVA) {
            throw new IllegalArgumentException("Solo puede reactivarse una etiqueta dada de baja.");
        }
        validarNombreDisponible(etiqueta.getNombre(), id);

        Snapshot antes = Snapshot.de(etiqueta, CAMPOS);
        etiqueta.setEstado(EtiquetaEstado.VALIDA);
        etiqueta.setEvaluadoPor(admin);
        etiqueta.setFechaEvaluacion(LocalDateTime.now());

        Etiqueta guardada = etiquetaRepository.save(etiqueta);
        auditoria.registrar(
            AccionesAdmin.ETIQUETA_REACTIVAR, ENTIDAD, guardada.getIdEtiqueta(),
            "Reactivó la etiqueta “" + guardada.getNombre() + "”", antes, guardada, CAMPOS);
        return EtiquetaDTO.desdeEtiqueta(guardada);
    }

    // ---------------------------------------------------------------
    // Artista (UC-54 alta propuesta, UC-59 re-solicitud)
    // ---------------------------------------------------------------

    @Transactional
    public EtiquetaDTO crearDesdeArtista(Usuario artista, EtiquetaRequest request) {
        String nombre = request.getNombre().trim();
        Optional<Etiqueta> existente = etiquetaRepository.findByNombreIgnoreCase(nombre);

        if (existente.isPresent()) {
            return reutilizarOCrearDuplicada(artista, existente.get());
        }

        CategoriaEtiqueta categoria = categoriaActiva(request.getIdCategoria());

        Etiqueta etiqueta = Etiqueta.builder()
            .nombre(nombre)
            .descripcion(request.getDescripcion().trim())
            .observaciones(request.getObservaciones() != null ? request.getObservaciones().trim() : null)
            .categoria(categoria)
            .creadoPor(artista)
            .estado(EtiquetaEstado.PENDIENTE)
            .vecesSolicitada(1)
            .fechaCreacion(LocalDateTime.now())
            .build();

        return EtiquetaDTO.desdeEtiqueta(etiquetaRepository.save(etiqueta));
    }

    @Transactional
    public EtiquetaDTO reSolicitar(Usuario artista, Long id) {
        Etiqueta etiqueta = obtener(id);

        boolean esPropia = etiqueta.getCreadoPor() != null
            && etiqueta.getCreadoPor().getIdUsuario().equals(artista.getIdUsuario());
        if (!esPropia) {
            throw new IllegalArgumentException("Solo el artista que propuso la etiqueta puede re-solicitarla.");
        }
        if (etiqueta.getEstado() != EtiquetaEstado.RECHAZADA) {
            throw new IllegalArgumentException(
                "Solo se pueden re-solicitar etiquetas rechazadas. Estado actual: " + etiqueta.getEstado() + ".");
        }

        etiqueta.setVecesSolicitada(etiqueta.getVecesSolicitada() + 1);
        return EtiquetaDTO.desdeEtiqueta(etiquetaRepository.save(etiqueta));
    }

    @Transactional(readOnly = true)
    public List<EtiquetaDTO> misEtiquetas(Usuario usuario) {
        return etiquetaRepository.findByCreadoPorIdUsuarioOrderByFechaCreacionDesc(usuario.getIdUsuario()).stream()
            .map(EtiquetaDTO::desdeEtiqueta)
            .toList();
    }

    // ---------------------------------------------------------------
    // Selector y búsqueda (público autenticado)
    // ---------------------------------------------------------------

    @Transactional(readOnly = true)
    public List<EtiquetaDTO> selector(Usuario usuario) {
        List<Etiqueta> opciones = new ArrayList<>(
            etiquetaRepository.findByEstadoOrderByNombreAsc(EtiquetaEstado.VALIDA));
        opciones.addAll(etiquetaRepository.findByEstadoAndCreadoPorIdUsuario(
            EtiquetaEstado.PENDIENTE, usuario.getIdUsuario()));
        return opciones.stream().map(EtiquetaDTO::desdeEtiqueta).toList();
    }

    @Transactional(readOnly = true)
    public Optional<EtiquetaDTO> buscar(String nombre) {
        return etiquetaRepository.findByNombreIgnoreCase(nombre.trim())
            .map(EtiquetaDTO::desdeEtiqueta);
    }

    @Transactional(readOnly = true)
    public List<CategoriaEtiquetaDTO> categoriasActivas() {
        return categoriaEtiquetaRepository.findByActivoTrueOrderByNombreAsc().stream()
            .map(CategoriaEtiquetaDTO::desdeCategoria)
            .toList();
    }

    // ---------------------------------------------------------------
    // Privados
    // ---------------------------------------------------------------

    private EtiquetaDTO reutilizarOCrearDuplicada(Usuario artista, Etiqueta existente) {
        return switch (existente.getEstado()) {
            case VALIDA -> throw new IllegalArgumentException(
                "Ya existe una etiqueta válida con ese nombre; puedes usarla directamente en el selector.");
            case RECHAZADA -> throw new IllegalArgumentException(
                "La etiqueta fue rechazada por los administradores. Puedes volver a solicitarla desde \"Mis etiquetas\".");
            case INACTIVA -> throw new IllegalArgumentException(
                "La etiqueta fue dada de baja por los administradores y no puede asignarse.");
            case PENDIENTE -> {
                boolean esPropia = existente.getCreadoPor() != null
                    && existente.getCreadoPor().getIdUsuario().equals(artista.getIdUsuario());
                if (esPropia) {
                    yield EtiquetaDTO.desdeEtiqueta(existente);
                }
                throw new IllegalArgumentException(
                    "Otro artista propuso esa etiqueta y está pendiente de evaluación.");
            }
        };
    }

    private Pageable pagina(int pagina, int tamanio, Sort orden) {
        return PageRequest.of(
            Math.max(0, pagina),
            Math.min(Math.max(1, tamanio), TAMANIO_MAXIMO),
            orden);
    }

    private Sort ordenDe(EtiquetaEstado estado) {
        if (estado == EtiquetaEstado.PENDIENTE) {
            return Sort.by(Sort.Direction.ASC, "fechaCreacion");
        }
        if (estado == EtiquetaEstado.RECHAZADA) {
            return Sort.by(Sort.Direction.DESC, "vecesSolicitada");
        }
        return Sort.by(Sort.Direction.ASC, "nombre");
    }

    private Etiqueta obtener(Long id) {
        return etiquetaRepository.findByIdEtiqueta(id)
            .orElseThrow(() -> new NoSuchElementException("Etiqueta no encontrada: " + id));
    }

    private CategoriaEtiqueta categoriaActiva(Long idCategoria) {
        CategoriaEtiqueta categoria = categoriaEtiquetaRepository.findByIdCategoriaEtiqueta(idCategoria)
            .orElseThrow(() -> new NoSuchElementException("Categoría de etiqueta no encontrada: " + idCategoria));
        if (!categoria.isActivo()) {
            throw new IllegalArgumentException("La categoría seleccionada está inactiva.");
        }
        return categoria;
    }

    private void validarNombreDisponible(String nombre, Long idExcluido) {
        Optional<Etiqueta> existente = etiquetaRepository.findByNombreIgnoreCase(nombre);
        if (existente.isEmpty()
                || (idExcluido != null && existente.get().getIdEtiqueta().equals(idExcluido))) {
            return;
        }

        String mensaje = switch (existente.get().getEstado()) {
            case VALIDA -> "Ya existe una etiqueta válida con ese nombre.";
            case PENDIENTE -> "Ya existe una etiqueta pendiente de evaluación con ese nombre.";
            case RECHAZADA -> "Ya existe una etiqueta rechazada con ese nombre.";
            case INACTIVA -> "Ya existe una etiqueta dada de baja con ese nombre.";
        };
        throw new IllegalArgumentException(mensaje);
    }
}
