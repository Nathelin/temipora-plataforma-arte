package com.temipora.backend_springboot.servicios;

import java.util.List;
import java.util.NoSuchElementException;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import com.temipora.backend_springboot.modelo.TipoServicio;
import com.temipora.backend_springboot.modelo.TipoServicioDTO;
import com.temipora.backend_springboot.modelo.TipoServicioRequest;
import com.temipora.backend_springboot.repositorios.TipoServicioRepository;

@Service
@Validated
public class TipoServicioService {

    private static final String ENTIDAD = "TipoServicio";
    private static final List<CampoAuditado> CAMPOS = List.of(
        CampoAuditado.de("nombre", "Nombre"),
        CampoAuditado.de("descripcion", "Descripción"),
        CampoAuditado.de("observaciones", "Observaciones"),
        CampoAuditado.de("activo", "Activo"));

    private final TipoServicioRepository tipoServicioRepository;
    private final AuditoriaService auditoria;

    public TipoServicioService(
            TipoServicioRepository tipoServicioRepository,
            AuditoriaService auditoria) {
        this.tipoServicioRepository = tipoServicioRepository;
        this.auditoria = auditoria;
    }

    public List<TipoServicioDTO> listar() {
        return tipoServicioRepository.findAllByOrderByNombreAsc().stream()
            .map(TipoServicioDTO::desdeTipoServicio)
            .toList();
    }

    public List<TipoServicioDTO> listarActivos() {
        return tipoServicioRepository.findByActivoTrueOrderByNombreAsc().stream()
            .map(TipoServicioDTO::desdeTipoServicio)
            .toList();
    }

    @Transactional
    public TipoServicioDTO crear(TipoServicioRequest request) {
        validarNombreUnico(request.getNombre(), null);

        TipoServicio tipo = TipoServicio.builder()
            .nombre(request.getNombre().trim())
            .descripcion(request.getDescripcion().trim())
            .observaciones(request.getObservaciones() != null ? request.getObservaciones().trim() : null)
            .activo(true)
            .build();

        TipoServicio guardado = tipoServicioRepository.save(tipo);
        auditoria.registrar(
            AccionesAdmin.TIPO_SERVICIO_CREAR, ENTIDAD, guardado.getIdTipoServicio(),
            "Creó el tipo de servicio “" + guardado.getNombre() + "”", null, guardado, CAMPOS);
        return TipoServicioDTO.desdeTipoServicio(guardado);
    }

    @Transactional
    public TipoServicioDTO modificar(Long id, TipoServicioRequest request) {
        TipoServicio tipo = obtener(id);
        Snapshot antes = Snapshot.de(tipo, CAMPOS);

        validarNombreUnico(request.getNombre(), id);

        tipo.setNombre(request.getNombre().trim());
        tipo.setDescripcion(request.getDescripcion().trim());
        tipo.setObservaciones(request.getObservaciones() != null ? request.getObservaciones().trim() : null);

        TipoServicio guardado = tipoServicioRepository.save(tipo);
        auditoria.registrar(
            AccionesAdmin.TIPO_SERVICIO_MODIFICAR, ENTIDAD, guardado.getIdTipoServicio(),
            "Modificó el tipo de servicio “" + guardado.getNombre() + "”", antes, guardado, CAMPOS);
        return TipoServicioDTO.desdeTipoServicio(guardado);
    }

    @Transactional
    public TipoServicioDTO darDeBaja(Long id) {
        TipoServicio tipo = obtener(id);
        Snapshot antes = Snapshot.de(tipo, CAMPOS);
        tipo.setActivo(false);
        TipoServicio guardado = tipoServicioRepository.save(tipo);
        auditoria.registrar(
            AccionesAdmin.TIPO_SERVICIO_BAJA, ENTIDAD, guardado.getIdTipoServicio(),
            "Dio de baja el tipo de servicio “" + guardado.getNombre() + "”", antes, guardado, CAMPOS);
        return TipoServicioDTO.desdeTipoServicio(guardado);
    }

    @Transactional
    public TipoServicioDTO darDeAlta(Long id) {
        TipoServicio tipo = obtener(id);
        Snapshot antes = Snapshot.de(tipo, CAMPOS);
        tipo.setActivo(true);
        TipoServicio guardado = tipoServicioRepository.save(tipo);
        auditoria.registrar(
            AccionesAdmin.TIPO_SERVICIO_REACTIVAR, ENTIDAD, guardado.getIdTipoServicio(),
            "Reactivó el tipo de servicio “" + guardado.getNombre() + "”", antes, guardado, CAMPOS);
        return TipoServicioDTO.desdeTipoServicio(guardado);
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