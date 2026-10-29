package com.temipora.backend_springboot.servicios;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import com.temipora.backend_springboot.modelo.AmbitoResenia;
import com.temipora.backend_springboot.modelo.ConfiguracionResenia;
import com.temipora.backend_springboot.modelo.CriterioResenia;
import com.temipora.backend_springboot.modelo.EscalaCriterio;
import com.temipora.backend_springboot.modelo.EstadoPedido;
import com.temipora.backend_springboot.modelo.EstadoResenia;
import com.temipora.backend_springboot.modelo.EtiquetaReputacion;
import com.temipora.backend_springboot.modelo.EntregaArchivo;
import com.temipora.backend_springboot.modelo.OpcionCriterio;
import com.temipora.backend_springboot.modelo.Pedido;
import com.temipora.backend_springboot.modelo.ReputacionClienteDTO;
import com.temipora.backend_springboot.modelo.Resenia;
import com.temipora.backend_springboot.modelo.ReseniaDTO;
import com.temipora.backend_springboot.modelo.ReseniaEstadoDTO;
import com.temipora.backend_springboot.modelo.ReseniaImagen;
import com.temipora.backend_springboot.modelo.ReseniaImagenDisponibleDTO;
import com.temipora.backend_springboot.modelo.ReseniaOpcion;
import com.temipora.backend_springboot.modelo.ReseniaRequest;
import com.temipora.backend_springboot.modelo.ReseniaValor;
import com.temipora.backend_springboot.modelo.ReseniaValorRequest;
import com.temipora.backend_springboot.modelo.ReseniaVersion;
import com.temipora.backend_springboot.modelo.Usuario;
import com.temipora.backend_springboot.repositorios.CriterioReseniaRepository;
import com.temipora.backend_springboot.repositorios.EntregaArchivoRepository;
import com.temipora.backend_springboot.repositorios.OpcionCriterioRepository;
import com.temipora.backend_springboot.repositorios.PedidoRepository;
import com.temipora.backend_springboot.repositorios.ReseniaRepository;
import com.temipora.backend_springboot.repositorios.SolicitudRepository;
import com.temipora.backend_springboot.repositorios.UsuarioRepository;

@Service
@Validated
public class ReseniaService {

    private static final int EDICIONES_MAXIMAS = 1;
    private static final String NOTIFICACION_RESENIA = "RESENIA_RECIBIDA";
    private static final String TIPO_REFERENCIA_PEDIDO = "PEDIDO";

    private final ReseniaRepository reseniaRepository;
    private final PedidoRepository pedidoRepository;
    private final UsuarioRepository usuarioRepository;
    private final CriterioReseniaRepository criterioRepository;
    private final OpcionCriterioRepository opcionRepository;
    private final SolicitudRepository solicitudRepository;
    private final ConfiguracionReseniaService configuracionService;
    private final NotificacionService notificacionService;
    private final EntregaArchivoRepository archivoRepository;

    public ReseniaService(
            ReseniaRepository reseniaRepository,
            PedidoRepository pedidoRepository,
            UsuarioRepository usuarioRepository,
            CriterioReseniaRepository criterioRepository,
            OpcionCriterioRepository opcionRepository,
            SolicitudRepository solicitudRepository,
            ConfiguracionReseniaService configuracionService,
            NotificacionService notificacionService,
            EntregaArchivoRepository archivoRepository) {
        this.reseniaRepository = reseniaRepository;
        this.pedidoRepository = pedidoRepository;
        this.usuarioRepository = usuarioRepository;
        this.criterioRepository = criterioRepository;
        this.opcionRepository = opcionRepository;
        this.solicitudRepository = solicitudRepository;
        this.configuracionService = configuracionService;
        this.notificacionService = notificacionService;
        this.archivoRepository = archivoRepository;
    }

