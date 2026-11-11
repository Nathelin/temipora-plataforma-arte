package com.temipora.backend_springboot;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import com.temipora.backend_springboot.modelo.CarritoDTO;
import com.temipora.backend_springboot.modelo.CarritoItemRequest;
import com.temipora.backend_springboot.modelo.CheckoutRequest;
import com.temipora.backend_springboot.modelo.CompraDTO;
import com.temipora.backend_springboot.modelo.CompraGrupoDTO;
import com.temipora.backend_springboot.modelo.CompraItemDTO;
import com.temipora.backend_springboot.modelo.CompraResumenDTO;
import com.temipora.backend_springboot.modelo.DerechoTermino;
import com.temipora.backend_springboot.modelo.ERol;
import com.temipora.backend_springboot.modelo.EstadoCompra;
import com.temipora.backend_springboot.modelo.EstadoEntregaItem;
import com.temipora.backend_springboot.modelo.EstadoProducto;
import com.temipora.backend_springboot.modelo.EstadoReserva;
import com.temipora.backend_springboot.modelo.EstadoVariante;
import com.temipora.backend_springboot.modelo.LicenciaProductoRequest;
import com.temipora.backend_springboot.modelo.Medio;
import com.temipora.backend_springboot.modelo.Moneda;
import com.temipora.backend_springboot.modelo.PaginaDTO;
import com.temipora.backend_springboot.modelo.PagoCompraDTO;
import com.temipora.backend_springboot.modelo.Producto;
import com.temipora.backend_springboot.modelo.ProductoDTO;
import com.temipora.backend_springboot.modelo.ProductoImagenRequest;
import com.temipora.backend_springboot.modelo.ProductoRequest;
import com.temipora.backend_springboot.modelo.ProductoVarianteRequest;
import com.temipora.backend_springboot.modelo.Rol;
import com.temipora.backend_springboot.modelo.TipoEntrega;
import com.temipora.backend_springboot.modelo.TipoEventoLedger;
import com.temipora.backend_springboot.modelo.TipoProducto;
import com.temipora.backend_springboot.modelo.Usuario;
import com.temipora.backend_springboot.repositorios.DerechoTerminoRepository;
import com.temipora.backend_springboot.repositorios.EventoLedgerRepository;
import com.temipora.backend_springboot.repositorios.MedioRepository;
import com.temipora.backend_springboot.repositorios.ProductoRepository;
import com.temipora.backend_springboot.repositorios.ProductoVarianteRepository;
import com.temipora.backend_springboot.repositorios.ReservaStockRepository;
import com.temipora.backend_springboot.repositorios.RolRepository;
import com.temipora.backend_springboot.repositorios.TipoProductoRepository;
import com.temipora.backend_springboot.repositorios.UsuarioRepository;
import com.temipora.backend_springboot.servicios.CarritoService;
import com.temipora.backend_springboot.servicios.ConflictoException;
import com.temipora.backend_springboot.servicios.CompraPagoService;
import com.temipora.backend_springboot.servicios.CompraService;
import com.temipora.backend_springboot.servicios.MonedaService;
import com.temipora.backend_springboot.servicios.ProductoService;

@SpringBootTest(properties = "pagos.proveedor=simulado")
@Transactional
class CompraProductoIntegracionTest {

    @Autowired private CarritoService carritoService;
    @Autowired private CompraService compraService;
    @Autowired private CompraPagoService compraPagoService;
    @Autowired private ProductoService productoService;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private RolRepository rolRepository;
    @Autowired private TipoProductoRepository tipoProductoRepository;
    @Autowired private MedioRepository medioRepository;
    @Autowired private DerechoTerminoRepository derechoTerminoRepository;
    @Autowired private ProductoRepository productoRepository;
    @Autowired private ProductoVarianteRepository varianteRepository;
    @Autowired private ReservaStockRepository reservaStockRepository;
    @Autowired private EventoLedgerRepository eventoLedgerRepository;
    @Autowired private MonedaService monedaService;

