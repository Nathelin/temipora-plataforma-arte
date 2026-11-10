package com.temipora.backend_springboot.servicios;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import com.temipora.backend_springboot.modelo.AlcanceEntregable;
import com.temipora.backend_springboot.modelo.DerechoTermino;
import com.temipora.backend_springboot.modelo.DerechoTerminoDTO;
import com.temipora.backend_springboot.modelo.EstadoProducto;
import com.temipora.backend_springboot.modelo.EstadoVariante;
import com.temipora.backend_springboot.modelo.Etiqueta;
import com.temipora.backend_springboot.modelo.EtiquetaDTO;
import com.temipora.backend_springboot.modelo.EtiquetaEstado;
import com.temipora.backend_springboot.modelo.LicenciaProducto;
import com.temipora.backend_springboot.modelo.LicenciaProductoDTO;
import com.temipora.backend_springboot.modelo.LicenciaProductoDerecho;
import com.temipora.backend_springboot.modelo.LicenciaProductoRequest;
import com.temipora.backend_springboot.modelo.Medio;
import com.temipora.backend_springboot.modelo.Moneda;
import com.temipora.backend_springboot.modelo.PaginaDTO;
import com.temipora.backend_springboot.modelo.Producto;
import com.temipora.backend_springboot.modelo.ProductoDTO;
import com.temipora.backend_springboot.modelo.ProductoEtiqueta;
import com.temipora.backend_springboot.modelo.ProductoEntregable;
import com.temipora.backend_springboot.modelo.ProductoEntregableDTO;
import com.temipora.backend_springboot.modelo.ProductoEntregableRequest;
import com.temipora.backend_springboot.modelo.ProductoImagen;
import com.temipora.backend_springboot.modelo.ProductoImagenDTO;
import com.temipora.backend_springboot.modelo.ProductoImagenRequest;
import com.temipora.backend_springboot.modelo.ProductoRequest;
import com.temipora.backend_springboot.modelo.ProductoVariante;
import com.temipora.backend_springboot.modelo.ProductoVarianteDTO;
import com.temipora.backend_springboot.modelo.ProductoVarianteRequest;
import com.temipora.backend_springboot.modelo.TipoProducto;
import com.temipora.backend_springboot.modelo.TipoEntrega;
import com.temipora.backend_springboot.modelo.Usuario;
import com.temipora.backend_springboot.repositorios.DerechoTerminoRepository;
import com.temipora.backend_springboot.repositorios.EtiquetaRepository;
import com.temipora.backend_springboot.repositorios.EventoLedgerRepository;
import com.temipora.backend_springboot.repositorios.MedioRepository;
import com.temipora.backend_springboot.repositorios.ProductoImagenRepository;
import com.temipora.backend_springboot.repositorios.ProductoEntregableRepository;
import com.temipora.backend_springboot.repositorios.ProductoRepository;
import com.temipora.backend_springboot.repositorios.ProductoVarianteRepository;
import com.temipora.backend_springboot.repositorios.TipoProductoRepository;
import com.temipora.backend_springboot.repositorios.UsuarioRepository;

@Service
@Validated
public class ProductoService {

    private static final String ENTIDAD = "Producto";
    private static final BigDecimal REGALIA_MAXIMA = new BigDecimal("10.00");
    private static final int MAX_VARIANTES = 50;
    private static final int MAX_IMAGENES = 60;
    private static final List<CampoAuditado> CAMPOS = List.of(
        CampoAuditado.de("titulo", "Título"),
        CampoAuditado.de("descripcion", "Descripción"),
        CampoAuditado.de("tipoProducto", "Tipo de producto"),
        CampoAuditado.de("medio", "Medio"),
        CampoAuditado.de("moneda", "Moneda"),
        CampoAuditado.de("contenidoSensible", "Contenido sensible"),
        CampoAuditado.de("estado", "Estado"),
        CampoAuditado.de("imagenPortada", "Imagen de portada"));

    private final ProductoRepository productoRepository;
    private final ProductoVarianteRepository varianteRepository;
    private final ProductoImagenRepository imagenRepository;
    private final EventoLedgerRepository eventoLedgerRepository;
    private final UsuarioRepository usuarioRepository;
    private final TipoProductoRepository tipoProductoRepository;
    private final MedioRepository medioRepository;
    private final EtiquetaRepository etiquetaRepository;
    private final MonedaService monedaService;
    private final DerechoTerminoRepository derechoTerminoRepository;
    private final ProductoEntregableRepository entregableRepository;
    private final AlmacenPrivadoService almacenPrivadoService;
    private final LedgerService ledgerService;
    private final AuditoriaService auditoria;

    public ProductoService(
            ProductoRepository productoRepository,
            ProductoVarianteRepository varianteRepository,
            ProductoImagenRepository imagenRepository,
            EventoLedgerRepository eventoLedgerRepository,
            UsuarioRepository usuarioRepository,
            TipoProductoRepository tipoProductoRepository,
            MedioRepository medioRepository,
            EtiquetaRepository etiquetaRepository,
            MonedaService monedaService,
            DerechoTerminoRepository derechoTerminoRepository,
            ProductoEntregableRepository entregableRepository,
            AlmacenPrivadoService almacenPrivadoService,
            LedgerService ledgerService,
            AuditoriaService auditoria) {
        this.productoRepository = productoRepository;
        this.varianteRepository = varianteRepository;
        this.imagenRepository = imagenRepository;
        this.eventoLedgerRepository = eventoLedgerRepository;
        this.usuarioRepository = usuarioRepository;
        this.tipoProductoRepository = tipoProductoRepository;
        this.medioRepository = medioRepository;
        this.etiquetaRepository = etiquetaRepository;
        this.monedaService = monedaService;
        this.derechoTerminoRepository = derechoTerminoRepository;
        this.entregableRepository = entregableRepository;
        this.almacenPrivadoService = almacenPrivadoService;
        this.ledgerService = ledgerService;
        this.auditoria = auditoria;
    }

    @Transactional(readOnly = true)
    public List<ProductoDTO> listarMios(Long idArtista) {
        List<Producto> productos = productoRepository
            .findByArtistaIdUsuarioAndEstadoNotOrderByTituloAsc(
                idArtista, EstadoProducto.ARCHIVADO);
        if (productos.isEmpty()) {
            return List.of();
        }
        Set<Long> conLedger = eventoLedgerRepository.findVariantesConLedger(
            productos.stream().map(Producto::getIdProducto).toList());
        Map<Long, List<ProductoEntregableDTO>> entregables = entregablesPorProducto(
            productos.stream().map(Producto::getIdProducto).toList());
        return productos.stream()
            .map(producto -> aDTO(producto, conLedger, entregables))
            .toList();
    }

    @Transactional(readOnly = true)
    public ProductoDTO obtenerPropio(Long idProducto, Long idArtista) {
        Producto producto = obtenerEntidadPropia(idProducto, idArtista);
        Set<Long> conLedger = eventoLedgerRepository.findVariantesConLedger(
            List.of(idProducto));
        Map<Long, List<ProductoEntregableDTO>> entregables = entregablesPorProducto(
            List.of(idProducto));
        return aDTO(producto, conLedger, entregables);
    }

    @Transactional(readOnly = true)
    public PaginaDTO<ProductoVarianteDTO> obtenerVariantes(
            Long idProducto, Long idArtista, int pagina, int tamano) {
        Producto producto = productoRepository.findById(idProducto)
            .orElseThrow(() -> new NoSuchElementException(
                "Producto no encontrado: " + idProducto));
        verificarDuenio(producto, idArtista);

        int paginaSegura = Math.max(pagina, 1);
        int tamanoSeguro = Math.min(Math.max(tamano, 1), MAX_VARIANTES);
        Pageable pageable = PageRequest.of(
            paginaSegura - 1,
            tamanoSeguro,
            Sort.by(Sort.Direction.ASC, "orden"));
        Page<ProductoVariante> page = varianteRepository
            .findByProductoIdProducto(idProducto, pageable);
        Set<Long> conLedger = eventoLedgerRepository.findVariantesConLedger(
            List.of(idProducto));
        List<ProductoVarianteDTO> contenido = page.getContent().stream()
            .map(variante -> aVarianteDTO(variante, conLedger))
            .toList();
        int totalPaginas = Math.max(1, page.getTotalPages());
        return PaginaDTO.<ProductoVarianteDTO>builder()
            .contenido(contenido)
            .pagina(paginaSegura)
            .tamanio(tamanoSeguro)
            .totalElementos(page.getTotalElements())
            .totalPaginas(totalPaginas)
            .hayMas(paginaSegura < totalPaginas)
            .build();
    }

