package com.temipora.backend_springboot.servicios;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.temipora.backend_springboot.modelo.Carrito;
import com.temipora.backend_springboot.modelo.CarritoDTO;
import com.temipora.backend_springboot.modelo.CarritoGrupoDTO;
import com.temipora.backend_springboot.modelo.CarritoItem;
import com.temipora.backend_springboot.modelo.CarritoItemDTO;
import com.temipora.backend_springboot.modelo.CarritoItemRequest;
import com.temipora.backend_springboot.modelo.CompraItem;
import com.temipora.backend_springboot.modelo.EstadoProducto;
import com.temipora.backend_springboot.modelo.EstadoPublicacionReventa;
import com.temipora.backend_springboot.modelo.EstadoReserva;
import com.temipora.backend_springboot.modelo.Moneda;
import com.temipora.backend_springboot.modelo.Producto;
import com.temipora.backend_springboot.modelo.ProductoVariante;
import com.temipora.backend_springboot.modelo.PublicacionReventa;
import com.temipora.backend_springboot.modelo.Usuario;
import com.temipora.backend_springboot.repositorios.CarritoItemRepository;
import com.temipora.backend_springboot.repositorios.CarritoRepository;
import com.temipora.backend_springboot.repositorios.ProductoVarianteRepository;
import com.temipora.backend_springboot.repositorios.PublicacionReventaRepository;
import com.temipora.backend_springboot.repositorios.ReservaStockRepository;
import com.temipora.backend_springboot.servicios.pagos.mercadopago.MercadoPagoOAuthService;

@Service
public class CarritoService {

    private static final String ARS = "ARS";

    private final CarritoRepository carritoRepository;
    private final CarritoItemRepository carritoItemRepository;
    private final ProductoVarianteRepository varianteRepository;
    private final ReservaStockRepository reservaStockRepository;
    private final PublicacionReventaRepository publicacionReventaRepository;
    private final UsuarioService usuarioService;
    private final MonedaService monedaService;
    private final ConversionMonedaService conversionMonedaService;
    private final MercadoPagoOAuthService oauthService;
    private final boolean unArtistaPorCarrito;
    private final String proveedorPago;

    public CarritoService(
            CarritoRepository carritoRepository,
            CarritoItemRepository carritoItemRepository,
            ProductoVarianteRepository varianteRepository,
            ReservaStockRepository reservaStockRepository,
            PublicacionReventaRepository publicacionReventaRepository,
            UsuarioService usuarioService,
            MonedaService monedaService,
            ConversionMonedaService conversionMonedaService,
            MercadoPagoOAuthService oauthService,
            @Value("${carrito.un-artista:false}") boolean unArtistaPorCarrito,
            @Value("${pagos.proveedor:simulado}") String proveedorPago) {
        this.carritoRepository = carritoRepository;
        this.carritoItemRepository = carritoItemRepository;
        this.varianteRepository = varianteRepository;
        this.reservaStockRepository = reservaStockRepository;
        this.publicacionReventaRepository = publicacionReventaRepository;
        this.usuarioService = usuarioService;
        this.monedaService = monedaService;
        this.conversionMonedaService = conversionMonedaService;
        this.oauthService = oauthService;
        this.unArtistaPorCarrito = unArtistaPorCarrito;
        this.proveedorPago = proveedorPago;
    }

    @Transactional(readOnly = true)
    public CarritoDTO obtener(String nombreUsuario) {
        Usuario usuario = usuarioService.getByNombreUsuario(nombreUsuario);
        Carrito carrito = carritoRepository
            .findByUsuarioIdUsuario(usuario.getIdUsuario())
            .orElse(null);
        return aDTO(carrito);
    }

