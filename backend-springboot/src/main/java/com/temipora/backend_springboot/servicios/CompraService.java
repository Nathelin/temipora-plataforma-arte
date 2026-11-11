package com.temipora.backend_springboot.servicios;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.temipora.backend_springboot.modelo.Carrito;
import com.temipora.backend_springboot.modelo.CarritoItem;
import com.temipora.backend_springboot.modelo.CheckoutRequest;
import com.temipora.backend_springboot.modelo.Compra;
import com.temipora.backend_springboot.modelo.CompraDTO;
import com.temipora.backend_springboot.modelo.CompraGrupo;
import com.temipora.backend_springboot.modelo.CompraGrupoDTO;
import com.temipora.backend_springboot.modelo.CompraItem;
import com.temipora.backend_springboot.modelo.CompraItemDTO;
import com.temipora.backend_springboot.modelo.CompraResumenDTO;
import com.temipora.backend_springboot.modelo.CuentaPagoUsuario;
import com.temipora.backend_springboot.modelo.DerechoTermino;
import com.temipora.backend_springboot.modelo.EstadoCompra;
import com.temipora.backend_springboot.modelo.EstadoCompraGrupo;
import com.temipora.backend_springboot.modelo.EstadoCuentaPago;
import com.temipora.backend_springboot.modelo.EstadoEntregaItem;
import com.temipora.backend_springboot.modelo.EstadoProducto;
import com.temipora.backend_springboot.modelo.EstadoReserva;
import com.temipora.backend_springboot.modelo.LicenciaProducto;
import com.temipora.backend_springboot.modelo.LicenciaProductoDerecho;
import com.temipora.backend_springboot.modelo.Moneda;
import com.temipora.backend_springboot.modelo.PaginaDTO;
import com.temipora.backend_springboot.modelo.Producto;
import com.temipora.backend_springboot.modelo.ProductoVariante;
import com.temipora.backend_springboot.modelo.ReservaStock;
import com.temipora.backend_springboot.modelo.TipoEntrega;
import com.temipora.backend_springboot.modelo.Usuario;
import com.temipora.backend_springboot.repositorios.CarritoRepository;
import com.temipora.backend_springboot.repositorios.CompraRepository;
import com.temipora.backend_springboot.repositorios.CuentaPagoUsuarioRepository;
import com.temipora.backend_springboot.repositorios.ProductoVarianteRepository;
import com.temipora.backend_springboot.repositorios.ReservaStockRepository;
import com.temipora.backend_springboot.servicios.pagos.mercadopago.MercadoPagoOAuthService;

@Service
public class CompraService {

    private static final String ARS = "ARS";
    private static final String PROVEEDOR_MP = MercadoPagoOAuthService.PROVEEDOR;
    private static final int TAMANIO_MAXIMO = 24;

    private final CompraRepository compraRepository;
    private final CarritoRepository carritoRepository;
    private final ProductoVarianteRepository varianteRepository;
    private final ReservaStockRepository reservaStockRepository;
    private final CuentaPagoUsuarioRepository cuentaPagoRepository;
    private final UsuarioService usuarioService;
    private final MonedaService monedaService;
    private final ConversionMonedaService conversionMonedaService;
    private final ComisionService comisionService;
    private final String proveedorPago;
    private final int reservaMinutos;

    public CompraService(
            CompraRepository compraRepository,
            CarritoRepository carritoRepository,
            ProductoVarianteRepository varianteRepository,
            ReservaStockRepository reservaStockRepository,
            CuentaPagoUsuarioRepository cuentaPagoRepository,
            UsuarioService usuarioService,
            MonedaService monedaService,
            ConversionMonedaService conversionMonedaService,
            ComisionService comisionService,
            @Value("${pagos.proveedor:simulado}") String proveedorPago,
            @Value("${compra.reserva-minutos:30}") int reservaMinutos) {
        this.compraRepository = compraRepository;
        this.carritoRepository = carritoRepository;
        this.varianteRepository = varianteRepository;
        this.reservaStockRepository = reservaStockRepository;
        this.cuentaPagoRepository = cuentaPagoRepository;
        this.usuarioService = usuarioService;
        this.monedaService = monedaService;
        this.conversionMonedaService = conversionMonedaService;
        this.comisionService = comisionService;
        this.proveedorPago = proveedorPago;
        this.reservaMinutos = reservaMinutos;
    }