    @Transactional
    public ReseniaEstadoDTO estado(Long idPedido, Long idUsuario) {
        Pedido pedido = obtenerPedido(idPedido);
        AmbitoResenia direccion = direccionDe(pedido, idUsuario);
        ConfiguracionResenia config = configuracionService.obtener();

        Resenia miResenia = reseniaRepository
            .findByPedidoIdPedidoAndAutorIdUsuario(idPedido, idUsuario)
            .orElse(null);
        boolean miReseniaEliminada = miResenia != null
            && miResenia.getEstado() == EstadoResenia.ELIMINADA;
        boolean miReseniaOculta = miResenia != null
            && miResenia.getEstado() == EstadoResenia.OCULTA;

        Resenia recibida = null;
        if (direccion == AmbitoResenia.CLIENTE) {
            Long idCliente = pedido.getCliente().getIdUsuario();
            recibida = reseniaRepository
                .findByPedidoIdPedidoAndAutorIdUsuario(idPedido, idCliente)
                .filter(resenia -> resenia.getEstado() == EstadoResenia.PUBLICADA)
                .orElse(null);
        }

        LocalDateTime limiteCreacion = limiteCreacion(pedido, config);
        LocalDateTime limiteEdicion = miResenia != null
            ? miResenia.getFechaCreacion().plusDays(config.getDiasEdicion())
            : null;

        boolean finalizado = pedido.getEstado() == EstadoPedido.FINALIZADO;
        boolean puedeCrear = finalizado
            && miResenia == null
            && dentroDeVentana(limiteCreacion);
        boolean puedeEditar = miResenia != null
            && miResenia.getEstado() == EstadoResenia.PUBLICADA
            && !miResenia.estaEditada()
            && dentroDeVentana(limiteEdicion);
        boolean puedeEliminar = miResenia != null
            && miResenia.getEstado() == EstadoResenia.PUBLICADA;

        String motivo = null;
        if (!finalizado) {
            motivo = "El pedido todavía no está finalizado.";
        } else if (miReseniaEliminada) {
            motivo = "Eliminaste la reseña de este pedido.";
        } else if (miReseniaOculta) {
            motivo = "La reseña fue oculta por moderación.";
        } else if (miResenia != null && miResenia.estaEditada()) {
            motivo = "Ya usaste la única edición disponible.";
        } else if (miResenia != null && !dentroDeVentana(limiteEdicion)) {
            motivo = "El plazo para editar la reseña venció.";
        } else if (miResenia == null && !dentroDeVentana(limiteCreacion)) {
            motivo = "El plazo para dejar la reseña venció.";
        }

        int maxImagenes = config.getMaxImagenesResenia() != null
            ? config.getMaxImagenesResenia()
            : 0;
        List<ReseniaImagenDisponibleDTO> imagenesDisponibles = List.of();
        if (direccion == AmbitoResenia.SERVICIO
                && maxImagenes > 0
                && pedido.isPublicacionAutorizada()
                && !pedido.isPublicacionOcultadaCliente()) {
            imagenesDisponibles = archivoRepository
                .listarFinalesDelPedido(pedido.getIdPedido()).stream()
                .map(archivo -> ReseniaImagenDisponibleDTO.builder()
                    .idArchivo(archivo.getIdArchivo())
                    .urlPreview("/galeria/archivos/"
                        + archivo.getIdArchivo() + "/preview")
                    .ancho(archivo.getAncho())
                    .alto(archivo.getAlto())
                    .build())
                .toList();
        }

        return ReseniaEstadoDTO.builder()
            .direccion(direccion)
            .miResenia(miResenia != null && !miReseniaEliminada
                ? ReseniaDTO.desde(miResenia, true)
                : null)
            .reseniaRecibida(recibida != null
                ? ReseniaDTO.desde(recibida, true)
                : null)
            .puedeCrear(puedeCrear)
            .puedeEditar(puedeEditar)
            .puedeEliminar(puedeEliminar)
            .anonimoForzado(pedido.getServicio() != null
                && pedido.getServicio().isContenidoSensible())
            .motivoNoDisponible(motivo)
            .fechaLimiteCreacion(limiteCreacion)
            .fechaLimiteEdicion(limiteEdicion)
            .maxImagenes(maxImagenes)
            .imagenesDisponibles(imagenesDisponibles)
            .build();
    }

    @Transactional
    public ReseniaDTO crear(
            Long idPedido, Long idUsuario, ReseniaRequest request) {
        Pedido pedido = obtenerPedido(idPedido);
        AmbitoResenia direccion = direccionDe(pedido, idUsuario);
        validarPedidoFinalizado(pedido);

        ConfiguracionResenia config = configuracionService.obtener();
        LocalDateTime limite = limiteCreacion(pedido, config);
        if (!dentroDeVentana(limite)) {
            throw new IllegalArgumentException(
                "El plazo para dejar la reseña venció.");
        }
        if (reseniaRepository.existsByPedidoIdPedidoAndAutorIdUsuario(
                idPedido, idUsuario)) {
            throw new IllegalArgumentException(
                "Ya dejaste una reseña para este pedido.");
        }

        ReseniaVersion version = construirVersion(1, request, direccion, pedido, config);

        Usuario autor = usuarioRepository.findByIdUsuario(idUsuario)
            .orElseThrow(() -> new NoSuchElementException(
                "Usuario no encontrado: " + idUsuario));

        Resenia resenia = new Resenia();
        resenia.setPedido(pedido);
        resenia.setAutor(autor);
        resenia.setEstado(EstadoResenia.PUBLICADA);
        resenia.setMostrarNombre(mostrarNombre(pedido, request, true));
        resenia.setFechaCreacion(LocalDateTime.now());
        resenia.agregarVersion(version);

        Resenia guardada = reseniaRepository.save(resenia);

        if (direccion == AmbitoResenia.SERVICIO) {
            notificarResenia(pedido, autor);
        }

        return ReseniaDTO.desde(guardada, true);
    }