    @Transactional(readOnly = true)
    public Producto obtenerEntidadPropia(Long idProducto, Long idArtista) {
        Producto producto = productoRepository.findByIdProducto(idProducto)
            .orElseThrow(() -> new NoSuchElementException(
                "Producto no encontrado: " + idProducto));
        verificarDuenio(producto, idArtista);
        return producto;
    }

    @Transactional
    public ProductoDTO crear(Long idArtista, ProductoRequest request) {
        validarParaPublicar(idArtista, request, null);

        Usuario artista = usuarioRepository.findByIdUsuario(idArtista)
            .orElseThrow(() -> new NoSuchElementException(
                "Usuario no encontrado: " + idArtista));

        Producto producto = new Producto();
        producto.setArtista(artista);
        producto.setEstado(EstadoProducto.DISPONIBLE);
        aplicarProducto(producto, request, false);

        int indice = 0;
        for (ProductoVarianteRequest varianteRequest : request.getVariantes()) {
            ProductoVariante variante = new ProductoVariante();
            aplicarVariante(variante, producto, artista, varianteRequest, true, indice, false);
            producto.agregarVariante(variante);
            indice++;
        }

        producto.reemplazarImagenes(
            construirImagenes(producto, request.getImagenes(), null));
        producto.reemplazarEtiquetas(
            cargarEtiquetas(artista, request.getIdsEtiquetas()));
        producto.agregarLicencia(construirLicencia(
            producto, request.getLicencia(), validarLicencia(
                request.getLicencia(), request.getVariantes()),
            1, true));

        Producto guardado = productoRepository.saveAndFlush(producto);
        reemplazarEntregables(guardado, request);

        String hashLicencia = hashLicencia(guardado.licenciaVigente());
        for (ProductoVariante variante : guardado.getVariantes()) {
            if (variante.esUnica()) {
                ledgerService.registrarGenesis(variante, hashLicencia);
            }
        }
        productoRepository.flush();

        auditoria.registrar(
            AccionesAdmin.PRODUCTO_CREAR, ENTIDAD, guardado.getIdProducto(),
            "Publicó el producto “" + guardado.getTitulo() + "” con "
                + guardado.getVariantes().size() + " variante(s) y licencia v"
                + versionLicencia(guardado),
            null, guardado, CAMPOS);
        return aDTO(guardado);
    }

    /**
     * Guarda un producto incompleto. No valida los datos de publicación, no
     * exige licencia ni imágenes, y nunca emite eventos del ledger: un
     * borrador no representa todavía una pieza publicada.
     */
    @Transactional
    public ProductoDTO guardarBorrador(
            Long idArtista, Long idProducto, ProductoRequest request) {
        validarBorrador(idArtista, request);

        Usuario artista = usuarioRepository.findByIdUsuario(idArtista)
            .orElseThrow(() -> new NoSuchElementException(
                "Usuario no encontrado: " + idArtista));

        Producto producto;
        boolean esNuevo = idProducto == null;
        if (esNuevo) {
            producto = new Producto();
            producto.setArtista(artista);
            producto.setEstado(EstadoProducto.BORRADOR);
        } else {
            producto = productoRepository.findByIdProductoForUpdate(idProducto)
                .orElseThrow(() -> new NoSuchElementException(
                    "Producto no encontrado: " + idProducto));
            verificarDuenio(producto, idArtista);
            if (producto.getEstado() == EstadoProducto.ARCHIVADO) {
                throw new ConflictoException(
                    "El producto está archivado y no se puede modificar.");
            }
            if (producto.getEstado() != EstadoProducto.BORRADOR) {
                throw new ConflictoException(
                    "El producto ya está publicado. Editá la publicación en lugar "
                        + "de guardarla como borrador.");
            }
        }

        aplicarProducto(producto, request, true);
        actualizarVariantes(producto, producto.getArtista(),
            request.getVariantes(), true);
        producto.reemplazarImagenes(
            construirImagenes(producto, request.getImagenes(), producto.getImagenes()));
        producto.reemplazarEtiquetas(
            cargarEtiquetas(producto.getArtista(), request.getIdsEtiquetas()));
        aplicarLicenciaBorrador(producto, request.getLicencia());
        producto.setFechaActualizacion(LocalDateTime.now());

        Producto guardado = productoRepository.saveAndFlush(producto);
        reemplazarEntregables(guardado, request);

        auditoria.registrar(
            esNuevo ? AccionesAdmin.PRODUCTO_BORRADOR_CREAR
                : AccionesAdmin.PRODUCTO_BORRADOR_MODIFICAR,
            ENTIDAD, guardado.getIdProducto(),
            (esNuevo ? "Creó" : "Actualizó") + " el borrador de producto “"
                + (guardado.getTitulo() != null ? guardado.getTitulo() : "Sin título")
                + "”",
            null, guardado, CAMPOS);
        return aDTO(guardado);
    }

    /**
     * Publica un borrador. Es la única transición BORRADOR -> DISPONIBLE y el
     * único punto donde se emite el génesis del ledger.
     */
    @Transactional
    public ProductoDTO publicarBorrador(
            Long idProducto, Long idArtista, ProductoRequest request) {
        Producto producto = productoRepository.findByIdProductoForUpdate(idProducto)
            .orElseThrow(() -> new NoSuchElementException(
                "Producto no encontrado: " + idProducto));
        verificarDuenio(producto, idArtista);
        if (producto.getEstado() == EstadoProducto.ARCHIVADO) {
            throw new ConflictoException(
                "El producto está archivado y no se puede publicar.");
        }
        if (producto.getEstado() != EstadoProducto.BORRADOR) {
            throw new ConflictoException("El producto ya está publicado.");
        }

        validarParaPublicar(idArtista, request, idProducto);

        aplicarProducto(producto, request, false);
        actualizarVariantes(producto, producto.getArtista(),
            request.getVariantes(), false);
        producto.reemplazarImagenes(
            construirImagenes(producto, request.getImagenes(), producto.getImagenes()));
        producto.reemplazarEtiquetas(
            cargarEtiquetas(producto.getArtista(), request.getIdsEtiquetas()));
        actualizarLicencia(producto, request.getLicencia(), validarLicencia(
            request.getLicencia(), request.getVariantes()));
        producto.setEstado(EstadoProducto.DISPONIBLE);
        producto.setPausadoEn(null);
        producto.setFechaActualizacion(LocalDateTime.now());

        Producto guardado = productoRepository.saveAndFlush(producto);
        reemplazarEntregables(guardado, request);

        String hashLicencia = hashLicencia(guardado.licenciaVigente());
        for (ProductoVariante variante : guardado.getVariantes()) {
            if (variante.esUnica()
                    && !eventoLedgerRepository.existsByVarianteIdVariante(
                        variante.getIdVariante())) {
                ledgerService.registrarGenesis(variante, hashLicencia);
            }
        }
        productoRepository.flush();

        auditoria.registrar(
            AccionesAdmin.PRODUCTO_PUBLICAR, ENTIDAD, guardado.getIdProducto(),
            "Publicó el borrador “" + guardado.getTitulo() + "” con "
                + guardado.getVariantes().size() + " variante(s) y licencia v"
                + versionLicencia(guardado),
            null, guardado, CAMPOS);
        return aDTO(guardado);
    }

