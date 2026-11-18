package com.temipora.backend_springboot.servicios;

import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.temipora.backend_springboot.modelo.AceptacionTerminosPlataforma;
import com.temipora.backend_springboot.modelo.ComisionConfigDTO;
import com.temipora.backend_springboot.modelo.TerminosPlataforma;
import com.temipora.backend_springboot.modelo.TerminosPlataformaDTO;
import com.temipora.backend_springboot.modelo.TerminosPlataformaEstadoDTO;
import com.temipora.backend_springboot.modelo.TerminosPlataformaRequest;
import com.temipora.backend_springboot.modelo.Usuario;
import com.temipora.backend_springboot.repositorios.AceptacionTerminosPlataformaRepository;
import com.temipora.backend_springboot.repositorios.TerminosPlataformaRepository;
import com.temipora.backend_springboot.repositorios.UsuarioRepository;

@Service
public class TerminosPlataformaService {

    private static final String ENTIDAD = "TerminosPlataforma";
    private static final String ENTIDAD_USUARIO = "Usuario";
    private static final List<CampoAuditado> CAMPOS = List.of(
        CampoAuditado.de("version", "Versión"),
        CampoAuditado.de("titulo", "Título"),
        CampoAuditado.de("comisionServicio", "Comisión de servicios"),
        CampoAuditado.de("comisionProducto", "Comisión de productos"));

    private final TerminosPlataformaRepository terminosRepository;
    private final AceptacionTerminosPlataformaRepository aceptacionRepository;
    private final UsuarioRepository usuarioRepository;
    private final ComisionService comisionService;
    private final AuditoriaService auditoria;

    public TerminosPlataformaService(
            TerminosPlataformaRepository terminosRepository,
            AceptacionTerminosPlataformaRepository aceptacionRepository,
            UsuarioRepository usuarioRepository,
            ComisionService comisionService,
            AuditoriaService auditoria) {
        this.terminosRepository = terminosRepository;
        this.aceptacionRepository = aceptacionRepository;
        this.usuarioRepository = usuarioRepository;
        this.comisionService = comisionService;
        this.auditoria = auditoria;
    }

    @Transactional(readOnly = true)
    public TerminosPlataforma obtenerEntidadVigente() {
        return terminosRepository.findFirstByVigenteTrueOrderByVersionDesc()
            .orElseThrow(() -> new NoSuchElementException(
                "La plataforma todavía no publicó sus términos y condiciones."));
    }

    @Transactional(readOnly = true)
    public TerminosPlataformaDTO obtenerVigente() {
        return TerminosPlataformaDTO.desdeTerminos(obtenerEntidadVigente());
    }

    /**
     * Obtiene la aceptación de la versión vigente para un usuario. Si no existe,
     * la acción debe bloquearse: primero hay que aceptar los TOS actuales.
     */
    @Transactional(readOnly = true)
    public AceptacionTerminosPlataforma obtenerAceptacionVigente(Long idUsuario) {
        TerminosPlataforma actual;
        try {
            actual = obtenerEntidadVigente();
        } catch (NoSuchElementException e) {
            throw new ConflictoException(
                "La plataforma todavía no publicó sus términos y condiciones.");
        }
        return aceptacionRepository
            .findByUsuarioIdUsuarioAndVersion(idUsuario, actual.getVersion())
            .orElseThrow(() -> new ConflictoException(
                "Tenés que aceptar los términos de la plataforma vigentes."));
    }

    /**
     * Estado de los términos para el usuario: versión vigente y si ya la aceptó.
     * Si la plataforma no publicó términos, no hay nada pendiente.
     */
    @Transactional(readOnly = true)
    public TerminosPlataformaEstadoDTO obtenerEstadoUsuario(Long idUsuario) {
        Optional<TerminosPlataforma> actual = terminosRepository
            .findFirstByVigenteTrueOrderByVersionDesc();
        if (actual.isEmpty()) {
            return TerminosPlataformaEstadoDTO.builder()
                .aceptada(true)
                .build();
        }
        TerminosPlataforma terminos = actual.get();
        Optional<AceptacionTerminosPlataforma> aceptacion =
            aceptacionRepository.findByUsuarioIdUsuarioAndVersion(
                idUsuario, terminos.getVersion());
        return estado(terminos, aceptacion);
    }

