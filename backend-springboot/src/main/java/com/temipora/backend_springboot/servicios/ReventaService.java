package com.temipora.backend_springboot.servicios;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.temipora.backend_springboot.modelo.CompraItem;
import com.temipora.backend_springboot.modelo.EstadoEntregaItem;
import com.temipora.backend_springboot.modelo.EstadoPublicacionReventa;
import com.temipora.backend_springboot.modelo.LicenciaCompraDTO;
import com.temipora.backend_springboot.modelo.PiezaRevendibleDTO;
import com.temipora.backend_springboot.modelo.Producto;
import com.temipora.backend_springboot.modelo.ProductoVariante;
import com.temipora.backend_springboot.modelo.PublicacionReventa;
import com.temipora.backend_springboot.modelo.PublicacionReventaDTO;
import com.temipora.backend_springboot.modelo.ReventaDetalleDTO;
import com.temipora.backend_springboot.modelo.ReventaRequest;
import com.temipora.backend_springboot.modelo.TipoTopeReventa;
import com.temipora.backend_springboot.modelo.Usuario;
import com.temipora.backend_springboot.repositorios.CompraItemRepository;
import com.temipora.backend_springboot.repositorios.EventoLedgerRepository;
import com.temipora.backend_springboot.repositorios.ProductoVarianteRepository;
import com.temipora.backend_springboot.repositorios.PublicacionReventaRepository;
import com.temipora.backend_springboot.repositorios.UsuarioRepository;

import tools.jackson.databind.ObjectMapper;

@Service
public class ReventaService {

    private static final String ENTIDAD = "PublicacionReventa";
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final List<CampoAuditado> CAMPOS = List.of(
        CampoAuditado.de("precio", "Precio"),
        CampoAuditado.de("estado", "Estado"));
    private static final Set<EstadoPublicacionReventa> ESTADOS_ACTIVOS =
        EnumSet.of(EstadoPublicacionReventa.PUBLICADA,
            EstadoPublicacionReventa.RESERVADA,
            EstadoPublicacionReventa.SUSPENDIDA);

    private final PublicacionReventaRepository publicacionRepository;
    private final CompraItemRepository compraItemRepository;
    private final ProductoVarianteRepository varianteRepository;
    private final EventoLedgerRepository eventoLedgerRepository;
    private final UsuarioRepository usuarioRepository;
    private final CobroCapacidadService cobroCapacidadService;
    private final ProductoCatalogoService productoCatalogoService;
    private final AuditoriaService auditoria;

    public ReventaService(
            PublicacionReventaRepository publicacionRepository,
            CompraItemRepository compraItemRepository,
            ProductoVarianteRepository varianteRepository,
            EventoLedgerRepository eventoLedgerRepository,
            UsuarioRepository usuarioRepository,
            CobroCapacidadService cobroCapacidadService,
            ProductoCatalogoService productoCatalogoService,
            AuditoriaService auditoria) {
        this.publicacionRepository = publicacionRepository;
        this.compraItemRepository = compraItemRepository;
        this.varianteRepository = varianteRepository;
        this.eventoLedgerRepository = eventoLedgerRepository;
        this.usuarioRepository = usuarioRepository;
        this.cobroCapacidadService = cobroCapacidadService;
        this.productoCatalogoService = productoCatalogoService;
        this.auditoria = auditoria;
    }