    @Transactional
    public CarritoDTO agregar(String nombreUsuario, CarritoItemRequest request) {
        Usuario usuario = usuarioService.getByNombreUsuario(nombreUsuario);
        if (request == null || request.getIdVariante() == null) {
            throw new IllegalArgumentException("La variante es obligatoria.");
        }
        ProductoVariante variante = varianteRepository
            .findByIdVariante(request.getIdVariante())
            .orElseThrow(() -> new NoSuchElementException(
                "Variante no encontrada: " + request.getIdVariante()));
        Producto producto = variante.getProducto();
        PublicacionReventa publicacion = request.getIdPublicacionReventa() != null
            ? publicacionDe(request.getIdPublicacionReventa())
            : null;
        if (publicacion != null) {
            validarReventa(publicacion, variante, usuario);
        } else {
            validarComprable(producto, variante, usuario);
        }

        int cantidad = request.getCantidad() != null ? request.getCantidad() : 1;
        if (cantidad != 1) {
            throw new IllegalArgumentException(
                "Por ahora cada producto se agrega de a una unidad.");
        }
        if (publicacion == null) {
            validarStock(variante, cantidad);
        }

        Carrito carrito = carritoDe(usuario);
        validarUnSoloArtista(carrito,
            publicacion != null ? publicacion.getVendedor() : producto.getArtista());

        CarritoItem item = carritoItemRepository
            .findByCarritoIdCarritoAndVarianteIdVariante(
                carrito.getIdCarrito(), variante.getIdVariante())
            .orElse(null);
        if (item != null) {
            throw new ConflictoException(
                "Ese producto ya está en tu carrito.");
        }
        item = new CarritoItem();
        item.setVariante(variante);
        item.setIdPublicacionReventa(
            publicacion != null ? publicacion.getIdPublicacionReventa() : null);
        item.setCantidad(1);
        carrito.agregarItem(item);
        carrito.setFechaActualizacion(LocalDateTime.now());
        carritoRepository.save(carrito);
        return aDTO(carrito);
    }

    @Transactional
    public CarritoDTO actualizar(
            String nombreUsuario, Long idItem, Integer cantidad) {
        Usuario usuario = usuarioService.getByNombreUsuario(nombreUsuario);
        Carrito carrito = carritoDe(usuario);
        CarritoItem item = carritoItemRepository
            .findByIdCarritoItemAndCarritoIdCarrito(idItem, carrito.getIdCarrito())
            .orElseThrow(() -> new NoSuchElementException(
                "Ítem de carrito no encontrado: " + idItem));
        if (cantidad == null || cantidad != 1) {
            throw new ConflictoException(
                "Por ahora cada producto se agrega de a una unidad.");
        }
        validarStock(item.getVariante(), 1);
        item.setCantidad(1);
        carrito.setFechaActualizacion(LocalDateTime.now());
        carritoRepository.save(carrito);
        return aDTO(carrito);
    }

    @Transactional
    public CarritoDTO quitar(String nombreUsuario, Long idItem) {
        Usuario usuario = usuarioService.getByNombreUsuario(nombreUsuario);
        Carrito carrito = carritoDe(usuario);
        CarritoItem item = carritoItemRepository
            .findByIdCarritoItemAndCarritoIdCarrito(idItem, carrito.getIdCarrito())
            .orElseThrow(() -> new NoSuchElementException(
                "Ítem de carrito no encontrado: " + idItem));
        carrito.quitarItem(item);
        carrito.setFechaActualizacion(LocalDateTime.now());
        carritoRepository.save(carrito);
        return aDTO(carrito);
    }

    @Transactional
    public CarritoDTO vaciar(String nombreUsuario) {
        Usuario usuario = usuarioService.getByNombreUsuario(nombreUsuario);
        Carrito carrito = carritoDe(usuario);
        carrito.getItems().clear();
        carrito.setFechaActualizacion(LocalDateTime.now());
        carritoRepository.save(carrito);
        return aDTO(carrito);
    }

    @Transactional
    public CarritoDTO vaciarArtista(String nombreUsuario, Long idArtista) {
        if (idArtista == null) {
            return vaciar(nombreUsuario);
        }
        Usuario usuario = usuarioService.getByNombreUsuario(nombreUsuario);
        Carrito carrito = carritoRepository
            .findByUsuarioIdUsuario(usuario.getIdUsuario())
            .orElse(null);
        if (carrito == null) {
            return aDTO(null);
        }
        carrito.getItems().removeIf(item -> {
            Usuario vendedor = vendedorDe(item);
            return vendedor != null
                && idArtista.equals(vendedor.getIdUsuario());
        });
        carrito.setFechaActualizacion(LocalDateTime.now());
        carritoRepository.save(carrito);
        return aDTO(carrito);
    }

