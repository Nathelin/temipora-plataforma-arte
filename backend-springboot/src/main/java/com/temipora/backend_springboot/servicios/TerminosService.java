package com.temipora.backend_springboot.servicios;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.stream.Stream;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import com.temipora.backend_springboot.modelo.DerechoTermino;
import com.temipora.backend_springboot.modelo.Servicio;
import com.temipora.backend_springboot.modelo.TerminoDerecho;
import com.temipora.backend_springboot.modelo.TerminoDerechoRequest;
import com.temipora.backend_springboot.modelo.Terminos;
import com.temipora.backend_springboot.modelo.TerminosDTO;
import com.temipora.backend_springboot.modelo.TerminosRequest;
import com.temipora.backend_springboot.modelo.Usuario;
import com.temipora.backend_springboot.repositorios.DerechoTerminoRepository;
import com.temipora.backend_springboot.repositorios.ServicioRepository;
import com.temipora.backend_springboot.repositorios.TerminosRepository;
import com.temipora.backend_springboot.repositorios.UsuarioRepository;

@Service
@Validated
public class TerminosService {

    private static final String DERECHO_PERSONAL = "PERSONAL";

    private final TerminosRepository terminosRepository;
    private final DerechoTerminoRepository derechoTerminoRepository;
    private final UsuarioRepository usuarioRepository;
    private final ServicioRepository servicioRepository;

    public TerminosService(
            TerminosRepository terminosRepository,
            DerechoTerminoRepository derechoTerminoRepository,
            UsuarioRepository usuarioRepository,
            ServicioRepository servicioRepository) {
        this.terminosRepository = terminosRepository;
        this.derechoTerminoRepository = derechoTerminoRepository;
        this.usuarioRepository = usuarioRepository;
        this.servicioRepository = servicioRepository;
    }

    @Transactional(readOnly = true)
    public TerminosDTO obtenerGlobal(Long idArtista) {
        return terminosRepository
            .findFirstByArtistaIdUsuarioAndIdProductoIsNullAndServicioIsNullAndVigenteTrueOrderByVersionDesc(
                idArtista)
            .map(TerminosDTO::desdeTerminos)
            .orElseGet(TerminosDTO::vacio);
    }

    @Transactional
    public TerminosDTO guardarGlobal(Long idArtista, TerminosRequest request) {
        validarContenido(request);
        Terminos actual = terminosRepository
            .findFirstByArtistaIdUsuarioAndIdProductoIsNullAndServicioIsNullOrderByVersionDesc(
                idArtista)
            .orElse(null);
        return guardar(actual, idArtista, null, request);
    }

    @Transactional(readOnly = true)
    public TerminosDTO obtenerServicio(Long idServicio, Long idArtista) {
        return terminosRepository
            .findFirstByArtistaIdUsuarioAndServicioIdServicioAndVigenteTrueOrderByVersionDesc(
                idArtista, idServicio)
            .map(terminos -> TerminosDTO.desdeTerminos(terminos, true))
            .orElseGet(() -> obtenerGlobal(idArtista));
    }

    @Transactional
    public TerminosDTO guardarServicio(Long idServicio, Long idArtista, TerminosRequest request) {
        validarContenido(request);
        Terminos actual = terminosRepository
            .findFirstByArtistaIdUsuarioAndServicioIdServicioOrderByVersionDesc(
                idArtista, idServicio)
            .orElse(null);
        return guardar(actual, idArtista, idServicio, request);
    }

    @Transactional(readOnly = true)
    public TerminosDTO obtenerProducto(Long idProducto, Long idArtista) {
        return terminosRepository
            .findFirstByArtistaIdUsuarioAndIdProductoAndVigenteTrueOrderByVersionDesc(
                idArtista, idProducto)
            .map(terminos -> TerminosDTO.desdeTerminos(terminos, false))
            .orElseGet(() -> obtenerGlobal(idArtista));
    }

    @Transactional
    public TerminosDTO guardarProducto(Long idProducto, Long idArtista, TerminosRequest request) {
        validarContenido(request);
        Terminos actual = terminosRepository
            .findFirstByArtistaIdUsuarioAndIdProductoOrderByVersionDesc(
                idArtista, idProducto)
            .orElse(null);
        return guardar(actual, idArtista, null, idProducto, request);
    }

