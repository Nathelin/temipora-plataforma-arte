package com.temipora.backend_springboot.servicios;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import com.temipora.backend_springboot.modelo.EstadoProducto;
import com.temipora.backend_springboot.modelo.EstadoVariante;
import com.temipora.backend_springboot.modelo.Etiqueta;
import com.temipora.backend_springboot.modelo.EtiquetaDTO;
import com.temipora.backend_springboot.modelo.EtiquetaEstado;
import com.temipora.backend_springboot.modelo.Medio;
import com.temipora.backend_springboot.modelo.Moneda;
import com.temipora.backend_springboot.modelo.Producto;
import com.temipora.backend_springboot.modelo.ProductoDTO;
import com.temipora.backend_springboot.modelo.ProductoEtiqueta;
import com.temipora.backend_springboot.modelo.ProductoImagen;
import com.temipora.backend_springboot.modelo.ProductoImagenDTO;
import com.temipora.backend_springboot.modelo.ProductoImagenRequest;
import com.temipora.backend_springboot.modelo.ProductoRequest;
import com.temipora.backend_springboot.modelo.ProductoVariante;
import com.temipora.backend_springboot.modelo.ProductoVarianteDTO;
import com.temipora.backend_springboot.modelo.ProductoVarianteRequest;
import com.temipora.backend_springboot.modelo.TipoProducto;
import com.temipora.backend_springboot.modelo.Usuario;
import com.temipora.backend_springboot.repositorios.EtiquetaRepository;
import com.temipora.backend_springboot.repositorios.EventoLedgerRepository;
import com.temipora.backend_springboot.repositorios.MedioRepository;
import com.temipora.backend_springboot.repositorios.ProductoImagenRepository;
import com.temipora.backend_springboot.repositorios.ProductoRepository;
import com.temipora.backend_springboot.repositorios.ProductoVarianteRepository;
import com.temipora.backend_springboot.repositorios.TipoProductoRepository;
import com.temipora.backend_springboot.repositorios.UsuarioRepository;

@Service
@Validated
public class ProductoService {