    @Test
    void carritoCheckoutYPagoSimulado_transfierePropiedadYRegistraVenta() {
        Usuario artista = crearUsuario(ERol.ARTISTA);
        Usuario cliente = crearUsuario(ERol.CLIENTE);
        ProductoDTO producto = crearProductoDigitalUnico(artista);

        Long idVariante = producto.getVariantes().get(0).getIdVariante();
        CarritoItemRequest item = new CarritoItemRequest();
        item.setIdVariante(idVariante);
        item.setCantidad(1);
        carritoService.agregar(cliente.getNombreUsuario(), item);

        CheckoutRequest checkout = new CheckoutRequest();
        checkout.setClaveIdempotencia("compra-test-" + UUID.randomUUID());
        CompraDTO compra = compraService.crearDesdeCarrito(
            cliente.getNombreUsuario(), checkout);

        assertThat(compra.getEstado()).isEqualTo(EstadoCompra.PENDIENTE_PAGO);
        assertThat(compra.getGrupos()).hasSize(1);
        CompraGrupoDTO grupo = compra.getGrupos().get(0);
        assertThat(grupo.getIdArtista()).isEqualTo(artista.getIdUsuario());
        assertThat(grupo.getItems()).hasSize(1);
        CompraItemDTO itemComprado = grupo.getItems().get(0);
        assertThat(itemComprado.getEstadoEntrega()).isEqualTo(EstadoEntregaItem.PENDIENTE);

        var reserva = reservaStockRepository.findByCompraItemIdCompraItem(
            itemComprado.getIdCompraItem()).orElseThrow();
        assertThat(reserva.getEstado()).isEqualTo(EstadoReserva.ACTIVA);
        assertThat(varianteRepository.findByIdVariante(idVariante).orElseThrow()
            .getStockDisponible()).isEqualTo(1);

        PagoCompraDTO pago = compraPagoService.iniciarPago(
            cliente.getNombreUsuario(), compra.getIdCompra(), grupo.getIdCompraGrupo());
        assertThat(pago.getInstruccion()).contains("simulado");

        CompraDTO pagada = compraPagoService.confirmarSimulado(
            cliente.getNombreUsuario(), pago.getIdIntentoCobro());

        assertThat(pagada.getEstado()).isEqualTo(EstadoCompra.PAGA);
        assertThat(pagada.getGrupos().get(0).getEstado().name()).isEqualTo("PAGADO");
        assertThat(pagada.getGrupos().get(0).getItems().get(0)
            .getEstadoEntrega()).isEqualTo(EstadoEntregaItem.ENTREGADO);

        var variante = varianteRepository.findByIdVariante(idVariante).orElseThrow();
        assertThat(variante.getStockDisponible()).isZero();
        assertThat(variante.getEstado()).isEqualTo(EstadoVariante.AGOTADO);
        assertThat(variante.getPropietarioActual().getIdUsuario())
            .isEqualTo(cliente.getIdUsuario());

        var eventos = eventoLedgerRepository
            .findByVarianteIdVarianteOrderBySecuenciaAsc(idVariante);
        assertThat(eventos).hasSize(2);
        assertThat(eventos.get(1).getTipo()).isEqualTo(TipoEventoLedger.VENTA);
        assertThat(eventos.get(1).getActorOrigen().getIdUsuario())
            .isEqualTo(artista.getIdUsuario());
        assertThat(eventos.get(1).getActorDestino().getIdUsuario())
            .isEqualTo(cliente.getIdUsuario());
        assertThat(eventos.get(1).getHashPrevio())
            .isEqualTo(eventos.get(0).getHashEvento());
        assertThat(eventos.get(1).getMonto()).isEqualByComparingTo("150.00");

        CompraDTO detalle = compraService.obtener(
            cliente.getNombreUsuario(), compra.getIdCompra());
        assertThat(detalle.getGrupos()).hasSize(1);

        var itemDto = detalle.getGrupos().get(0).getItems().get(0);
        assertThat(itemDto.getNombreArtistaOriginal())
            .isEqualTo(artista.getNombreUsuario());
        assertThat(itemDto.getImagenUrl()).isNotBlank();
        assertThat(itemDto.getLicencia()).isNotNull();
        assertThat(itemDto.getLicencia().getDerechos()).isNotEmpty();
        assertThat(itemDto.isTieneLedger()).isTrue();
        assertThat(itemDto.getPrecioTotalCobro()).isEqualByComparingTo("150.00");

        PaginaDTO<CompraResumenDTO> pagina = compraService.listar(
            cliente.getNombreUsuario(), 0, 10);
        assertThat(pagina.getContenido()).isNotEmpty();

        var resumen = pagina.getContenido().get(0);
        assertThat(resumen.getItemsPreview()).isNotEmpty();
        assertThat(resumen.getCantidadProductos()).isGreaterThanOrEqualTo(1);
        assertThat(resumen.isPuedeDescargar()).isTrue();

        PaginaDTO<CompraResumenDTO> pagadas = compraService.listar(
            cliente.getNombreUsuario(), 0, 10, "pagadas");
        assertThat(pagadas.getContenido())
            .extracting(CompraResumenDTO::getIdCompra)
            .contains(compra.getIdCompra());

        PaginaDTO<CompraResumenDTO> pendientes = compraService.listar(
            cliente.getNombreUsuario(), 0, 10, "pendientes");
        assertThat(pendientes.getContenido())
            .extracting(CompraResumenDTO::getIdCompra)
            .doesNotContain(compra.getIdCompra());

        var contadores = compraService.contadores(cliente.getNombreUsuario());
        assertThat(contadores.getTodas()).isGreaterThanOrEqualTo(1);
        assertThat(contadores.getPagadas()).isGreaterThanOrEqualTo(1);

        CompraDTO repetida = compraService.crearDesdeCarrito(
            cliente.getNombreUsuario(), checkout);
        assertThat(repetida.getIdCompra()).isEqualTo(compra.getIdCompra());
    }

