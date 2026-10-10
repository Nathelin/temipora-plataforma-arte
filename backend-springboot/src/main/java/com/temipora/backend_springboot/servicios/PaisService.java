package com.temipora.backend_springboot.servicios;

import java.util.List;
import java.util.NoSuchElementException;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import com.temipora.backend_springboot.modelo.Pais;
import com.temipora.backend_springboot.modelo.PaisDTO;
import com.temipora.backend_springboot.modelo.PaisRequest;
import com.temipora.backend_springboot.repositorios.PaisRepository;

@Service
@Validated
public class PaisService {

    private final PaisRepository paisRepository;

    public PaisService(PaisRepository paisRepository) {
        this.paisRepository = paisRepository;
    }

    @Transactional(readOnly = true)
    public List<PaisDTO> listar() {
        return paisRepository.findAllByOrderByNombreAsc().stream()
            .map(PaisDTO::desdePais)
            .toList();
    }

    @Transactional(readOnly = true)
    public List<PaisDTO> listarActivos() {
        return paisRepository.findByEstadoTrueOrderByNombreAsc().stream()
            .map(PaisDTO::desdePais)
            .toList();
    }

    @Transactional
    public PaisDTO crear(PaisRequest request) {
        String nombre = request.getNombre().trim();
        String codigoIso = request.getCodigoIso().trim().toUpperCase();

        validarNombreUnico(nombre, null);
        validarCodigoUnico(codigoIso, null);

        Pais pais = Pais.builder()
            .nombre(nombre)
            .codigoIso(codigoIso)
            .prefijoTelefonico(request.getPrefijoTelefonico().trim())
            .estado(true)
            .build();

        return PaisDTO.desdePais(paisRepository.save(pais));
    }

    @Transactional
    public PaisDTO modificar(Long id, PaisRequest request) {
        Pais pais = obtener(id);
        String nombre = request.getNombre().trim();
        String codigoIso = request.getCodigoIso().trim().toUpperCase();

        validarNombreUnico(nombre, id);
        validarCodigoUnico(codigoIso, id);

        pais.setNombre(nombre);
        pais.setCodigoIso(codigoIso);
        pais.setPrefijoTelefonico(request.getPrefijoTelefonico().trim());

        return PaisDTO.desdePais(paisRepository.save(pais));
    }

    @Transactional
    public PaisDTO darDeBaja(Long id) {
        Pais pais = obtener(id);
        pais.setEstado(false);
        return PaisDTO.desdePais(paisRepository.save(pais));
    }

    @Transactional
    public PaisDTO darDeAlta(Long id) {
        Pais pais = obtener(id);
        pais.setEstado(true);
        return PaisDTO.desdePais(paisRepository.save(pais));
    }

    private Pais obtener(Long id) {
        return paisRepository.findByIdPais(id)
            .orElseThrow(() -> new NoSuchElementException("País no encontrado: " + id));
    }

    private void validarNombreUnico(String nombre, Long idExcluido) {
        boolean existe = (idExcluido == null)
            ? paisRepository.existsByNombreIgnoreCase(nombre)
            : paisRepository.existsByNombreIgnoreCaseAndIdPaisNot(nombre, idExcluido);

        if (existe) {
            throw new IllegalArgumentException("Ya existe un país con ese nombre.");
        }
    }

    private void validarCodigoUnico(String codigoIso, Long idExcluido) {
        boolean existe = (idExcluido == null)
            ? paisRepository.existsByCodigoIsoIgnoreCase(codigoIso)
            : paisRepository.existsByCodigoIsoIgnoreCaseAndIdPaisNot(codigoIso, idExcluido);

        if (existe) {
            throw new IllegalArgumentException("Ya existe un país con ese código ISO.");
        }
    }
}
