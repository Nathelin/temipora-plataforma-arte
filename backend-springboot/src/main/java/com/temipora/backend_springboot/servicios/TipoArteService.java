package com.temipora.backend_springboot.servicios;

import java.util.List;
import java.util.NoSuchElementException;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import com.temipora.backend_springboot.modelo.Medio;
import com.temipora.backend_springboot.modelo.TipoArte;
import com.temipora.backend_springboot.modelo.TipoArteDTO;
import com.temipora.backend_springboot.modelo.TipoArteRequest;
import com.temipora.backend_springboot.repositorios.MedioRepository;
import com.temipora.backend_springboot.repositorios.TipoArteRepository;

@Service
@Validated
public class TipoArteService {

    private final TipoArteRepository tipoArteRepository;
    private final MedioRepository medioRepository;

    public TipoArteService(TipoArteRepository tipoArteRepository, MedioRepository medioRepository) {
        this.tipoArteRepository = tipoArteRepository;
        this.medioRepository = medioRepository;
    }

    @Transactional(readOnly = true)
    public List<TipoArteDTO> listar() {
        return tipoArteRepository.findAllByOrderByNombreAsc().stream()
            .map(TipoArteDTO::desdeTipoArte)
            .toList();
    }

    @Transactional(readOnly = true)
    public List<TipoArteDTO> listarActivos() {
        return tipoArteRepository.findByActivoTrueOrderByNombreAsc().stream()
            .map(TipoArteDTO::desdeTipoArte)
            .toList();
    }

    @Transactional
    public TipoArteDTO crear(TipoArteRequest request) {
        validarNombreUnico(request.getNombre(), null);
        Medio medio = cargarMedioActivo(request.getIdMedio());

        TipoArte tipoArte = TipoArte.builder()
            .nombre(request.getNombre().trim())
            .descripcion(request.getDescripcion() != null ? request.getDescripcion().trim() : null)
            .medio(medio)
            .observaciones(request.getObservaciones() != null ? request.getObservaciones().trim() : null)
            .activo(true)
            .build();

        return TipoArteDTO.desdeTipoArte(tipoArteRepository.save(tipoArte));
    }

    @Transactional
    public TipoArteDTO modificar(Long id, TipoArteRequest request) {
        TipoArte tipoArte = obtener(id);
        validarNombreUnico(request.getNombre(), id);

        Medio medio = cargarMedio(request.getIdMedio());
        boolean medioYaAsignado = tipoArte.getMedio() != null
            && medio.getIdMedio().equals(tipoArte.getMedio().getIdMedio());
        if (!medio.isActivo() && !medioYaAsignado) {
            throw new IllegalArgumentException(
                "El medio '" + medio.getNombre() + "' fue dado de baja y no puede asignarse.");
        }

        tipoArte.setNombre(request.getNombre().trim());
        tipoArte.setDescripcion(request.getDescripcion() != null ? request.getDescripcion().trim() : null);
        tipoArte.setMedio(medio);
        tipoArte.setObservaciones(request.getObservaciones() != null ? request.getObservaciones().trim() : null);

        return TipoArteDTO.desdeTipoArte(tipoArteRepository.save(tipoArte));
    }

    @Transactional
    public TipoArteDTO darDeBaja(Long id) {
        TipoArte tipoArte = obtener(id);
        tipoArte.setActivo(false);
        return TipoArteDTO.desdeTipoArte(tipoArteRepository.save(tipoArte));
    }

    @Transactional
    public TipoArteDTO darDeAlta(Long id) {
        TipoArte tipoArte = obtener(id);
        tipoArte.setActivo(true);
        return TipoArteDTO.desdeTipoArte(tipoArteRepository.save(tipoArte));
    }

    private TipoArte obtener(Long id) {
        return tipoArteRepository.findByIdTipoArte(id)
            .orElseThrow(() -> new NoSuchElementException("Tipo de arte no encontrado: " + id));
    }

    private Medio cargarMedio(Long idMedio) {
        if (idMedio == null) {
            throw new IllegalArgumentException("El medio es obligatorio.");
        }
        return medioRepository.findByIdMedio(idMedio)
            .orElseThrow(() -> new NoSuchElementException("Medio no encontrado: " + idMedio));
    }

    private Medio cargarMedioActivo(Long idMedio) {
        Medio medio = cargarMedio(idMedio);
        if (!medio.isActivo()) {
            throw new IllegalArgumentException(
                "El medio '" + medio.getNombre() + "' fue dado de baja y no puede asignarse a tipos de arte nuevos.");
        }
        return medio;
    }

    private void validarNombreUnico(String nombre, Long idExcluido) {
        boolean existe = (idExcluido == null)
            ? tipoArteRepository.existsByNombreIgnoreCase(nombre.trim())
            : tipoArteRepository.existsByNombreIgnoreCaseAndIdTipoArteNot(nombre.trim(), idExcluido);

        if (existe) {
            throw new IllegalArgumentException("Ya existe un tipo de arte con ese nombre.");
        }
    }
}