    @Transactional
    public PublicacionReventaDTO publicar(String nombreUsuario, ReventaRequest request) {
        Usuario vendedor = usuario(nombreUsuario);
        if (request == null || request.getIdCompraItem() == null) {
            throw new IllegalArgumentException("Elegí la pieza que querés revender.");
        }
        CompraItem origen = compraItemRepository.findById(request.getIdCompraItem())
            .orElseThrow(() -> new IllegalArgumentException(
                "No encontramos la compra de esa pieza."));
        if (origen.getGrupo() == null || origen.getGrupo().getCompra() == null
                || !origen.getGrupo().getCompra().getCliente().getIdUsuario()
                    .equals(vendedor.getIdUsuario())) {
            throw new AccesoDenegadoException("Esa pieza no es tuya.");
        }
        if (origen.getEstadoEntrega() != EstadoEntregaItem.ENTREGADO) {
            throw new ConflictoException("La pieza todavía no está entregada.");
        }
        ProductoVariante variante = origen.getVariante();
        // Una pieza única vendida queda con stock 0 y estado AGOTADO: la señal
        // de unicidad es su evento génesis en el ledger, no esUnica().
        if (variante == null || variante.getIdVariante() == null
                || !eventoLedgerRepository.existsByVarianteIdVariante(
                    variante.getIdVariante())) {
            throw new ConflictoException("Solo las piezas únicas se pueden revender.");
        }
        if (variante.getPropietarioActual() == null
                || !variante.getPropietarioActual().getIdUsuario()
                    .equals(vendedor.getIdUsuario())) {
            throw new ConflictoException("Ya no sos el propietario de esta pieza.");
        }
        Producto producto = variante.getProducto();
        if (producto == null) {
            throw new ConflictoException("La pieza no tiene un producto asociado.");
        }
        if (producto.licenciaVigente() == null
                || !producto.licenciaVigente().permiteReventa()) {
            throw new ConflictoException("La licencia de esta pieza no permite la reventa.");
        }
        if (publicacionRepository.existsByVarianteIdVarianteAndEstadoIn(
                variante.getIdVariante(), ESTADOS_ACTIVOS)) {
            throw new ConflictoException(
                "Esta pieza ya tiene una publicación de reventa activa o suspendida.");
        }
        BigDecimal tope = resolverTope(variante, origen);
        BigDecimal precio = normalizarPrecio(request.getPrecio());
        validarTope(precio, tope, producto);
        if (!cobroCapacidadService.puedeCobrar(vendedor.getIdUsuario())) {
            throw new ConflictoException(
                "Conectá tu cuenta de MercadoPago para poder cobrar la reventa.");
        }

        PublicacionReventa publicacion = new PublicacionReventa();
        publicacion.setVariante(variante);
        publicacion.setVendedor(vendedor);
        publicacion.setCompraItemOrigen(origen);
        publicacion.setMoneda(producto.getMoneda());
        publicacion.setPrecio(precio);
        publicacion.setEstado(EstadoPublicacionReventa.PUBLICADA);
        publicacion.setFechaPublicacion(LocalDateTime.now());
        publicacion.setFechaCreacion(publicacion.getFechaPublicacion());
        PublicacionReventa guardada = publicacionRepository.save(publicacion);

        auditoria.registrar(
            AccionesAdmin.REVENTA_PUBLICAR, ENTIDAD, guardada.getIdPublicacionReventa(),
            "Publicó en reventa la pieza \"" + producto.getTitulo() + "\"",
            null, guardada, CAMPOS);
        return PublicacionReventaDTO.desde(guardada);
    }

    @Transactional
    public PublicacionReventaDTO editar(
            String nombreUsuario, Long idPublicacion, ReventaRequest request) {
        Usuario vendedor = usuario(nombreUsuario);
        PublicacionReventa publicacion = publicacionDelVendedor(vendedor, idPublicacion);
        if (publicacion.getEstado() != EstadoPublicacionReventa.PUBLICADA) {
            throw new ConflictoException(
                "Solo podés cambiar el precio de una publicación activa.");
        }
        BigDecimal tope = resolverTope(
            publicacion.getVariante(), publicacion.getCompraItemOrigen());
        BigDecimal precio = normalizarPrecio(request != null ? request.getPrecio() : null);
        validarTope(precio, tope, publicacion.getVariante().getProducto());

        Snapshot antes = Snapshot.de(publicacion, CAMPOS);
        publicacion.setPrecio(precio);
        publicacion.setFechaActualizacion(LocalDateTime.now());
        PublicacionReventa guardada = publicacionRepository.save(publicacion);

        auditoria.registrar(
            AccionesAdmin.REVENTA_EDITAR, ENTIDAD, guardada.getIdPublicacionReventa(),
            "Cambió el precio de reventa de \"" + producto(guardada).getTitulo() + "\"",
            antes, guardada, CAMPOS);
        return PublicacionReventaDTO.desde(guardada);
    }

    @Transactional
    public PublicacionReventaDTO cancelar(String nombreUsuario, Long idPublicacion) {
        Usuario vendedor = usuario(nombreUsuario);
        PublicacionReventa publicacion = publicacionDelVendedor(vendedor, idPublicacion);
        if (publicacion.getEstado() == EstadoPublicacionReventa.RESERVADA) {
            throw new ConflictoException(
                "Hay una compra en curso. Esperá a que venza la reserva para cancelar.");
        }
        if (publicacion.getEstado() != EstadoPublicacionReventa.PUBLICADA) {
            throw new ConflictoException("La publicación ya no está activa.");
        }

        Snapshot antes = Snapshot.de(publicacion, CAMPOS);
        publicacion.setEstado(EstadoPublicacionReventa.ARCHIVADA);
        publicacion.setFechaCierre(LocalDateTime.now());
        publicacion.setFechaActualizacion(publicacion.getFechaCierre());
        PublicacionReventa guardada = publicacionRepository.save(publicacion);

        auditoria.registrar(
            AccionesAdmin.REVENTA_CANCELAR, ENTIDAD, guardada.getIdPublicacionReventa(),
            "Canceló la reventa de \"" + producto(guardada).getTitulo() + "\"",
            antes, guardada, CAMPOS);
        return PublicacionReventaDTO.desde(guardada);
    }

