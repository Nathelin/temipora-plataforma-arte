package com.temipora.backend_springboot.servicios;

import java.util.List;
import java.util.NoSuchElementException;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import com.temipora.backend_springboot.modelo.TipoProducto;
import com.temipora.backend_springboot.modelo.TipoProductoDTO;
import com.temipora.backend_springboot.modelo.TipoProductoRequest;
import com.temipora.backend_springboot.repositorios.TipoProductoRepository;

@Service
@Validated
public class TipoProductoService {

    private static final String ENTIDAD = "TipoProducto";
    private static final List<CampoAuditado> CAMPOS = List.of(
        CampoAuditado.de("nombre", "Nombre"),
        CampoAuditado.de("descripcion", "Descripción"),
        CampoAuditado.de("observaciones", "Observaciones"),
        CampoAuditado.de("activo", "Activo"));

    private final TipoProductoRepository tipoProductoRepository;
    private final AuditoriaService auditoria;

    public TipoProductoService(
            TipoProductoRepository tipoProductoRepository,
            AuditoriaService auditoria) {
        this.tipoProductoRepository = tipoProductoRepository;
        this.auditoria = auditoria;
    }

    public List<TipoProductoDTO> listar() {
        return tipoProductoRepository.findAllByOrderByNombreAsc().stream()
            .map(TipoProductoDTO::desdeTipoProducto)
            .toList();
    }

    @Transactional
    public TipoProductoDTO crear(TipoProductoRequest request) {
        validarNombreUnico(request.getNombre(), null);

        TipoProducto tipo = TipoProducto.builder()
            .nombre(request.getNombre().trim())
            .descripcion(request.getDescripcion().trim())
            .observaciones(request.getObservaciones() != null ? request.getObservaciones().trim() : null)
            .activo(true)
            .build();

        TipoProducto guardado = tipoProductoRepository.save(tipo);
        auditoria.registrar(
            AccionesAdmin.TIPO_PRODUCTO_CREAR, ENTIDAD, guardado.getIdTipoProducto(),
            "Creó el tipo de producto “" + guardado.getNombre() + "”", null, guardado, CAMPOS);
        return TipoProductoDTO.desdeTipoProducto(guardado);
    }

    @Transactional
    public TipoProductoDTO modificar(Long id, TipoProductoRequest request) {
        TipoProducto tipo = obtener(id);
        Snapshot antes = Snapshot.de(tipo, CAMPOS);

        validarNombreUnico(request.getNombre(), id);

        tipo.setNombre(request.getNombre().trim());
        tipo.setDescripcion(request.getDescripcion().trim());
        tipo.setObservaciones(request.getObservaciones() != null ? request.getObservaciones().trim() : null);

        TipoProducto guardado = tipoProductoRepository.save(tipo);
        auditoria.registrar(
            AccionesAdmin.TIPO_PRODUCTO_MODIFICAR, ENTIDAD, guardado.getIdTipoProducto(),
            "Modificó el tipo de producto “" + guardado.getNombre() + "”", antes, guardado, CAMPOS);
        return TipoProductoDTO.desdeTipoProducto(guardado);
    }

    @Transactional
    public TipoProductoDTO darDeBaja(Long id) {
        TipoProducto tipo = obtener(id);
        Snapshot antes = Snapshot.de(tipo, CAMPOS);
        tipo.setActivo(false);
        TipoProducto guardado = tipoProductoRepository.save(tipo);
        auditoria.registrar(
            AccionesAdmin.TIPO_PRODUCTO_BAJA, ENTIDAD, guardado.getIdTipoProducto(),
            "Dio de baja el tipo de producto “" + guardado.getNombre() + "”", antes, guardado, CAMPOS);
        return TipoProductoDTO.desdeTipoProducto(guardado);
    }

    @Transactional
    public TipoProductoDTO darDeAlta(Long id) {
        TipoProducto tipo = obtener(id);
        Snapshot antes = Snapshot.de(tipo, CAMPOS);
        tipo.setActivo(true);
        TipoProducto guardado = tipoProductoRepository.save(tipo);
        auditoria.registrar(
            AccionesAdmin.TIPO_PRODUCTO_REACTIVAR, ENTIDAD, guardado.getIdTipoProducto(),
            "Reactivó el tipo de producto “" + guardado.getNombre() + "”", antes, guardado, CAMPOS);
        return TipoProductoDTO.desdeTipoProducto(guardado);
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