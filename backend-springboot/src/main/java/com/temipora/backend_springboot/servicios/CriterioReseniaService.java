package com.temipora.backend_springboot.servicios;

import java.util.List;
import java.util.NoSuchElementException;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import com.temipora.backend_springboot.modelo.AmbitoResenia;
import com.temipora.backend_springboot.modelo.CriterioResenia;
import com.temipora.backend_springboot.modelo.CriterioReseniaDTO;
import com.temipora.backend_springboot.modelo.CriterioReseniaRequest;
import com.temipora.backend_springboot.modelo.EscalaCriterio;
import com.temipora.backend_springboot.modelo.OpcionCriterio;
import com.temipora.backend_springboot.modelo.OpcionCriterioRequest;
import com.temipora.backend_springboot.modelo.PolaridadOpcion;
import com.temipora.backend_springboot.repositorios.CriterioReseniaRepository;
import com.temipora.backend_springboot.repositorios.OpcionCriterioRepository;
import com.temipora.backend_springboot.repositorios.ReseniaOpcionRepository;
import com.temipora.backend_springboot.repositorios.ReseniaValorRepository;

@Service
@Validated
public class CriterioReseniaService {

    private final CriterioReseniaRepository criterioRepository;
    private final OpcionCriterioRepository opcionRepository;
    private final ReseniaValorRepository valorRepository;
    private final ReseniaOpcionRepository reseniaOpcionRepository;

    public CriterioReseniaService(
            CriterioReseniaRepository criterioRepository,
            OpcionCriterioRepository opcionRepository,
            ReseniaValorRepository valorRepository,
            ReseniaOpcionRepository reseniaOpcionRepository) {
        this.criterioRepository = criterioRepository;
        this.opcionRepository = opcionRepository;
        this.valorRepository = valorRepository;
        this.reseniaOpcionRepository = reseniaOpcionRepository;
    }

    @Transactional(readOnly = true)
    public List<CriterioReseniaDTO> listar(
            AmbitoResenia ambito, EscalaCriterio escala, Boolean activo) {
        return criterioRepository
            .findAllByOrderByAmbitoAscOrdenAscNombreAsc().stream()
            .filter(criterio -> ambito == null || criterio.getAmbito() == ambito)
            .filter(criterio -> escala == null || criterio.getEscala() == escala)
            .filter(criterio -> activo == null || criterio.isActivo() == activo)
            .map(this::aDto)
            .toList();
    }

    @Transactional(readOnly = true)
    public List<CriterioReseniaDTO> listarActivos(AmbitoResenia ambito) {
        if (ambito == null) {
            throw new IllegalArgumentException("El ámbito es obligatorio.");
        }
        return criterioRepository
            .findByActivoTrueAndAmbitoOrderByOrdenAscNombreAsc(ambito).stream()
            .map(criterio -> CriterioReseniaDTO.desdeCriterio(criterio, 0, true))
            .toList();
    }

    @Transactional
    public CriterioReseniaDTO crear(CriterioReseniaRequest request) {
        validarNombreUnico(request.getNombre(), request.getAmbito(), null);

        CriterioResenia criterio = CriterioResenia.builder()
            .nombre(request.getNombre().trim())
            .descripcion(request.getDescripcion().trim())
            .observaciones(limpiar(request.getObservaciones()))
            .ambito(request.getAmbito())
            .escala(request.getEscala())
            .obligatorio(request.isObligatorio())
            .orden(request.getOrden() != null ? request.getOrden() : 0)
            .activo(true)
            .build();

        return aDto(criterioRepository.save(criterio));
    }

    @Transactional
    public CriterioReseniaDTO modificar(Long id, CriterioReseniaRequest request) {
        CriterioResenia criterio = obtener(id);
        boolean tieneResenias = contarResenias(criterio) > 0;
        boolean cambiaAmbito = criterio.getAmbito() != request.getAmbito();

        if (tieneResenias && cambiaAmbito) {
            throw new IllegalArgumentException(
                "No podés cambiar el ámbito de un criterio que ya tiene reseñas. "
                    + "Creá un criterio nuevo para el otro ámbito.");
        }

        validarNombreUnico(request.getNombre(), request.getAmbito(), id);

        criterio.setNombre(request.getNombre().trim());
        criterio.setDescripcion(request.getDescripcion().trim());
        criterio.setObservaciones(limpiar(request.getObservaciones()));
        criterio.setAmbito(request.getAmbito());
        criterio.setEscala(request.getEscala());
        criterio.setObligatorio(request.isObligatorio());
        criterio.setOrden(request.getOrden() != null ? request.getOrden() : 0);

        return aDto(criterioRepository.save(criterio));
    }

    @Transactional
    public CriterioReseniaDTO darDeBaja(Long id) {
        CriterioResenia criterio = obtener(id);
        criterio.setActivo(false);
        return aDto(criterioRepository.save(criterio));
    }

    @Transactional
    public CriterioReseniaDTO darDeAlta(Long id) {
        CriterioResenia criterio = obtener(id);
        criterio.setActivo(true);
        return aDto(criterioRepository.save(criterio));
    }

