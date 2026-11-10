package com.temipora.backend_springboot.servicios;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.temipora.backend_springboot.modelo.DerechoTermino;
import com.temipora.backend_springboot.modelo.DerechoTerminoDTO;
import com.temipora.backend_springboot.modelo.EPreferenciaContenidoSensible;
import com.temipora.backend_springboot.modelo.Etiqueta;
import com.temipora.backend_springboot.modelo.EtiquetaDTO;
import com.temipora.backend_springboot.modelo.EtiquetaEstado;
import com.temipora.backend_springboot.modelo.EstadoProducto;
import com.temipora.backend_springboot.modelo.EstadoVariante;
import com.temipora.backend_springboot.modelo.FiltroProductoRequest;
import com.temipora.backend_springboot.modelo.LicenciaProducto;
import com.temipora.backend_springboot.modelo.LicenciaProductoDTO;
import com.temipora.backend_springboot.modelo.LicenciaProductoDerecho;
import com.temipora.backend_springboot.modelo.Moneda;
import com.temipora.backend_springboot.modelo.OrdenProducto;
import com.temipora.backend_springboot.modelo.PaginaDTO;
import com.temipora.backend_springboot.modelo.Producto;
import com.temipora.backend_springboot.modelo.ProductoCatalogoDTO;
import com.temipora.backend_springboot.modelo.ProductoEtiqueta;
import com.temipora.backend_springboot.modelo.ProductoImagen;
import com.temipora.backend_springboot.modelo.ProductoImagenDTO;
import com.temipora.backend_springboot.modelo.ProductoLedgerDTO;
import com.temipora.backend_springboot.modelo.ProductoVariante;
import com.temipora.backend_springboot.modelo.ProductoVarianteDTO;
import com.temipora.backend_springboot.modelo.Usuario;
import com.temipora.backend_springboot.repositorios.EventoLedgerRepository;
import com.temipora.backend_springboot.repositorios.ProductoRepository;
import com.temipora.backend_springboot.repositorios.UsuarioRepository;

/**
 * Catálogo público de productos. Es el espejo de {@code ServicioService} para
 * el listado y el detalle de solo lectura: nada de gestión ni de variantes
 * completas.
 *
 * El ranking personalizado todavía no se aplica: `RecomendacionService` opera
 * sobre servicios y las señales de compra de producto no existen hasta que
 * haya flujo de compra. Por eso `RELEVANCIA` cae hoy en "más recientes".
 */
@Service
public class ProductoCatalogoService {

    private static final int TAMANIO_MAXIMO = 48;

    private final ProductoRepository productoRepository;
    private final UsuarioRepository usuarioRepository;
    private final EventoLedgerRepository eventoLedgerRepository;
    private final MonedaService monedaService;
    private final ConversionMonedaService conversionMonedaService;
    private final LedgerService ledgerService;

    public ProductoCatalogoService(
            ProductoRepository productoRepository,
            UsuarioRepository usuarioRepository,
            EventoLedgerRepository eventoLedgerRepository,
            MonedaService monedaService,
            ConversionMonedaService conversionMonedaService,
            LedgerService ledgerService) {
        this.productoRepository = productoRepository;
        this.usuarioRepository = usuarioRepository;
        this.eventoLedgerRepository = eventoLedgerRepository;
        this.monedaService = monedaService;
        this.conversionMonedaService = conversionMonedaService;
        this.ledgerService = ledgerService;
    }

