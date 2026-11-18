package com.temipora.backend_springboot.servicios;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.temipora.backend_springboot.modelo.ColeccionProductoDTO;
import com.temipora.backend_springboot.modelo.ColeccionProductosDTO;
import com.temipora.backend_springboot.modelo.ColeccionVisibilidadDTO;
import com.temipora.backend_springboot.modelo.CompraItem;
import com.temipora.backend_springboot.modelo.EPreferenciaContenidoSensible;
import com.temipora.backend_springboot.modelo.EstadoCompraGrupo;
import com.temipora.backend_springboot.modelo.EstadoEntregaItem;
import com.temipora.backend_springboot.modelo.EstadoProducto;
import com.temipora.backend_springboot.modelo.FiltroGaleriaDTO;
import com.temipora.backend_springboot.modelo.OrigenCompraItem;
import com.temipora.backend_springboot.modelo.PaginaDTO;
import com.temipora.backend_springboot.modelo.Producto;
import com.temipora.backend_springboot.modelo.ProductoVariante;
import com.temipora.backend_springboot.modelo.ReseniaPublicaResumenDTO;
import com.temipora.backend_springboot.modelo.Usuario;
import com.temipora.backend_springboot.modelo.UsuarioResumenDTO;
import com.temipora.backend_springboot.repositorios.CompraItemRepository;
import com.temipora.backend_springboot.repositorios.EventoLedgerRepository;
import com.temipora.backend_springboot.repositorios.UsuarioRepository;

@Service
public class ColeccionService {

    private static final int TAMANIO_MAXIMO = 50;

    private final CompraItemRepository compraItemRepository;
    private final EventoLedgerRepository eventoLedgerRepository;
    private final UsuarioRepository usuarioRepository;
    private final UsuarioService usuarioService;
    private final CompraService compraService;
    private final ReseniaPublicaService reseniaPublicaService;

    public ColeccionService(
            CompraItemRepository compraItemRepository,
            EventoLedgerRepository eventoLedgerRepository,
            UsuarioRepository usuarioRepository,
            UsuarioService usuarioService,
            CompraService compraService,
            ReseniaPublicaService reseniaPublicaService) {
        this.compraItemRepository = compraItemRepository;
        this.eventoLedgerRepository = eventoLedgerRepository;
        this.usuarioRepository = usuarioRepository;
        this.usuarioService = usuarioService;
        this.compraService = compraService;
        this.reseniaPublicaService = reseniaPublicaService;
    }

    @Transactional(readOnly = true)
    public ColeccionProductosDTO listar(
            String nombreUsuario,
            Long viewerId,
            Long idArtista,
            int pagina,
            int tamanio) {
        Usuario duenio = usuarioService.getByNombreUsuario(nombreUsuario);
        boolean esDuenio = viewerId != null
            && duenio.getIdUsuario().equals(viewerId);
        boolean visible = duenio.isColeccionPublica();
        if (!esDuenio && !visible) {
            return vacia(pagina, tamanio);
        }

        EPreferenciaContenidoSensible preferencia = viewerId == null
            ? EPreferenciaContenidoSensible.ADVERTIR
            : usuarioRepository.findByIdUsuario(viewerId)
                .map(Usuario::getPreferenciaSensible)
                .orElse(EPreferenciaContenidoSensible.ADVERTIR);
        boolean ocultarSensibles = !esDuenio
            && preferencia == EPreferenciaContenidoSensible.OCULTAR;
        boolean advertir = !esDuenio
            && preferencia == EPreferenciaContenidoSensible.ADVERTIR;

        Pageable pageable = PageRequest.of(
            Math.max(0, pagina),
            Math.min(Math.max(1, tamanio), TAMANIO_MAXIMO));
        Page<CompraItem> page = compraItemRepository.findColeccion(
            duenio.getIdUsuario(),
            esDuenio,
            ocultarSensibles,
            idArtista,
            EstadoCompraGrupo.PAGADO,
            EstadoEntregaItem.CANCELADO,
            pageable);

        List<CompraItem> items = page.getContent();

        Set<Long> idsVariante = items.stream()
            .map(CompraItem::getVariante)
            .filter(Objects::nonNull)
            .map(ProductoVariante::getIdVariante)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());
        Set<Long> variantesUnicas = idsVariante.isEmpty()
            ? Set.of()
            : eventoLedgerRepository.findVarianteIdsConLedger(idsVariante);

        List<Long> idsProducto = items.stream()
            .map(CompraItem::getProducto)
            .filter(Objects::nonNull)
            .map(Producto::getIdProducto)
            .distinct()
            .toList();
        Map<Long, ReseniaPublicaResumenDTO> resenias =
            reseniaPublicaService.resumenesDeProductosPorAutor(
                idsProducto, duenio.getIdUsuario());

        List<ColeccionProductoDTO> productos = items.stream()
            .map(item -> aProducto(
                item, esDuenio, advertir, variantesUnicas, resenias))
            .toList();

        List<FiltroGaleriaDTO> artistas = compraItemRepository
            .artistasDeColeccion(
                duenio.getIdUsuario(),
                esDuenio,
                ocultarSensibles,
                EstadoCompraGrupo.PAGADO,
                EstadoEntregaItem.CANCELADO)
            .stream()
            .map(this::aFiltroArtista)
            .toList();