    @Transactional
    public ProductoDTO modificar(Long idProducto, Long idArtista, ProductoRequest request) {
        Producto producto = productoRepository.findByIdProductoForUpdate(idProducto)
            .orElseThrow(() -> new NoSuchElementException(
                "Producto no encontrado: " + idProducto));
        verificarDuenio(producto, idArtista);
        if (producto.getEstado() == EstadoProducto.ARCHIVADO) {
            throw new ConflictoException(
                "El producto está archivado y no se puede modificar.");
        }
        if (producto.getEstado() == EstadoProducto.BORRADOR) {
            throw new ConflictoException(
                "El producto es un borrador. Guardalo como borrador o publicalo.");
        }

        Snapshot antes = Snapshot.de(producto, CAMPOS);
        validarParaPublicar(idArtista, request, idProducto);

        aplicarProducto(producto, request, false);
        actualizarVariantes(producto, producto.getArtista(),
            request.getVariantes(), false);
        producto.reemplazarImagenes(
            construirImagenes(producto, request.getImagenes(), producto.getImagenes()));
        producto.reemplazarEtiquetas(
            cargarEtiquetas(producto.getArtista(), request.getIdsEtiquetas()));
        actualizarLicencia(producto, request.getLicencia(), validarLicencia(
            request.getLicencia(), request.getVariantes()));
        producto.setFechaActualizacion(LocalDateTime.now());

        Producto guardado = productoRepository.saveAndFlush(producto);
        reemplazarEntregables(guardado, request);

        String hashLicencia = hashLicencia(guardado.licenciaVigente());
        for (ProductoVariante variante : guardado.getVariantes()) {
            if (variante.esUnica()
                    && !eventoLedgerRepository.existsByVarianteIdVariante(
                        variante.getIdVariante())) {
                ledgerService.registrarGenesis(variante, hashLicencia);
            }
        }
        productoRepository.flush();

        auditoria.registrar(
            AccionesAdmin.PRODUCTO_MODIFICAR, ENTIDAD, guardado.getIdProducto(),
            "Modificó el producto “" + guardado.getTitulo() + "” (licencia v"
                + versionLicencia(guardado) + ")",
            antes, guardado, CAMPOS);
        return aDTO(guardado);
    }

    @Transactional
    public ProductoDTO pausar(Long idProducto, Long idArtista) {
        Producto producto = bloquearPropio(idProducto, idArtista);
        if (producto.getEstado() == EstadoProducto.ARCHIVADO) {
            throw new ConflictoException(
                "El producto está archivado y no se puede pausar.");
        }
        if (producto.getEstado() == EstadoProducto.BORRADOR) {
            throw new ConflictoException(
                "El producto es un borrador: todavía no está publicado.");
        }
        if (producto.getEstado() != EstadoProducto.PAUSADO) {
            Snapshot antes = Snapshot.de(producto, CAMPOS);
            producto.setEstado(EstadoProducto.PAUSADO);
            producto.setPausadoEn(LocalDateTime.now());
            producto.setFechaActualizacion(LocalDateTime.now());
            productoRepository.save(producto);
            auditoria.registrar(
                AccionesAdmin.PRODUCTO_PAUSAR, ENTIDAD, producto.getIdProducto(),
                "Pausó el producto “" + producto.getTitulo() + "”",
                antes, producto, CAMPOS);
        }
        return obtenerPropio(idProducto, idArtista);
    }

    @Transactional
    public ProductoDTO reactivar(Long idProducto, Long idArtista) {
        Producto producto = bloquearPropio(idProducto, idArtista);
        if (producto.getEstado() == EstadoProducto.ARCHIVADO) {
            throw new ConflictoException(
                "El producto está archivado: solo un administrador puede restaurarlo.");
        }
        if (producto.getEstado() == EstadoProducto.DISPONIBLE) {
            return obtenerPropio(idProducto, idArtista);
        }
        if (producto.getEstado() == EstadoProducto.BORRADOR) {
            throw new ConflictoException(
                "El producto es un borrador: publicalo en lugar de reactivarlo.");
        }
        if (producto.licenciaVigente() == null) {
            throw new IllegalArgumentException(
                "El producto no tiene una licencia configurada.");
        }
        Snapshot antes = Snapshot.de(producto, CAMPOS);
        producto.setEstado(EstadoProducto.DISPONIBLE);
        producto.setPausadoEn(null);
        producto.setFechaActualizacion(LocalDateTime.now());
        productoRepository.save(producto);
        auditoria.registrar(
            AccionesAdmin.PRODUCTO_REACTIVAR, ENTIDAD, producto.getIdProducto(),
            "Reactivó el producto “" + producto.getTitulo() + "”",
            antes, producto, CAMPOS);
        return obtenerPropio(idProducto, idArtista);
    }

    public void verificarDuenio(Producto producto, Long idArtista) {
        if (producto.getArtista() == null
                || !producto.getArtista().getIdUsuario().equals(idArtista)) {
            throw new IllegalArgumentException(
                "No podés modificar un producto que no es tuyo.");
        }
    }

    public ProductoDTO aDTO(Producto producto) {
        if (producto.getIdProducto() == null) {
            return aDTO(producto, Set.of(), Map.of());
        }
        Set<Long> conLedger = eventoLedgerRepository.findVariantesConLedger(
            List.of(producto.getIdProducto()));
        return aDTO(producto, conLedger,
            entregablesPorProducto(List.of(producto.getIdProducto())));
    }

    private ProductoDTO aDTO(
            Producto producto,
            Set<Long> variantesConLedger,
            Map<Long, List<ProductoEntregableDTO>> entregables) {
        List<ProductoVariante> variantes = producto.getVariantes().stream()
            .sorted(Comparator.comparing(ProductoVariante::getOrden,
                Comparator.nullsLast(Integer::compareTo)))
            .toList();
        List<ProductoVarianteDTO> variantesDTO = variantes.stream()
            .map(variante -> aVarianteDTO(variante, variantesConLedger))
            .toList();

        boolean disponible = variantes.stream()
            .anyMatch(ProductoVariante::estaDisponible);
        int disponibles = (int) variantes.stream()
            .filter(ProductoVariante::estaDisponible)
            .count();
        BigDecimal precioDesde = variantes.stream()
            .filter(ProductoVariante::estaDisponible)
            .map(ProductoVariante::getPrecio)
            .filter(Objects::nonNull)
            .min(BigDecimal::compareTo)
            .orElseGet(() -> variantes.stream()
                .filter(v -> v.getEstado() != EstadoVariante.ARCHIVADO)
                .map(ProductoVariante::getPrecio)
                .filter(Objects::nonNull)
                .min(BigDecimal::compareTo)
                .orElse(null));

        Usuario artista = producto.getArtista();
        LicenciaProducto licencia = producto.licenciaVigente();
        LicenciaProductoDTO licenciaDTO = licenciaDTO(licencia);
        boolean permiteReventa = licencia != null && licencia.permiteReventa();
        BigDecimal porcentajeRegalia = licencia != null
            && licencia.getPorcentajeRegalia() != null
                ? licencia.getPorcentajeRegalia()
                : BigDecimal.ZERO;
        return ProductoDTO.builder()
            .idProducto(producto.getIdProducto())
            .idArtista(artista != null ? artista.getIdUsuario() : null)
            .nombreArtista(artista != null ? artista.getNombreUsuario() : null)
            .urlFotoArtista(artista != null
                ? (artista.getUrlFotoPerfilMiniatura() != null
                    ? artista.getUrlFotoPerfilMiniatura()
                    : artista.getUrlFotoPerfil())
                : null)
            .idTipoProducto(producto.getTipoProducto() != null
                ? producto.getTipoProducto().getIdTipoProducto() : null)
            .nombreTipoProducto(producto.getTipoProducto() != null
                ? producto.getTipoProducto().getNombre() : null)
            .idMedio(producto.getMedio() != null
                ? producto.getMedio().getIdMedio() : null)
            .nombreMedio(producto.getMedio() != null
                ? producto.getMedio().getNombre() : null)
            .tipoEntregaMedio(producto.getMedio() != null
                ? producto.getMedio().getTipoEntrega() : null)
            .titulo(producto.getTitulo())
            .descripcion(producto.getDescripcion())
            .idMoneda(producto.getMoneda() != null
                ? producto.getMoneda().getIdMoneda() : null)
            .moneda(producto.getMoneda() != null
                ? producto.getMoneda().getCodigo() : null)
            .contenidoSensible(producto.isContenidoSensible())
            .contenidosSensibles(producto.getContenidosSensibles().stream()
                .map(Enum::name)
                .toList())
            .permiteReventa(permiteReventa)
            .porcentajeRegalia(porcentajeRegalia)
            .licencia(licenciaDTO)
            .estado(producto.getEstado())
            .imagenPortada(producto.getImagenPortada())
            .imagenes(producto.getImagenes().stream()
                .sorted(Comparator.comparing(ProductoImagen::getOrden,
                    Comparator.nullsLast(Integer::compareTo)))
                .map(imagen -> ProductoImagenDTO.builder()
                    .idProductoImagen(imagen.getIdProductoImagen())
                    .url(imagen.getUrl())
                    .orden(imagen.getOrden())
                    .sha256(imagen.getSha256())
                    .build())
                .toList())
            .variantes(variantesDTO)
            .entregables(entregables.getOrDefault(
                producto.getIdProducto(), List.of()))
            .etiquetas(producto.getEtiquetas().stream()
                .map(ProductoEtiqueta::getEtiqueta)
                .sorted(Comparator.comparing(Etiqueta::getNombre,
                    String.CASE_INSENSITIVE_ORDER))
                .map(EtiquetaDTO::desdeEtiqueta)
                .toList())
            .precioDesde(precioDesde)
            .disponible(disponible)
            .cantidadVariantes(variantes.size())
            .cantidadVariantesDisponibles(disponibles)
            .fechaCreacion(producto.getFechaCreacion())
            .fechaActualizacion(producto.getFechaActualizacion())
            .pausadoEn(producto.getPausadoEn())
            .archivadoEn(producto.getArchivadoEn())
            .build();
    }