    @Test
    void checkoutPorArtista_compraSoloEseGrupoYDejaElRestoEnElCarrito() {
        Usuario artistaA = crearUsuario(ERol.ARTISTA);
        Usuario artistaB = crearUsuario(ERol.ARTISTA);
        Usuario cliente = crearUsuario(ERol.CLIENTE);
        ProductoDTO productoA = crearProductoDigitalUnico(artistaA);
        ProductoDTO productoB = crearProductoDigitalUnico(artistaB);

        CarritoItemRequest itemA = new CarritoItemRequest();
        itemA.setIdVariante(productoA.getVariantes().get(0).getIdVariante());
        itemA.setCantidad(1);
        carritoService.agregar(cliente.getNombreUsuario(), itemA);

        CarritoItemRequest itemB = new CarritoItemRequest();
        itemB.setIdVariante(productoB.getVariantes().get(0).getIdVariante());
        itemB.setCantidad(1);
        carritoService.agregar(cliente.getNombreUsuario(), itemB);

        CarritoDTO carrito = carritoService.obtener(cliente.getNombreUsuario());
        assertThat(carrito.getGrupos()).hasSize(2);

        CheckoutRequest checkout = new CheckoutRequest();
        checkout.setClaveIdempotencia("compra-artista-" + UUID.randomUUID());
        checkout.setIdArtista(artistaA.getIdUsuario());
        CompraDTO compra = compraService.crearDesdeCarrito(
            cliente.getNombreUsuario(), checkout);

        assertThat(compra.getGrupos()).hasSize(1);
        assertThat(compra.getGrupos().get(0).getIdArtista())
            .isEqualTo(artistaA.getIdUsuario());

        CarritoDTO restante = carritoService.obtener(cliente.getNombreUsuario());
        assertThat(restante.getGrupos()).hasSize(1);
        assertThat(restante.getGrupos().get(0).getIdArtista())
            .isEqualTo(artistaB.getIdUsuario());
    }

    @Test
    void checkout_artistaSinItems_lanzaConflicto() {
        Usuario artista = crearUsuario(ERol.ARTISTA);
        Usuario cliente = crearUsuario(ERol.CLIENTE);
        ProductoDTO producto = crearProductoDigitalUnico(artista);
        agregar(cliente, producto);

        CheckoutRequest checkout = new CheckoutRequest();
        checkout.setClaveIdempotencia("sin-items-" + UUID.randomUUID());
        checkout.setIdArtista(999L);

        assertThatThrownBy(() -> compraService.crearDesdeCarrito(
            cliente.getNombreUsuario(), checkout))
            .isInstanceOf(ConflictoException.class)
            .hasMessageContaining("ese artista");
    }

