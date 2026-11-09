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

    private static final String ENTIDAD = "Medio";
    private static final List<CampoAuditado> CAMPOS = List.of(
        CampoAuditado.de("nombre", "Nombre"),
        CampoAuditado.de("descripcion", "Descripción"),
        CampoAuditado.de("observaciones", "Observaciones"),
        CampoAuditado.de("tipoEntrega", "Tipo de entrega"),
        CampoAuditado.de("activo", "Activo"));

    private final MedioRepository medioRepository;
    private final AuditoriaService auditoria;

    public MedioService(MedioRepository medioRepository, AuditoriaService auditoria) {
        this.medioRepository = medioRepository;
        this.auditoria = auditoria;
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
            .tipoEntrega(request.getTipoEntrega())
            .activo(true)
            .build();

        Medio guardado = medioRepository.save(medio);
        auditoria.registrar(
            AccionesAdmin.MEDIO_CREAR, ENTIDAD, guardado.getIdMedio(),
            "Creó el medio “" + guardado.getNombre() + "”", null, guardado, CAMPOS);
        return MedioDTO.desdeMedio(guardado);
    }

    @Transactional
    public MedioDTO modificar(Long id, MedioRequest request) {
        Medio medio = obtener(id);
        Snapshot antes = Snapshot.de(medio, CAMPOS);
        validarNombreUnico(request.getNombre(), id);

        medio.setNombre(request.getNombre().trim());
        medio.setDescripcion(request.getDescripcion() != null ? request.getDescripcion().trim() : null);
        medio.setObservaciones(request.getObservaciones() != null ? request.getObservaciones().trim() : null);
        medio.setTipoEntrega(request.getTipoEntrega());

        Medio guardado = medioRepository.save(medio);
        auditoria.registrar(
            AccionesAdmin.MEDIO_MODIFICAR, ENTIDAD, guardado.getIdMedio(),
            "Modificó el medio “" + guardado.getNombre() + "”", antes, guardado, CAMPOS);
        return MedioDTO.desdeMedio(guardado);
    }

    @Transactional
    public MedioDTO darDeBaja(Long id) {
        Medio medio = obtener(id);
        Snapshot antes = Snapshot.de(medio, CAMPOS);
        medio.setActivo(false);
        Medio guardado = medioRepository.save(medio);
        auditoria.registrar(
            AccionesAdmin.MEDIO_BAJA, ENTIDAD, guardado.getIdMedio(),
            "Dio de baja el medio “" + guardado.getNombre() + "”", antes, guardado, CAMPOS);
        return MedioDTO.desdeMedio(guardado);
    }

    @Transactional
    public MedioDTO darDeAlta(Long id) {
        Medio medio = obtener(id);
        Snapshot antes = Snapshot.de(medio, CAMPOS);
        medio.setActivo(true);
        Medio guardado = medioRepository.save(medio);
        auditoria.registrar(
            AccionesAdmin.MEDIO_REACTIVAR, ENTIDAD, guardado.getIdMedio(),
            "Reactivó el medio “" + guardado.getNombre() + "”", antes, guardado, CAMPOS);
        return MedioDTO.desdeMedio(guardado);
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