    @Transactional(readOnly = true)
    public List<PublicacionReventaDTO> mias(String nombreUsuario) {
        Usuario vendedor = usuario(nombreUsuario);
        List<PublicacionReventaDTO> salida = new ArrayList<>();
        for (PublicacionReventa publicacion : publicacionRepository
                .findByVendedorIdUsuarioOrderByFechaPublicacionDesc(
                    vendedor.getIdUsuario())) {
            PublicacionReventaDTO dto = PublicacionReventaDTO.desde(publicacion);
            if (publicacion.getEstado() == EstadoPublicacionReventa.VENDIDA) {
                compraItemRepository
                    .findFirstByIdPublicacionReventa(
                        publicacion.getIdPublicacionReventa())
                    .ifPresent(venta -> {
                        if (venta.getGrupo() != null
                                && venta.getGrupo().getCompra() != null
                                && venta.getGrupo().getCompra().getCliente() != null) {
                            dto.setIdComprador(venta.getGrupo().getCompra()
                                .getCliente().getIdUsuario());
                            dto.setNombreComprador(venta.getGrupo().getCompra()
                                .getCliente().getNombreUsuario());
                        }
                        if (venta.getGrupo() != null) {
                            dto.setMontoComision(venta.getGrupo().getComision());
                            dto.setMontoNeto(venta.getGrupo().getNetoArtista());
                        }
                    });
            }
            salida.add(dto);
        }
        return salida;
    }

    @Transactional(readOnly = true)
    public List<PiezaRevendibleDTO> elegibles(String nombreUsuario) {
        Usuario usuario = usuario(nombreUsuario);
        List<PiezaRevendibleDTO> salida = new ArrayList<>();
        Set<Long> vistas = new HashSet<>();
        for (CompraItem item : compraItemRepository.findEntregadosDeCliente(
                usuario.getIdUsuario(), EstadoEntregaItem.ENTREGADO)) {
            ProductoVariante variante = item.getVariante();
            if (variante == null || variante.getIdVariante() == null
                    || !vistas.add(variante.getIdVariante())) {
                continue;
            }
            Producto producto = variante.getProducto();
            if (variante.getPropietarioActual() == null
                    || !variante.getPropietarioActual().getIdUsuario()
                        .equals(usuario.getIdUsuario())
                    || !eventoLedgerRepository.existsByVarianteIdVariante(
                        variante.getIdVariante())
                    || producto == null
                    || producto.licenciaVigente() == null
                    || !producto.licenciaVigente().permiteReventa()
                    || publicacionRepository.existsByVarianteIdVarianteAndEstadoIn(
                        variante.getIdVariante(), ESTADOS_ACTIVOS)) {
                continue;
            }
            BigDecimal tope = variante.getPrecioTopeReventa() != null
                ? variante.getPrecioTopeReventa() : item.getPrecioOriginal();
            salida.add(PiezaRevendibleDTO.builder()
                .idCompraItem(item.getIdCompraItem())
                .idProducto(producto.getIdProducto())
                .idVariante(variante.getIdVariante())
                .titulo(item.getTituloProducto())
                .nombreVariante(NombrePiezaVisible.nombreDeVariante(
                    item.getNombreVariante()))
                .imagenUrl(imagen(variante, producto))
                .moneda(item.getMonedaOriginal() != null
                    ? item.getMonedaOriginal().getCodigo()
                    : (producto.getMoneda() != null
                        ? producto.getMoneda().getCodigo() : null))
                .precioTope(tope)
                .precioPagado(item.getPrecioOriginal())
                .ultimoPrecio(publicacionRepository
                    .findFirstByVarianteIdVarianteOrderByFechaPublicacionDesc(
                        variante.getIdVariante())
                    .map(PublicacionReventa::getPrecio)
                    .orElse(null))
                .idArtistaOriginal(producto.getArtista() != null
                    ? producto.getArtista().getIdUsuario() : null)
                .nombreArtistaOriginal(producto.getArtista() != null
                    ? producto.getArtista().getNombreUsuario() : null)
                .tipoEntrega(item.getTipoEntrega())
                .fechaEntrega(item.getFechaEntrega())
                .licencia(parseLicencia(item.getLicenciaSnapshot()))
                .build());
        }
        return salida;
    }

