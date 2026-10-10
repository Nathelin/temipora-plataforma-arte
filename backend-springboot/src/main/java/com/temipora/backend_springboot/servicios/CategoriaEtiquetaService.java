package com.temipora.backend_springboot.servicios;

import java.util.List;
import java.util.NoSuchElementException;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import com.temipora.backend_springboot.modelo.CategoriaEtiqueta;
import com.temipora.backend_springboot.modelo.CategoriaEtiquetaDTO;
import com.temipora.backend_springboot.modelo.CategoriaEtiquetaRequest;
import com.temipora.backend_springboot.modelo.EtiquetaEstado;
import com.temipora.backend_springboot.repositorios.CategoriaEtiquetaRepository;
import com.temipora.backend_springboot.repositorios.EtiquetaRepository;

@Service
@Validated
public class CategoriaEtiquetaService {

    private static final List<EtiquetaEstado> ESTADOS_QUE_BLOQUEAN_BAJA =
            List.of(EtiquetaEstado.VALIDA, EtiquetaEstado.PENDIENTE);

    private final CategoriaEtiquetaRepository categoriaEtiquetaRepository;
    private final EtiquetaRepository etiquetaRepository;

    public CategoriaEtiquetaService(
            CategoriaEtiquetaRepository categoriaEtiquetaRepository,
            EtiquetaRepository etiquetaRepository) {
        this.categoriaEtiquetaRepository = categoriaEtiquetaRepository;
        this.etiquetaRepository = etiquetaRepository;
    }

    @Transactional(readOnly = true)
    public List<CategoriaEtiquetaDTO> listar() {
        return categoriaEtiquetaRepository.findAllByOrderByNombreAsc().stream()
            .map(CategoriaEtiquetaDTO::desdeCategoria)
            .toList();
    }

    @Transactional(readOnly = true)
    public List<CategoriaEtiquetaDTO> listarActivas() {
        return categoriaEtiquetaRepository.findByActivoTrueOrderByNombreAsc().stream()
            .map(CategoriaEtiquetaDTO::desdeCategoria)
            .toList();
    }

    @Transactional
    public CategoriaEtiquetaDTO crear(CategoriaEtiquetaRequest request) {
        validarNombreUnico(request.getNombre(), null);

        CategoriaEtiqueta categoria = CategoriaEtiqueta.builder()
            .nombre(request.getNombre().trim())
            .descripcion(request.getDescripcion().trim())
            .observaciones(request.getObservaciones() != null ? request.getObservaciones().trim() : null)
            .activo(true)
            .build();

        return CategoriaEtiquetaDTO.desdeCategoria(categoriaEtiquetaRepository.save(categoria));
    }

    @Transactional
    public CategoriaEtiquetaDTO modificar(Long id, CategoriaEtiquetaRequest request) {
        CategoriaEtiqueta categoria = obtener(id);
        validarNombreUnico(request.getNombre(), id);

        categoria.setNombre(request.getNombre().trim());
        categoria.setDescripcion(request.getDescripcion().trim());
        categoria.setObservaciones(request.getObservaciones() != null ? request.getObservaciones().trim() : null);

        return CategoriaEtiquetaDTO.desdeCategoria(categoriaEtiquetaRepository.save(categoria));
    }

    @Transactional
    public CategoriaEtiquetaDTO darDeBaja(Long id) {
        CategoriaEtiqueta categoria = obtener(id);

        long etiquetasVivas = etiquetaRepository.countByCategoriaIdCategoriaEtiquetaAndEstadoIn(
                id, ESTADOS_QUE_BLOQUEAN_BAJA);
        if (etiquetasVivas > 0) {
            throw new IllegalArgumentException(
                "No se puede dar de baja: la categoría tiene " + etiquetasVivas
                    + " etiqueta(s) en estado válida o pendiente.");
        }

        categoria.setActivo(false);
        return CategoriaEtiquetaDTO.desdeCategoria(categoriaEtiquetaRepository.save(categoria));
    }

    @Transactional
    public CategoriaEtiquetaDTO darDeAlta(Long id) {
        CategoriaEtiqueta categoria = obtener(id);
        categoria.setActivo(true);
        return CategoriaEtiquetaDTO.desdeCategoria(categoriaEtiquetaRepository.save(categoria));
    }

    private CategoriaEtiqueta obtener(Long id) {
        return categoriaEtiquetaRepository.findByIdCategoriaEtiqueta(id)
            .orElseThrow(() -> new NoSuchElementException("Categoría de etiqueta no encontrada: " + id));
    }

    private void validarNombreUnico(String nombre, Long idExcluido) {
        boolean existe = (idExcluido == null)
            ? categoriaEtiquetaRepository.existsByNombreIgnoreCase(nombre.trim())
            : categoriaEtiquetaRepository.existsByNombreIgnoreCaseAndIdCategoriaEtiquetaNot(nombre.trim(), idExcluido);

        if (existe) {
            throw new IllegalArgumentException("Ya existe una categoría con ese nombre.");
        }
    }
}