    private ProductoVarianteDTO aVarianteDTO(
            ProductoVariante variante, Set<Long> variantesConLedger) {
        return ProductoVarianteDTO.builder()
            .idVariante(variante.getIdVariante())
            .nombre(variante.getNombre())
            .descripcion(variante.getDescripcion())
            .precio(variante.getPrecio())
            .stockDisponible(variante.getStockDisponible())
            .orden(variante.getOrden())
            .estado(variante.getEstado())
            .imagenUrl(variante.getImagenUrl())
            .sha256(variante.getSha256())
            .numeroEdicion(variante.getNumeroEdicion())
            .idPropietarioActual(variante.getPropietarioActual() != null
                ? variante.getPropietarioActual().getIdUsuario() : null)
            .nombrePropietario(variante.getPropietarioActual() != null
                ? variante.getPropietarioActual().getNombreUsuario() : null)
            .esUnica(variante.esUnica())
            .tieneLedger(variante.getIdVariante() != null
                && variantesConLedger.contains(variante.getIdVariante()))
            .build();
    }

    private Map<Long, List<ProductoEntregableDTO>> entregablesPorProducto(
            Collection<Long> idsProducto) {
        if (idsProducto == null || idsProducto.isEmpty()) {
            return Map.of();
        }
        return entregableRepository
            .findByProductoIdProductoInOrderByOrdenAsc(idsProducto)
            .stream()
            .collect(Collectors.groupingBy(
                entregable -> entregable.getProducto().getIdProducto(),
                Collectors.mapping(entregable -> ProductoEntregableDTO.builder()
                    .idEntregable(entregable.getIdEntregable())
                    .alcance(entregable.getAlcance())
                    .idVariante(entregable.getVariante() != null
                        ? entregable.getVariante().getIdVariante() : null)
                    .nombreVariante(entregable.getVariante() != null
                        ? entregable.getVariante().getNombre() : null)
                    .nombre(entregable.getNombre())
                    .mime(entregable.getMime())
                    .tamanoBytes(entregable.getTamanoBytes())
                    .sha256(entregable.getSha256())
                    .orden(entregable.getOrden())
                    .build(),
                    Collectors.toList())));
    }

    private void aplicarProducto(
            Producto producto, ProductoRequest request, boolean borrador) {
        if (!borrador) {
            if (request.getImagenes() == null || request.getImagenes().isEmpty()) {
                throw new IllegalArgumentException(
                    "Debés agregar al menos una imagen del producto.");
            }
        }
        List<ProductoImagenRequest> imagenes = request.getImagenes() != null
            ? request.getImagenes()
            : List.of();
        if (imagenes.size() > MAX_IMAGENES) {
            throw new IllegalArgumentException(
                "Un producto puede tener como máximo " + MAX_IMAGENES + " imágenes.");
        }
        TipoProducto tipo = request.getIdTipoProducto() != null
            ? tipoProductoRepository
                .findByIdTipoProducto(request.getIdTipoProducto())
                .orElseThrow(() -> new IllegalArgumentException(
                    "Tipo de producto no encontrado: " + request.getIdTipoProducto()))
            : null;
        if (tipo != null && !tipo.isActivo()) {
            throw new IllegalArgumentException(
                "El tipo de producto “" + tipo.getNombre() + "” está dado de baja.");
        }
        Medio medio = request.getIdMedio() != null
            ? medioRepository.findByIdMedio(request.getIdMedio())
                .orElseThrow(() -> new IllegalArgumentException(
                    "Medio no encontrado: " + request.getIdMedio()))
            : null;
        if (medio != null && !medio.isActivo()) {
            throw new IllegalArgumentException(
                "El medio “" + medio.getNombre() + "” está dado de baja.");
        }

        producto.setTipoProducto(tipo);
        producto.setMedio(medio);
        producto.setTitulo(limpiar(request.getTitulo()));
        producto.setDescripcion(limpiar(request.getDescripcion()));
        producto.setMoneda(resolverMoneda(request.getIdMoneda()));
        validarContenidoSensible(request);
        producto.setContenidoSensible(request.isContenidoSensible());
        producto.reemplazarContenidosSensibles(
            request.isContenidoSensible() && request.getContenidosSensibles() != null
                ? new HashSet<>(request.getContenidosSensibles())
                : Set.of());

        producto.setImagenPortada(
            imagenes.isEmpty()
                ? null
                : imagenes.get(0).getUrl().trim());
        producto.setFechaActualizacion(LocalDateTime.now());
    }

    private void actualizarVariantes(
            Producto producto,
            Usuario artista,
            List<ProductoVarianteRequest> requests,
            boolean borrador) {
        List<ProductoVarianteRequest> lista = requests != null ? requests : List.of();
        Map<Long, ProductoVariante> existentes = new HashMap<>();
        for (ProductoVariante variante : producto.getVariantes()) {
            existentes.put(variante.getIdVariante(), variante);
        }

        Set<Long> idsSolicitados = new HashSet<>();
        int indice = 0;
        for (ProductoVarianteRequest request : lista) {
            ProductoVariante variante;
            boolean esNueva;
            if (request.getIdVariante() != null) {
                variante = existentes.get(request.getIdVariante());
                if (variante == null) {
                    throw new NoSuchElementException(
                        "Variante no encontrada: " + request.getIdVariante());
                }
                idsSolicitados.add(variante.getIdVariante());
                esNueva = false;
            } else {
                variante = new ProductoVariante();
                producto.agregarVariante(variante);
                esNueva = true;
            }
            aplicarVariante(variante, producto, artista, request, esNueva, indice, borrador);
            indice++;
        }

        for (ProductoVariante variante : new ArrayList<>(producto.getVariantes())) {
            if (variante.getIdVariante() != null
                    && !idsSolicitados.contains(variante.getIdVariante())) {
                if (eventoLedgerRepository.existsByVarianteIdVariante(
                        variante.getIdVariante())) {
                    variante.setEstado(EstadoVariante.ARCHIVADO);
                    variante.setFechaActualizacion(LocalDateTime.now());
                } else {
                    producto.quitarVariante(variante);
                }
            }
        }
    }