    @Transactional(readOnly = true)
    public ReventaDetalleDTO detallePublico(Long idPublicacion, Long viewerId) {
        PublicacionReventa publicacion = publicacionRepository
            .findByIdPublicacionReventa(idPublicacion)
            .orElseThrow(() -> new NoSuchElementException(
                "Publicación de reventa no encontrada."));
        ProductoVariante variante = publicacion.getVariante();
        Producto producto = variante != null ? variante.getProducto() : null;
        CompraItem origen = publicacion.getCompraItemOrigen();
        Usuario artista = producto != null ? producto.getArtista() : null;
        Usuario vendedor = publicacion.getVendedor();
        boolean propia = viewerId != null && vendedor != null
            && viewerId.equals(vendedor.getIdUsuario());
        if (!propia && vendedor != null && !vendedor.isEstadoCuenta()) {
            throw new ServicioNoDisponibleException(
                "Esta publicación no está disponible.");
        }
        if (!propia && artista != null && !artista.isEstadoCuenta()) {
            throw new ServicioNoDisponibleException(
                "Esta publicación no está disponible.");
        }
        boolean puedeComprar = publicacion.getEstado()
            == EstadoPublicacionReventa.PUBLICADA && !propia;
        String nombreVariante = NombrePiezaVisible.nombreDeVariante(
            origen != null ? origen.getNombreVariante()
                : (variante != null ? variante.getNombre() : null));
        String tituloProducto = origen != null ? origen.getTituloProducto()
            : (producto != null ? producto.getTitulo() : null);
        return ReventaDetalleDTO.builder()
            .idPublicacionReventa(publicacion.getIdPublicacionReventa())
            .idProducto(producto != null ? producto.getIdProducto() : null)
            .idVariante(variante != null ? variante.getIdVariante() : null)
            .titulo(nombreVariante != null ? nombreVariante : tituloProducto)
            .tituloOriginal(nombreVariante != null ? tituloProducto : null)
            .nombreVariante(nombreVariante)
            .descripcion(producto != null ? producto.getDescripcion() : null)
            .imagenUrl(imagen(variante, producto))
            .moneda(publicacion.getMoneda() != null
                ? publicacion.getMoneda().getCodigo() : null)
            .precio(publicacion.getPrecio())
            .estado(publicacion.getEstado())
            .idArtista(artista != null ? artista.getIdUsuario() : null)
            .nombreArtista(artista != null ? artista.getNombreUsuario() : null)
            .nombreDisplayArtista(artista != null
                ? artista.getNombreDisplay() : null)
            .urlFotoArtista(artista != null ? foto(artista) : null)
            .idVendedor(vendedor != null ? vendedor.getIdUsuario() : null)
            .nombreVendedor(vendedor != null
                ? vendedor.getNombreUsuario() : null)
            .esPropia(propia)
            .puedeComprar(puedeComprar)
            .contenidoSensible(producto != null
                && producto.isContenidoSensible())
            .contenidosSensibles(producto != null
                ? producto.getContenidosSensibles().stream()
                    .map(Enum::name).toList()
                : List.of())
            .tipoEntrega(origen != null ? origen.getTipoEntrega()
                : (producto != null && producto.getMedio() != null
                    ? producto.getMedio().getTipoEntrega() : null))
            .licencia(parseLicencia(origen != null
                ? origen.getLicenciaSnapshot() : null))
            .ledger(variante != null
                ? productoCatalogoService.ledgerPublicoDeVariante(variante)
                : null)
            .build();
    }