    private Carrito carritoDe(Usuario usuario) {
        return carritoRepository.findByUsuarioIdUsuario(usuario.getIdUsuario())
            .orElseGet(() -> {
                Carrito nuevo = new Carrito();
                nuevo.setUsuario(usuario);
                return carritoRepository.save(nuevo);
            });
    }

    private void validarComprable(
            Producto producto, ProductoVariante variante, Usuario usuario) {
        if (producto == null) {
            throw new IllegalStateException("La variante no tiene producto.");
        }
        if (producto.getEstado() != EstadoProducto.DISPONIBLE) {
            throw new ConflictoException("El producto no está disponible.");
        }
        if (!variante.estaDisponible()) {
            throw new ConflictoException("La variante no está disponible.");
        }
        if (producto.getArtista() != null
                && producto.getArtista().getIdUsuario().equals(usuario.getIdUsuario())) {
            throw new IllegalArgumentException(
                "No podés comprar tu propio producto.");
        }
    }

    private void validarStock(ProductoVariante variante, int cantidad) {
        if (variante.getStockDisponible() == null) {
            return;
        }
        long reservado = reservaStockRepository.sumarCantidad(
            variante.getIdVariante(), EstadoReserva.ACTIVA);
        long disponible = variante.getStockDisponible() - reservado;
        if (cantidad > disponible) {
            throw new ConflictoException(
                "No hay stock suficiente para “" + variante.getNombre() + "”. "
                    + "Disponible: " + Math.max(0, disponible) + ".");
        }
    }

    private void validarUnSoloArtista(Carrito carrito, Usuario vendedor) {
        if (!unArtistaPorCarrito || carrito.getItems().isEmpty()) {
            return;
        }
        Usuario actual = vendedorDe(carrito.getItems().get(0));
        Long artistaActual = actual != null ? actual.getIdUsuario() : null;
        Long artistaNuevo = vendedor != null ? vendedor.getIdUsuario() : null;
        if (artistaActual != null && !artistaActual.equals(artistaNuevo)) {
            throw new ConflictoException(
                "Por ahora el carrito permite productos de un solo artista. "
                    + "Terminá esta compra o vaciá el carrito para agregar otro.");
        }
    }

    private CarritoDTO aDTO(Carrito carrito) {
        Moneda ars = monedaArs();
        String codigoArs = ars != null ? ars.getCodigo() : ARS;

        if (carrito == null || carrito.getItems().isEmpty()) {
            return CarritoDTO.builder()
                .idCarrito(carrito != null ? carrito.getIdCarrito() : null)
                .grupos(new ArrayList<>())
                .subtotal(cero())
                .total(cero())
                .moneda(codigoArs)
                .cantidadItems(0)
                .puedeCheckout(false)
                .avisos(new ArrayList<>())
                .build();
        }

        Map<Long, CarritoGrupoDTO> grupos = new LinkedHashMap<>();
        List<String> avisos = new ArrayList<>();
        int cantidadItems = 0;
        boolean todoDisponible = true;
        boolean todosPuedenCobrar = true;

        for (CarritoItem item : carrito.getItems()) {
            CarritoItemDTO dto = aItemDTO(item, ars);
            cantidadItems += dto.getCantidad() != null ? dto.getCantidad() : 0;
            if (!dto.isDisponible()) {
                todoDisponible = false;
                avisos.add(dto.getMotivoNoDisponible());
            }

            Long idArtista = dto.getIdArtista();
            CarritoGrupoDTO grupo = grupos.computeIfAbsent(
                idArtista, clave -> nuevoGrupo(vendedorDe(item)));
            grupo.getItems().add(dto);
            grupo.setEsReventa(grupo.isEsReventa() || dto.isEsReventa());
            grupo.setSubtotal(sumar(grupo.getSubtotal(), subtotalItem(dto)));
        }

        BigDecimal subtotal = cero();
        for (CarritoGrupoDTO grupo : grupos.values()) {
            grupo.setTotal(grupo.getSubtotal());
            subtotal = sumar(subtotal, grupo.getSubtotal());
            if (!grupo.isPuedeCobrar()) {
                todosPuedenCobrar = false;
                avisos.add(grupo.getMotivoNoCobro());
            }
        }

        boolean puedeCheckout = todoDisponible && todosPuedenCobrar
            && !grupos.isEmpty();

        return CarritoDTO.builder()
            .idCarrito(carrito.getIdCarrito())
            .grupos(new ArrayList<>(grupos.values()))
            .subtotal(subtotal)
            .total(subtotal)
            .moneda(codigoArs)
            .cantidadItems(cantidadItems)
            .puedeCheckout(puedeCheckout)
            .avisos(avisos)
            .build();
    }