    private void aplicarVariante(
            ProductoVariante variante,
            Producto producto,
            Usuario artista,
            ProductoVarianteRequest request,
            boolean esNueva,
            int indice,
            boolean borrador) {
        String oldSha = variante.getSha256();
        boolean tieneLedger = !esNueva
            && variante.getIdVariante() != null
            && eventoLedgerRepository.existsByVarianteIdVariante(
                variante.getIdVariante());

        if (tieneLedger) {
            if (request.getStockDisponible() == null
                    || request.getStockDisponible() != 1) {
                throw new ConflictoException(
                    "La variante única ya está registrada: no podés cambiar su stock.");
            }
            if (hayTexto(request.getSha256())
                    && oldSha != null
                    && !request.getSha256().trim().equalsIgnoreCase(oldSha)) {
                throw new ConflictoException(
                    "La imagen de una variante única ya registrada no se puede cambiar. "
                        + "Archivá la variante y creá una nueva.");
            }
        }

        String nombre = hayTexto(request.getNombre())
            ? request.getNombre().trim()
            : "Variante " + (indice + 1);
        variante.setNombre(nombre);
        variante.setDescripcion(limpiar(request.getDescripcion()));
        variante.setPrecio(request.getPrecio());
        variante.setOrden(request.getOrden() != null ? request.getOrden() : indice);
        variante.setNumeroEdicion(request.getNumeroEdicion());

        Integer stock = request.getStockDisponible();
        if (stock != null && stock < 0) {
            throw new IllegalArgumentException("El stock no puede ser negativo.");
        }
        variante.setStockDisponible(stock);

        EstadoVariante estado = request.getEstado();
        if (estado == null) {
            if (stock != null && stock == 0) {
                estado = EstadoVariante.AGOTADO;
            } else if (tieneLedger && variante.getEstado() != null) {
                estado = variante.getEstado();
            } else {
                estado = EstadoVariante.DISPONIBLE;
            }
        }
        variante.setEstado(estado);

        if (stock != null && stock == 1) {
            if (variante.getPropietarioActual() == null) {
                variante.setPropietarioActual(artista);
            }
        } else {
            variante.setPropietarioActual(null);
        }

        boolean imagenEnviada = hayTexto(request.getUrlImagen())
            || hayTexto(request.getSha256())
            || hayTexto(request.getClaveArchivoOriginal());
        if (!imagenEnviada) {
            if (!borrador && (esNueva
                    || !hayTexto(variante.getImagenUrl())
                    || !hayTexto(variante.getSha256()))) {
                throw new IllegalArgumentException(
                    "La imagen de la variante es obligatoria.");
            }
        } else {
            if (!hayTexto(request.getUrlImagen()) || !hayTexto(request.getSha256())) {
                throw new IllegalArgumentException(
                    "La variante necesita la URL y la huella SHA-256 de la imagen.");
            }
            String sha = request.getSha256().trim().toLowerCase(Locale.ROOT);
            if (tieneLedger && oldSha != null && !sha.equalsIgnoreCase(oldSha)) {
                throw new ConflictoException(
                    "La imagen de una variante única ya registrada no se puede cambiar.");
            }
            String clave = limpiar(request.getClaveArchivoOriginal());
            if (clave == null) {
                String claveGaleria = producto.getImagenes().stream()
                    .filter(imagen -> imagen.getSha256() != null
                        && imagen.getSha256().equalsIgnoreCase(sha))
                    .map(ProductoImagen::getClaveArchivoOriginal)
                    .filter(Objects::nonNull)
                    .findFirst()
                    .orElse(null);
                if (claveGaleria != null) {
                    clave = claveGaleria;
                } else if (oldSha == null || !sha.equalsIgnoreCase(oldSha)) {
                    throw new IllegalArgumentException(
                        "Volvé a subir la imagen de la variante.");
                } else {
                    clave = variante.getClaveArchivoOriginal();
                }
            }
            variante.setImagenUrl(request.getUrlImagen().trim());
            variante.setSha256(sha);
            variante.setClaveArchivoOriginal(clave);
        }

        if (!esNueva) {
            variante.setFechaActualizacion(LocalDateTime.now());
        }
    }

    private List<ProductoImagen> construirImagenes(
            Producto producto,
            List<ProductoImagenRequest> requests,
            java.util.Collection<ProductoImagen> existentes) {
        if (requests == null || requests.isEmpty()) {
            // Solo se llega acá en un borrador: publicar exige imágenes.
            return new ArrayList<>();
        }
        Map<String, String> clavesPorSha = new HashMap<>();
        if (existentes != null) {
            for (ProductoImagen imagen : existentes) {
                if (imagen.getSha256() != null) {
                    clavesPorSha.put(
                        imagen.getSha256().toLowerCase(Locale.ROOT),
                        imagen.getClaveArchivoOriginal());
                }
            }
        }

        List<ProductoImagen> imagenes = new ArrayList<>();
        int orden = 0;
        for (ProductoImagenRequest request : requests) {
            String url = limpiar(request.getUrl());
            String sha = limpiar(request.getSha256());
            if (url == null || sha == null) {
                throw new IllegalArgumentException(
                    "Cada imagen necesita su URL y su huella SHA-256.");
            }
            sha = sha.toLowerCase(Locale.ROOT);
            String clave = limpiar(request.getClaveArchivoOriginal());
            if (clave == null) {
                clave = clavesPorSha.get(sha);
            }
            ProductoImagen imagen = new ProductoImagen();
            imagen.setProducto(producto);
            imagen.setUrl(url);
            imagen.setOrden(request.getOrden() != null ? request.getOrden() : orden);
            imagen.setSha256(sha);
            imagen.setClaveArchivoOriginal(clave);
            imagenes.add(imagen);
            orden++;
        }
        return imagenes;
    }

    private void validarCantidadVariantes(List<ProductoVarianteRequest> variantes) {
        if (variantes != null && variantes.size() > MAX_VARIANTES) {
            throw new IllegalArgumentException(
                "Un producto puede tener como máximo " + MAX_VARIANTES + " variantes.");
        }
    }

    private void validarOrdenesVariantes(List<ProductoVarianteRequest> variantes) {
        Set<Integer> ordenes = new HashSet<>();
        int indice = 0;
        for (ProductoVarianteRequest variante : variantes) {
            int orden = variante.getOrden() != null ? variante.getOrden() : indice;
            if (!ordenes.add(orden)) {
                throw new IllegalArgumentException(
                    "El orden de las variantes no puede repetirse.");
            }
            indice++;
        }
    }

    /**
     * Un borrador puede no tener licencia todavía. Si la tiene, se guarda como
     * versión vigente sin exigir los derechos completos.
     */
    private void aplicarLicenciaBorrador(
            Producto producto, LicenciaProductoRequest request) {
        if (request == null) {
            return;
        }
        Set<Long> ids = request.getIdsDerechos() == null
            ? Set.of()
            : request.getIdsDerechos().stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        LicenciaContenido contenido;
        if (ids.isEmpty()) {
            contenido = new LicenciaContenido(Set.of(), BigDecimal.ZERO);
        } else {
            Set<DerechoTermino> derechos = new LinkedHashSet<>(
                derechoTerminoRepository.findAllById(ids));
            BigDecimal regalia = request.getPorcentajeRegalia() != null
                ? request.getPorcentajeRegalia()
                : BigDecimal.ZERO;
            contenido = new LicenciaContenido(derechos, regalia);
        }
        actualizarLicencia(producto, request, contenido);
    }