    @Transactional
    public ReseniaDTO editar(
            Long idPedido, Long idUsuario, ReseniaRequest request) {
        Pedido pedido = obtenerPedido(idPedido);
        AmbitoResenia direccion = direccionDe(pedido, idUsuario);
        ConfiguracionResenia config = configuracionService.obtener();

        Resenia resenia = reseniaRepository
            .findForUpdateByPedidoAndAutor(idPedido, idUsuario)
            .orElseThrow(() -> new NoSuchElementException(
                "No dejaste una reseña para este pedido."));

        if (resenia.getEstado() == EstadoResenia.OCULTA) {
            throw new IllegalArgumentException(
                "La reseña fue oculta por moderación y no se puede editar.");
        }
        if (resenia.getEstado() != EstadoResenia.PUBLICADA) {
            throw new IllegalArgumentException(
                "La reseña fue eliminada y no se puede editar.");
        }
        if (resenia.getVersiones().size() > EDICIONES_MAXIMAS) {
            throw new IllegalArgumentException(
                "Ya usaste la única edición disponible.");
        }

        LocalDateTime limite = resenia.getFechaCreacion()
            .plusDays(config.getDiasEdicion());
        if (LocalDateTime.now().isAfter(limite)) {
            throw new IllegalArgumentException(
                "El plazo para editar la reseña venció.");
        }

        int siguienteNumero = resenia.getVersiones().size() + 1;
        resenia.agregarVersion(
            construirVersion(siguienteNumero, request, direccion, pedido, config));
        resenia.setMostrarNombre(
            mostrarNombre(pedido, request, resenia.isMostrarNombre()));
        resenia.setFechaActualizacion(LocalDateTime.now());

        return ReseniaDTO.desde(reseniaRepository.save(resenia), true);
    }

    @Transactional
    public void eliminar(Long idPedido, Long idUsuario) {
        Resenia resenia = reseniaRepository
            .findForUpdateByPedidoAndAutor(idPedido, idUsuario)
            .orElseThrow(() -> new NoSuchElementException(
                "No dejaste una reseña para este pedido."));

        if (resenia.getEstado() == EstadoResenia.OCULTA) {
            throw new IllegalArgumentException(
                "La reseña no está publicada.");
        }
        if (resenia.getEstado() != EstadoResenia.PUBLICADA) {
            throw new IllegalArgumentException("La reseña ya fue eliminada.");
        }

        LocalDateTime ahora = LocalDateTime.now();
        resenia.setEstado(EstadoResenia.ELIMINADA);
        resenia.setEliminadaEn(ahora);
        resenia.setFechaActualizacion(ahora);
        reseniaRepository.save(resenia);
    }

    @Transactional
    public ReputacionClienteDTO reputacionCliente(
            Long idCliente, Long idArtista) {
        usuarioRepository.findByIdUsuario(idCliente)
            .orElseThrow(() -> new NoSuchElementException(
                "Cliente no encontrado: " + idCliente));

        boolean relacion =
            solicitudRepository.existsByArtistaIdUsuarioAndClienteIdUsuario(
                idArtista, idCliente)
            || pedidoRepository.existsByArtistaIdUsuarioAndClienteIdUsuario(
                idArtista, idCliente);
        if (!relacion) {
            throw new AccesoDenegadoException(
                "Solo podés ver la reputación de clientes con los que hayas trabajado.");
        }

        ConfiguracionResenia config = configuracionService.obtener();
        long cantidad = reseniaRepository.contarReseniasDeCliente(
            idCliente, EstadoResenia.PUBLICADA);
        Double promedioCrudo = reseniaRepository.promedioReseniasDeCliente(
            idCliente, EstadoResenia.PUBLICADA);

        BigDecimal promedio = promedioCrudo != null
            ? BigDecimal.valueOf(promedioCrudo).setScale(2, RoundingMode.HALF_UP)
            : null;

        EtiquetaReputacion etiqueta =
            ReputacionCalculator.calcular(cantidad, promedio, config);

        return ReputacionClienteDTO.builder()
            .idCliente(idCliente)
            .cantidadResenias(cantidad)
            .promedio(etiqueta == EtiquetaReputacion.SIN_DATOS ? null : promedio)
            .etiqueta(etiqueta)
            .hayDatos(etiqueta != EtiquetaReputacion.SIN_DATOS)
            .minimoResenias(config.getMinimoReseniasEtiqueta())
            .build();
    }