    @Transactional(readOnly = true)
    public PaginaDTO<ProductoCatalogoDTO> listar(
            FiltroProductoRequest filtro,
            Long viewerId,
            int pagina,
            int tamanio,
            OrdenProducto orden) {
        int paginaSegura = Math.max(0, pagina);
        int tamanioSeguro = Math.min(TAMANIO_MAXIMO, Math.max(1, tamanio));
        OrdenProducto ordenSeguro = orden != null ? orden : OrdenProducto.RELEVANCIA;

        EPreferenciaContenidoSensible pref = preferenciaDe(viewerId);
        boolean ocultarSensibles = pref == EPreferenciaContenidoSensible.OCULTAR;
        Moneda moneda = monedaPreferidaDe(viewerId);

        List<Producto> candidatos = productoRepository
            .findByEstadoOrderByFechaCreacionDesc(EstadoProducto.DISPONIBLE).stream()
            .filter(producto -> !esDelViewer(producto, viewerId))
            .filter(producto -> !ocultarSensibles || !producto.isContenidoSensible())
            .filter(producto -> cumpleFiltros(producto, filtro, moneda))
            .sorted(comparador(ordenSeguro, moneda))
            .toList();

        int total = candidatos.size();
        int desde = Math.min(paginaSegura * tamanioSeguro, total);
        int hasta = Math.min(desde + tamanioSeguro, total);
        List<ProductoCatalogoDTO> contenido = candidatos.subList(desde, hasta).stream()
            .map(producto -> aDTO(producto, moneda, viewerId))
            .toList();

        int totalPaginas = total == 0
            ? 0
            : (int) Math.ceil((double) total / tamanioSeguro);

        return PaginaDTO.<ProductoCatalogoDTO>builder()
            .contenido(contenido)
            .pagina(paginaSegura)
            .tamanio(tamanioSeguro)
            .totalElementos(total)
            .totalPaginas(totalPaginas)
            .hayMas(hasta < total)
            .personalizado(false)
            .build();
    }

    @Transactional(readOnly = true)
    public List<ProductoCatalogoDTO> listarDeArtista(String nombreUsuario, Long viewerId) {
        Usuario artista = usuarioRepository.findByNombreUsuario(nombreUsuario)
            .orElseThrow(() -> new NoSuchElementException(
                "Artista no encontrado: " + nombreUsuario));
        EPreferenciaContenidoSensible pref = preferenciaDe(viewerId);
        boolean ocultarSensibles = pref == EPreferenciaContenidoSensible.OCULTAR;
        Moneda moneda = monedaPreferidaDe(viewerId);

        return productoRepository
            .findByArtistaIdUsuarioAndEstadoOrderByTituloAsc(
                artista.getIdUsuario(), EstadoProducto.DISPONIBLE).stream()
            .filter(producto -> !ocultarSensibles || !producto.isContenidoSensible())
            .map(producto -> aDTO(producto, moneda, viewerId))
            .toList();
    }

    /**
     * Detalle público. Solo productos en estado DISPONIBLE: un borrador,
     * pausado o archivado no se expone.
     */
    @Transactional(readOnly = true)
    public ProductoCatalogoDTO obtener(Long idProducto, Long viewerId) {
        Producto producto = obtenerEntidadPublica(idProducto);
        Moneda moneda = monedaPreferidaDe(viewerId);
        ProductoCatalogoDTO dto = aDTO(producto, moneda, viewerId);
        return completarDetalle(dto, producto, moneda);
    }

    /**
     * Variantes visibles de un producto público. No expone hash, archivo
     * original ni identificadores internos de propietario.
     */
    @Transactional(readOnly = true)
    public PaginaDTO<ProductoVarianteDTO> obtenerVariantes(
            Long idProducto, Long viewerId, int pagina, int tamano) {
        Producto producto = obtenerEntidadPublica(idProducto);
        Moneda moneda = monedaPreferidaDe(viewerId);
        int paginaSegura = Math.max(pagina, 1);
        int tamanoSeguro = Math.min(Math.max(tamano, 1), TAMANIO_MAXIMO);
        Set<Long> conLedger = eventoLedgerRepository.findVariantesConLedger(
            List.of(producto.getIdProducto()));

        List<ProductoVarianteDTO> visibles = producto.getVariantes().stream()
            .filter(variante -> variante.getEstado() != EstadoVariante.ARCHIVADO)
            .sorted(Comparator.comparing(ProductoVariante::getOrden,
                Comparator.nullsLast(Integer::compareTo)))
            .map(variante -> aVarianteDTOPublica(variante, conLedger, producto, moneda))
            .toList();

        int total = visibles.size();
        int desde = Math.min((paginaSegura - 1) * tamanoSeguro, total);
        int hasta = Math.min(desde + tamanoSeguro, total);
        int totalPaginas = total == 0
            ? 1
            : (int) Math.ceil((double) total / tamanoSeguro);

        return PaginaDTO.<ProductoVarianteDTO>builder()
            .contenido(visibles.subList(desde, hasta))
            .pagina(paginaSegura)
            .tamanio(tamanoSeguro)
            .totalElementos(total)
            .totalPaginas(totalPaginas)
            .hayMas(hasta < total)
            .build();
    }