    @Test
    void checkout_productoPausado_lanzaConflicto() {
        Usuario artista = crearUsuario(ERol.ARTISTA);
        Usuario cliente = crearUsuario(ERol.CLIENTE);
        ProductoDTO producto = crearProductoDigitalUnico(artista);
        agregar(cliente, producto);
        Producto entidad = productoRepository
            .findByIdProducto(producto.getIdProducto()).orElseThrow();
        entidad.setEstado(EstadoProducto.PAUSADO);
        productoRepository.save(entidad);

        CheckoutRequest checkout = new CheckoutRequest();
        checkout.setClaveIdempotencia("pausado-" + UUID.randomUUID());

        assertThatThrownBy(() -> compraService.crearDesdeCarrito(
            cliente.getNombreUsuario(), checkout))
            .isInstanceOf(ConflictoException.class)
            .hasMessageContaining("no está disponible");
    }

    @Test
    void checkout_varianteAgotada_lanzaConflicto() {
        Usuario artista = crearUsuario(ERol.ARTISTA);
        Usuario cliente = crearUsuario(ERol.CLIENTE);
        ProductoDTO producto = crearProductoDigitalUnico(artista);
        agregar(cliente, producto);
        var variante = varianteRepository
            .findByIdVariante(producto.getVariantes().get(0).getIdVariante())
            .orElseThrow();
        variante.setEstado(EstadoVariante.AGOTADO);
        variante.setStockDisponible(0);
        varianteRepository.save(variante);

        CheckoutRequest checkout = new CheckoutRequest();
        checkout.setClaveIdempotencia("agotada-" + UUID.randomUUID());

        assertThatThrownBy(() -> compraService.crearDesdeCarrito(
            cliente.getNombreUsuario(), checkout))
            .isInstanceOf(ConflictoException.class)
            .hasMessageContaining("no está disponible");
    }

    @Test
    void checkout_sinPrecio_lanzaConflicto() {
        Usuario artista = crearUsuario(ERol.ARTISTA);
        Usuario cliente = crearUsuario(ERol.CLIENTE);
        ProductoDTO producto = crearProductoDigitalUnico(artista);
        agregar(cliente, producto);
        var variante = varianteRepository
            .findByIdVariante(producto.getVariantes().get(0).getIdVariante())
            .orElseThrow();
        variante.setPrecio(null);
        varianteRepository.save(variante);

        CheckoutRequest checkout = new CheckoutRequest();
        checkout.setClaveIdempotencia("sin-precio-" + UUID.randomUUID());

        assertThatThrownBy(() -> compraService.crearDesdeCarrito(
            cliente.getNombreUsuario(), checkout))
            .isInstanceOf(ConflictoException.class)
            .hasMessageContaining("precio");
    }

    @Test
    void compraFisica_pagoSimulado_marcaEntregadoSinDescarga() {
        Usuario artista = crearUsuario(ERol.ARTISTA);
        Usuario cliente = crearUsuario(ERol.CLIENTE);
        ProductoDTO producto = crearProducto(artista, TipoEntrega.FISICA);
        agregar(cliente, producto);

        CheckoutRequest checkout = new CheckoutRequest();
        checkout.setClaveIdempotencia("fisica-" + UUID.randomUUID());
        CompraDTO compra = compraService.crearDesdeCarrito(
            cliente.getNombreUsuario(), checkout);
        CompraGrupoDTO grupo = compra.getGrupos().get(0);
        PagoCompraDTO pago = compraPagoService.iniciarPago(
            cliente.getNombreUsuario(), compra.getIdCompra(), grupo.getIdCompraGrupo());
        CompraDTO pagada = compraPagoService.confirmarSimulado(
            cliente.getNombreUsuario(), pago.getIdIntentoCobro());

        CompraItemDTO item = pagada.getGrupos().get(0).getItems().get(0);
        assertThat(item.getTipoEntrega()).isEqualTo(TipoEntrega.FISICA);
        assertThat(item.getEstadoEntrega()).isEqualTo(EstadoEntregaItem.ENTREGADO);
        assertThat(item.isPuedeDescargar()).isFalse();
        assertThat(item.getMotivoNoDescarga()).contains("coordina");
    }