    @Transactional
    public CriterioReseniaDTO crearOpcion(
            Long idCriterio, OpcionCriterioRequest request) {
        CriterioResenia criterio = obtener(idCriterio);
        if (criterio.getEscala() != EscalaCriterio.OPCIONES_MULTIPLES) {
            throw new IllegalArgumentException(
                "Solo los criterios de opciones múltiples admiten opciones.");
        }
        validarEtiquetaUnica(idCriterio, request.getEtiqueta(), null);

        OpcionCriterio opcion = OpcionCriterio.builder()
            .etiqueta(request.getEtiqueta().trim())
            .polaridad(request.getPolaridad() != null
                ? request.getPolaridad()
                : PolaridadOpcion.POSITIVA)
            .orden(request.getOrden() != null ? request.getOrden() : 0)
            .activo(true)
            .build();
        criterio.agregarOpcion(opcion);

        return aDto(criterioRepository.save(criterio));
    }

    @Transactional
    public CriterioReseniaDTO modificarOpcion(
            Long idCriterio, Long idOpcion, OpcionCriterioRequest request) {
        OpcionCriterio opcion = obtenerOpcion(idCriterio, idOpcion);
        validarEtiquetaUnica(idCriterio, request.getEtiqueta(), idOpcion);

        opcion.setEtiqueta(request.getEtiqueta().trim());
        if (request.getPolaridad() != null) {
            opcion.setPolaridad(request.getPolaridad());
        }
        opcion.setOrden(request.getOrden() != null ? request.getOrden() : 0);
        opcionRepository.save(opcion);

        return aDto(obtener(idCriterio));
    }

    @Transactional
    public CriterioReseniaDTO darDeBajaOpcion(Long idCriterio, Long idOpcion) {
        OpcionCriterio opcion = obtenerOpcion(idCriterio, idOpcion);
        boolean criterioConResenias =
            reseniaOpcionRepository.existsByOpcionCriterioIdCriterioResenia(idCriterio);
        long activas =
            opcionRepository.countByCriterioIdCriterioReseniaAndActivoTrue(idCriterio);

        if (criterioConResenias && activas <= 1) {
            throw new IllegalArgumentException(
                "No podés dar de baja la última opción activa de un criterio que ya tiene reseñas.");
        }

        opcion.setActivo(false);
        opcionRepository.save(opcion);

        return aDto(obtener(idCriterio));
    }

    @Transactional
    public CriterioReseniaDTO darDeAltaOpcion(Long idCriterio, Long idOpcion) {
        OpcionCriterio opcion = obtenerOpcion(idCriterio, idOpcion);
        opcion.setActivo(true);
        opcionRepository.save(opcion);

        return aDto(obtener(idCriterio));
    }

    private CriterioReseniaDTO aDto(CriterioResenia criterio) {
        return CriterioReseniaDTO.desdeCriterio(criterio, contarResenias(criterio));
    }

    private long contarResenias(CriterioResenia criterio) {
        Long id = criterio.getIdCriterioResenia();
        return valorRepository.contarReseniasPorCriterio(id)
            + reseniaOpcionRepository.contarReseniasPorCriterio(id);
    }

    private CriterioResenia obtener(Long id) {
        return criterioRepository.findByIdCriterioResenia(id)
            .orElseThrow(() -> new NoSuchElementException(
                "Criterio de reseña no encontrado: " + id));
    }

    private OpcionCriterio obtenerOpcion(Long idCriterio, Long idOpcion) {
        obtener(idCriterio);
        return opcionRepository
            .findByIdOpcionCriterioAndCriterioIdCriterioResenia(idOpcion, idCriterio)
            .orElseThrow(() -> new NoSuchElementException(
                "Opción de criterio no encontrada: " + idOpcion));
    }

    private void validarNombreUnico(
            String nombre, AmbitoResenia ambito, Long idExcluido) {
        if (ambito == null) {
            throw new IllegalArgumentException("El ámbito es obligatorio.");
        }
        String limpio = nombre.trim();
        boolean existe = (idExcluido == null)
            ? criterioRepository.existsByNombreIgnoreCaseAndAmbito(limpio, ambito)
            : criterioRepository
                .existsByNombreIgnoreCaseAndAmbitoAndIdCriterioReseniaNot(
                    limpio, ambito, idExcluido);

        if (existe) {
            throw new IllegalArgumentException(
                "Ya existe un criterio con ese nombre para este ámbito.");
        }
    }

    private void validarEtiquetaUnica(
            Long idCriterio, String etiqueta, Long idExcluido) {
        String limpia = etiqueta.trim();
        boolean existe = (idExcluido == null)
            ? opcionRepository
                .existsByCriterioIdCriterioReseniaAndEtiquetaIgnoreCase(idCriterio, limpia)
            : opcionRepository
                .existsByCriterioIdCriterioReseniaAndEtiquetaIgnoreCaseAndIdOpcionCriterioNot(
                    idCriterio, limpia, idExcluido);

        if (existe) {
            throw new IllegalArgumentException(
                "Ya existe una opción con esa etiqueta en el criterio.");
        }
    }

    private String limpiar(String valor) {
        if (valor == null) {
            return null;
        }
        String limpio = valor.trim();
        return limpio.isEmpty() ? null : limpio;
    }
}