    private CarritoGrupoDTO nuevoGrupo(Usuario vendedor) {
        boolean requiereCuenta = "mercadopago".equalsIgnoreCase(proveedorPago);
        boolean puedeCobrar = !requiereCuenta
            || (vendedor != null && oauthService.estado(vendedor.getIdUsuario()).conectada());
        return CarritoGrupoDTO.builder()
            .idArtista(vendedor != null ? vendedor.getIdUsuario() : null)
            .nombreArtista(vendedor != null ? vendedor.getNombreUsuario() : null)
            .urlFotoArtista(vendedor != null ? fotoArtista(vendedor) : null)
            .puedeCobrar(puedeCobrar)
            .motivoNoCobro(puedeCobrar ? null
                : "El vendedor todavía no conectó su cuenta de MercadoPago.")
            .items(new ArrayList<>())
            .subtotal(cero())
            .total(cero())
            .build();
    }

    private CarritoItemDTO aItemDTO(
            CarritoItem item, Moneda ars) {
        ProductoVariante variante = item.getVariante();
        Producto producto = variante.getProducto();
        PublicacionReventa publicacion = item.getIdPublicacionReventa() != null
            ? publicacionReventaRepository
                .findByIdPublicacionReventa(item.getIdPublicacionReventa())
                .orElse(null)
            : null;
        boolean reventa = publicacion != null;
        Moneda monedaOriginal = reventa ? publicacion.getMoneda() : producto.getMoneda();
        BigDecimal precioOriginal = reventa
            ? publicacion.getPrecio() : variante.getPrecio();
        BigDecimal precioCobro = null;
        String motivo = null;
        boolean disponible = true;

        if (reventa) {
            if (publicacion.getEstado() != EstadoPublicacionReventa.PUBLICADA) {
                disponible = false;
                motivo = "La publicación de reventa ya no está disponible.";
            } else if (monedaOriginal == null) {
                disponible = false;
                motivo = "La publicación de reventa no tiene una moneda configurada.";
            } else {
                BigDecimal factor = conversionMonedaService.factor(
                    monedaOriginal, ars, LocalDateTime.now());
                if (factor == null) {
                    disponible = false;
                    motivo = "No hay cotización de " + monedaOriginal.getCodigo()
                        + " a " + ARS + ".";
                } else {
                    precioCobro = publicacion.getPrecio().multiply(factor)
                        .setScale(2, RoundingMode.HALF_UP);
                }
            }
        } else if (producto.getEstado() != EstadoProducto.DISPONIBLE
                || !variante.estaDisponible()) {
            disponible = false;
            motivo = "“" + producto.getTitulo() + "” ya no está disponible.";
        } else if (monedaOriginal == null) {
            disponible = false;
            motivo = "El producto no tiene una moneda configurada.";
        } else {
            BigDecimal factor = conversionMonedaService.factor(
                monedaOriginal, ars, LocalDateTime.now());
            if (factor == null) {
                disponible = false;
                motivo = "No hay cotización de " + monedaOriginal.getCodigo()
                    + " a " + ARS + ".";
            } else if (variante.getPrecio() != null) {
                precioCobro = variante.getPrecio().multiply(factor)
                    .setScale(2, RoundingMode.HALF_UP);
            }
        }

        Usuario vendedor = reventa ? publicacion.getVendedor() : producto.getArtista();
        Usuario artistaOriginal = producto.getArtista();
        CompraItem origen = reventa ? publicacion.getCompraItemOrigen() : null;

        return CarritoItemDTO.builder()
            .idCarritoItem(item.getIdCarritoItem())
            .idVariante(variante.getIdVariante())
            .idProducto(producto.getIdProducto())
            .idArtista(vendedor != null ? vendedor.getIdUsuario() : null)
            .idArtistaOriginal(artistaOriginal != null
                ? artistaOriginal.getIdUsuario() : null)
            .nombreArtistaOriginal(artistaOriginal != null
                ? artistaOriginal.getNombreUsuario() : null)
            .idPublicacionReventa(reventa
                ? publicacion.getIdPublicacionReventa() : null)
            .esReventa(reventa)
            .titulo(reventa && origen != null
                ? origen.getTituloProducto() : producto.getTitulo())
            .nombreVariante(reventa && origen != null
                ? origen.getNombreVariante() : variante.getNombre())
            .imagenUrl(variante.getImagenUrl())
            .precioOriginal(precioOriginal)
            .monedaOriginal(monedaOriginal != null
                ? monedaOriginal.getCodigo() : null)
            .precioCobro(precioCobro)
            .monedaCobro(ARS)
            .cantidad(item.getCantidad())
            .cantidadEditable(false)
            .motivoCantidadFija(
                "") // Por ahora cada producto se agrega de a una unidad.
            .stockDisponible(variante.getStockDisponible())
            .esUnica(reventa || variante.esUnica())
            .tipoEntrega(reventa && origen != null
                ? origen.getTipoEntrega()
                : (producto.getMedio() != null
                    ? producto.getMedio().getTipoEntrega() : null))
            .disponible(disponible)
            .motivoNoDisponible(motivo)
            .build();
    }