    private ReseniaVersion construirVersion(
            int numero,
            ReseniaRequest request,
            AmbitoResenia direccion,
            Pedido pedido,
            ConfiguracionResenia config) {
        Map<Long, CriterioResenia> criterios = criteriosDelAmbito(direccion);

        ReseniaVersion version = new ReseniaVersion();
        version.setNumero(numero);
        String descripcion = request.getDescripcion() != null
            ? request.getDescripcion().trim()
            : null;
        version.setDescripcion(
            descripcion == null || descripcion.isEmpty() ? null : descripcion);
        version.setCalificacionGeneral(request.getCalificacionGeneral());
        version.setFechaVersion(LocalDateTime.now());

        Set<Long> criteriosConValor = new HashSet<>();
        if (request.getValores() != null) {
            for (ReseniaValorRequest item : request.getValores()) {
                CriterioResenia criterio = criterios.get(item.getIdCriterio());
                if (criterio == null) {
                    throw new IllegalArgumentException(
                        "El criterio indicado no pertenece a este tipo de reseña.");
                }
                if (criterio.getEscala() != EscalaCriterio.ESTRELLAS) {
                    throw new IllegalArgumentException(
                        "El criterio '" + criterio.getNombre()
                            + "' no se responde con estrellas.");
                }
                if (!criteriosConValor.add(criterio.getIdCriterioResenia())) {
                    throw new IllegalArgumentException(
                        "El criterio '" + criterio.getNombre() + "' está repetido.");
                }

                ReseniaValor valor = new ReseniaValor();
                valor.setCriterio(criterio);
                valor.setValor(item.getValor());
                version.agregarValor(valor);
            }
        }

        Set<Long> criteriosConOpcion = new HashSet<>();
        Set<Long> opcionesVistas = new HashSet<>();
        if (request.getOpciones() != null) {
            for (Long idOpcion : request.getOpciones()) {
                if (idOpcion == null || !opcionesVistas.add(idOpcion)) {
                    continue;
                }
                OpcionCriterio opcion = opcionRepository.findById(idOpcion)
                    .orElseThrow(() -> new IllegalArgumentException(
                        "La opción indicada no existe."));
                CriterioResenia criterio = opcion.getCriterio();

                if (criterio.getAmbito() != direccion) {
                    throw new IllegalArgumentException(
                        "La opción indicada no pertenece a este tipo de reseña.");
                }
                if (criterio.getEscala() != EscalaCriterio.OPCIONES_MULTIPLES) {
                    throw new IllegalArgumentException(
                        "El criterio '" + criterio.getNombre()
                            + "' no admite opciones.");
                }
                if (!opcion.isActivo()) {
                    throw new IllegalArgumentException(
                        "La opción '" + opcion.getEtiqueta() + "' está dada de baja.");
                }

                ReseniaOpcion seleccion = new ReseniaOpcion();
                seleccion.setOpcion(opcion);
                version.agregarOpcion(seleccion);
                criteriosConOpcion.add(criterio.getIdCriterioResenia());
            }
        }

        for (CriterioResenia criterio : criterios.values()) {
            if (!criterio.isObligatorio()) {
                continue;
            }
            boolean respondido;
            if (criterio.getEscala() == EscalaCriterio.ESTRELLAS) {
                respondido = criteriosConValor.contains(criterio.getIdCriterioResenia());
            } else {
                boolean tieneOpciones = criterio.getOpciones().stream()
                    .anyMatch(OpcionCriterio::isActivo);
                respondido = !tieneOpciones
                    || criteriosConOpcion.contains(criterio.getIdCriterioResenia());
            }
            if (!respondido) {
                throw new IllegalArgumentException(
                    "Completá el criterio '" + criterio.getNombre() + "'.");
            }
        }

        agregarImagenes(version, request, pedido, direccion, config);

        return version;
    }

