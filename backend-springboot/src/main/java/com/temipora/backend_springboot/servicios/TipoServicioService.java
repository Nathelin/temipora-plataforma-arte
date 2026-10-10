package com.temipora.backend_springboot.servicios;

import java.util.List;
import java.util.NoSuchElementException;

import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import com.temipora.backend_springboot.modelo.TipoServicio;
import com.temipora.backend_springboot.modelo.TipoServicioDTO;
import com.temipora.backend_springboot.modelo.TipoServicioRequest;
import com.temipora.backend_springboot.repositorios.TipoServicioRepository;

@Service
@Validated
public class TipoServicioService {

    private final TipoServicioRepository tipoServicioRepository;

    public TipoServicioService(TipoServicioRepository tipoServicioRepository) {
        this.tipoServicioRepository = tipoServicioRepository;
    }

    public List<TipoServicioDTO> listar() {
        return tipoServicioRepository.findAllByOrderByNombreAsc().stream()
            .map(TipoServicioDTO::desdeTipoServicio)
            .toList();
    }

    public TipoServicioDTO crear(TipoServicioRequest request) {
        validarNombreUnico(request.getNombre(), null);

        TipoServicio tipo = TipoServicio.builder()
            .nombre(request.getNombre().trim())
            .descripcion(request.getDescripcion().trim())
            .observaciones(request.getObservaciones() != null ? request.getObservaciones().trim() : null)
            .activo(true)
            .build();

        return TipoServicioDTO.desdeTipoServicio(tipoServicioRepository.save(tipo));
    }

    public TipoServicioDTO modificar(Long id, TipoServicioRequest request) {
        TipoServicio tipo = obtener(id);

        validarNombreUnico(request.getNombre(), id);

        tipo.setNombre(request.getNombre().trim());
        tipo.setDescripcion(request.getDescripcion().trim());
        tipo.setObservaciones(request.getObservaciones() != null ? request.getObservaciones().trim() : null);

        return TipoServicioDTO.desdeTipoServicio(tipoServicioRepository.save(tipo));
    }

    public TipoServicioDTO darDeBaja(Long id) {
        TipoServicio tipo = obtener(id);
        tipo.setActivo(false);
        return TipoServicioDTO.desdeTipoServicio(tipoServicioRepository.save(tipo));
    }

    public TipoServicioDTO darDeAlta(Long id) {
        TipoServicio tipo = obtener(id);
        tipo.setActivo(true);
        return TipoServicioDTO.desdeTipoServicio(tipoServicioRepository.save(tipo));
    }

    private TipoServicio obtener(Long id) {
        return tipoServicioRepository.findByIdTipoServicio(id)
            .orElseThrow(() -> new NoSuchElementException("Tipo de servicio no encontrado: " + id));
    }

    private void validarNombreUnico(String nombre, Long idExcluido) {
        boolean existe = (idExcluido == null)
            ? tipoServicioRepository.existsByNombreIgnoreCase(nombre.trim())
            : tipoServicioRepository.existsByNombreIgnoreCaseAndIdTipoServicioNot(nombre.trim(), idExcluido);

        if (existe) {
            throw new IllegalArgumentException("Ya existe un tipo de servicio con ese nombre.");
        }
    }
}