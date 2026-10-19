package com.temipora.backend_springboot.servicios;

import java.util.List;
import java.util.NoSuchElementException;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import com.temipora.backend_springboot.modelo.Moneda;
import com.temipora.backend_springboot.modelo.MonedaDTO;
import com.temipora.backend_springboot.modelo.MonedaRequest;
import com.temipora.backend_springboot.repositorios.MonedaRepository;

@Service
@Validated
public class MonedaService {

    private final MonedaRepository monedaRepository;

    public MonedaService(MonedaRepository monedaRepository) {
        this.monedaRepository = monedaRepository;
    }

    @Transactional(readOnly = true)
    public List<MonedaDTO> listar() {
        return monedaRepository.findAllByOrderByCodigoAsc().stream()
            .map(MonedaDTO::desdeMoneda)
            .toList();
    }

    @Transactional(readOnly = true)
    public List<MonedaDTO> listarActivas() {
        return monedaRepository.findByEstadoTrueOrderByCodigoAsc().stream()
            .map(MonedaDTO::desdeMoneda)
            .toList();
    }

    @Transactional(readOnly = true)
    public List<Moneda> activas() {
        return monedaRepository.findByEstadoTrueOrderByCodigoAsc();
    }

    @Transactional(readOnly = true)
    public Moneda predeterminada() {
        return monedaRepository.findFirstByPredeterminadaTrueAndEstadoTrue()
            .or(() -> monedaRepository.findFirstByEstadoTrueOrderByIdMonedaAsc())
            .or(() -> monedaRepository.findFirstByOrderByIdMonedaAsc())
            .orElse(null);
    }

    @Transactional(readOnly = true)
    public Moneda obtener(Long id) {
        return monedaRepository.findByIdMoneda(id)
            .orElseThrow(() -> new NoSuchElementException("Moneda no encontrada: " + id));
    }

    @Transactional(readOnly = true)
    public Moneda obtenerPorCodigo(String codigo) {
        if (codigo == null) {
            return null;
        }
        return monedaRepository.findByCodigoIgnoreCase(codigo).orElse(null);
    }

    @Transactional
    public MonedaDTO crear(MonedaRequest request) {
        String codigo = request.getCodigo().trim().toUpperCase();
        validarCodigoUnico(codigo, null);

        Moneda moneda = Moneda.builder()
            .codigo(codigo)
            .nombre(request.getNombre().trim())
            .simbolo(limpiar(request.getSimbolo()))
            .predeterminada(false)
            .estado(true)
            .build();

        Moneda guardada = monedaRepository.save(moneda);
        if (request.isPredeterminada()) {
            marcarPredeterminada(guardada.getIdMoneda());
        }
        return MonedaDTO.desdeMoneda(guardada);
    }

    @Transactional
    public MonedaDTO modificar(Long id, MonedaRequest request) {
        Moneda moneda = obtener(id);
        String codigo = request.getCodigo().trim().toUpperCase();
        validarCodigoUnico(codigo, id);

        moneda.setCodigo(codigo);
        moneda.setNombre(request.getNombre().trim());
        moneda.setSimbolo(limpiar(request.getSimbolo()));

        if (request.isPredeterminada()) {
            marcarPredeterminada(id);
        }
        return MonedaDTO.desdeMoneda(monedaRepository.save(moneda));
    }

    @Transactional
    public MonedaDTO marcarPredeterminada(Long id) {
        Moneda moneda = obtener(id);
        if (!moneda.isEstado()) {
            throw new IllegalArgumentException("No podés marcar como predeterminada una moneda dada de baja.");
        }
        List<Moneda> todas = monedaRepository.findAll();
        for (Moneda otra : todas) {
            otra.setPredeterminada(otra.getIdMoneda().equals(id));
        }
        monedaRepository.saveAll(todas);
        return MonedaDTO.desdeMoneda(moneda);
    }

    @Transactional
    public MonedaDTO darDeBaja(Long id) {
        Moneda moneda = obtener(id);
        if (moneda.isPredeterminada()) {
            throw new IllegalArgumentException(
                "No podés dar de baja la moneda predeterminada. Elegí otra como predeterminada primero.");
        }
        if (moneda.isEstado() && monedaRepository.countByEstadoTrue() <= 1) {
            throw new IllegalArgumentException("Debe quedar al menos una moneda activa.");
        }
        moneda.setEstado(false);
        return MonedaDTO.desdeMoneda(monedaRepository.save(moneda));
    }

    @Transactional
    public MonedaDTO darDeAlta(Long id) {
        Moneda moneda = obtener(id);
        moneda.setEstado(true);
        return MonedaDTO.desdeMoneda(monedaRepository.save(moneda));
    }

    private void validarCodigoUnico(String codigo, Long idExcluido) {
        boolean existe = (idExcluido == null)
            ? monedaRepository.existsByCodigoIgnoreCase(codigo)
            : monedaRepository.existsByCodigoIgnoreCaseAndIdMonedaNot(codigo, idExcluido);

        if (existe) {
            throw new IllegalArgumentException("Ya existe una moneda con ese código.");
        }
    }

    private String limpiar(String valor) {
        return valor != null && !valor.isBlank() ? valor.trim() : null;
    }
}