    @Transactional
    public CompraDTO crearDesdeCarrito(
            String nombreUsuario, CheckoutRequest request) {
        Usuario cliente = usuarioService.getByNombreUsuario(nombreUsuario);
        String clave = request != null && request.getClaveIdempotencia() != null
            && !request.getClaveIdempotencia().isBlank()
                ? request.getClaveIdempotencia().trim()
                : UUID.randomUUID().toString();

        Compra existente = compraRepository.findByClaveIdempotencia(clave)
            .orElse(null);
        if (existente != null) {
            if (!existente.getCliente().getIdUsuario().equals(cliente.getIdUsuario())) {
                throw new ConflictoException("La clave de idempotencia no es válida.");
            }
            return aDTO(existente);
        }

        Carrito carrito = carritoRepository
            .findByUsuarioIdUsuario(cliente.getIdUsuario())
            .orElseThrow(() -> new ConflictoException("El carrito está vacío."));
        if (carrito.getItems().isEmpty()) {
            throw new ConflictoException("El carrito está vacío.");
        }

        Long idArtista = request != null ? request.getIdArtista() : null;
        List<CarritoItem> itemsSeleccionados = seleccionarItems(carrito, idArtista);
        if (itemsSeleccionados.isEmpty()) {
            throw new ConflictoException(
                "No hay productos de ese artista en el carrito.");
        }

        Moneda ars = monedaArs();
        BigDecimal porcentaje = comisionService.obtener().getPorcentajeProducto();
        LocalDateTime ahora = LocalDateTime.now();
        Map<Long, List<CarritoItem>> porArtista =
            agruparPorArtista(itemsSeleccionados);

        Compra compra = new Compra();
        compra.setCliente(cliente);
        compra.setMoneda(ars);
        compra.setClaveIdempotencia(clave);
        compra.setEstado(EstadoCompra.PENDIENTE_PAGO);
        compra.setFechaCreacion(ahora);
        compra.setFechaActualizacion(ahora);

        BigDecimal subtotalCompra = cero();
        BigDecimal comisionCompra = cero();

        for (Map.Entry<Long, List<CarritoItem>> entry : porArtista.entrySet()) {
            List<CarritoItem> items = entry.getValue();
            Producto primerProducto = items.get(0).getVariante().getProducto();
            Usuario artista = primerProducto.getArtista();
            if (artista == null) {
                throw new IllegalStateException("El producto no tiene artista.");
            }

            CuentaPagoUsuario cuenta = validarCuentaPago(artista);
            CompraGrupo grupo = new CompraGrupo();
            grupo.setArtista(artista);
            grupo.setMoneda(ars);
            grupo.setEstado(EstadoCompraGrupo.PENDIENTE);
            grupo.setCuentaPago(cuenta);
            grupo.setCuentaExternaId(cuenta != null ? cuenta.getIdCuentaExterna() : null);
            grupo.setFechaCreacion(ahora);

            BigDecimal subtotalGrupo = cero();
            BigDecimal comisionGrupo = cero();

            for (CarritoItem item : items) {
                CompraItem comprado = crearItem(
                    item, grupo, ars, porcentaje, ahora);
                BigDecimal linea = comprado.getPrecioCobro()
                    .multiply(BigDecimal.valueOf(comprado.getCantidad()))
                    .setScale(2, RoundingMode.HALF_UP);
                subtotalGrupo = sumar(subtotalGrupo, linea);
                comisionGrupo = sumar(comisionGrupo, comprado.getComisionMonto());
                grupo.agregarItem(comprado);
            }

            grupo.setSubtotal(subtotalGrupo);
            grupo.setComision(comisionGrupo);
            grupo.setNetoArtista(sumar(subtotalGrupo, comisionGrupo.negate()));
            compra.agregarGrupo(grupo);
            subtotalCompra = sumar(subtotalCompra, subtotalGrupo);
            comisionCompra = sumar(comisionCompra, comisionGrupo);
        }

        compra.setSubtotal(subtotalCompra);
        compra.setComisionTotal(comisionCompra);
        compra.setTotal(subtotalCompra);
        Compra guardada = compraRepository.saveAndFlush(compra);
        crearReservas(guardada, ahora);

        Set<Long> idsSeleccionados = itemsSeleccionados.stream()
            .map(CarritoItem::getIdCarritoItem)
            .collect(java.util.stream.Collectors.toSet());
        carrito.getItems().removeIf(item ->
            idsSeleccionados.contains(item.getIdCarritoItem()));
        carrito.setFechaActualizacion(ahora);
        carritoRepository.save(carrito);

        return aDTO(guardada);
    }