    private PublicacionReventa publicacionDe(Long idPublicacion) {
        return publicacionReventaRepository.findByIdPublicacionReventa(idPublicacion)
            .orElseThrow(() -> new NoSuchElementException(
                "Publicación de reventa no encontrada: " + idPublicacion));
    }

    private void validarReventa(
            PublicacionReventa publicacion,
            ProductoVariante variante,
            Usuario comprador) {
        if (publicacion.getEstado() != EstadoPublicacionReventa.PUBLICADA) {
            throw new ConflictoException(
                "La publicación de reventa ya no está disponible.");
        }
        if (publicacion.getVendedor() == null
                || publicacion.getVendedor().getIdUsuario()
                    .equals(comprador.getIdUsuario())) {
            throw new IllegalArgumentException(
                "No podés comprar tu propia reventa.");
        }
        if (publicacion.getVariante() == null
                || !publicacion.getVariante().getIdVariante()
                    .equals(variante.getIdVariante())) {
            throw new IllegalArgumentException(
                "La publicación de reventa no corresponde a esa variante.");
        }
    }

    private Usuario vendedorDe(CarritoItem item) {
        if (item.getIdPublicacionReventa() != null) {
            return publicacionReventaRepository
                .findByIdPublicacionReventa(item.getIdPublicacionReventa())
                .map(PublicacionReventa::getVendedor)
                .orElse(null);
        }
        Producto producto = item.getVariante() != null
            ? item.getVariante().getProducto() : null;
        return producto != null ? producto.getArtista() : null;
    }

    private BigDecimal subtotalItem(CarritoItemDTO item) {
        if (item.getPrecioCobro() == null || item.getCantidad() == null) {
            return cero();
        }
        return item.getPrecioCobro()
            .multiply(BigDecimal.valueOf(item.getCantidad()))
            .setScale(2, RoundingMode.HALF_UP);
    }

    private Moneda monedaArs() {
        Moneda ars = monedaService.obtenerPorCodigo(ARS);
        return ars != null ? ars : monedaService.predeterminada();
    }

    private String fotoArtista(Usuario artista) {
        if (artista.getUrlFotoPerfilMiniatura() != null
                && !artista.getUrlFotoPerfilMiniatura().isBlank()) {
            return artista.getUrlFotoPerfilMiniatura();
        }
        return artista.getUrlFotoPerfil();
    }

    private BigDecimal sumar(BigDecimal a, BigDecimal b) {
        return (a != null ? a : cero()).add(b != null ? b : cero());
    }

    private BigDecimal cero() {
        return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    }
}
