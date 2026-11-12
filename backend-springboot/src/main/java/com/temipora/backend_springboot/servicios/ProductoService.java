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

import com.temipora.backend_springboot.modelo.DerechoTermino;
import com.temipora.backend_springboot.modelo.DerechoTerminoDTO;
import com.temipora.backend_springboot.modelo.EstadoProducto;
import com.temipora.backend_springboot.modelo.EstadoPublicacionReventa;
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
import com.temipora.backend_springboot.modelo.ProductoEntregableDTO;
import com.temipora.backend_springboot.modelo.ProductoImagen;
import com.temipora.backend_springboot.modelo.ProductoImagenDTO;
import com.temipora.backend_springboot.modelo.ProductoImagenRequest;
import com.temipora.backend_springboot.modelo.ProductoRequest;
import com.temipora.backend_springboot.modelo.ProductoVariante;
import com.temipora.backend_springboot.modelo.ProductoVarianteDTO;
import com.temipora.backend_springboot.modelo.ProductoVarianteImagen;
import com.temipora.backend_springboot.modelo.ProductoVarianteRequest;
import com.temipora.backend_springboot.modelo.PublicacionReventa;
import com.temipora.backend_springboot.modelo.TipoProducto;
import com.temipora.backend_springboot.modelo.TipoEntrega;
import com.temipora.backend_springboot.modelo.Usuario;
import com.temipora.backend_springboot.repositorios.DerechoTerminoRepository;
import com.temipora.backend_springboot.repositorios.EtiquetaRepository;
import com.temipora.backend_springboot.repositorios.EventoLedgerRepository;
import com.temipora.backend_springboot.repositorios.MedioRepository;
import com.temipora.backend_springboot.repositorios.ProductoImagenRepository;
import com.temipora.backend_springboot.repositorios.ProductoRepository;
import com.temipora.backend_springboot.repositorios.ProductoVarianteImagenRepository;
import com.temipora.backend_springboot.repositorios.ProductoVarianteRepository;
import com.temipora.backend_springboot.repositorios.PublicacionReventaRepository;
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
    private final PublicacionReventaRepository publicacionReventaRepository;
    private final UsuarioRepository usuarioRepository;
    private final TipoProductoRepository tipoProductoRepository;
    private final MedioRepository medioRepository;
    private final EtiquetaRepository etiquetaRepository;
    private final MonedaService monedaService;
    private final DerechoTerminoRepository derechoTerminoRepository;
    private final ProductoVarianteImagenRepository varianteImagenRepository;
    private final AlmacenPrivadoService almacenPrivadoService;
    private final LedgerService ledgerService;
    private final AuditoriaService auditoria;

    public ProductoService(
            ProductoRepository productoRepository,
            ProductoVarianteRepository varianteRepository,
            ProductoImagenRepository imagenRepository,
            EventoLedgerRepository eventoLedgerRepository,
            PublicacionReventaRepository publicacionReventaRepository,
            UsuarioRepository usuarioRepository,
            TipoProductoRepository tipoProductoRepository,
            MedioRepository medioRepository,
            EtiquetaRepository etiquetaRepository,
            MonedaService monedaService,
            DerechoTerminoRepository derechoTerminoRepository,
            ProductoVarianteImagenRepository varianteImagenRepository,
            AlmacenPrivadoService almacenPrivadoService,
            LedgerService ledgerService,
            AuditoriaService auditoria) {
        this.productoRepository = productoRepository;
        this.varianteRepository = varianteRepository;
        this.imagenRepository = imagenRepository;
        this.eventoLedgerRepository = eventoLedgerRepository;
        this.publicacionReventaRepository = publicacionReventaRepository;
        this.usuarioRepository = usuarioRepository;
        this.tipoProductoRepository = tipoProductoRepository;
        this.medioRepository = medioRepository;
        this.etiquetaRepository = etiquetaRepository;
        this.monedaService = monedaService;
        this.derechoTerminoRepository = derechoTerminoRepository;
        this.varianteImagenRepository = varianteImagenRepository;
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
        return productos.stream()
            .map(producto -> aDTO(producto, conLedger))
            .toList();
    }

    @Transactional(readOnly = true)
    public ProductoDTO obtenerPropio(Long idProducto, Long idArtista) {
        Producto producto = obtenerEntidadPropia(idProducto, idArtista);
        Set<Long> conLedger = eventoLedgerRepository.findVariantesConLedger(
            List.of(idProducto));
        return aDTO(producto, conLedger);
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
        registrarVersionesDeImagen(guardado, guardado.getArtista());

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
        registrarVersionesDeImagen(guardado, guardado.getArtista());

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
        registrarVersionesDeImagen(guardado, guardado.getArtista());

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
        registrarVersionesDeImagen(guardado, guardado.getArtista());

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
            return aDTO(producto, Set.of());
        }
        Set<Long> conLedger = eventoLedgerRepository.findVariantesConLedger(
            List.of(producto.getIdProducto()));
        return aDTO(producto, conLedger);
    }

    private ProductoDTO aDTO(
            Producto producto,
            Set<Long> variantesConLedger) {
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
            .entregables(entregablesDe(producto))
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
        PublicacionReventa publicacion = variante.getIdVariante() != null
            ? publicacionReventaRepository
                .findFirstByVarianteIdVarianteAndEstadoOrderByFechaPublicacionDesc(
                    variante.getIdVariante(),
                    EstadoPublicacionReventa.PUBLICADA)
                .orElse(null)
            : null;
        Usuario propietario = variante.getPropietarioActual();
        boolean mostrarPropietario = propietario != null
            && propietario.isMostrarPropietario();
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
            .idPropietarioActual(mostrarPropietario
                ? propietario.getIdUsuario() : null)
            .nombrePropietario(mostrarPropietario
                ? propietario.getNombreUsuario() : null)
            .esUnica(variante.esUnica())
            .tieneLedger(variante.getIdVariante() != null
                && variantesConLedger.contains(variante.getIdVariante()))
            .idPublicacionReventa(publicacion != null
                ? publicacion.getIdPublicacionReventa() : null)
            .precioReventa(publicacion != null ? publicacion.getPrecio() : null)
            .enReventa(publicacion != null)
            .build();
    }

    /**
     * Entregable de una variante. Hoy es siempre el archivo original de su
     * imagen: no se elige ni se puede quitar, de modo que el hash del ledger
     * (hash_imagen) coincide con el archivo que recibe el comprador.
     *
     * Cuando se admitan varios archivos por variante (o bundles), este método
     * pasa a devolver una lista sin cambiar a sus llamadores.
     */
    private List<ProductoEntregableDTO> entregablesDeVariante(ProductoVariante variante) {
        if (variante == null || !hayTexto(variante.getClaveArchivoOriginal())) {
            return List.of();
        }
        return List.of(ProductoEntregableDTO.builder()
            .idVariante(variante.getIdVariante())
            .nombreVariante(variante.getNombre())
            .nombre(variante.getNombre())
            .sha256(variante.getSha256())
            .mime(mimeDe(variante.getClaveArchivoOriginal()))
            .tamanoBytes(tamanoSeguro(variante.getClaveArchivoOriginal()))
            .orden(0)
            .build());
    }

    /**
     * Entregables de un producto: la concatenación de los de sus variantes.
     */
    private List<ProductoEntregableDTO> entregablesDe(Producto producto) {
        if (producto == null) {
            return List.of();
        }
        return producto.getVariantes().stream()
            .filter(variante -> variante.getEstado() != EstadoVariante.ARCHIVADO)
            .sorted(Comparator.comparing(ProductoVariante::getOrden,
                Comparator.nullsLast(Integer::compareTo)))
            .flatMap(variante -> entregablesDeVariante(variante).stream()
                .map(entregable -> {
                    entregable.setIdProducto(producto.getIdProducto());
                    return entregable;
                }))
            .toList();
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
        }
        // Reventa v1: las regalías están deshabilitadas. La columna se conserva
        // para reactivarlas junto con el Split de Pagos 1:N.
        regalia = BigDecimal.ZERO;
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
     * El entregable de una variante es su imagen, así que publicar un producto
     * digital sin imagen de variante dejaría al comprador sin archivo. Tampoco
     * se admite que dos variantes del mismo producto entreguen la misma imagen.
     */
    private void validarEntregables(ProductoRequest request) {
        boolean digital = esEntregaDigital(request.getIdMedio());
        if (!digital) {
            return;
        }
        Set<String> shasDelProducto = new HashSet<>();
        for (ProductoVarianteRequest variante : request.getVariantes()) {
            if (!hayTexto(variante.getSha256())) {
                throw new IllegalArgumentException(
                    "Falta el archivo que recibe el comprador de la variante “"
                        + (variante.getNombre() != null ? variante.getNombre().trim()
                            : "sin nombre")
                        + "”. Cada variante entrega su propia imagen.");
            }
            String sha = variante.getSha256().trim().toLowerCase(Locale.ROOT);
            if (!shasDelProducto.add(sha)) {
                throw new IllegalArgumentException(
                    "Dos variantes del mismo producto no pueden entregar la misma "
                        + "imagen. Cada variante necesita su propio archivo.");
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
     * Deja registrada la imagen vigente de cada variante en el historial.
     *
     * Las variantes de stock 1 no rotan: el ledger fija su imagen y el servicio
     * rechaza cualquier cambio. Las de más stock sí pueden cambiar, y cada
     * cambio abre una versión nueva, de modo que queda trazado qué archivo se
     * entregaba en cada momento.
     */
    private void registrarVersionesDeImagen(
            Producto producto, Usuario autor) {
        for (ProductoVariante variante : producto.getVariantes()) {
            if (variante.getIdVariante() == null || !hayTexto(variante.getSha256())) {
                continue;
            }
            ProductoVarianteImagen vigente = varianteImagenRepository
                .findByVarianteIdVarianteAndVigenteTrue(variante.getIdVariante())
                .orElse(null);
            if (vigente != null
                    && Objects.equals(vigente.getSha256(), variante.getSha256())) {
                continue;
            }
            if (vigente != null) {
                vigente.setVigente(false);
                vigente.setFechaHasta(LocalDateTime.now());
                varianteImagenRepository.save(vigente);
                // Flush del cierre antes del alta: el índice único parcial de
                // vigencia no admite dos filas vigentes a la vez.
                varianteImagenRepository.flush();
            }
            ProductoVarianteImagen nueva = new ProductoVarianteImagen();
            nueva.setVariante(variante);
            nueva.setNumero(varianteImagenRepository
                .findTopByVarianteIdVarianteOrderByNumeroDesc(variante.getIdVariante())
                .map(anterior -> anterior.getNumero() + 1)
                .orElse(1));
            nueva.setUrl(variante.getImagenUrl());
            nueva.setSha256(variante.getSha256());
            nueva.setClaveArchivoOriginal(variante.getClaveArchivoOriginal());
            nueva.setVigente(true);
            nueva.setUsuarioCambio(autor);
            varianteImagenRepository.save(nueva);
        }
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