    private void validarVariantesBasicas(List<ProductoVarianteRequest> variantes) {
        if (variantes == null || variantes.isEmpty()) {
            throw new IllegalArgumentException(
                "Debés agregar al menos una variante.");
        }
        Set<String> nombres = new HashSet<>();
        Set<Integer> ordenes = new HashSet<>();
        Set<String> hashesUnicos = new HashSet<>();
        for (ProductoVarianteRequest variante : variantes) {
            String nombre = variante.getNombre() != null
                ? variante.getNombre().trim()
                : "";
            if (nombre.isEmpty()) {
                throw new IllegalArgumentException(
                    "El nombre de la variante es obligatorio.");
            }
            if (!nombres.add(nombre.toLowerCase(Locale.ROOT))) {
                throw new IllegalArgumentException(
                    "Los nombres de las variantes no pueden repetirse.");
            }
            if (variante.getOrden() == null) {
                throw new IllegalArgumentException(
                    "El orden de la variante es obligatorio.");
            }
            if (!ordenes.add(variante.getOrden())) {
                throw new IllegalArgumentException(
                    "El orden de las variantes no puede repetirse.");
            }
            if (variante.getPrecio() == null
                    || variante.getPrecio().compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException(
                    "El precio de la variante debe ser mayor a cero.");
            }
            if (variante.getStockDisponible() != null
                    && variante.getStockDisponible() < 0) {
                throw new IllegalArgumentException(
                    "El stock no puede ser negativo.");
            }
            if (variante.getStockDisponible() != null
                    && variante.getStockDisponible() == 1
                    && hayTexto(variante.getSha256())) {
                String sha = variante.getSha256().trim().toLowerCase(Locale.ROOT);
                if (!hashesUnicos.add(sha)) {
                    throw new ConflictoException(
                        "Dos variantes únicas no pueden compartir la misma imagen.");
                }
            }
        }
    }

    private void validarDuplicados(ProductoRequest request, Long idProductoExcluido) {
        Long excluir = idProductoExcluido != null ? idProductoExcluido : -1L;
        Set<String> hashes = new LinkedHashSet<>();
        if (request.getImagenes() != null) {
            for (ProductoImagenRequest imagen : request.getImagenes()) {
                if (hayTexto(imagen.getSha256())) {
                    hashes.add(imagen.getSha256().trim().toLowerCase(Locale.ROOT));
                }
            }
        }
        if (request.getVariantes() != null) {
            for (ProductoVarianteRequest variante : request.getVariantes()) {
                if (hayTexto(variante.getSha256())) {
                    hashes.add(variante.getSha256().trim().toLowerCase(Locale.ROOT));
                }
            }
        }
        for (String sha : hashes) {
            boolean enVariante = varianteRepository
                .existsBySha256AndEstadoNotAndProductoIdProductoNot(
                    sha, EstadoVariante.ARCHIVADO, excluir);
            boolean enImagen = imagenRepository
                .existsBySha256AndProductoIdProductoNot(sha, excluir);
            if (enVariante || enImagen) {
                throw new ConflictoException(
                    "Ya existe un producto con la misma imagen (SHA-256 "
                        + sha.substring(0, 12)
                        + "…). No se puede volver a publicar la misma pieza.");
            }
        }
    }

    private LicenciaContenido validarLicencia(
            LicenciaProductoRequest request,
            List<ProductoVarianteRequest> variantes) {
        if (request == null) {
            throw new IllegalArgumentException(
                "La licencia del producto es obligatoria.");
        }
        Set<Long> ids = request.getIdsDerechos() == null
            ? Set.of()
            : request.getIdsDerechos().stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (ids.isEmpty()) {
            throw new IllegalArgumentException(
                "Elegí al menos un uso permitido para la licencia.");
        }
        List<DerechoTermino> encontrados = derechoTerminoRepository.findAllById(ids);
        if (encontrados.size() != ids.size()) {
            throw new IllegalArgumentException(
                "Algún derecho de la licencia no existe.");
        }
        Set<DerechoTermino> derechos = new LinkedHashSet<>(encontrados);
        if (derechos.stream().anyMatch(derecho -> !derecho.isActivo())) {
            throw new IllegalArgumentException(
                "La licencia incluye un derecho dado de baja.");
        }
        boolean requierePiezaUnica = derechos.stream()
            .anyMatch(DerechoTermino::isRequierePiezaUnica);
        if (requierePiezaUnica) {
            boolean hayUnica = variantes != null && variantes.stream()
                .anyMatch(variante -> variante.getStockDisponible() != null
                    && variante.getStockDisponible() == 1);
            if (!hayUnica) {
                throw new IllegalArgumentException(
                    "El derecho de reventa solo se habilita si hay al menos "
                        + "una variante con stock 1.");
            }
        }
        BigDecimal regalia = request.getPorcentajeRegalia() != null
            ? request.getPorcentajeRegalia()
            : BigDecimal.ZERO;
        if (requierePiezaUnica) {
            if (regalia.compareTo(BigDecimal.ZERO) < 0
                    || regalia.compareTo(REGALIA_MAXIMA) > 0) {
                throw new IllegalArgumentException(
                    "El porcentaje de regalía debe estar entre 0 y 10.");
            }
        } else {
            regalia = BigDecimal.ZERO;
        }
        return new LicenciaContenido(derechos, regalia);
    }

    private LicenciaProducto construirLicencia(
            Producto producto,
            LicenciaProductoRequest request,
            LicenciaContenido contenido,
            int version,
            boolean vigente) {
        LicenciaProducto licencia = new LicenciaProducto();
        licencia.setProducto(producto);
        licencia.setPermiteModificaciones(request.isPermiteModificaciones());
        licencia.setRequiereCredito(request.isRequiereCredito());
        licencia.setPorcentajeRegalia(contenido.porcentajeRegalia());
        licencia.setTextoAdicional(limpiar(request.getTextoAdicional()));
        licencia.setVersion(version);
        licencia.setVigente(vigente);
        licencia.reemplazarDerechos(contenido.derechos());
        return licencia;
    }

    private void actualizarLicencia(
            Producto producto,
            LicenciaProductoRequest request,
            LicenciaContenido contenido) {
        LicenciaProducto actual = producto.licenciaVigente();
        if (actual != null && licenciaIgual(actual, request, contenido)) {
            return;
        }
        if (actual != null) {
            actual.setVigente(false);
            // Flush del UPDATE antes del INSERT de la versión nueva para no
            // violar uk_licencia_producto_vigente.
            productoRepository.saveAndFlush(producto);
        }
        int version = actual != null ? actual.getVersion() + 1 : 1;
        producto.agregarLicencia(construirLicencia(
            producto, request, contenido, version, true));
    }

