package com.temipora.backend_springboot.servicios;

import java.util.List;
import java.util.NoSuchElementException;

import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import com.temipora.backend_springboot.modelo.TipoProducto;
import com.temipora.backend_springboot.modelo.TipoProductoDTO;
import com.temipora.backend_springboot.modelo.TipoProductoRequest;
import com.temipora.backend_springboot.repositorios.TipoProductoRepository;

@Service
@Validated
public class TipoProductoService {

    private final TipoProductoRepository tipoProductoRepository;

    public TipoProductoService(TipoProductoRepository tipoProductoRepository) {
        this.tipoProductoRepository = tipoProductoRepository;
    }

    public List<TipoProductoDTO> listar() {
        return tipoProductoRepository.findAllByOrderByNombreAsc().stream()
            .map(TipoProductoDTO::desdeTipoProducto)
            .toList();
    }

    public TipoProductoDTO crear(TipoProductoRequest request) {
        validarNombreUnico(request.getNombre(), null);

        TipoProducto tipo = TipoProducto.builder()
            .nombre(request.getNombre().trim())
            .descripcion(request.getDescripcion().trim())
            .observaciones(request.getObservaciones() != null ? request.getObservaciones().trim() : null)
            .activo(true)
            .build();

        return TipoProductoDTO.desdeTipoProducto(tipoProductoRepository.save(tipo));
    }

    public TipoProductoDTO modificar(Long id, TipoProductoRequest request) {
        TipoProducto tipo = obtener(id);

        validarNombreUnico(request.getNombre(), id);

        tipo.setNombre(request.getNombre().trim());
        tipo.setDescripcion(request.getDescripcion().trim());
        tipo.setObservaciones(request.getObservaciones() != null ? request.getObservaciones().trim() : null);

        return TipoProductoDTO.desdeTipoProducto(tipoProductoRepository.save(tipo));
    }

    public TipoProductoDTO darDeBaja(Long id) {
        TipoProducto tipo = obtener(id);
        tipo.setActivo(false);
        return TipoProductoDTO.desdeTipoProducto(tipoProductoRepository.save(tipo));
    }

    public TipoProductoDTO darDeAlta(Long id) {
        TipoProducto tipo = obtener(id);
        tipo.setActivo(true);
        return TipoProductoDTO.desdeTipoProducto(tipoProductoRepository.save(tipo));
    }

    private TipoProducto obtener(Long id) {
        return tipoProductoRepository.findByIdTipoProducto(id)
            .orElseThrow(() -> new NoSuchElementException("Tipo de producto no encontrado: " + id));
    }

    private void validarNombreUnico(String nombre, Long idExcluido) {
        boolean existe = (idExcluido == null)
            ? tipoProductoRepository.existsByNombreIgnoreCase(nombre.trim())
            : tipoProductoRepository.existsByNombreIgnoreCaseAndIdTipoProductoNot(nombre.trim(), idExcluido);

        if (existe) {
            throw new IllegalArgumentException("Ya existe un tipo de producto con ese nombre.");
        }
    }
}