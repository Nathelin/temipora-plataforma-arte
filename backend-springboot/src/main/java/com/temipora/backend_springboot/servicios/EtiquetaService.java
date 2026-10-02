package com.temipora.backend_springboot.servicios;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.temipora.backend_springboot.modelo.CategoriaEtiqueta;
import com.temipora.backend_springboot.modelo.CategoriaEtiquetaDTO;
import com.temipora.backend_springboot.modelo.Etiqueta;
import com.temipora.backend_springboot.modelo.EtiquetaDTO;
import com.temipora.backend_springboot.modelo.EtiquetaEstado;
import com.temipora.backend_springboot.modelo.EtiquetaRequest;
import com.temipora.backend_springboot.modelo.Usuario;
import com.temipora.backend_springboot.repositorios.CategoriaEtiquetaRepository;
import com.temipora.backend_springboot.repositorios.EtiquetaRepository;
import com.temipora.backend_springboot.repositorios.ImagenEtiquetaRepository;

@Service
public class EtiquetaService {

    private final EtiquetaRepository etiquetaRepository;
    private final CategoriaEtiquetaRepository categoriaEtiquetaRepository;
    private final ImagenEtiquetaRepository imagenEtiquetaRepository;

    public EtiquetaService(
            EtiquetaRepository etiquetaRepository,
            CategoriaEtiquetaRepository categoriaEtiquetaRepository,
            ImagenEtiquetaRepository imagenEtiquetaRepository) {
        this.etiquetaRepository = etiquetaRepository;
        this.categoriaEtiquetaRepository = categoriaEtiquetaRepository;
        this.imagenEtiquetaRepository = imagenEtiquetaRepository;
    }

    // ---------------------------------------------------------------
    // Administración (UC-54, UC-55, UC-56, UC-57)
    // ---------------------------------------------------------------

    @Transactional(readOnly = true)
    public List<EtiquetaDTO> listarAdmin(EtiquetaEstado estado, Long idCategoria) {
        List<Etiqueta> etiquetas;

        if (estado != null && idCategoria != null) {
            etiquetas = etiquetaRepository.findByEstadoAndCategoriaIdCategoriaEtiquetaOrderByNombreAsc(estado, idCategoria);
        } else if (estado != null) {
            etiquetas = switch (estado) {
                case PENDIENTE -> etiquetaRepository.findByEstadoOrderByFechaCreacionAsc(estado);
                case RECHAZADA -> etiquetaRepository.findByEstadoOrderByVecesSolicitadaDesc(estado);
                default -> etiquetaRepository.findByEstadoOrderByNombreAsc(estado);
            };
        } else if (idCategoria != null) {
            etiquetas = etiquetaRepository.findByCategoriaIdCategoriaEtiquetaOrderByNombreAsc(idCategoria);
        } else {
            etiquetas = etiquetaRepository.findAllByOrderByNombreAsc();
        }

        return etiquetas.stream().map(EtiquetaDTO::desdeEtiqueta).toList();
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

        return EtiquetaDTO.desdeEtiqueta(etiquetaRepository.save(etiqueta));
    }

    @Transactional
    public EtiquetaDTO modificar(Long id, EtiquetaRequest request) {
        Etiqueta etiqueta = obtener(id);
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

        return EtiquetaDTO.desdeEtiqueta(etiquetaRepository.save(etiqueta));
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

        etiqueta.setEstado(EtiquetaEstado.VALIDA);
        etiqueta.setEvaluadoPor(admin);
        etiqueta.setFechaEvaluacion(LocalDateTime.now());

        return EtiquetaDTO.desdeEtiqueta(etiquetaRepository.save(etiqueta));
    }

    @Transactional
    public EtiquetaDTO rechazar(Usuario admin, Long id) {
        Etiqueta etiqueta = obtener(id);

        if (etiqueta.getEstado() != EtiquetaEstado.PENDIENTE) {
            throw new IllegalArgumentException("Solo se pueden rechazar etiquetas pendientes de evaluación.");
        }

        etiqueta.setEstado(EtiquetaEstado.RECHAZADA);
        etiqueta.setEvaluadoPor(admin);
        etiqueta.setFechaEvaluacion(LocalDateTime.now());
        Etiqueta guardada = etiquetaRepository.save(etiqueta);

        // UC-57: se quita de las imágenes a las que se asignó mientras estaba PENDIENTE
        imagenEtiquetaRepository.deleteByEtiquetaIdEtiqueta(id);

        return EtiquetaDTO.desdeEtiqueta(guardada);
    }

    @Transactional
    public EtiquetaDTO darDeBaja(Long id) {
        Etiqueta etiqueta = obtener(id);

        if (etiqueta.getEstado() != EtiquetaEstado.VALIDA) {
            throw new IllegalArgumentException("Solo puede darse de baja una etiqueta en estado VALIDA.");
        }

        etiqueta.setEstado(EtiquetaEstado.INACTIVA);
        return EtiquetaDTO.desdeEtiqueta(etiquetaRepository.save(etiqueta));
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