    @Transactional
    public PublicacionReventaDTO suspender(Long idPublicacion, String motivo) {
        PublicacionReventa publicacion = publicacionRepository
            .findByIdPublicacionReventa(idPublicacion)
            .orElseThrow(() -> new IllegalArgumentException(
                "No encontramos esa publicación."));
        if (publicacion.getEstado() == EstadoPublicacionReventa.RESERVADA) {
            throw new ConflictoException(
                "Hay una compra en curso. Esperá a que venza la reserva.");
        }
        if (publicacion.getEstado() != EstadoPublicacionReventa.PUBLICADA) {
            throw new ConflictoException(
                "Solo se puede suspender una publicación activa.");
        }
        publicacion.setEstado(EstadoPublicacionReventa.SUSPENDIDA);
        publicacion.setFechaCierre(LocalDateTime.now());
        publicacion.setFechaActualizacion(publicacion.getFechaCierre());
        PublicacionReventa guardada = publicacionRepository.save(publicacion);
        auditoria.registrar(
            AccionesAdmin.REVENTA_SUSPENDER, ENTIDAD,
            guardada.getIdPublicacionReventa(),
            "Suspendió la reventa de \"" + producto(guardada).getTitulo() + "\"",
            motivo != null && !motivo.isBlank() ? motivo : "no especificado");
        return PublicacionReventaDTO.desde(guardada);
    }

    private LicenciaCompraDTO parseLicencia(String json) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            return JSON.readValue(json, LicenciaCompraDTO.class);
        } catch (Exception e) {
            return null;
        }
    }

    private String imagen(ProductoVariante variante, Producto producto) {
        if (variante != null && variante.getImagenUrl() != null
                && !variante.getImagenUrl().isBlank()) {
            return variante.getImagenUrl();
        }
        if (producto != null && producto.getImagenPortada() != null
                && !producto.getImagenPortada().isBlank()) {
            return producto.getImagenPortada();
        }
        if (producto != null && producto.getImagenes() != null) {
            return producto.getImagenes().stream()
                .filter(imagen -> imagen != null && imagen.getUrl() != null)
                .map(imagen -> imagen.getUrl())
                .findFirst()
                .orElse(null);
        }
        return null;
    }

    private String foto(Usuario usuario) {
        if (usuario == null) {
            return null;
        }
        String miniatura = usuario.getUrlFotoPerfilMiniatura();
        return miniatura != null && !miniatura.isBlank()
            ? miniatura : usuario.getUrlFotoPerfil();
    }

    private BigDecimal resolverTope(ProductoVariante variante, CompraItem origen) {
        if (variante.getPrecioTopeReventa() != null) {
            return variante.getPrecioTopeReventa();
        }
        CompraItem primeraVenta = compraItemRepository
            .findFirstByVarianteIdVarianteAndEstadoEntregaOrderByFechaEntregaAsc(
                variante.getIdVariante(), EstadoEntregaItem.ENTREGADO)
            .orElse(origen);
        BigDecimal tope = primeraVenta.getPrecioOriginal();
        if (tope == null) {
            return null;
        }
        variante.setTipoTopeReventa(TipoTopeReventa.PRECIO_PUBLICACION);
        variante.setPrecioTopeReventa(tope);
        varianteRepository.save(variante);
        return tope;
    }

    private void validarTope(BigDecimal precio, BigDecimal tope, Producto producto) {
        if (tope == null || precio.compareTo(tope) <= 0) {
            return;
        }
        String moneda = producto != null && producto.getMoneda() != null
            ? producto.getMoneda().getCodigo() : "";
        throw new IllegalArgumentException(
            "El precio máximo para esta pieza es "
                + tope.setScale(2, RoundingMode.HALF_UP).toPlainString()
                + (moneda.isBlank() ? "" : " " + moneda)
                + ", el precio original de publicación.");
    }

    private BigDecimal normalizarPrecio(BigDecimal precio) {
        if (precio == null || precio.signum() <= 0) {
            throw new IllegalArgumentException("El precio de reventa debe ser mayor a cero.");
        }
        return precio.setScale(2, RoundingMode.HALF_UP);
    }

    private PublicacionReventa publicacionDelVendedor(Usuario vendedor, Long idPublicacion) {
        PublicacionReventa publicacion = publicacionRepository
            .findByIdPublicacionReventa(idPublicacion)
            .orElseThrow(() -> new IllegalArgumentException("No encontramos esa publicación."));
        if (publicacion.getVendedor() == null
                || !publicacion.getVendedor().getIdUsuario().equals(vendedor.getIdUsuario())) {
            throw new AccesoDenegadoException("Esa publicación no es tuya.");
        }
        return publicacion;
    }

    private Producto producto(PublicacionReventa publicacion) {
        return publicacion.getVariante() != null
            ? publicacion.getVariante().getProducto() : null;
    }

    private Usuario usuario(String nombreUsuario) {
        return usuarioRepository.findByNombreUsuario(nombreUsuario)
            .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado."));
    }
}