    @Transactional
    public void eliminarServicio(Long idServicio, Long idArtista) {
        if (!tieneGlobalValido(idArtista)) {
            throw new IllegalArgumentException(
                "Antes de quitar los términos propios, completá tus términos globales.");
        }
        terminosRepository
            .findFirstByArtistaIdUsuarioAndServicioIdServicioAndVigenteTrueOrderByVersionDesc(
                idArtista, idServicio)
            .ifPresent(terminos -> {
                terminos.setVigente(false);
                terminosRepository.save(terminos);
            });
    }

    @Transactional
    public void eliminarProducto(Long idProducto, Long idArtista) {
        if (!tieneGlobalValido(idArtista)) {
            throw new IllegalArgumentException(
                "Antes de quitar los términos propios, completá tus términos globales.");
        }
        terminosRepository
            .findFirstByArtistaIdUsuarioAndIdProductoAndVigenteTrueOrderByVersionDesc(
                idArtista, idProducto)
            .ifPresent(terminos -> {
                terminos.setVigente(false);
                terminosRepository.save(terminos);
            });
    }

    @Transactional(readOnly = true)
    public boolean tieneGlobalValido(Long idArtista) {
        return terminosRepository
            .findFirstByArtistaIdUsuarioAndIdProductoIsNullAndServicioIsNullAndVigenteTrueOrderByVersionDesc(
                idArtista)
            .map(this::tieneContenido)
            .orElse(false);
    }

    @Transactional(readOnly = true)
    public boolean tieneTerminosValidos(Long idServicio, Long idArtista) {
        return terminosRepository
            .findFirstByArtistaIdUsuarioAndServicioIdServicioAndVigenteTrueOrderByVersionDesc(
                idArtista, idServicio)
            .map(this::tieneContenido)
            .orElseGet(() -> tieneGlobalValido(idArtista));
    }

    @Transactional(readOnly = true)
    public boolean tieneProductoValido(Long idProducto, Long idArtista) {
        return terminosRepository
            .findFirstByArtistaIdUsuarioAndIdProductoAndVigenteTrueOrderByVersionDesc(
                idArtista, idProducto)
            .map(this::tieneContenido)
            .orElseGet(() -> tieneGlobalValido(idArtista));
    }