        return ColeccionProductosDTO.builder()
            .visible(visible)
            .esDuenio(esDuenio)
            .productos(aPagina(page, productos))
            .artistas(artistas)
            .sugerenciaPendiente(esDuenio
                ? sugerenciaPendiente(duenio)
                : null)
            .build();
    }

    /**
     * El aviso aparece una sola vez: cuando el dueño ya tiene algo para
     * mostrar, su colección está privada y todavía no lo descartó.
     */
    private boolean sugerenciaPendiente(Usuario duenio) {
        if (duenio.isColeccionPublica()
                || duenio.getColeccionSugeridaEn() != null) {
            return false;
        }
        return compraItemRepository.contarColeccionables(
            duenio.getIdUsuario(),
            EstadoCompraGrupo.PAGADO,
            EstadoEntregaItem.CANCELADO) > 0;
    }

    @Transactional
    public void marcarSugerenciaVista(Long idUsuario) {
        Usuario usuario = usuarioRepository.findByIdUsuario(idUsuario)
            .orElseThrow(() -> new NoSuchElementException(
                "Usuario no encontrado: " + idUsuario));
        if (usuario.getColeccionSugeridaEn() == null) {
            usuario.setColeccionSugeridaEn(LocalDateTime.now());
            usuarioRepository.save(usuario);
        }
    }

    @Transactional
    public ColeccionVisibilidadDTO alternarOculto(
            Long idCompraItem, Long idUsuario, boolean ocultar) {
        CompraItem item = compraItemRepository.findById(idCompraItem)
            .orElseThrow(() -> new NoSuchElementException(
                "Producto de colección no encontrado."));
        if (item.getGrupo() == null
                || item.getGrupo().getCompra() == null
                || item.getGrupo().getCompra().getCliente() == null
                || !item.getGrupo().getCompra().getCliente()
                    .getIdUsuario().equals(idUsuario)) {
            throw new AccesoDenegadoException("Esa compra no es tuya.");
        }
        item.setOcultoColeccion(ocultar);
        item.setOcultoColeccionEn(ocultar ? LocalDateTime.now() : null);
        compraItemRepository.save(item);
        return ColeccionVisibilidadDTO.builder()
            .idCompraItem(item.getIdCompraItem())
            .oculto(item.isOcultoColeccion())
            .build();
    }

    private ColeccionProductoDTO aProducto(
            CompraItem item,
            boolean esDuenio,
            boolean advertir,
            Set<Long> variantesUnicas,
            Map<Long, ReseniaPublicaResumenDTO> resenias) {
        Producto producto = item.getProducto();
        ProductoVariante variante = item.getVariante();
        boolean sensible = producto != null && producto.isContenidoSensible();

        ReseniaPublicaResumenDTO resenia = producto != null
            ? resenias.get(producto.getIdProducto())
            : null;
        if (resenia != null && !esDuenio && resenia.isAnonima()) {
            resenia = null;
        }

        return ColeccionProductoDTO.builder()
            .idCompraItem(item.getIdCompraItem())
            .idCompra(item.getGrupo() != null
                    && item.getGrupo().getCompra() != null
                ? item.getGrupo().getCompra().getIdCompra()
                : null)
            .idProducto(producto != null ? producto.getIdProducto() : null)
            .idVariante(variante != null ? variante.getIdVariante() : null)
            .tituloProducto(item.getTituloProducto())
            .nombreVariante(item.getNombreVariante())
            .imagenUrl(compraService.imagenDeItem(item))
            .fechaCompra(item.getGrupo() != null
                ? item.getGrupo().getFechaPago()
                : null)
            .fechaEntrega(item.getFechaEntrega())
            .esReventa(item.getOrigen() == OrigenCompraItem.REVENTA)
            .esPiezaUnica(variante != null
                && variante.getIdVariante() != null
                && variantesUnicas.contains(variante.getIdVariante()))
            .estadoProducto(producto != null && producto.getEstado() != null
                ? producto.getEstado().name()
                : null)
            .productoDisponible(producto != null
                && producto.getEstado() == EstadoProducto.DISPONIBLE)
            .artista(producto != null
                ? UsuarioResumenDTO.desdeUsuario(producto.getArtista())
                : null)
            .contenidoSensible(sensible)
            .contenidosSensibles(producto != null
                ? producto.getContenidosSensibles().stream()
                    .map(Enum::name)
                    .toList()
                : List.of())
            .requiereAdvertencia(advertir && sensible)
            .oculto(esDuenio ? item.isOcultoColeccion() : null)
            .resenia(resenia)
            .build();
    }

    private FiltroGaleriaDTO aFiltroArtista(Usuario artista) {
        String nombre = artista.getNombreDisplay() != null
                && !artista.getNombreDisplay().isBlank()
            ? artista.getNombreDisplay()
            : artista.getNombreUsuario();
        return FiltroGaleriaDTO.builder()
            .id(artista.getIdUsuario())
            .nombre(nombre)
            .nombreUsuario(artista.getNombreUsuario())
            .build();
    }

    private PaginaDTO<ColeccionProductoDTO> aPagina(
            Page<?> page,
            List<ColeccionProductoDTO> contenido) {
        return PaginaDTO.<ColeccionProductoDTO>builder()
            .contenido(contenido)
            .pagina(page.getNumber())
            .tamanio(page.getSize())
            .totalElementos(page.getTotalElements())
            .totalPaginas(page.getTotalPages())
            .hayMas(page.hasNext())
            .build();
    }

    private ColeccionProductosDTO vacia(int pagina, int tamanio) {
        return ColeccionProductosDTO.builder()
            .visible(false)
            .esDuenio(false)
            .productos(PaginaDTO.<ColeccionProductoDTO>builder()
                .contenido(List.of())
                .pagina(Math.max(0, pagina))
                .tamanio(Math.max(1, tamanio))
                .totalElementos(0)
                .totalPaginas(0)
                .hayMas(false)
                .build())
            .artistas(List.of())
            .build();
    }
}
