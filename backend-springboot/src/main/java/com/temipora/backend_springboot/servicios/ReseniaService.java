package com.temipora.backend_springboot.servicios;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import com.temipora.backend_springboot.modelo.AmbitoResenia;
import com.temipora.backend_springboot.modelo.CompraItem;
import com.temipora.backend_springboot.modelo.ConfiguracionResenia;
import com.temipora.backend_springboot.modelo.CriterioResenia;
import com.temipora.backend_springboot.modelo.EscalaCriterio;
import com.temipora.backend_springboot.modelo.EstadoCompraGrupo;
import com.temipora.backend_springboot.modelo.EstadoPedido;
import com.temipora.backend_springboot.modelo.EstadoResenia;
import com.temipora.backend_springboot.modelo.EtiquetaReputacion;
import com.temipora.backend_springboot.modelo.OpcionCriterio;
import com.temipora.backend_springboot.modelo.Pedido;
import com.temipora.backend_springboot.modelo.Producto;
import com.temipora.backend_springboot.modelo.ReputacionClienteDTO;
import com.temipora.backend_springboot.modelo.Resenia;
import com.temipora.backend_springboot.modelo.ReseniaDTO;
import com.temipora.backend_springboot.modelo.ReseniaEstadoDTO;
import com.temipora.backend_springboot.modelo.ReseniaOpcion;
import com.temipora.backend_springboot.modelo.ReseniaRequest;
import com.temipora.backend_springboot.modelo.ReseniaValor;
import com.temipora.backend_springboot.modelo.ReseniaValorRequest;
import com.temipora.backend_springboot.modelo.ReseniaVersion;
import com.temipora.backend_springboot.modelo.Usuario;
import com.temipora.backend_springboot.repositorios.CompraItemRepository;
import com.temipora.backend_springboot.repositorios.CriterioReseniaRepository;
import com.temipora.backend_springboot.repositorios.OpcionCriterioRepository;
import com.temipora.backend_springboot.repositorios.PedidoRepository;
import com.temipora.backend_springboot.repositorios.ProductoRepository;
import com.temipora.backend_springboot.repositorios.ReseniaRepository;
import com.temipora.backend_springboot.repositorios.SolicitudRepository;
import com.temipora.backend_springboot.repositorios.UsuarioRepository;

@Service
@Validated
public class ReseniaService {

    private static final int EDICIONES_MAXIMAS = 1;
    private static final String NOTIFICACION_RESENIA = "RESENIA_RECIBIDA";
    private static final String TIPO_REFERENCIA_PEDIDO = "PEDIDO";

    private static final String ENTIDAD_RESENIA = "Resenia";
    private static final List<CampoAuditado> CAMPOS_RESENIA = List.of(
        CampoAuditado.de("estado", "Estado"),
        CampoAuditado.de("mostrarNombre", "Mostrar nombre"),
        CampoAuditado.de("eliminadaEn", "Eliminada en"));
    private static final List<CampoAuditado> CAMPOS_VERSION = List.of(
        CampoAuditado.de("numero", "Versión"),
        CampoAuditado.de("descripcion", "Descripción"),
        CampoAuditado.de("calificacionGeneral", "Calificación"));

    private final ReseniaRepository reseniaRepository;
    private final PedidoRepository pedidoRepository;
    private final ProductoRepository productoRepository;
    private final CompraItemRepository compraItemRepository;
    private final UsuarioRepository usuarioRepository;
    private final CriterioReseniaRepository criterioRepository;
    private final OpcionCriterioRepository opcionRepository;
    private final SolicitudRepository solicitudRepository;
    private final ConfiguracionReseniaService configuracionService;
    private final NotificacionService notificacionService;
    private final AuditoriaService auditoria;

    public ReseniaService(
            ReseniaRepository reseniaRepository,
            PedidoRepository pedidoRepository,
            ProductoRepository productoRepository,
            CompraItemRepository compraItemRepository,
            UsuarioRepository usuarioRepository,
            CriterioReseniaRepository criterioRepository,
            OpcionCriterioRepository opcionRepository,
            SolicitudRepository solicitudRepository,
            ConfiguracionReseniaService configuracionService,
            NotificacionService notificacionService,
            AuditoriaService auditoria) {
        this.reseniaRepository = reseniaRepository;
        this.pedidoRepository = pedidoRepository;
        this.productoRepository = productoRepository;
        this.compraItemRepository = compraItemRepository;
        this.usuarioRepository = usuarioRepository;
        this.criterioRepository = criterioRepository;
        this.opcionRepository = opcionRepository;
        this.solicitudRepository = solicitudRepository;
        this.configuracionService = configuracionService;
        this.notificacionService = notificacionService;
        this.auditoria = auditoria;
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
            .motivoNoDisponible(motivo)
            .fechaLimiteCreacion(limiteCreacion)
            .fechaLimiteEdicion(limiteEdicion)
            .build();
    }

