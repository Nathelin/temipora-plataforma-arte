package com.temipora.backend_springboot.servicios;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.temipora.backend_springboot.modelo.EPreferenciaContenidoSensible;
import com.temipora.backend_springboot.modelo.EstadoProducto;
import com.temipora.backend_springboot.modelo.EstadoVariante;
import com.temipora.backend_springboot.modelo.FiltroProductoRequest;
import com.temipora.backend_springboot.modelo.Moneda;
import com.temipora.backend_springboot.modelo.OrdenProducto;
import com.temipora.backend_springboot.modelo.PaginaDTO;
import com.temipora.backend_springboot.modelo.Producto;
import com.temipora.backend_springboot.modelo.ProductoCatalogoDTO;
import com.temipora.backend_springboot.modelo.ProductoImagen;
import com.temipora.backend_springboot.modelo.ProductoVariante;
import com.temipora.backend_springboot.modelo.Usuario;
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
    private final MonedaService monedaService;
    private final ConversionMonedaService conversionMonedaService;

    public ProductoCatalogoService(
            ProductoRepository productoRepository,
            UsuarioRepository usuarioRepository,
            MonedaService monedaService,
            ConversionMonedaService conversionMonedaService) {
        this.productoRepository = productoRepository;
        this.usuarioRepository = usuarioRepository;
        this.monedaService = monedaService;
        this.conversionMonedaService = conversionMonedaService;
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
        Producto producto = productoRepository.findByIdProducto(idProducto)
            .orElseThrow(() -> new NoSuchElementException(
                "Producto no encontrado: " + idProducto));
        if (producto.getEstado() != EstadoProducto.DISPONIBLE) {
            throw new NoSuchElementException("Producto no encontrado: " + idProducto);
        }
        return aDTO(producto, monedaPreferidaDe(viewerId), viewerId);
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