    /** Historial de autenticidad del producto público. */
    @Transactional(readOnly = true)
    public ProductoLedgerDTO obtenerLedger(
            Long idProducto, int pagina, int tamano) {
        Producto producto = obtenerEntidadPublica(idProducto);
        return ledgerService.verificarProducto(producto, pagina, tamano);
    }

    private Producto obtenerEntidadPublica(Long idProducto) {
        Producto producto = productoRepository.findByIdProducto(idProducto)
            .orElseThrow(() -> new NoSuchElementException(
                "Producto no encontrado: " + idProducto));
        if (producto.getEstado() != EstadoProducto.DISPONIBLE) {
            throw new NoSuchElementException("Producto no encontrado: " + idProducto);
        }
        return producto;
    }

    /**
     * Completa el DTO de detalle con la galería, las variantes públicas, la
     * licencia vigente y las etiquetas válidas. Lo que se agrega acá nunca
     * incluye huellas SHA-256, claves de archivo ni datos de gestión.
     */
    private ProductoCatalogoDTO completarDetalle(
            ProductoCatalogoDTO dto, Producto producto, Moneda monedaDestino) {
        List<ProductoVariante> variantes = producto.getVariantes().stream()
            .filter(variante -> variante.getEstado() != EstadoVariante.ARCHIVADO)
            .sorted(Comparator.comparing(ProductoVariante::getOrden,
                Comparator.nullsLast(Integer::compareTo)))
            .toList();
        Set<Long> conLedger = eventoLedgerRepository.findVariantesConLedger(
            List.of(producto.getIdProducto()));

        dto.setImagenes(producto.getImagenes().stream()
            .sorted(Comparator.comparing(ProductoImagen::getOrden,
                Comparator.nullsLast(Integer::compareTo)))
            .map(imagen -> ProductoImagenDTO.builder()
                .idProductoImagen(imagen.getIdProductoImagen())
                .url(imagen.getUrl())
                .orden(imagen.getOrden())
                .build())
            .toList());
        dto.setVariantes(variantes.stream()
            .map(variante -> aVarianteDTOPublica(
                variante, conLedger, producto, monedaDestino))
            .toList());
        dto.setLicencia(licenciaDTO(producto.licenciaVigente()));
        dto.setEtiquetas(producto.getEtiquetas().stream()
            .map(ProductoEtiqueta::getEtiqueta)
            .filter(etiqueta -> etiqueta.getEstado() == EtiquetaEstado.VALIDA)
            .sorted(Comparator.comparing(Etiqueta::getNombre,
                String.CASE_INSENSITIVE_ORDER))
            .map(EtiquetaDTO::desdeEtiqueta)
            .toList());
        dto.setCantidadVariantesDisponibles((int) variantes.stream()
            .filter(ProductoVariante::estaDisponible)
            .count());
        return dto;
    }

