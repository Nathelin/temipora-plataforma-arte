package com.temipora.backend_springboot.servicios;

import java.util.List;
import java.util.NoSuchElementException;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import com.temipora.backend_springboot.modelo.Medio;
import com.temipora.backend_springboot.modelo.MedioDTO;
import com.temipora.backend_springboot.modelo.MedioRequest;
import com.temipora.backend_springboot.repositorios.MedioRepository;

@Service
@Validated
public class MedioService {

    private final MedioRepository medioRepository;

    public MedioService(MedioRepository medioRepository) {
        this.medioRepository = medioRepository;
    }

    @Transactional(readOnly = true)
    public List<MedioDTO> listar() {
        return medioRepository.findAllByOrderByNombreAsc().stream()
            .map(MedioDTO::desdeMedio)
            .toList();
    }

    @Transactional(readOnly = true)
    public List<MedioDTO> listarActivos() {
        return medioRepository.findByActivoTrueOrderByNombreAsc().stream()
            .map(MedioDTO::desdeMedio)
            .toList();
    }

    @Transactional
    public MedioDTO crear(MedioRequest request) {
        validarNombreUnico(request.getNombre(), null);

        Medio medio = Medio.builder()
            .nombre(request.getNombre().trim())
            .descripcion(request.getDescripcion() != null ? request.getDescripcion().trim() : null)
            .observaciones(request.getObservaciones() != null ? request.getObservaciones().trim() : null)
            .activo(true)
            .build();

        return MedioDTO.desdeMedio(medioRepository.save(medio));
    }

    @Transactional
    public MedioDTO modificar(Long id, MedioRequest request) {
        Medio medio = obtener(id);
        validarNombreUnico(request.getNombre(), id);

        medio.setNombre(request.getNombre().trim());
        medio.setDescripcion(request.getDescripcion() != null ? request.getDescripcion().trim() : null);
        medio.setObservaciones(request.getObservaciones() != null ? request.getObservaciones().trim() : null);

        return MedioDTO.desdeMedio(medioRepository.save(medio));
    }

    @Transactional
    public MedioDTO darDeBaja(Long id) {
        Medio medio = obtener(id);
        medio.setActivo(false);
        return MedioDTO.desdeMedio(medioRepository.save(medio));
    }

    @Transactional
    public MedioDTO darDeAlta(Long id) {
        Medio medio = obtener(id);
        medio.setActivo(true);
        return MedioDTO.desdeMedio(medioRepository.save(medio));
    }

    private Medio obtener(Long id) {
        return medioRepository.findByIdMedio(id)
            .orElseThrow(() -> new NoSuchElementException("Medio no encontrado: " + id));
    }

    private void validarNombreUnico(String nombre, Long idExcluido) {
        boolean existe = (idExcluido == null)
            ? medioRepository.existsByNombreIgnoreCase(nombre.trim())
            : medioRepository.existsByNombreIgnoreCaseAndIdMedioNot(nombre.trim(), idExcluido);

        if (existe) {
            throw new IllegalArgumentException("Ya existe un medio con ese nombre.");
        }
    }
}