    @Transactional
    public ReseniaEstadoDTO estadoProducto(Long idProducto, Long idUsuario) {
        Producto producto = obtenerProducto(idProducto);
        ConfiguracionResenia config = configuracionService.obtener();

        Resenia miResenia = reseniaRepository
            .findByProductoIdProductoAndAutorIdUsuario(idProducto, idUsuario)
            .orElse(null);
        boolean miReseniaEliminada = miResenia != null
            && miResenia.getEstado() == EstadoResenia.ELIMINADA;
        boolean miReseniaOculta = miResenia != null
            && miResenia.getEstado() == EstadoResenia.OCULTA;

        Optional<CompraItem> compra = compraElegible(idProducto, idUsuario);
        LocalDateTime limiteCreacion = compra
            .map(item -> limiteCreacionProducto(item, config))
            .orElse(null);
        LocalDateTime limiteEdicion = miResenia != null
            ? miResenia.getFechaCreacion().plusDays(config.getDiasEdicion())
            : null;

        boolean puedeCrear = compra.isPresent()
            && miResenia == null
            && dentroDeVentana(limiteCreacion);
        boolean puedeEditar = miResenia != null
            && miResenia.getEstado() == EstadoResenia.PUBLICADA
            && !miResenia.estaEditada()
            && dentroDeVentana(limiteEdicion);
        boolean puedeEliminar = miResenia != null
            && miResenia.getEstado() == EstadoResenia.PUBLICADA;

        String motivo = null;
        if (miReseniaEliminada) {
            motivo = "Eliminaste la reseña de este producto.";
        } else if (miReseniaOculta) {
            motivo = "La reseña fue oculta por moderación.";
        } else if (miResenia != null && miResenia.estaEditada()) {
            motivo = "Ya usaste la única edición disponible.";
        } else if (miResenia != null && !dentroDeVentana(limiteEdicion)) {
            motivo = "El plazo para editar la reseña venció.";
        } else if (compra.isEmpty()) {
            motivo = "Solo pueden reseñar quienes compraron este producto.";
        } else if (miResenia == null && !dentroDeVentana(limiteCreacion)) {
            motivo = "El plazo para dejar la reseña venció.";
        }

        return ReseniaEstadoDTO.builder()
            .direccion(AmbitoResenia.PRODUCTO)
            .miResenia(miResenia != null && !miReseniaEliminada
                ? ReseniaDTO.desde(miResenia, true)
                : null)
            .puedeCrear(puedeCrear)
            .puedeEditar(puedeEditar)
            .puedeEliminar(puedeEliminar)
            .motivoNoDisponible(motivo)
            .fechaLimiteCreacion(limiteCreacion)
            .fechaLimiteEdicion(limiteEdicion)
            .build();
    }

    @Transactional
    public ReseniaDTO crearProducto(
            Long idProducto, Long idUsuario, ReseniaRequest request) {
        Producto producto = obtenerProducto(idProducto);
        CompraItem compra = compraElegible(idProducto, idUsuario)
            .orElseThrow(() -> new IllegalArgumentException(
                "Solo pueden reseñar quienes compraron este producto."));

        ConfiguracionResenia config = configuracionService.obtener();
        LocalDateTime limite = limiteCreacionProducto(compra, config);
        if (!dentroDeVentana(limite)) {
            throw new IllegalArgumentException(
                "El plazo para dejar la reseña venció.");
        }
        if (reseniaRepository.existsByProductoIdProductoAndAutorIdUsuario(
                idProducto, idUsuario)) {
            throw new IllegalArgumentException(
                "Ya dejaste una reseña para este producto.");
        }

        ReseniaVersion version = construirVersion(
            1, request, AmbitoResenia.PRODUCTO);

        Usuario autor = usuarioRepository.findByIdUsuario(idUsuario)
            .orElseThrow(() -> new NoSuchElementException(
                "Usuario no encontrado: " + idUsuario));

        Resenia resenia = new Resenia();
        resenia.setAmbito(AmbitoResenia.PRODUCTO);
        resenia.setProducto(producto);
        resenia.setCompraItem(compra);
        resenia.setAutor(autor);
        resenia.setEstado(EstadoResenia.PUBLICADA);
        resenia.setMostrarNombre(mostrarNombre(request, true));
        resenia.setFechaCreacion(LocalDateTime.now());
        resenia.agregarVersion(version);

        Resenia guardada = reseniaRepository.save(resenia);

        auditoria.registrar(
            AccionesAdmin.RESENIA_CREAR, ENTIDAD_RESENIA, guardada.getIdResenia(),
            "Dejó una reseña del producto #" + idProducto
                + " (calificación " + version.getCalificacionGeneral() + "/5)",
            null, version, CAMPOS_VERSION);

        notificarReseniaProducto(producto, autor);

        return ReseniaDTO.desde(guardada, true);
    }