    private void agregarImagenes(
            ReseniaVersion version,
            ReseniaRequest request,
            Pedido pedido,
            AmbitoResenia direccion,
            ConfiguracionResenia config) {
        List<Long> ids = request.getImagenes() == null
            ? List.of()
            : request.getImagenes().stream()
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (ids.isEmpty()) {
            return;
        }
        if (direccion != AmbitoResenia.SERVICIO) {
            throw new IllegalArgumentException(
                "Solo el cliente puede adjuntar imágenes a la reseña.");
        }

        int max = config.getMaxImagenesResenia() != null
            ? config.getMaxImagenesResenia()
            : 0;
        if (max <= 0) {
            throw new IllegalArgumentException(
                "La carga de imágenes en reseñas está desactivada.");
        }
        if (ids.size() > max) {
            throw new IllegalArgumentException(
                "Podés adjuntar hasta " + max + " imágenes.");
        }
        if (!pedido.isPublicacionAutorizada()
                || pedido.isPublicacionOcultadaCliente()) {
            throw new IllegalArgumentException(
                "El pedido no está autorizado para publicar imágenes.");
        }

        int orden = 0;
        for (Long id : ids) {
            EntregaArchivo archivo = archivoRepository.buscarParaGaleria(id)
                .orElseThrow(() -> new IllegalArgumentException(
                    "La imagen indicada no existe."));
            Pedido archivoPedido = archivo.getEntrega()
                .getHitoPedido().getPedido();
            if (!archivoPedido.getIdPedido().equals(pedido.getIdPedido())) {
                throw new IllegalArgumentException(
                    "La imagen no pertenece a este pedido.");
            }
            if (!archivo.getEntrega().isEsFinal()
                    || !archivo.getEntrega().isVigente()
                    || archivo.getEntrega().isRetirada()
                    || archivo.isRetirado()) {
                throw new IllegalArgumentException(
                    "Solo podés adjuntar imágenes de la entrega final vigente.");
            }
            if (archivo.getMime() == null
                    || !archivo.getMime().startsWith("image/")) {
                throw new IllegalArgumentException(
                    "Solo podés adjuntar imágenes.");
            }

            ReseniaImagen imagen = new ReseniaImagen();
            imagen.setArchivo(archivo);
            imagen.setOrden(orden++);
            version.agregarImagen(imagen);
        }
    }

    private Map<Long, CriterioResenia> criteriosDelAmbito(AmbitoResenia direccion) {
        List<CriterioResenia> activos = criterioRepository
            .findByActivoTrueAndAmbitoOrderByOrdenAscNombreAsc(direccion);
        Map<Long, CriterioResenia> mapa = new HashMap<>();
        for (CriterioResenia criterio : activos) {
            mapa.put(criterio.getIdCriterioResenia(), criterio);
        }
        return mapa;
    }

    private void notificarResenia(Pedido pedido, Usuario autor) {
        String servicio = pedido.getServicio() != null
            ? pedido.getServicio().getNombre()
            : "tu servicio";
        notificacionService.crearParaUsuario(
            pedido.getArtista().getIdUsuario(),
            NOTIFICACION_RESENIA,
            "Recibiste una reseña",
            autor.getNombreUsuario() + " dejó una reseña de '" + servicio + "'.",
            pedido.getIdPedido(),
            TIPO_REFERENCIA_PEDIDO);
    }

    private Pedido obtenerPedido(Long idPedido) {
        return pedidoRepository.findById(idPedido)
            .orElseThrow(() -> new NoSuchElementException(
                "Pedido no encontrado: " + idPedido));
    }

    private AmbitoResenia direccionDe(Pedido pedido, Long idUsuario) {
        if (pedido.getCliente().getIdUsuario().equals(idUsuario)) {
            return AmbitoResenia.SERVICIO;
        }
        if (pedido.getArtista().getIdUsuario().equals(idUsuario)) {
            return AmbitoResenia.CLIENTE;
        }
        throw new AccesoDenegadoException("No participaste de este pedido.");
    }

    private void validarPedidoFinalizado(Pedido pedido) {
        if (pedido.getEstado() != EstadoPedido.FINALIZADO) {
            throw new IllegalArgumentException(
                "Solo podés reseñar un pedido finalizado.");
        }
    }

    private boolean mostrarNombre(
            Pedido pedido, ReseniaRequest request, boolean actual) {
        if (pedido.getServicio() != null
                && pedido.getServicio().isContenidoSensible()) {
            return false;
        }
        return request.getMostrarNombre() != null
            ? request.getMostrarNombre()
            : actual;
    }

    private LocalDateTime limiteCreacion(
            Pedido pedido, ConfiguracionResenia config) {
        if (config.getDiasVentanaCreacion() == null
                || config.getDiasVentanaCreacion() <= 0
                || pedido.getFinalizadoEn() == null) {
            return null;
        }
        return pedido.getFinalizadoEn()
            .plusDays(config.getDiasVentanaCreacion());
    }

    private boolean dentroDeVentana(LocalDateTime limite) {
        return limite == null || !LocalDateTime.now().isAfter(limite);
    }
}