    private ProductoVarianteDTO aVarianteDTOPublica(
            ProductoVariante variante,
            Set<Long> variantesConLedger,
            Producto producto,
            Moneda monedaDestino) {
        return ProductoVarianteDTO.builder()
            .idVariante(variante.getIdVariante())
            .nombre(variante.getNombre())
            .descripcion(variante.getDescripcion())
            .precio(convertir(variante.getPrecio(), producto, monedaDestino))
            .stockDisponible(variante.getStockDisponible())
            .orden(variante.getOrden())
            .estado(variante.getEstado())
            .imagenUrl(variante.getImagenUrl())
            .numeroEdicion(variante.getNumeroEdicion())
            .nombrePropietario(variante.getPropietarioActual() != null
                ? variante.getPropietarioActual().getNombreUsuario() : null)
            .esUnica(variante.esUnica())
            .tieneLedger(variante.getIdVariante() != null
                && variantesConLedger.contains(variante.getIdVariante()))
            .build();
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

    // ------------------------------------------------------------------
    // Filtros y orden
    // ------------------------------------------------------------------

    private boolean esDelViewer(Producto producto, Long viewerId) {
        return viewerId != null
            && producto.getArtista() != null
            && viewerId.equals(producto.getArtista().getIdUsuario());
    }

    private boolean cumpleFiltros(
            Producto producto, FiltroProductoRequest filtro, Moneda moneda) {
        if (filtro == null) {
            return true;
        }
        if (hayTexto(filtro.getTitulo())) {
            String buscado = filtro.getTitulo().trim().toLowerCase();
            String titulo = producto.getTitulo() != null
                ? producto.getTitulo().toLowerCase() : "";
            if (!titulo.contains(buscado)) {
                return false;
            }
        }
        if (hayTexto(filtro.getNombreArtista())) {
            String buscado = filtro.getNombreArtista().trim().toLowerCase();
            String usuario = producto.getArtista() != null
                && producto.getArtista().getNombreUsuario() != null
                    ? producto.getArtista().getNombreUsuario().toLowerCase() : "";
            if (!usuario.contains(buscado)) {
                return false;
            }
        }
        if (filtro.getIdTipoProducto() != null
                && (producto.getTipoProducto() == null
                    || !filtro.getIdTipoProducto()
                        .equals(producto.getTipoProducto().getIdTipoProducto()))) {
            return false;
        }
        if (filtro.getIdMedio() != null
                && (producto.getMedio() == null
                    || !filtro.getIdMedio().equals(producto.getMedio().getIdMedio()))) {
            return false;
        }
        if (filtro.getSensible() != null
                && filtro.getSensible() != producto.isContenidoSensible()) {
            return false;
        }
        if (Boolean.TRUE.equals(filtro.getSoloConStock()) && !tieneStock(producto)) {
            return false;
        }
        BigDecimal precio = precioDesde(producto, moneda);
        if (filtro.getPrecioMin() != null
                && (precio == null || precio.compareTo(filtro.getPrecioMin()) < 0)) {
            return false;
        }
        if (filtro.getPrecioMax() != null
                && (precio == null || precio.compareTo(filtro.getPrecioMax()) > 0)) {
            return false;
        }
        return true;
    }

    private Comparator<Producto> comparador(OrdenProducto orden, Moneda moneda) {
        Comparator<Producto> porTitulo = Comparator.comparing(
            producto -> producto.getTitulo() != null
                ? producto.getTitulo().toLowerCase() : "",
            String.CASE_INSENSITIVE_ORDER);
        return switch (orden) {
            case PRECIO_ASC -> Comparator.comparing(
                (Producto producto) -> precioDesde(producto, moneda),
                Comparator.nullsLast(BigDecimal::compareTo)).thenComparing(porTitulo);
            case PRECIO_DESC -> Comparator.comparing(
                (Producto producto) -> precioDesde(producto, moneda),
                Comparator.nullsLast(Comparator.reverseOrder())).thenComparing(porTitulo);
            case TITULO -> porTitulo;
            // RELEVANCIA y RECIENTES comparten orden hasta que exista ranking
            // de productos: el listado base ya viene por fecha descendente.
            default -> Comparator.comparing(
                (Producto producto) -> producto.getFechaCreacion(),
                Comparator.nullsLast(Comparator.reverseOrder()));
        };
    }

    // ------------------------------------------------------------------
    // Mapeo
    // ------------------------------------------------------------------

    private ProductoCatalogoDTO aDTO(
            Producto producto, Moneda monedaDestino, Long viewerId) {
        List<ProductoVariante> variantes = producto.getVariantes().stream()
            .filter(variante -> variante.getEstado() != EstadoVariante.ARCHIVADO)
            .toList();
        List<ProductoVariante> disponibles = variantes.stream()
            .filter(ProductoVariante::estaDisponible)
            .toList();

        boolean ilimitado = variantes.stream()
            .anyMatch(variante -> variante.getStockDisponible() == null
                && variante.getEstado() == EstadoVariante.DISPONIBLE);
        Integer stockTotal = ilimitado
            ? null
            : variantes.stream()
                .map(ProductoVariante::getStockDisponible)
                .filter(Objects::nonNull)
                .reduce(0, Integer::sum);

        Usuario artista = producto.getArtista();
        return ProductoCatalogoDTO.builder()
            .idProducto(producto.getIdProducto())
            .titulo(producto.getTitulo())
            .descripcion(producto.getDescripcion())
            .imagenPortada(imagenPortada(producto))
            .idArtista(artista != null ? artista.getIdUsuario() : null)
            .nombreArtista(artista != null ? artista.getNombreUsuario() : null)
            .nombreDisplayArtista(artista != null ? artista.getNombreDisplay() : null)
            .urlFotoArtista(fotoArtista(artista))
            .idTipoProducto(producto.getTipoProducto() != null
                ? producto.getTipoProducto().getIdTipoProducto() : null)
            .nombreTipoProducto(producto.getTipoProducto() != null
                ? producto.getTipoProducto().getNombre() : null)
            .idMedio(producto.getMedio() != null
                ? producto.getMedio().getIdMedio() : null)
            .nombreMedio(producto.getMedio() != null
                ? producto.getMedio().getNombre() : null)
            .precioDesde(precioDesde(producto, monedaDestino))
            .moneda(monedaDestino != null
                ? monedaDestino.getCodigo()
                : (producto.getMoneda() != null ? producto.getMoneda().getCodigo() : null))
            .tipoEntrega(producto.getMedio() != null
                ? producto.getMedio().getTipoEntrega() : null)
            .stockTotal(stockTotal)
            .tieneStockIlimitado(ilimitado)
            .esPiezaUnica(variantes.size() == 1
                && variantes.get(0).esUnica())
            .disponible(!disponibles.isEmpty())
            .cantidadVariantes(variantes.size())
            .contenidoSensible(producto.isContenidoSensible())
            .contenidosSensibles(producto.getContenidosSensibles().stream()
                .map(Enum::name)
                .toList())
            .cantidadResenias(0)
            .build();
    }

    private String imagenPortada(Producto producto) {
        if (hayTexto(producto.getImagenPortada())) {
            return producto.getImagenPortada();
        }
        return producto.getImagenes().stream()
            .sorted(Comparator.comparing(ProductoImagen::getOrden,
                Comparator.nullsLast(Integer::compareTo)))
            .map(ProductoImagen::getUrl)
            .findFirst()
            .orElse(null);
    }

    private String fotoArtista(Usuario artista) {
        if (artista == null) {
            return null;
        }
        if (hayTexto(artista.getUrlFotoPerfilMiniatura())) {
            return artista.getUrlFotoPerfilMiniatura();
        }
        return artista.getUrlFotoPerfil();
    }

    private BigDecimal convertir(
            BigDecimal monto, Producto producto, Moneda monedaDestino) {
        if (monto == null) {
            return null;
        }
        if (monedaDestino == null || producto.getMoneda() == null
                || Objects.equals(producto.getMoneda().getIdMoneda(),
                    monedaDestino.getIdMoneda())) {
            return monto.setScale(2, RoundingMode.HALF_UP);
        }
        BigDecimal factor = conversionMonedaService.factor(
            producto.getMoneda(), monedaDestino, LocalDateTime.now());
        if (factor == null) {
            return monto.setScale(2, RoundingMode.HALF_UP);
        }
        return monto.multiply(factor).setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal precioDesde(Producto producto, Moneda monedaDestino) {
        BigDecimal minimo = producto.getVariantes().stream()
            .filter(variante -> variante.getEstado() != EstadoVariante.ARCHIVADO)
            .map(ProductoVariante::getPrecio)
            .filter(Objects::nonNull)
            .min(BigDecimal::compareTo)
            .orElse(null);
        if (minimo == null) {
            return null;
        }
        if (monedaDestino == null || producto.getMoneda() == null
                || Objects.equals(producto.getMoneda().getIdMoneda(),
                    monedaDestino.getIdMoneda())) {
            return minimo.setScale(2, RoundingMode.HALF_UP);
        }
        BigDecimal factor = conversionMonedaService.factor(
            producto.getMoneda(), monedaDestino, LocalDateTime.now());
        if (factor == null) {
            return minimo.setScale(2, RoundingMode.HALF_UP);
        }
        return minimo.multiply(factor).setScale(2, RoundingMode.HALF_UP);
    }

    private boolean tieneStock(Producto producto) {
        return producto.getVariantes().stream()
            .anyMatch(ProductoVariante::estaDisponible);
    }

    private EPreferenciaContenidoSensible preferenciaDe(Long viewerId) {
        if (viewerId == null) {
            return EPreferenciaContenidoSensible.ADVERTIR;
        }
        return usuarioRepository.findByIdUsuario(viewerId)
            .map(Usuario::getPreferenciaSensible)
            .orElse(EPreferenciaContenidoSensible.ADVERTIR);
    }

    private Moneda monedaPreferidaDe(Long viewerId) {
        if (viewerId != null) {
            Moneda preferida = usuarioRepository.findByIdUsuario(viewerId)
                .map(Usuario::getMonedaPreferida)
                .orElse(null);
            if (preferida != null) {
                return preferida;
            }
        }
        return monedaService.predeterminada();
    }

    private boolean hayTexto(String valor) {
        return valor != null && !valor.isBlank();
    }
}