    private static final String ENTIDAD = "Producto";
    private static final BigDecimal REGALIA_MAXIMA = new BigDecimal("10.00");
    private static final List<CampoAuditado> CAMPOS = List.of(
        CampoAuditado.de("titulo", "Título"),
        CampoAuditado.de("descripcion", "Descripción"),
        CampoAuditado.de("tipoProducto", "Tipo de producto"),
        CampoAuditado.de("medio", "Medio"),
        CampoAuditado.de("moneda", "Moneda"),
        CampoAuditado.de("contenidoSensible", "Contenido sensible"),
        CampoAuditado.de("permiteReventa", "Permite reventa"),
        CampoAuditado.de("porcentajeRegalia", "Porcentaje de regalía"),
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
    private final TerminosService terminosService;
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
            TerminosService terminosService,
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
        this.terminosService = terminosService;
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
    public Producto obtenerEntidadPropia(Long idProducto, Long idArtista) {
        Producto producto = productoRepository.findByIdProducto(idProducto)
            .orElseThrow(() -> new NoSuchElementException(
                "Producto no encontrado: " + idProducto));
        verificarDuenio(producto, idArtista);
        return producto;
    }

    @Transactional
    public ProductoDTO crear(Long idArtista, ProductoRequest request) {
        validarUnico(idArtista, request.getIdTipoProducto(), request.getTitulo(), null);
        validarTerminos(idArtista, null, request);
        validarDuplicados(request, null);
        validarVariantesBasicas(request.getVariantes());

        Usuario artista = usuarioRepository.findByIdUsuario(idArtista)
            .orElseThrow(() -> new NoSuchElementException(
                "Usuario no encontrado: " + idArtista));

        Producto producto = new Producto();
        producto.setArtista(artista);
        producto.setEstado(EstadoProducto.DISPONIBLE);
        aplicarProducto(producto, request);

        int indice = 0;
        for (ProductoVarianteRequest varianteRequest : request.getVariantes()) {
            ProductoVariante variante = new ProductoVariante();
            aplicarVariante(variante, producto, artista, varianteRequest, true, indice);
            producto.agregarVariante(variante);
            indice++;
        }

        producto.reemplazarImagenes(
            construirImagenes(producto, request.getImagenes(), null));
        producto.reemplazarEtiquetas(
            cargarEtiquetas(artista, request.getIdsEtiquetas()));

        Producto guardado = productoRepository.saveAndFlush(producto);

        if (request.getTerminos() != null) {
            terminosService.guardarProducto(
                guardado.getIdProducto(), idArtista, request.getTerminos());
        }
        String hashTerminos = terminosService.hashVigenteProducto(
            guardado.getIdProducto(), idArtista);
        for (ProductoVariante variante : guardado.getVariantes()) {
            if (variante.esUnica()) {
                ledgerService.registrarGenesis(variante, hashTerminos);
            }
        }
        productoRepository.flush();

        auditoria.registrar(
            AccionesAdmin.PRODUCTO_CREAR, ENTIDAD, guardado.getIdProducto(),
            "Creó el producto “" + guardado.getTitulo() + "” con "
                + guardado.getVariantes().size() + " variante(s)",
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

        Snapshot antes = Snapshot.de(producto, CAMPOS);
        validarUnico(idArtista, request.getIdTipoProducto(), request.getTitulo(), idProducto);
        validarTerminos(idArtista, idProducto, request);
        validarDuplicados(request, idProducto);
        validarVariantesBasicas(request.getVariantes());

        aplicarProducto(producto, request);
        actualizarVariantes(producto, producto.getArtista(), request.getVariantes());
        producto.reemplazarImagenes(
            construirImagenes(producto, request.getImagenes(), producto.getImagenes()));
        producto.reemplazarEtiquetas(
            cargarEtiquetas(producto.getArtista(), request.getIdsEtiquetas()));
        producto.setFechaActualizacion(LocalDateTime.now());

        Producto guardado = productoRepository.saveAndFlush(producto);

        if (request.getTerminos() != null) {
            terminosService.guardarProducto(
                idProducto, idArtista, request.getTerminos());
        }
        String hashTerminos = terminosService.hashVigenteProducto(idProducto, idArtista);
        for (ProductoVariante variante : guardado.getVariantes()) {
            if (variante.esUnica()
                    && !eventoLedgerRepository.existsByVarianteIdVariante(
                        variante.getIdVariante())) {
                ledgerService.registrarGenesis(variante, hashTerminos);
            }
        }
        productoRepository.flush();

        auditoria.registrar(
            AccionesAdmin.PRODUCTO_MODIFICAR, ENTIDAD, guardado.getIdProducto(),
            "Modificó el producto “" + guardado.getTitulo() + "”",
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
        if (!terminosService.tieneProductoValido(idProducto, idArtista)) {
            throw new IllegalArgumentException(
                "Completá tus términos de uso antes de publicar el producto.");
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
        Set<Long> conLedger = producto.getIdProducto() != null
            ? eventoLedgerRepository.findVariantesConLedger(
                List.of(producto.getIdProducto()))
            : Set.of();
        return aDTO(producto, conLedger);
    }

    private ProductoDTO aDTO(Producto producto, Set<Long> variantesConLedger) {
        List<ProductoVariante> variantes = producto.getVariantes().stream()
            .sorted(Comparator.comparing(ProductoVariante::getOrden,
                Comparator.nullsLast(Integer::compareTo)))
            .toList();
        List<ProductoVarianteDTO> variantesDTO = variantes.stream()
            .map(variante -> ProductoVarianteDTO.builder()
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
                .build())
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
            .permiteReventa(producto.isPermiteReventa())
            .porcentajeRegalia(producto.getPorcentajeRegalia())
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

    private void aplicarProducto(Producto producto, ProductoRequest request) {
        if (request.getImagenes() == null || request.getImagenes().isEmpty()) {
            throw new IllegalArgumentException(
                "Debés agregar al menos una imagen del producto.");
        }
        TipoProducto tipo = tipoProductoRepository
            .findByIdTipoProducto(request.getIdTipoProducto())
            .orElseThrow(() -> new IllegalArgumentException(
                "Tipo de producto no encontrado: " + request.getIdTipoProducto()));
        if (!tipo.isActivo()) {
            throw new IllegalArgumentException(
                "El tipo de producto “" + tipo.getNombre() + "” está dado de baja.");
        }
        Medio medio = medioRepository.findByIdMedio(request.getIdMedio())
            .orElseThrow(() -> new IllegalArgumentException(
                "Medio no encontrado: " + request.getIdMedio()));
        if (!medio.isActivo()) {
            throw new IllegalArgumentException(
                "El medio “" + medio.getNombre() + "” está dado de baja.");
        }

        producto.setTipoProducto(tipo);
        producto.setMedio(medio);
        producto.setTitulo(request.getTitulo().trim());
        producto.setDescripcion(request.getDescripcion().trim());
        producto.setMoneda(resolverMoneda(request.getIdMoneda()));
        validarContenidoSensible(request);
        producto.setContenidoSensible(request.isContenidoSensible());
        producto.reemplazarContenidosSensibles(
            request.isContenidoSensible() && request.getContenidosSensibles() != null
                ? new HashSet<>(request.getContenidosSensibles())
                : Set.of());

        boolean hayUnica = request.getVariantes().stream()
            .anyMatch(v -> v.getStockDisponible() != null
                && v.getStockDisponible() == 1);
        if (request.isPermiteReventa() && !hayUnica) {
            throw new IllegalArgumentException(
                "La reventa solo se habilita si hay al menos una variante con stock 1.");
        }
        producto.setPermiteReventa(request.isPermiteReventa());
        BigDecimal regalia = request.getPorcentajeRegalia() != null
            ? request.getPorcentajeRegalia()
            : BigDecimal.ZERO;
        if (regalia.compareTo(BigDecimal.ZERO) < 0
                || regalia.compareTo(REGALIA_MAXIMA) > 0) {
            throw new IllegalArgumentException(
                "El porcentaje de regalía debe estar entre 0 y 10.");
        }
        producto.setPorcentajeRegalia(regalia);
        producto.setImagenPortada(
            request.getImagenes().isEmpty()
                ? null
                : request.getImagenes().get(0).getUrl().trim());
        producto.setFechaActualizacion(LocalDateTime.now());
    }

    private void actualizarVariantes(
            Producto producto,
            Usuario artista,
            List<ProductoVarianteRequest> requests) {
        Map<Long, ProductoVariante> existentes = new HashMap<>();
        for (ProductoVariante variante : producto.getVariantes()) {
            existentes.put(variante.getIdVariante(), variante);
        }

        Set<Long> idsSolicitados = new HashSet<>();
        int indice = 0;
        for (ProductoVarianteRequest request : requests) {
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
            aplicarVariante(variante, producto, artista, request, esNueva, indice);
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
            int indice) {
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

        String nombre = request.getNombre().trim();
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
            if (esNueva
                    || !hayTexto(variante.getImagenUrl())
                    || !hayTexto(variante.getSha256())) {
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
                if (oldSha == null || !sha.equalsIgnoreCase(oldSha)) {
                    throw new IllegalArgumentException(
                        "Volvé a subir la imagen de la variante.");
                }
                clave = variante.getClaveArchivoOriginal();
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
            throw new IllegalArgumentException(
                "Debés agregar al menos una imagen del producto.");
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
            String url = request.getUrl().trim();
            String sha = request.getSha256().trim().toLowerCase(Locale.ROOT);
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

    private void validarTerminos(
            Long idArtista, Long idProducto, ProductoRequest request) {
        if (request.getTerminos() != null) {
            return;
        }
        boolean valido = idProducto == null
            ? terminosService.tieneGlobalValido(idArtista)
            : terminosService.tieneProductoValido(idProducto, idArtista);
        if (!valido) {
            throw new IllegalArgumentException(
                "Completá tus términos de uso antes de publicar el producto.");
        }
    }

    private void validarUnico(
            Long idArtista,
            Long idTipoProducto,
            String titulo,
            Long idExcluido) {
        String limpio = titulo.trim();
        boolean existe = idExcluido == null
            ? productoRepository
                .existsByArtistaIdUsuarioAndTipoProductoIdTipoProductoAndTituloIgnoreCaseAndEstadoNot(
                    idArtista, idTipoProducto, limpio, EstadoProducto.ARCHIVADO)
            : productoRepository
                .existsByArtistaIdUsuarioAndTipoProductoIdTipoProductoAndTituloIgnoreCaseAndIdProductoNotAndEstadoNot(
                    idArtista, idTipoProducto, limpio, idExcluido,
                    EstadoProducto.ARCHIVADO);
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