    /**
     * Registra la aceptación de la versión vigente. Es idempotente y rechaza
     * aceptar una versión que ya no es la actual.
     */
    @Transactional
    public TerminosPlataformaEstadoDTO aceptar(Long idUsuario, Integer version) {
        if (version == null) {
            throw new IllegalArgumentException(
                "La versión de los términos es obligatoria.");
        }
        TerminosPlataforma actual = obtenerEntidadVigente();
        if (!actual.getVersion().equals(version)) {
            throw new ConflictoException(
                "Los términos de la plataforma cambiaron. Revisalos y volvé a aceptarlos.");
        }
        Usuario usuario = usuarioRepository.findById(idUsuario)
            .orElseThrow(() -> new NoSuchElementException(
                "Usuario no encontrado: " + idUsuario));

        boolean yaAceptada = aceptacionRepository
            .findByUsuarioIdUsuarioAndVersion(idUsuario, version)
            .isPresent();
        if (!yaAceptada) {
            AceptacionTerminosPlataforma aceptacion =
                new AceptacionTerminosPlataforma();
            aceptacion.setUsuario(usuario);
            aceptacion.setTerminosPlataforma(actual);
            aceptacion.setVersion(version);
            aceptacion.setSnapshot(actual.getContenido());
            aceptacion.setFechaAceptacion(LocalDateTime.now());
            aceptacionRepository.save(aceptacion);
            auditoria.registrar(
                AccionesAdmin.TERMINOS_ACEPTAR, ENTIDAD_USUARIO, idUsuario,
                "Aceptó los términos de la plataforma (versión " + version + ")",
                "Aceptación registrada");
        }
        return obtenerEstadoUsuario(idUsuario);
    }

    @Transactional(readOnly = true)
    public List<TerminosPlataformaDTO> listarVersiones() {
        return terminosRepository.findAllByOrderByVersionDesc().stream()
            .map(TerminosPlataformaDTO::desdeTerminos)
            .toList();
    }

    /**
     * Publica una versión nueva e inmutable. La comisión declarada se congela
     * con los valores vigentes al momento de publicar, para que el documento
     * no cambie si después se modifica la configuración.
     */
    @Transactional
    public TerminosPlataformaDTO publicar(TerminosPlataformaRequest request) {
        String titulo = limpiar(request.getTitulo(), "El título es obligatorio.", 120);
        String contenido = limpiar(
            request.getContenido(), "El contenido es obligatorio.", Integer.MAX_VALUE);

        ComisionConfigDTO comision = comisionService.obtenerEstado();

        int version = terminosRepository.findFirstByOrderByVersionDesc()
            .map(actual -> actual.getVersion() + 1)
            .orElse(1);

        for (TerminosPlataforma anterior : terminosRepository.findByVigenteTrue()) {
            anterior.setVigente(false);
            terminosRepository.save(anterior);
        }

        TerminosPlataforma terminos = new TerminosPlataforma();
        terminos.setVersion(version);
        terminos.setTitulo(titulo);
        terminos.setContenido(contenido);
        terminos.setComisionServicio(comision.getPorcentajeServicio());
        terminos.setComisionProducto(comision.getPorcentajeProducto());
        terminos.setFechaPublicacion(LocalDateTime.now());
        terminos.setVigente(true);
        TerminosPlataforma guardada = terminosRepository.save(terminos);
        auditoria.registrar(
            AccionesAdmin.TERMINOS_PUBLICAR, ENTIDAD, guardada.getIdTerminosPlataforma(),
            "Publicó la versión " + guardada.getVersion() + " de los términos ("
                + contenido.length() + " caracteres)",
            null, guardada, CAMPOS);
        return TerminosPlataformaDTO.desdeTerminos(guardada);
    }

    private TerminosPlataformaEstadoDTO estado(
            TerminosPlataforma terminos,
            Optional<AceptacionTerminosPlataforma> aceptacion) {
        return TerminosPlataformaEstadoDTO.builder()
            .version(terminos.getVersion())
            .titulo(terminos.getTitulo())
            .contenido(terminos.getContenido())
            .fechaPublicacion(terminos.getFechaPublicacion())
            .aceptada(aceptacion.isPresent())
            .fechaAceptacion(aceptacion
                .map(AceptacionTerminosPlataforma::getFechaAceptacion)
                .orElse(null))
            .build();
    }

    private String limpiar(String valor, String mensaje, int maximo) {
        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException(mensaje);
        }
        String limpio = valor.trim();
        if (limpio.length() > maximo) {
            throw new IllegalArgumentException(
                "El texto no puede superar los " + maximo + " caracteres.");
        }
        return limpio;
    }
}
