package com.temipora.backend_springboot.servicios;

import java.util.List;
import java.util.NoSuchElementException;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import com.temipora.backend_springboot.modelo.Material;
import com.temipora.backend_springboot.modelo.MaterialDTO;
import com.temipora.backend_springboot.modelo.MaterialRequest;
import com.temipora.backend_springboot.modelo.TipoArte;
import com.temipora.backend_springboot.repositorios.MaterialRepository;
import com.temipora.backend_springboot.repositorios.TipoArteRepository;

@Service
@Validated
public class MaterialService {

    private final MaterialRepository materialRepository;
    private final TipoArteRepository tipoArteRepository;

    public MaterialService(
            MaterialRepository materialRepository,
            TipoArteRepository tipoArteRepository) {
        this.materialRepository = materialRepository;
        this.tipoArteRepository = tipoArteRepository;
    }

    @Transactional(readOnly = true)
    public List<MaterialDTO> listar() {
        return materialRepository.findAllByOrderByNombreAsc().stream()
            .map(MaterialDTO::desdeMaterial)
            .toList();
    }

    @Transactional(readOnly = true)
    public List<MaterialDTO> selector(Long idMedio) {
        return materialRepository.buscarSelector(idMedio).stream()
            .map(MaterialDTO::desdeMaterial)
            .toList();
    }

    @Transactional
    public MaterialDTO crear(MaterialRequest request) {
        validarNombreUnico(request.getNombre(), null);
        TipoArte tipoArte = cargarTipoActivo(request.getIdTipoArte());

        Material material = Material.builder()
            .nombre(request.getNombre().trim())
            .descripcion(request.getDescripcion() != null ? request.getDescripcion().trim() : null)
            .tipoArte(tipoArte)
            .observaciones(request.getObservaciones() != null ? request.getObservaciones().trim() : null)
            .activo(true)
            .build();

        return MaterialDTO.desdeMaterial(materialRepository.save(material));
    }

    @Transactional
    public MaterialDTO modificar(Long id, MaterialRequest request) {
        Material material = obtener(id);
        validarNombreUnico(request.getNombre(), id);

        TipoArte tipoArte = cargarTipo(request.getIdTipoArte());
        boolean tipoYaAsignado = material.getTipoArte() != null
            && tipoArte.getIdTipoArte().equals(material.getTipoArte().getIdTipoArte());
        if (!tipoArte.isActivo() && !tipoYaAsignado) {
            throw new IllegalArgumentException(
                "El tipo de arte '" + tipoArte.getNombre() + "' fue dado de baja y no puede asignarse.");
        }

        material.setNombre(request.getNombre().trim());
        material.setDescripcion(request.getDescripcion() != null ? request.getDescripcion().trim() : null);
        material.setTipoArte(tipoArte);
        material.setObservaciones(request.getObservaciones() != null ? request.getObservaciones().trim() : null);

        return MaterialDTO.desdeMaterial(materialRepository.save(material));
    }

    @Transactional
    public MaterialDTO darDeBaja(Long id) {
        Material material = obtener(id);
        material.setActivo(false);
        return MaterialDTO.desdeMaterial(materialRepository.save(material));
    }

    @Transactional
    public MaterialDTO darDeAlta(Long id) {
        Material material = obtener(id);
        if (material.getTipoArte() == null || !material.getTipoArte().isActivo()) {
            String nombreTipo = material.getTipoArte() != null
                ? material.getTipoArte().getNombre() : "";
            throw new IllegalArgumentException(
                "El tipo de arte '" + nombreTipo + "' está dado de baja; reactívalo antes de dar de alta el material.");
        }
        material.setActivo(true);
        return MaterialDTO.desdeMaterial(materialRepository.save(material));
    }

    private Material obtener(Long id) {
        return materialRepository.findByIdMaterial(id)
            .orElseThrow(() -> new NoSuchElementException("Material no encontrado: " + id));
    }

    private TipoArte cargarTipo(Long idTipoArte) {
        if (idTipoArte == null) {
            throw new IllegalArgumentException("El tipo de arte es obligatorio.");
        }
        return tipoArteRepository.findByIdTipoArte(idTipoArte)
            .orElseThrow(() -> new NoSuchElementException("Tipo de arte no encontrado: " + idTipoArte));
    }

    private TipoArte cargarTipoActivo(Long idTipoArte) {
        TipoArte tipoArte = cargarTipo(idTipoArte);
        if (!tipoArte.isActivo()) {
            throw new IllegalArgumentException(
                "El tipo de arte '" + tipoArte.getNombre() + "' fue dado de baja y no puede recibir materiales nuevos.");
        }
        return tipoArte;
    }

    private void validarNombreUnico(String nombre, Long idExcluido) {
        boolean existe = (idExcluido == null)
            ? materialRepository.existsByNombreIgnoreCase(nombre.trim())
            : materialRepository.existsByNombreIgnoreCaseAndIdMaterialNot(nombre.trim(), idExcluido);

        if (existe) {
            throw new IllegalArgumentException("Ya existe un material con ese nombre.");
        }
    }
}