    private void agregar(Usuario cliente, ProductoDTO producto) {
        CarritoItemRequest item = new CarritoItemRequest();
        item.setIdVariante(producto.getVariantes().get(0).getIdVariante());
        item.setCantidad(1);
        carritoService.agregar(cliente.getNombreUsuario(), item);
    }

    private ProductoDTO crearProductoDigitalUnico(Usuario artista) {
        return crearProducto(artista, TipoEntrega.DIGITAL);
    }

    private ProductoDTO crearProducto(Usuario artista, TipoEntrega tipoEntrega) {
        TipoProducto tipo = tipoProductoRepository
            .findByActivoTrueOrderByNombreAsc().get(0);
        Medio medio = medioRepository.findByActivoTrueOrderByNombreAsc().stream()
            .filter(m -> m.getTipoEntrega() == tipoEntrega)
            .findFirst()
            .orElseThrow();
        Moneda moneda = monedaService.predeterminada();
        DerechoTermino personal = derechoTerminoRepository
            .findByActivoTrueOrderByOrdenAsc().stream()
            .filter(derecho -> "PERSONAL".equals(derecho.getCodigo()))
            .findFirst()
            .orElseThrow();

        String sha = UUID.randomUUID().toString().replace("-", "")
            + UUID.randomUUID().toString().replace("-", "");
        ProductoImagenRequest imagen = new ProductoImagenRequest();
        imagen.setUrl("/uploads/compra-" + UUID.randomUUID() + ".webp");
        imagen.setSha256(sha);
        imagen.setClaveArchivoOriginal("productos/compra-" + UUID.randomUUID() + ".jpg");
        imagen.setOrden(0);

        ProductoVarianteRequest variante = new ProductoVarianteRequest();
        variante.setNombre("Única");
        variante.setPrecio(new BigDecimal("150.00"));
        variante.setStockDisponible(1);
        variante.setOrden(0);
        variante.setUrlImagen(imagen.getUrl());
        variante.setSha256(sha);
        variante.setClaveArchivoOriginal(imagen.getClaveArchivoOriginal());

        LicenciaProductoRequest licencia = new LicenciaProductoRequest();
        licencia.setIdsDerechos(Set.of(personal.getIdDerechoTermino()));
        licencia.setPorcentajeRegalia(BigDecimal.ZERO);

        ProductoRequest request = new ProductoRequest();
        request.setIdTipoProducto(tipo.getIdTipoProducto());
        request.setIdMedio(medio.getIdMedio());
        request.setIdMoneda(moneda.getIdMoneda());
        request.setTitulo("Compra " + UUID.randomUUID());
        request.setDescripcion("Producto digital para probar la compra.");
        request.setImagenes(new ArrayList<>(List.of(imagen)));
        request.setVariantes(new ArrayList<>(List.of(variante)));
        request.setIdsEtiquetas(Set.of());
        request.setContenidosSensibles(Set.of());
        request.setLicencia(licencia);
        return productoService.crear(artista.getIdUsuario(), request);
    }

    private Usuario crearUsuario(ERol rolNombre) {
        Rol rol = rolRepository.findByNombre(rolNombre)
            .orElseThrow(() -> new IllegalStateException(
                "No existe el rol " + rolNombre));
        String nombre = rolNombre.name().toLowerCase()
            + UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        Usuario usuario = new Usuario();
        usuario.setNombreUsuario(nombre);
        usuario.setNombreDisplay(nombre);
        usuario.setContrasenia("test");
        usuario.setCorreo(nombre + "@test.local");
        usuario.setFechaCreacion(LocalDateTime.now());
        usuario.setMonedaPreferida(monedaService.predeterminada());
        usuario.setRol(rol);
        return usuarioRepository.save(usuario);
    }
}