    @Transactional
    public ReseniaDTO editarProducto(
            Long idProducto, Long idUsuario, ReseniaRequest request) {
        Producto producto = obtenerProducto(idProducto);
        ConfiguracionResenia config = configuracionService.obtener();

        Resenia resenia = reseniaRepository
            .findForUpdateByProductoAndAutor(idProducto, idUsuario)
            .orElseThrow(() -> new NoSuchElementException(
                "No dejaste una reseña para este producto."));
        validarEditable(resenia, config);

        ReseniaVersion anterior = resenia.getVersiones().isEmpty()
            ? null
            : resenia.getVersiones().get(resenia.getVersiones().size() - 1);
        int siguienteNumero = resenia.getVersiones().size() + 1;
        ReseniaVersion nuevaVersion = construirVersion(
            siguienteNumero, request, AmbitoResenia.PRODUCTO);
        resenia.agregarVersion(nuevaVersion);
        resenia.setMostrarNombre(
            mostrarNombre(request, resenia.isMostrarNombre()));
        resenia.setFechaActualizacion(LocalDateTime.now());

        Resenia guardada = reseniaRepository.save(resenia);
        auditoria.registrar(
            AccionesAdmin.RESENIA_EDITAR, ENTIDAD_RESENIA, guardada.getIdResenia(),
            "Editó su reseña del producto #" + idProducto
                + " (versión " + nuevaVersion.getNumero() + ")",
            anterior != null ? Snapshot.de(anterior, CAMPOS_VERSION) : null,
            nuevaVersion, CAMPOS_VERSION);
        return ReseniaDTO.desde(guardada, true);
    }

    @Transactional
    public void eliminarProducto(Long idProducto, Long idUsuario) {
        Resenia resenia = reseniaRepository
            .findForUpdateByProductoAndAutor(idProducto, idUsuario)
            .orElseThrow(() -> new NoSuchElementException(
                "No dejaste una reseña para este producto."));
        validarEliminable(resenia);

        Snapshot antes = Snapshot.de(resenia, CAMPOS_RESENIA);
        LocalDateTime ahora = LocalDateTime.now();
        resenia.setEstado(EstadoResenia.ELIMINADA);
        resenia.setEliminadaEn(ahora);
        resenia.setFechaActualizacion(ahora);
        reseniaRepository.save(resenia);
        auditoria.registrar(
            AccionesAdmin.RESENIA_ELIMINAR, ENTIDAD_RESENIA, resenia.getIdResenia(),
            "Eliminó su reseña del producto #" + idProducto,
            antes, resenia, CAMPOS_RESENIA);
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

        ReseniaVersion version = construirVersion(1, request, direccion);

        Usuario autor = usuarioRepository.findByIdUsuario(idUsuario)
            .orElseThrow(() -> new NoSuchElementException(
                "Usuario no encontrado: " + idUsuario));

        Resenia resenia = new Resenia();
        resenia.setAmbito(direccion);
        resenia.setPedido(pedido);
        resenia.setAutor(autor);
        resenia.setEstado(EstadoResenia.PUBLICADA);
        resenia.setMostrarNombre(mostrarNombre(request, true));
        resenia.setFechaCreacion(LocalDateTime.now());
        resenia.agregarVersion(version);

        Resenia guardada = reseniaRepository.save(resenia);

        auditoria.registrar(
            AccionesAdmin.RESENIA_CREAR, ENTIDAD_RESENIA, guardada.getIdResenia(),
            "Dejó una reseña del pedido #" + idPedido
                + " (calificación " + version.getCalificacionGeneral() + "/5)",
            null, version, CAMPOS_VERSION);

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

        validarEditable(resenia, config);

        ReseniaVersion anterior = resenia.getVersiones().isEmpty()
            ? null
            : resenia.getVersiones().get(resenia.getVersiones().size() - 1);
        int siguienteNumero = resenia.getVersiones().size() + 1;
        ReseniaVersion nuevaVersion = construirVersion(
            siguienteNumero, request, direccion);
        resenia.agregarVersion(nuevaVersion);
        resenia.setMostrarNombre(
            mostrarNombre(request, resenia.isMostrarNombre()));
        resenia.setFechaActualizacion(LocalDateTime.now());

        Resenia guardada = reseniaRepository.save(resenia);
        auditoria.registrar(
            AccionesAdmin.RESENIA_EDITAR, ENTIDAD_RESENIA, guardada.getIdResenia(),
            "Editó su reseña del pedido #" + idPedido
                + " (versión " + nuevaVersion.getNumero() + ")",
            anterior != null ? Snapshot.de(anterior, CAMPOS_VERSION) : null,
            nuevaVersion, CAMPOS_VERSION);
        return ReseniaDTO.desde(guardada, true);
    }