    @Transactional(readOnly = true)
    public PaginaDTO<CompraResumenDTO> listar(
            String nombreUsuario, int pagina, int tamanio) {
        Usuario cliente = usuarioService.getByNombreUsuario(nombreUsuario);
        int paginaSegura = Math.max(0, pagina);
        int tamanioSeguro = Math.min(Math.max(1, tamanio), TAMANIO_MAXIMO);
        Page<Compra> page = compraRepository
            .findByClienteIdUsuarioOrderByFechaCreacionDesc(
                cliente.getIdUsuario(),
                PageRequest.of(paginaSegura, tamanioSeguro,
                    Sort.by(Sort.Direction.DESC, "fechaCreacion")));

        List<CompraResumenDTO> contenido = page.getContent().stream()
            .map(this::aResumen)
            .toList();
        return PaginaDTO.<CompraResumenDTO>builder()
            .contenido(contenido)
            .pagina(paginaSegura)
            .tamanio(tamanioSeguro)
            .totalElementos(page.getTotalElements())
            .totalPaginas(page.getTotalPages())
            .hayMas(page.hasNext())
            .build();
    }

    @Transactional(readOnly = true)
    public CompraDTO obtener(String nombreUsuario, Long idCompra) {
        Usuario cliente = usuarioService.getByNombreUsuario(nombreUsuario);
        Compra compra = compraRepository
            .findByIdCompraAndClienteIdUsuario(idCompra, cliente.getIdUsuario())
            .orElseThrow(() -> new NoSuchElementException(
                "Compra no encontrada: " + idCompra));
        return aDTO(compra);
    }

    private List<CarritoItem> seleccionarItems(Carrito carrito, Long idArtista) {
        List<CarritoItem> items = new ArrayList<>(carrito.getItems());
        if (idArtista == null) {
            return items;
        }
        return items.stream()
            .filter(item -> {
                Producto producto = item.getVariante().getProducto();
                return producto != null
                    && producto.getArtista() != null
                    && idArtista.equals(producto.getArtista().getIdUsuario());
            })
            .toList();
    }

    private Map<Long, List<CarritoItem>> agruparPorArtista(
            List<CarritoItem> items) {
        Map<Long, List<CarritoItem>> grupos = new LinkedHashMap<>();
        for (CarritoItem item : items) {
            Producto producto = item.getVariante().getProducto();
            Long idArtista = producto.getArtista() != null
                ? producto.getArtista().getIdUsuario() : null;
            if (idArtista == null) {
                throw new IllegalStateException("El producto no tiene artista.");
            }
            grupos.computeIfAbsent(idArtista, clave -> new ArrayList<>()).add(item);
        }
        return grupos;
    }

