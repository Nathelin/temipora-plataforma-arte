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

    private static final String ENTIDAD = "Pais";
    private static final List<CampoAuditado> CAMPOS = List.of(
        CampoAuditado.de("nombre", "Nombre"),
        CampoAuditado.de("codigoIso", "Código ISO"),
        CampoAuditado.de("prefijoTelefonico", "Prefijo telefónico"),
        CampoAuditado.de("estado", "Activo"));

    private final PaisRepository paisRepository;
    private final AuditoriaService auditoria;

    public PaisService(PaisRepository paisRepository, AuditoriaService auditoria) {
        this.paisRepository = paisRepository;
        this.auditoria = auditoria;
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

        Pais guardado = paisRepository.save(pais);
        auditoria.registrar(
            AccionesAdmin.PAIS_CREAR, ENTIDAD, guardado.getIdPais(),
            "Creó el país “" + guardado.getNombre() + "”", null, guardado, CAMPOS);
        return PaisDTO.desdePais(guardado);
    }

    @Transactional
    public PaisDTO modificar(Long id, PaisRequest request) {
        Pais pais = obtener(id);
        Snapshot antes = Snapshot.de(pais, CAMPOS);
        String nombre = request.getNombre().trim();
        String codigoIso = request.getCodigoIso().trim().toUpperCase();

        validarNombreUnico(nombre, id);
        validarCodigoUnico(codigoIso, id);

        pais.setNombre(nombre);
        pais.setCodigoIso(codigoIso);
        pais.setPrefijoTelefonico(request.getPrefijoTelefonico().trim());

        Pais guardado = paisRepository.save(pais);
        auditoria.registrar(
            AccionesAdmin.PAIS_MODIFICAR, ENTIDAD, guardado.getIdPais(),
            "Modificó el país “" + guardado.getNombre() + "”", antes, guardado, CAMPOS);
        return PaisDTO.desdePais(guardado);
    }

    @Transactional
    public PaisDTO darDeBaja(Long id) {
        Pais pais = obtener(id);
        Snapshot antes = Snapshot.de(pais, CAMPOS);
        pais.setEstado(false);
        Pais guardado = paisRepository.save(pais);
        auditoria.registrar(
            AccionesAdmin.PAIS_BAJA, ENTIDAD, guardado.getIdPais(),
            "Dio de baja el país “" + guardado.getNombre() + "”", antes, guardado, CAMPOS);
        return PaisDTO.desdePais(guardado);
    }

    @Transactional
    public PaisDTO darDeAlta(Long id) {
        Pais pais = obtener(id);
        Snapshot antes = Snapshot.de(pais, CAMPOS);
        pais.setEstado(true);
        Pais guardado = paisRepository.save(pais);
        auditoria.registrar(
            AccionesAdmin.PAIS_REACTIVAR, ENTIDAD, guardado.getIdPais(),
            "Reactivó el país “" + guardado.getNombre() + "”", antes, guardado, CAMPOS);
        return PaisDTO.desdePais(guardado);
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