    @Transactional
    public void eliminar(Long idPedido, Long idUsuario) {
        Resenia resenia = reseniaRepository
            .findForUpdateByPedidoAndAutor(idPedido, idUsuario)
            .orElseThrow(() -> new NoSuchElementException(
                "No dejaste una reseña para este pedido."));

        validarEliminable(resenia);

        Snapshot antes = Snapshot.de(resenia, CAMPOS_RESENIA);
        LocalDateTime ahora = LocalDateTime.now();
        resenia.setEstado(EstadoResenia.ELIMINADA);
        resenia.setEliminadaEn(ahora);
        resenia.setFechaActualizacion(ahora);
        reseniaRepository.save(resenia);
        auditoria.registrar(
            AccionesAdmin.RESENIA_ELIMINAR, ENTIDAD_RESENIA, resenia.getIdResenia(),
            "Eliminó su reseña del pedido #" + idPedido,
            antes, resenia, CAMPOS_RESENIA);
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
            AmbitoResenia direccion) {
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

        return version;
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

    private Producto obtenerProducto(Long idProducto) {
        return productoRepository.findByIdProducto(idProducto)
            .orElseThrow(() -> new NoSuchElementException(
                "Producto no encontrado: " + idProducto));
    }

    private Optional<CompraItem> compraElegible(
            Long idProducto, Long idUsuario) {
        return compraItemRepository
            .findFirstByProductoIdProductoAndGrupoCompraClienteIdUsuarioAndGrupoEstadoOrderByGrupoFechaPagoDesc(
                idProducto, idUsuario, EstadoCompraGrupo.PAGADO);
    }

    private LocalDateTime limiteCreacionProducto(
            CompraItem compra, ConfiguracionResenia config) {
        if (config.getDiasVentanaCreacion() == null
                || config.getDiasVentanaCreacion() <= 0
                || compra.getGrupo() == null
                || compra.getGrupo().getFechaPago() == null) {
            return null;
        }
        return compra.getGrupo().getFechaPago()
            .plusDays(config.getDiasVentanaCreacion());
    }

    private void notificarReseniaProducto(Producto producto, Usuario autor) {
        Usuario artista = producto.getArtista();
        if (artista == null) {
            return;
        }
        notificacionService.crearParaUsuario(
            artista.getIdUsuario(),
            "RESENIA_PRODUCTO_RECIBIDA",
            "Recibiste una reseña de producto",
            autor.getNombreUsuario() + " dejó una reseña de '"
                + producto.getTitulo() + "'.",
            producto.getIdProducto(),
            "PRODUCTO");
    }

    private void validarEditable(Resenia resenia, ConfiguracionResenia config) {
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
    }

    private void validarEliminable(Resenia resenia) {
        if (resenia.getEstado() == EstadoResenia.OCULTA) {
            throw new IllegalArgumentException(
                "La reseña no está publicada.");
        }
        if (resenia.getEstado() != EstadoResenia.PUBLICADA) {
            throw new IllegalArgumentException("La reseña ya fue eliminada.");
        }
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

    private boolean mostrarNombre(ReseniaRequest request, boolean actual) {
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