    /**
     * Huella del contenido vigente de los términos aplicables al producto
     * (propios del producto o globales del artista). Se usa para dejar
     * registrado qué condiciones regían al momento del génesis.
     */
    @Transactional(readOnly = true)
    public String hashVigenteProducto(Long idProducto, Long idArtista) {
        Terminos terminos = terminosRepository
            .findFirstByArtistaIdUsuarioAndIdProductoAndVigenteTrueOrderByVersionDesc(
                idArtista, idProducto)
            .orElseGet(() -> terminosRepository
                .findFirstByArtistaIdUsuarioAndIdProductoIsNullAndServicioIsNullAndVigenteTrueOrderByVersionDesc(
                    idArtista)
                .orElse(null));
        if (terminos == null) {
            return null;
        }
        String contenido = String.join("|",
            String.valueOf(terminos.getIdTerminos()),
            String.valueOf(terminos.getVersion()),
            nvl(terminos.getInformacionGeneral()),
            nvl(terminos.getPoliticaRevisiones()),
            nvl(terminos.getPoliticaCancelaciones()),
            nvl(terminos.getUsoArtista()),
            nvl(terminos.getUsoCliente()),
            nvl(terminos.getPagoExtra()),
            nvl(terminos.getQueDibuja()),
            nvl(terminos.getQueNoDibuja()));
        try {
            return HexFormat.of().formatHex(
                MessageDigest.getInstance("SHA-256")
                    .digest(contenido.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 no disponible.", e);
        }
    }

    /**
     * Cada guardado crea una versión nueva e inmutable. La versión anterior se
     * conserva marcada como no vigente para que las solicitudes que ya la
     * aceptaron sigan apuntando a su contenido original.
     */
    private TerminosDTO guardar(
            Terminos actual, Long idArtista, Long idServicio, TerminosRequest request) {
        return guardar(actual, idArtista, idServicio, null, request);
    }

    private TerminosDTO guardar(
            Terminos actual, Long idArtista, Long idServicio, Long idProducto,
            TerminosRequest request) {
        Terminos nueva;
        if (actual == null) {
            nueva = nuevoTerminos(idArtista, idServicio, idProducto);
        } else {
            actual.setVigente(false);
            terminosRepository.save(actual);
            // Hibernate ordena los INSERT antes de los UPDATE. Sin este flush, la
            // versión nueva se insertaría con vigente=true mientras la anterior
            // todavía lo está, violando uk_terminos_*_vigente.
            terminosRepository.flush();
            nueva = nuevoTerminos(idArtista, idServicio, idProducto);
            nueva.setVersion(actual.getVersion() + 1);
            nueva.setVigente(true);
        }
        aplicar(nueva, request);
        Terminos guardados = terminosRepository.save(nueva);
        return TerminosDTO.desdeTerminos(guardados, idServicio != null);
    }

    private void aplicar(Terminos terminos, TerminosRequest request) {
        terminos.setInformacionGeneral(limpiar(request.getInformacionGeneral()));
        terminos.setPoliticaRevisiones(limpiar(request.getPoliticaRevisiones()));
        terminos.setPoliticaCancelaciones(limpiar(request.getPoliticaCancelaciones()));
        terminos.setUsoArtista(limpiar(request.getUsoArtista()));
        terminos.setUsoCliente(limpiar(request.getUsoCliente()));
        terminos.setPagoExtra(limpiar(request.getPagoExtra()));
        terminos.setQueDibuja(limpiar(request.getQueDibuja()));
        terminos.setQueNoDibuja(limpiar(request.getQueNoDibuja()));
        terminos.reemplazarDerechos(construirDerechos(request.getDerechos()));
        terminos.setFechaActualizacion(LocalDateTime.now());
    }

    private Terminos nuevoTerminos(Long idArtista, Long idServicio) {
        return nuevoTerminos(idArtista, idServicio, null);
    }

    private Terminos nuevoTerminos(Long idArtista, Long idServicio, Long idProducto) {
        Usuario artista = usuarioRepository.findByIdUsuario(idArtista)
            .orElseThrow(() -> new NoSuchElementException("Usuario no encontrado: " + idArtista));
        Terminos nuevo = new Terminos();
        nuevo.setArtista(artista);
        if (idServicio != null) {
            Servicio servicio = servicioRepository.findByIdServicio(idServicio)
                .orElseThrow(() -> new NoSuchElementException(
                    "Servicio no encontrado: " + idServicio));
            nuevo.setServicio(servicio);
        }
        if (idProducto != null) {
            nuevo.setIdProducto(idProducto);
        }
        nuevo.setVersion(1);
        nuevo.setVigente(true);
        return nuevo;
    }

    private List<TerminoDerecho> construirDerechos(List<TerminoDerechoRequest> requests) {
        List<TerminoDerecho> lista = new ArrayList<>();
        if (requests == null) {
            return lista;
        }
        Set<Long> vistos = new LinkedHashSet<>();
        for (TerminoDerechoRequest r : requests) {
            if (!vistos.add(r.getIdDerechoTermino())) {
                continue;
            }
            DerechoTermino derecho = derechoTerminoRepository.findByIdDerechoTermino(r.getIdDerechoTermino())
                .orElseThrow(() -> new IllegalArgumentException("Derecho no encontrado: " + r.getIdDerechoTermino()));

            boolean esPersonal = DERECHO_PERSONAL.equals(derecho.getCodigo());

            TerminoDerecho terminoDerecho = new TerminoDerecho();
            terminoDerecho.setDerecho(derecho);
            terminoDerecho.setDisponible(esPersonal || r.isDisponible());
            terminoDerecho.setPorcentajeExtra(esPersonal ? null : r.getPorcentajeExtra());
            lista.add(terminoDerecho);
        }
        return lista;
    }

    private void validarContenido(TerminosRequest r) {
        boolean hayTexto = Stream.of(
                r.getInformacionGeneral(),
                r.getPoliticaRevisiones(),
                r.getPoliticaCancelaciones(),
                r.getUsoArtista(),
                r.getUsoCliente(),
                r.getQueDibuja(),
                r.getQueNoDibuja())
            .anyMatch(s -> s != null && !s.isBlank());

        if (!hayTexto) {
            throw new IllegalArgumentException("Completá al menos una sección de tus términos.");
        }
    }

    private boolean tieneContenido(Terminos terminos) {
        return Stream.of(
                terminos.getInformacionGeneral(),
                terminos.getPoliticaRevisiones(),
                terminos.getPoliticaCancelaciones(),
                terminos.getUsoArtista(),
                terminos.getUsoCliente(),
                terminos.getQueDibuja(),
                terminos.getQueNoDibuja())
            .anyMatch(this::hayTexto);
    }

    private boolean hayTexto(String valor) {
        return valor != null && !valor.isBlank();
    }

    private String limpiar(String valor) {
        return valor != null && !valor.isBlank() ? valor.trim() : null;
    }

    private String nvl(String valor) {
        return valor != null ? valor : "";
    }
}