    private CuentaPagoUsuario validarCuentaPago(Usuario artista) {
        if (!"mercadopago".equalsIgnoreCase(proveedorPago)) {
            return null;
        }
        CuentaPagoUsuario cuenta = cuentaPagoRepository
            .findByUsuarioIdUsuarioAndProveedor(artista.getIdUsuario(), PROVEEDOR_MP)
            .orElseThrow(() -> new ConflictoException(
                "El artista @" + artista.getNombreUsuario()
                    + " todavía no conectó su cuenta de MercadoPago."));
        if (cuenta.getEstado() != EstadoCuentaPago.ACTIVA) {
            throw new ConflictoException(
                "La cuenta de MercadoPago del artista @"
                    + artista.getNombreUsuario() + " no está activa.");
        }
        if (cuenta.getAccessToken() == null || cuenta.getAccessToken().isBlank()) {
            throw new ConflictoException(
                "La cuenta de MercadoPago del artista @"
                    + artista.getNombreUsuario() + " no tiene un token válido.");
        }
        return cuenta;
    }

    private CompraItem crearItem(
            CarritoItem item,
            CompraGrupo grupo,
            Moneda ars,
            BigDecimal porcentaje,
            LocalDateTime ahora) {
        if (item.getCantidad() != 1) {
            throw new ConflictoException(
                "Por ahora cada producto se compra de a una unidad.");
        }
        ProductoVariante variante = varianteRepository
            .findByIdVarianteForUpdate(item.getVariante().getIdVariante())
            .orElseThrow(() -> new NoSuchElementException(
                "Variante no encontrada: " + item.getVariante().getIdVariante()));
        Producto producto = variante.getProducto();

        if (producto.getEstado() != EstadoProducto.DISPONIBLE) {
            throw new ConflictoException(
                "El producto “" + producto.getTitulo() + "” ya no está disponible.");
        }
        if (!variante.estaDisponible()) {
            throw new ConflictoException(
                "La variante “" + variante.getNombre() + "” ya no está disponible.");
        }
        validarStock(variante, item.getCantidad());

        Moneda monedaOriginal = producto.getMoneda();
        if (monedaOriginal == null) {
            throw new ConflictoException(
                "El producto “" + producto.getTitulo() + "” no tiene moneda.");
        }
        BigDecimal factor = conversionMonedaService.factor(
            monedaOriginal, ars, ahora);
        if (factor == null) {
            throw new ConflictoException(
                "No hay cotización de " + monedaOriginal.getCodigo()
                    + " a " + ARS + " para “" + producto.getTitulo() + "”.");
        }
        if (variante.getPrecio() == null) {
            throw new ConflictoException(
                "La variante “" + variante.getNombre() + "” no tiene precio.");
        }

        if (producto.getMedio() == null) {
            throw new ConflictoException(
                "El producto “" + producto.getTitulo()
                    + "” no tiene un medio de entrega configurado.");
        }
        BigDecimal precioCobro = variante.getPrecio().multiply(factor)
            .setScale(2, RoundingMode.HALF_UP);
        BigDecimal comision = comisionService.calcularComision(
            precioCobro.multiply(BigDecimal.valueOf(item.getCantidad())), porcentaje);
        TipoEntrega tipoEntrega = producto.getMedio().getTipoEntrega();

        CompraItem comprado = new CompraItem();
        comprado.setProducto(producto);
        comprado.setVariante(variante);
        comprado.setTituloProducto(producto.getTitulo());
        comprado.setNombreVariante(variante.getNombre());
        comprado.setTipoEntrega(tipoEntrega);
        comprado.setCantidad(item.getCantidad());
        comprado.setPrecioOriginal(variante.getPrecio());
        comprado.setMonedaOriginal(monedaOriginal);
        comprado.setCotizacion(factor);
        comprado.setPrecioCobro(precioCobro);
        comprado.setComisionPorcentaje(porcentaje);
        comprado.setComisionMonto(comision);
        comprado.setSha256Archivo(variante.getSha256());
        comprado.setLicenciaSnapshot(licenciaJson(producto.licenciaVigente()));
        comprado.setEstadoEntrega(EstadoEntregaItem.PENDIENTE);
        comprado.setGrupo(grupo);
        return comprado;
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

    private void crearReservas(Compra compra, LocalDateTime ahora) {
        LocalDateTime expira = ahora.plusMinutes(reservaMinutos);
        for (CompraGrupo grupo : compra.getGrupos()) {
            for (CompraItem item : grupo.getItems()) {
                ReservaStock reserva = new ReservaStock();
                reserva.setCompraItem(item);
                reserva.setVariante(item.getVariante());
                reserva.setCantidad(item.getCantidad());
                reserva.setExpiraEn(expira);
                reserva.setEstado(EstadoReserva.ACTIVA);
                reserva.setFechaCreacion(ahora);
                reservaStockRepository.save(reserva);
            }
        }
    }

    private CompraResumenDTO aResumen(Compra compra) {
        int cantidadItems = compra.getGrupos().stream()
            .flatMap(grupo -> grupo.getItems().stream())
            .mapToInt(CompraItem::getCantidad)
            .sum();
        boolean puedePagar = compra.getEstado() != EstadoCompra.VENCIDA
            && compra.getEstado() != EstadoCompra.CANCELADA
            && compra.getEstado() != EstadoCompra.PAGA
            && compra.getGrupos().stream()
                .anyMatch(grupo -> grupo.getEstado() == EstadoCompraGrupo.PENDIENTE);

        return CompraResumenDTO.builder()
            .idCompra(compra.getIdCompra())
            .estado(compra.getEstado())
            .total(compra.getTotal())
            .moneda(compra.getMoneda() != null
                ? compra.getMoneda().getCodigo() : ARS)
            .fechaCreacion(compra.getFechaCreacion())
            .cantidadItems(cantidadItems)
            .cantidadGrupos(compra.getGrupos().size())
            .puedePagar(puedePagar)
            .build();
    }

    private CompraDTO aDTO(Compra compra) {
        List<CompraGrupoDTO> grupos = compra.getGrupos().stream()
            .map(this::aGrupoDTO)
            .toList();
        return CompraDTO.builder()
            .idCompra(compra.getIdCompra())
            .estado(compra.getEstado())
            .subtotal(compra.getSubtotal())
            .comisionTotal(compra.getComisionTotal())
            .total(compra.getTotal())
            .moneda(compra.getMoneda() != null
                ? compra.getMoneda().getCodigo() : ARS)
            .fechaCreacion(compra.getFechaCreacion())
            .fechaActualizacion(compra.getFechaActualizacion())
            .grupos(grupos)
            .build();
    }

    private CompraGrupoDTO aGrupoDTO(CompraGrupo grupo) {
        boolean puedePagar = grupo.getEstado() == EstadoCompraGrupo.PENDIENTE
            && grupo.getCompra().getEstado() != EstadoCompra.VENCIDA
            && grupo.getCompra().getEstado() != EstadoCompra.CANCELADA
            && grupo.getCompra().getEstado() != EstadoCompra.PAGA;
        String mensaje = null;
        if (grupo.getEstado() == EstadoCompraGrupo.PAGADO) {
            mensaje = "Pago acreditado.";
        } else if (grupo.getEstado() == EstadoCompraGrupo.VENCIDO) {
            mensaje = "La reserva venció. Volvé a comprar el producto.";
        } else if (grupo.getEstado() == EstadoCompraGrupo.CANCELADO) {
            mensaje = "Este grupo fue cancelado.";
        }

        Usuario artista = grupo.getArtista();
        List<CompraItemDTO> items = grupo.getItems().stream()
            .map(this::aItemDTO)
            .toList();

        return CompraGrupoDTO.builder()
            .idCompraGrupo(grupo.getIdCompraGrupo())
            .idArtista(artista != null ? artista.getIdUsuario() : null)
            .nombreArtista(artista != null ? artista.getNombreUsuario() : null)
            .urlFotoArtista(artista != null ? fotoArtista(artista) : null)
            .subtotal(grupo.getSubtotal())
            .comision(grupo.getComision())
            .netoArtista(grupo.getNetoArtista())
            .moneda(grupo.getMoneda() != null
                ? grupo.getMoneda().getCodigo() : ARS)
            .estado(grupo.getEstado())
            .fechaPago(grupo.getFechaPago())
            .puedePagar(puedePagar)
            .mensajePago(mensaje)
            .items(items)
            .build();
    }

    private CompraItemDTO aItemDTO(CompraItem item) {
        boolean digital = item.getTipoEntrega() == TipoEntrega.DIGITAL;
        boolean entregado = item.getEstadoEntrega() == EstadoEntregaItem.ENTREGADO;
        boolean tieneArchivo = item.getVariante() != null
            && item.getVariante().getClaveArchivoOriginal() != null
            && !item.getVariante().getClaveArchivoOriginal().isBlank();
        boolean puedeDescargar = digital && entregado && tieneArchivo;
        String motivo = null;
        if (!digital) {
            motivo = "La entrega de esta pieza se coordina con el artista.";
        } else if (!entregado) {
            motivo = "El archivo se habilita cuando se acredita el pago.";
        } else if (!tieneArchivo) {
            motivo = "El archivo original no está disponible.";
        }

        return CompraItemDTO.builder()
            .idCompraItem(item.getIdCompraItem())
            .idProducto(item.getProducto() != null
                ? item.getProducto().getIdProducto() : null)
            .idVariante(item.getVariante() != null
                ? item.getVariante().getIdVariante() : null)
            .tituloProducto(item.getTituloProducto())
            .nombreVariante(item.getNombreVariante())
            .tipoEntrega(item.getTipoEntrega())
            .cantidad(item.getCantidad())
            .precioOriginal(item.getPrecioOriginal())
            .monedaOriginal(item.getMonedaOriginal() != null
                ? item.getMonedaOriginal().getCodigo() : null)
            .precioCobro(item.getPrecioCobro())
            .comisionMonto(item.getComisionMonto())
            .estadoEntrega(item.getEstadoEntrega())
            .puedeDescargar(puedeDescargar)
            .motivoNoDescarga(motivo)
            .build();
    }

    private String licenciaJson(LicenciaProducto licencia) {
        if (licencia == null) {
            return null;
        }
        String derechos = licencia.getDerechos().stream()
            .map(LicenciaProductoDerecho::getDerecho)
            .filter(derecho -> derecho != null)
            .map(this::derechoJson)
            .collect(java.util.stream.Collectors.joining(","));
        return "{\"idLicenciaProducto\":" + licencia.getIdLicenciaProducto()
            + ",\"version\":" + licencia.getVersion()
            + ",\"permiteModificaciones\":" + licencia.isPermiteModificaciones()
            + ",\"requiereCredito\":" + licencia.isRequiereCredito()
            + ",\"porcentajeRegalia\":" + numero(licencia.getPorcentajeRegalia())
            + ",\"textoAdicional\":" + json(licencia.getTextoAdicional())
            + ",\"derechos\":[" + derechos + "]}";
    }

    private String derechoJson(DerechoTermino derecho) {
        return "{\"idDerechoTermino\":" + derecho.getIdDerechoTermino()
            + ",\"codigo\":" + json(derecho.getCodigo())
            + ",\"nombre\":" + json(derecho.getNombre()) + "}";
    }

    private String numero(BigDecimal valor) {
        return valor != null ? valor.stripTrailingZeros().toPlainString() : "null";
    }

    private String json(String valor) {
        if (valor == null) {
            return "null";
        }
        StringBuilder salida = new StringBuilder("\"");
        for (char caracter : valor.toCharArray()) {
            switch (caracter) {
                case '\\' -> salida.append("\\\\");
                case '"' -> salida.append("\\\"");
                case '\n' -> salida.append("\\n");
                case '\r' -> salida.append("\\r");
                case '\t' -> salida.append("\\t");
                default -> {
                    if (caracter < 0x20) {
                        salida.append(String.format("\\u%04x", (int) caracter));
                    } else {
                        salida.append(caracter);
                    }
                }
            }
        }
        return salida.append('"').toString();
    }

    private Moneda monedaArs() {
        Moneda ars = monedaService.obtenerPorCodigo(ARS);
        if (ars == null) {
            ars = monedaService.predeterminada();
        }
        if (ars == null) {
            throw new IllegalStateException(
                "No hay una moneda configurada para cobrar en " + ARS + ".");
        }
        return ars;
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