    private boolean licenciaIgual(
            LicenciaProducto actual,
            LicenciaProductoRequest request,
            LicenciaContenido contenido) {
        Set<Long> idsActuales = actual.getDerechos().stream()
            .map(asignacion -> asignacion.getDerecho() != null
                ? asignacion.getDerecho().getIdDerechoTermino() : null)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());
        Set<Long> idsNuevos = contenido.derechos().stream()
            .map(DerechoTermino::getIdDerechoTermino)
            .collect(Collectors.toSet());
        return idsActuales.equals(idsNuevos)
            && actual.isPermiteModificaciones() == request.isPermiteModificaciones()
            && actual.isRequiereCredito() == request.isRequiereCredito()
            && normalizarRegalia(actual.getPorcentajeRegalia())
                .compareTo(contenido.porcentajeRegalia()) == 0
            && Objects.equals(
                limpiar(actual.getTextoAdicional()),
                limpiar(request.getTextoAdicional()));
    }

    private BigDecimal normalizarRegalia(BigDecimal valor) {
        return valor != null ? valor : BigDecimal.ZERO;
    }

    private String hashLicencia(LicenciaProducto licencia) {
        if (licencia == null) {
            return null;
        }
        String derechos = licencia.getDerechos().stream()
            .map(LicenciaProductoDerecho::getDerecho)
            .filter(Objects::nonNull)
            .map(derecho -> derecho.getCodigo() != null
                ? derecho.getCodigo()
                : String.valueOf(derecho.getIdDerechoTermino()))
            .sorted()
            .collect(Collectors.joining(","));
        String canonico = String.join("|",
            String.valueOf(licencia.getVersion()),
            derechos,
            String.valueOf(licencia.isPermiteModificaciones()),
            String.valueOf(licencia.isRequiereCredito()),
            licencia.getPorcentajeRegalia() != null
                ? licencia.getPorcentajeRegalia().stripTrailingZeros().toPlainString()
                : "",
            licencia.getTextoAdicional() != null ? licencia.getTextoAdicional() : "");
        return sha256Hex(canonico);
    }

    private String sha256Hex(String valor) {
        try {
            return HexFormat.of().formatHex(
                MessageDigest.getInstance("SHA-256")
                    .digest(valor.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 no disponible.", e);
        }
    }

    private String versionLicencia(Producto producto) {
        LicenciaProducto licencia = producto.licenciaVigente();
        return licencia != null ? String.valueOf(licencia.getVersion()) : "?";
    }

    private LicenciaProductoDTO licenciaDTO(LicenciaProducto licencia) {
        if (licencia == null) {
            return null;
        }
        List<DerechoTerminoDTO> derechos = licencia.getDerechos().stream()
            .map(LicenciaProductoDerecho::getDerecho)
            .filter(Objects::nonNull)
            .sorted(Comparator.comparing(DerechoTermino::getOrden,
                Comparator.nullsLast(Integer::compareTo)))
            .map(DerechoTerminoDTO::desdeDerecho)
            .toList();
        return LicenciaProductoDTO.builder()
            .idLicenciaProducto(licencia.getIdLicenciaProducto())
            .version(licencia.getVersion())
            .vigente(licencia.isVigente())
            .derechos(derechos)
            .permiteModificaciones(licencia.isPermiteModificaciones())
            .requiereCredito(licencia.isRequiereCredito())
            .porcentajeRegalia(licencia.getPorcentajeRegalia())
            .textoAdicional(licencia.getTextoAdicional())
            .permiteReventa(licencia.permiteReventa())
            .build();
    }

    private record LicenciaContenido(
            Set<DerechoTermino> derechos,
            BigDecimal porcentajeRegalia) {
    }

    /**
     * Validación mínima para guardar un borrador: solo integridad de lo que
     * vino, sin exigir nada de la publicación.
     */
    private void validarBorrador(Long idArtista, ProductoRequest request) {
        validarCantidadVariantes(request.getVariantes());
        if (request.getVariantes() != null && request.getVariantes().size() > 0) {
            validarOrdenesVariantes(request.getVariantes());
        }
        validarContenidoSensible(request);
    }

    /**
     * Validación completa de publicación. Es la que existía antes, ahora
     * invocada tanto por el alta directa como por la publicación de un
     * borrador.
     */
    private void validarParaPublicar(
            Long idArtista, ProductoRequest request, Long idExcluido) {
        if (request.getIdTipoProducto() == null) {
            throw new IllegalArgumentException("El tipo de producto es obligatorio.");
        }
        if (request.getIdMedio() == null) {
            throw new IllegalArgumentException("El medio es obligatorio.");
        }
        if (!hayTexto(request.getTitulo())) {
            throw new IllegalArgumentException("El título es obligatorio.");
        }
        if (!hayTexto(request.getDescripcion())) {
            throw new IllegalArgumentException("La descripción es obligatoria.");
        }
        if (request.getImagenes() == null || request.getImagenes().isEmpty()) {
            throw new IllegalArgumentException(
                "Debés agregar al menos una imagen del producto.");
        }
        if (request.getVariantes() == null || request.getVariantes().isEmpty()) {
            throw new IllegalArgumentException("Debés agregar al menos una variante.");
        }

        validarUnico(idArtista, request.getIdTipoProducto(), request.getTitulo(),
            idExcluido);
        validarLicencia(request.getLicencia(), request.getVariantes());
        validarCantidadVariantes(request.getVariantes());
        validarDuplicados(request, idExcluido);
        validarVariantesBasicas(request.getVariantes());
        validarEntregables(request);
    }

    /**
     * Los entregables son obligatorios para productos de entrega digital:
     * publicar un producto cuyo comprador no recibiría nada es un error.
     */
    private void validarEntregables(ProductoRequest request) {
        boolean digital = esEntregaDigital(request.getIdMedio());
        if (!digital) {
            return;
        }
        for (ProductoVarianteRequest variante : request.getVariantes()) {
            List<String> seleccion = variante.getEntregablesSha256();
            boolean traeLista = seleccion != null && !seleccion.isEmpty()
                && seleccion.stream().anyMatch(this::hayTexto);
            // Sin lista explícita se preselecciona la imagen de la variante.
            boolean tienePropia = hayTexto(variante.getSha256());
            if (!traeLista && !tienePropia) {
                throw new IllegalArgumentException(
                    "Falta el archivo que recibe el comprador de la variante “"
                        + (variante.getNombre() != null ? variante.getNombre().trim()
                            : "sin nombre")
                        + "”. Subí su imagen para usarla como entregable.");
            }
        }
    }

    private boolean esEntregaDigital(Long idMedio) {
        if (idMedio == null) {
            return false;
        }
        return medioRepository.findByIdMedio(idMedio)
            .map(medio -> medio.getTipoEntrega() == TipoEntrega.DIGITAL)
            .orElse(false);
    }

    /**
     * Persiste los entregables declarados. En v1 el archivo se resuelve a
     * partir de imágenes ya subidas del propio producto, de modo que una
     * variante sin entregable explícito usa su archivo privado original.
     */
    private void reemplazarEntregables(Producto producto, ProductoRequest request) {
        entregableRepository.deleteByProductoIdProducto(producto.getIdProducto());
        // Hibernate ejecuta los INSERT antes que los DELETE. Sin este flush, al
        // editar un producto que ya tenía entregables los nuevos chocan contra
        // uk_entregable_variante mientras las filas anteriores siguen presentes.
        entregableRepository.flush();
        if (!esEntregaDigital(producto.getMedio() != null
                ? producto.getMedio().getIdMedio() : null)) {
            return;
        }

        Map<String, ProductoImagen> imagenesPorSha = new HashMap<>();
        for (ProductoImagen imagen : producto.getImagenes()) {
            if (imagen.getSha256() != null) {
                imagenesPorSha.put(imagen.getSha256().toLowerCase(Locale.ROOT), imagen);
            }
        }

        List<ProductoEntregable> nuevos = new ArrayList<>();
        if (request.getEntregablesComunes() != null) {
            int orden = 0;
            for (ProductoEntregableRequest comun : request.getEntregablesComunes()) {
                ProductoImagen imagen = resolverImagenEntregable(
                    comun.getSha256(), imagenesPorSha);
                if (imagen == null) {
                    throw new IllegalArgumentException(
                        "El entregable común tiene que ser una imagen de este producto.");
                }
                nuevos.add(construirEntregable(
                    producto, null, AlcanceEntregable.COMUN, imagen,
                    comun.getNombre(), comun.getOrden() != null ? comun.getOrden() : orden));
                orden++;
            }
        }

        for (ProductoVarianteRequest varianteRequest : request.getVariantes()) {
            ProductoVariante variante = buscarVariante(
                producto, varianteRequest.getOrden(), varianteRequest.getIdVariante());
            if (variante == null) {
                continue;
            }
            agregarEntregablesDeVariante(
                producto, variante, varianteRequest, imagenesPorSha, nuevos);
        }
        entregableRepository.saveAll(nuevos);
    }

    /**
     * Resuelve los archivos de una variante. Si el request no trae la lista,
     * se preselecciona la imagen propia de la variante: publicar un producto
     * digital cuyo comprador no recibe nada sería un error.
     */
    private void agregarEntregablesDeVariante(
            Producto producto,
            ProductoVariante variante,
            ProductoVarianteRequest request,
            Map<String, ProductoImagen> imagenesPorSha,
            List<ProductoEntregable> destino) {
        List<String> seleccion = request.getEntregablesSha256();
        if (seleccion == null || seleccion.isEmpty()) {
            seleccion = new ArrayList<>();
            String propia = hayTexto(variante.getSha256())
                ? variante.getSha256()
                : request.getSha256();
            if (hayTexto(propia)) {
                seleccion.add(propia);
            }
        }

        Set<String> vistos = new LinkedHashSet<>();
        int orden = 0;
        for (String sha : seleccion) {
            if (!hayTexto(sha)) {
                continue;
            }
            String normalizado = sha.trim().toLowerCase(Locale.ROOT);
            if (!vistos.add(normalizado)) {
                continue;
            }
            ProductoImagen imagen = imagenesPorSha.get(normalizado);
            if (imagen != null) {
                destino.add(construirEntregable(
                    producto, variante, AlcanceEntregable.VARIANTE, imagen,
                    orden == 0 ? request.getEntregableNombre() : null, orden));
                orden++;
                continue;
            }
            // La imagen de la variante puede no estar en la galería.
            if (hayTexto(variante.getClaveArchivoOriginal())
                    && normalizado.equalsIgnoreCase(variante.getSha256())) {
                destino.add(entregableDesdeVariante(producto, variante, orden));
                orden++;
                continue;
            }
            throw new IllegalArgumentException(
                "Uno de los archivos de la variante “" + variante.getNombre()
                    + "” no pertenece a la galería del producto.");
        }
    }

    private ProductoEntregable entregableDesdeVariante(
            Producto producto, ProductoVariante variante, int orden) {
        ProductoEntregable entregable = new ProductoEntregable();
        entregable.setProducto(producto);
        entregable.setVariante(variante);
        entregable.setAlcance(AlcanceEntregable.VARIANTE);
        entregable.setNombre(nombreArchivo(variante));
        entregable.setClaveArchivo(variante.getClaveArchivoOriginal());
        entregable.setSha256(variante.getSha256());
        entregable.setMime(mimeDe(variante.getClaveArchivoOriginal()));
        entregable.setTamanoBytes(tamanoSeguro(variante.getClaveArchivoOriginal()));
        entregable.setOrden(orden);
        return entregable;
    }

    private ProductoEntregable construirEntregable(
            Producto producto,
            ProductoVariante variante,
            AlcanceEntregable alcance,
            ProductoImagen imagen,
            String nombre,
            Integer orden) {
        ProductoEntregable entregable = new ProductoEntregable();
        entregable.setProducto(producto);
        entregable.setVariante(variante);
        entregable.setAlcance(alcance);
        entregable.setNombre(hayTexto(nombre)
            ? nombre.trim()
            : (imagen.getUrl() != null
                ? imagen.getUrl().substring(imagen.getUrl().lastIndexOf('/') + 1)
                : "archivo"));
        entregable.setClaveArchivo(imagen.getClaveArchivoOriginal());
        entregable.setSha256(imagen.getSha256());
        entregable.setMime("image/webp");
        entregable.setTamanoBytes(imagen.getClaveArchivoOriginal() != null
            ? tamanoSeguro(imagen.getClaveArchivoOriginal()) : null);
        entregable.setOrden(orden != null ? orden : 0);
        return entregable;
    }

    private ProductoImagen resolverImagenEntregable(
            String sha256, Map<String, ProductoImagen> imagenesPorSha) {
        if (!hayTexto(sha256)) {
            return null;
        }
        return imagenesPorSha.get(sha256.trim().toLowerCase(Locale.ROOT));
    }

    private ProductoVariante buscarVariante(
            Producto producto, Integer orden, Long idVariante) {
        if (idVariante != null) {
            return producto.getVariantes().stream()
                .filter(variante -> idVariante.equals(variante.getIdVariante()))
                .findFirst()
                .orElse(null);
        }
        return producto.getVariantes().stream()
            .filter(variante -> Objects.equals(variante.getOrden(), orden))
            .findFirst()
            .orElse(null);
    }

    private String nombreArchivo(ProductoVariante variante) {
        return "Variante " + variante.getNombre();
    }

    private String mimeDe(String clave) {
        if (clave == null) {
            return null;
        }
        String minuscula = clave.toLowerCase(Locale.ROOT);
        if (minuscula.endsWith(".png")) {
            return "image/png";
        }
        if (minuscula.endsWith(".jpg") || minuscula.endsWith(".jpeg")) {
            return "image/jpeg";
        }
        if (minuscula.endsWith(".webp")) {
            return "image/webp";
        }
        return null;
    }

    private Long tamanoSeguro(String clave) {
        try {
            return almacenPrivadoService.tamano(clave);
        } catch (RuntimeException e) {
            return null;
        }
    }

    private void validarUnico(
            Long idArtista,
            Long idTipoProducto,
            String titulo,
            Long idExcluido) {
        String limpio = titulo.trim();
        boolean existe = productoRepository
            .existsByArtistaIdUsuarioAndTipoProductoIdTipoProductoAndTituloIgnoreCaseAndIdProductoNotAndEstadoNotIn(
                idArtista, idTipoProducto, limpio,
                idExcluido != null ? idExcluido : -1L,
                List.of(EstadoProducto.ARCHIVADO, EstadoProducto.BORRADOR));
        if (existe) {
            throw new IllegalArgumentException(
                "Ya tenés un producto con ese título y tipo.");
        }
    }

    private Set<Etiqueta> cargarEtiquetas(Usuario artista, Set<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Set.of();
        }
        if (ids.size() > 5) {
            throw new IllegalArgumentException(
                "Un producto puede tener como máximo 5 etiquetas.");
        }
        Set<Etiqueta> etiquetas = new LinkedHashSet<>();
        for (Long id : ids) {
            if (id == null) {
                continue;
            }
            Etiqueta etiqueta = etiquetaRepository.findByIdEtiqueta(id)
                .orElseThrow(() -> new NoSuchElementException(
                    "Etiqueta no encontrada: " + id));
            validarEtiquetaAsignable(etiqueta, artista);
            etiquetas.add(etiqueta);
        }
        return etiquetas;
    }

    private void validarEtiquetaAsignable(Etiqueta etiqueta, Usuario artista) {
        boolean propiaPendiente = etiqueta.getEstado() == EtiquetaEstado.PENDIENTE
            && etiqueta.getCreadoPor() != null
            && artista != null
            && etiqueta.getCreadoPor().getIdUsuario().equals(artista.getIdUsuario());
        if (etiqueta.getEstado() == EtiquetaEstado.VALIDA || propiaPendiente) {
            return;
        }
        if (etiqueta.getEstado() == EtiquetaEstado.INACTIVA) {
            throw new IllegalArgumentException(
                "La etiqueta '" + etiqueta.getNombre()
                    + "' fue dada de baja y no puede asignarse a un producto.");
        }
        if (etiqueta.getEstado() == EtiquetaEstado.RECHAZADA) {
            throw new IllegalArgumentException(
                "La etiqueta '" + etiqueta.getNombre()
                    + "' fue rechazada y no puede asignarse a un producto.");
        }
        throw new IllegalArgumentException(
            "La etiqueta '" + etiqueta.getNombre()
                + "' está pendiente de evaluación y solo su creador puede asignarla.");
    }

    private void validarContenidoSensible(ProductoRequest request) {
        if (request.isContenidoSensible()
                && (request.getContenidosSensibles() == null
                    || request.getContenidosSensibles().isEmpty())) {
            throw new IllegalArgumentException(
                "Seleccioná qué tipo de contenido sensible puede aparecer en el producto.");
        }
    }

    private Moneda resolverMoneda(Long idMoneda) {
        if (idMoneda != null) {
            return monedaService.obtener(idMoneda);
        }
        Moneda predeterminada = monedaService.predeterminada();
        if (predeterminada == null) {
            throw new IllegalStateException(
                "No hay monedas configuradas en el sistema.");
        }
        return predeterminada;
    }

    private Producto bloquearPropio(Long idProducto, Long idArtista) {
        Producto producto = productoRepository.findByIdProductoForUpdate(idProducto)
            .orElseThrow(() -> new NoSuchElementException(
                "Producto no encontrado: " + idProducto));
        verificarDuenio(producto, idArtista);
        return producto;
    }

    private String limpiar(String valor) {
        return valor != null && !valor.isBlank() ? valor.trim() : null;
    }

    private boolean hayTexto(String valor) {
        return valor != null && !valor.isBlank();
    }
}
