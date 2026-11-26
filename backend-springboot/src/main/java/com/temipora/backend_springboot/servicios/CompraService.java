package com.temipora.backend_springboot.servicios;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

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
import com.temipora.backend_springboot.modelo.AnclajeLedger;
import com.temipora.backend_springboot.modelo.CompraItem;
import com.temipora.backend_springboot.modelo.CompraItemDTO;
import com.temipora.backend_springboot.modelo.CompraLedgerDTO;
import com.temipora.backend_springboot.modelo.CompraPreviewDTO;
import com.temipora.backend_springboot.modelo.CompraResumenDTO;
import com.temipora.backend_springboot.modelo.ConfiguracionComision;
import com.temipora.backend_springboot.modelo.ContadoresComprasDTO;
import com.temipora.backend_springboot.modelo.CuentaPagoUsuario;
import com.temipora.backend_springboot.modelo.DerechoTermino;
import com.temipora.backend_springboot.modelo.EstadoCompra;
import com.temipora.backend_springboot.modelo.EstadoCompraGrupo;
import com.temipora.backend_springboot.modelo.EstadoCuentaPago;
import com.temipora.backend_springboot.modelo.EstadoEntregaItem;
import com.temipora.backend_springboot.modelo.EstadoIntentoCobro;
import com.temipora.backend_springboot.modelo.EstadoProducto;
import com.temipora.backend_springboot.modelo.EstadoPublicacionReventa;
import com.temipora.backend_springboot.modelo.EstadoReserva;
import com.temipora.backend_springboot.modelo.EventoLedger;
import com.temipora.backend_springboot.modelo.EventoLedgerCompraDTO;
import com.temipora.backend_springboot.modelo.EventoLedgerDTO;
import com.temipora.backend_springboot.modelo.LicenciaProducto;
import com.temipora.backend_springboot.modelo.LicenciaCompraDTO;
import com.temipora.backend_springboot.modelo.LicenciaProductoDerecho;
import com.temipora.backend_springboot.modelo.Moneda;
import com.temipora.backend_springboot.modelo.OrigenCompraItem;
import com.temipora.backend_springboot.modelo.PaginaDTO;
import com.temipora.backend_springboot.modelo.Producto;
import com.temipora.backend_springboot.modelo.ProductoImagen;
import com.temipora.backend_springboot.modelo.ProductoVariante;
import com.temipora.backend_springboot.modelo.PublicacionReventa;
import com.temipora.backend_springboot.modelo.ReservaStock;
import com.temipora.backend_springboot.modelo.TipoEntrega;
import com.temipora.backend_springboot.modelo.Usuario;
import com.temipora.backend_springboot.modelo.VarianteLedgerDTO;
import com.temipora.backend_springboot.repositorios.CarritoRepository;
import com.temipora.backend_springboot.repositorios.CompraRepository;
import com.temipora.backend_springboot.repositorios.CuentaPagoUsuarioRepository;
import com.temipora.backend_springboot.repositorios.EventoLedgerRepository;
import com.temipora.backend_springboot.repositorios.IntentoCobroGrupoRepository;
import com.temipora.backend_springboot.repositorios.ProductoVarianteImagenRepository;
import com.temipora.backend_springboot.repositorios.ProductoVarianteRepository;
import com.temipora.backend_springboot.repositorios.PublicacionReventaRepository;
import com.temipora.backend_springboot.repositorios.ReservaStockRepository;
import com.temipora.backend_springboot.servicios.pagos.mercadopago.MercadoPagoOAuthService;

import tools.jackson.databind.ObjectMapper;

@Service
public class CompraService {

    private static final String ARS = "ARS";
    private static final String PROVEEDOR_MP = MercadoPagoOAuthService.PROVEEDOR;
    private static final int TAMANIO_MAXIMO = 24;
    private static final Set<EstadoCompra> ESTADOS_PENDIENTES = Set.of(
        EstadoCompra.PENDIENTE_PAGO, EstadoCompra.PARCIALMENTE_PAGA);
    private static final Set<EstadoCompra> ESTADOS_PAGADAS = Set.of(
        EstadoCompra.PAGA);
    private static final Set<EstadoCompra> ESTADOS_ARCHIVO = Set.of(
        EstadoCompra.VENCIDA, EstadoCompra.CANCELADA);
    private static final ObjectMapper JSON = new ObjectMapper();

    private final CompraRepository compraRepository;
    private final CarritoRepository carritoRepository;
    private final ProductoVarianteRepository varianteRepository;
    private final ReservaStockRepository reservaStockRepository;
    private final IntentoCobroGrupoRepository intentoCobroGrupoRepository;
    private final PublicacionReventaRepository publicacionReventaRepository;
    private final CuentaPagoUsuarioRepository cuentaPagoRepository;
    private final UsuarioService usuarioService;
    private final MonedaService monedaService;
    private final ConversionMonedaService conversionMonedaService;
    private final ComisionService comisionService;
    private final ProductoVarianteImagenRepository varianteImagenRepository;
    private final EventoLedgerRepository eventoLedgerRepository;
    private final LedgerService ledgerService;
    private final AnclajeService anclajeService;
    private final SancionService sancionService;
    private final String proveedorPago;
    private final int reservaMinutos;

    public CompraService(
            CompraRepository compraRepository,
            CarritoRepository carritoRepository,
            ProductoVarianteRepository varianteRepository,
            ReservaStockRepository reservaStockRepository,
            IntentoCobroGrupoRepository intentoCobroGrupoRepository,
            PublicacionReventaRepository publicacionReventaRepository,
            CuentaPagoUsuarioRepository cuentaPagoRepository,
            UsuarioService usuarioService,
            MonedaService monedaService,
            ConversionMonedaService conversionMonedaService,
            ComisionService comisionService,
            ProductoVarianteImagenRepository varianteImagenRepository,
            EventoLedgerRepository eventoLedgerRepository,
            LedgerService ledgerService,
            AnclajeService anclajeService,
            SancionService sancionService,
            @Value("${pagos.proveedor:simulado}") String proveedorPago,
            @Value("${compra.reserva-minutos:30}") int reservaMinutos) {
        this.compraRepository = compraRepository;
        this.carritoRepository = carritoRepository;
        this.varianteRepository = varianteRepository;
        this.reservaStockRepository = reservaStockRepository;
        this.intentoCobroGrupoRepository = intentoCobroGrupoRepository;
        this.publicacionReventaRepository = publicacionReventaRepository;
        this.cuentaPagoRepository = cuentaPagoRepository;
        this.usuarioService = usuarioService;
        this.monedaService = monedaService;
        this.conversionMonedaService = conversionMonedaService;
        this.comisionService = comisionService;
        this.varianteImagenRepository = varianteImagenRepository;
        this.eventoLedgerRepository = eventoLedgerRepository;
        this.ledgerService = ledgerService;
        this.anclajeService = anclajeService;
        this.sancionService = sancionService;
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
        if (sancionService.tieneSancionDeCompra(cliente.getIdUsuario())) {
            throw new ConflictoException(
                MoraClienteService.MENSAJE_COMPRA_BLOQUEADA);
        }
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
        ConfiguracionComision comisionConfig = comisionService.obtener();
        LocalDateTime ahora = LocalDateTime.now();
        Map<Long, List<CarritoItem>> porVendedor =
            agruparPorVendedor(itemsSeleccionados);

        Compra compra = new Compra();
        compra.setCliente(cliente);
        compra.setMoneda(ars);
        compra.setClaveIdempotencia(clave);
        compra.setEstado(EstadoCompra.PENDIENTE_PAGO);
        compra.setFechaCreacion(ahora);
        compra.setFechaActualizacion(ahora);

        BigDecimal subtotalCompra = cero();
        BigDecimal comisionCompra = cero();

        for (Map.Entry<Long, List<CarritoItem>> entry : porVendedor.entrySet()) {
            List<CarritoItem> items = entry.getValue();
            Usuario vendedor = vendedorDe(items.get(0));
            if (vendedor == null) {
                throw new IllegalStateException("El producto no tiene vendedor.");
            }

            CuentaPagoUsuario cuenta = validarCuentaPago(vendedor);
            CompraGrupo grupo = new CompraGrupo();
            grupo.setArtista(vendedor);
            grupo.setMoneda(ars);
            grupo.setEstado(EstadoCompraGrupo.PENDIENTE);
            grupo.setCuentaPago(cuenta);
            grupo.setCuentaExternaId(cuenta != null ? cuenta.getIdCuentaExterna() : null);
            grupo.setFechaCreacion(ahora);

            BigDecimal subtotalGrupo = cero();
            BigDecimal comisionGrupo = cero();

            for (CarritoItem item : items) {
                CompraItem comprado = crearItem(
                    item, grupo, ars, comisionConfig, ahora);
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
        return listar(nombreUsuario, pagina, tamanio, null);
    }

    @Transactional(readOnly = true)
    public PaginaDTO<CompraResumenDTO> listar(
            String nombreUsuario, int pagina, int tamanio, String grupo) {
        Usuario cliente = usuarioService.getByNombreUsuario(nombreUsuario);
        int paginaSegura = Math.max(0, pagina);
        int tamanioSeguro = Math.min(Math.max(1, tamanio), TAMANIO_MAXIMO);
        PageRequest pageable = PageRequest.of(paginaSegura, tamanioSeguro,
            Sort.by(Sort.Direction.DESC, "fechaCreacion"));

        Set<EstadoCompra> estados = estadosDeGrupo(grupo);
        Page<Compra> page = estados == null
            ? compraRepository.findByClienteIdUsuarioOrderByFechaCreacionDesc(
                cliente.getIdUsuario(), pageable)
            : compraRepository
                .findByClienteIdUsuarioAndEstadoInOrderByFechaCreacionDesc(
                    cliente.getIdUsuario(), estados, pageable);

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
    public ContadoresComprasDTO contadores(String nombreUsuario) {
        Usuario cliente = usuarioService.getByNombreUsuario(nombreUsuario);
        long todas = 0;
        long pendientes = 0;
        long pagadas = 0;
        long archivo = 0;
        for (Object[] fila : compraRepository.contarPorEstado(
                cliente.getIdUsuario())) {
            if (fila == null || fila.length < 2 || fila[0] == null) {
                continue;
            }
            EstadoCompra estado = (EstadoCompra) fila[0];
            long cantidad = ((Number) fila[1]).longValue();
            todas += cantidad;
            if (ESTADOS_PENDIENTES.contains(estado)) {
                pendientes += cantidad;
            } else if (ESTADOS_PAGADAS.contains(estado)) {
                pagadas += cantidad;
            } else if (ESTADOS_ARCHIVO.contains(estado)) {
                archivo += cantidad;
            }
        }
        return ContadoresComprasDTO.builder()
            .todas(todas)
            .pendientes(pendientes)
            .pagadas(pagadas)
            .archivo(archivo)
            .build();
    }

    @Transactional(readOnly = true)
    public CompraLedgerDTO ledgerItem(
            String nombreUsuario, Long idCompra, Long idItem) {
        Usuario cliente = usuarioService.getByNombreUsuario(nombreUsuario);
        Compra compra = compraRepository
            .findByIdCompraAndClienteIdUsuario(idCompra, cliente.getIdUsuario())
            .orElseThrow(() -> new NoSuchElementException(
                "Compra no encontrada: " + idCompra));
        CompraItem item = compra.getGrupos().stream()
            .flatMap(grupo -> grupo.getItems().stream())
            .filter(candidato -> Objects.equals(
                candidato.getIdCompraItem(), idItem))
            .findFirst()
            .orElseThrow(() -> new NoSuchElementException(
                "Ítem no encontrado: " + idItem));

        if (item.getVariante() == null
                || item.getVariante().getIdVariante() == null) {
            throw new NoSuchElementException(
                "Este ítem no tiene historial de procedencia.");
        }

        VarianteLedgerDTO verificacion =
            ledgerService.verificarVariante(item.getVariante());
        Map<Long, Boolean> verificados = verificacion.getEventos().stream()
            .collect(Collectors.toMap(
                EventoLedgerDTO::getIdEventoLedger,
                EventoLedgerDTO::isVerificado,
                (anterior, actual) -> anterior));

        List<EventoLedger> eventos = eventoLedgerRepository
            .findByVarianteIdVarianteOrderBySecuenciaAsc(
                item.getVariante().getIdVariante());
        List<EventoLedgerCompraDTO> dtos = eventos.stream()
            .map(evento -> {
                boolean esEsta = Objects.equals(
                    evento.getIdCompraItem(), item.getIdCompraItem());
                Usuario origen = evento.getActorOrigen();
                Usuario destino = evento.getActorDestino();
                return EventoLedgerCompraDTO.builder()
                    .idEventoLedger(evento.getIdEventoLedger())
                    .secuencia(evento.getSecuencia())
                    .tipo(evento.getTipo())
                    .ocurridoEn(evento.getOcurridoEn())
                    .idCompraItem(evento.getIdCompraItem())
                    .idActorOrigen(origen != null
                        ? origen.getIdUsuario() : null)
                    .nombreOrigen(origen != null
                        ? origen.getNombreUsuario() : null)
                    .idActorDestino(destino != null
                        ? destino.getIdUsuario() : null)
                    .nombreDestino(destino != null
                        ? destino.getNombreUsuario() : null)
                    .monto(esEsta ? evento.getMonto() : null)
                    .moneda(esEsta && evento.getMoneda() != null
                        ? evento.getMoneda().getCodigo() : null)
                    .hashPrevio(evento.getHashPrevio())
                    .hashEvento(evento.getHashEvento())
                    .hashDatos(evento.getHashDatos())
                    .hashImagen(evento.getHashImagen())
                    .versionHash(evento.getVersionHash())
                    .verificado(verificados.getOrDefault(
                        evento.getIdEventoLedger(), false))
                    .esEventoDeEstaCompra(esEsta)
                    .build();
            })
            .toList();

        List<Long> idsEventos = eventos.stream()
            .map(EventoLedger::getIdEventoLedger)
            .toList();
        AnclajeLedger anclaje = anclajeService.mejorAnclajeDeEventos(idsEventos)
            .orElse(null);

        return CompraLedgerDTO.builder()
            .idCompra(idCompra)
            .idCompraItem(item.getIdCompraItem())
            .idProducto(item.getProducto() != null
                ? item.getProducto().getIdProducto() : null)
            .idVariante(item.getVariante().getIdVariante())
            .nombreVariante(item.getNombreVariante())
            .tieneLedger(verificacion.isTieneLedger())
            .integridad(verificacion.isIntegridad())
            .mensaje(verificacion.getMensaje())
            .codigoCertificado(verificacion.isTieneLedger()
                ? CodigoCertificado.codificar(item.getVariante().getIdVariante())
                : null)
            .anclado(anclaje != null)
            .bitcoinAltura(anclaje != null ? anclaje.getBitcoinAltura() : null)
            .fechaAnclaje(anclaje != null
                ? (anclaje.getFechaConfirmacion() != null
                    ? anclaje.getFechaConfirmacion()
                    : anclaje.getFechaSolicitud())
                : null)
            .eventos(dtos)
            .build();
    }

    private Set<EstadoCompra> estadosDeGrupo(String grupo) {
        if (grupo == null || grupo.isBlank()
                || "todas".equalsIgnoreCase(grupo)) {
            return null;
        }
        return switch (grupo.trim().toLowerCase()) {
            case "pendientes" -> ESTADOS_PENDIENTES;
            case "pagadas" -> ESTADOS_PAGADAS;
            case "archivo" -> ESTADOS_ARCHIVO;
            default -> null;
        };
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

    @Transactional
    public CompraDTO cancelarGrupo(
            String nombreUsuario, Long idCompra, Long idGrupo) {
        Usuario cliente = usuarioService.getByNombreUsuario(nombreUsuario);
        Compra compra = compraRepository
            .findByIdCompraAndClienteIdUsuario(idCompra, cliente.getIdUsuario())
            .orElseThrow(() -> new NoSuchElementException(
                "Compra no encontrada: " + idCompra));
        CompraGrupo grupo = compra.getGrupos().stream()
            .filter(item -> item.getIdCompraGrupo() != null
                && item.getIdCompraGrupo().equals(idGrupo))
            .findFirst()
            .orElseThrow(() -> new NoSuchElementException(
                "Grupo no encontrado: " + idGrupo));
        if (grupo.getEstado() != EstadoCompraGrupo.PENDIENTE) {
            throw new ConflictoException("Este grupo ya no se puede cancelar.");
        }
        boolean pagoIniciado = intentoCobroGrupoRepository
            .findFirstByGrupoIdCompraGrupoAndEstadoOrderByIntentoCobroFechaCreacionDesc(
                idGrupo, EstadoIntentoCobro.PENDIENTE)
            .isPresent();
        if (pagoIniciado) {
            throw new ConflictoException(
                "El pago ya fue iniciado. La reserva se libera automáticamente cuando vence.");
        }

        LocalDateTime ahora = LocalDateTime.now();
        grupo.setEstado(EstadoCompraGrupo.CANCELADO);
        grupo.setFechaActualizacion(ahora);
        for (CompraItem item : grupo.getItems()) {
            item.setEstadoEntrega(EstadoEntregaItem.CANCELADO);
            liberarPublicacionReventa(item, ahora);
            reservaStockRepository
                .findByCompraItemIdCompraItem(item.getIdCompraItem())
                .filter(reserva -> reserva.getEstado() == EstadoReserva.ACTIVA)
                .ifPresent(reserva -> {
                    reserva.setEstado(EstadoReserva.VENCIDA);
                    reserva.setFechaActualizacion(ahora);
                    reservaStockRepository.save(reserva);
                });
        }

        boolean hayPendientes = compra.getGrupos().stream()
            .anyMatch(item -> item.getEstado() == EstadoCompraGrupo.PENDIENTE);
        boolean hayPagados = compra.getGrupos().stream()
            .anyMatch(item -> item.getEstado() == EstadoCompraGrupo.PAGADO);
        if (hayPendientes) {
            compra.setEstado(hayPagados
                ? EstadoCompra.PARCIALMENTE_PAGA
                : EstadoCompra.PENDIENTE_PAGO);
        } else if (hayPagados && compra.getGrupos().stream()
                .allMatch(item -> item.getEstado() == EstadoCompraGrupo.PAGADO)) {
            compra.setEstado(EstadoCompra.PAGA);
        } else if (hayPagados) {
            compra.setEstado(EstadoCompra.PARCIALMENTE_PAGA);
        } else {
            compra.setEstado(EstadoCompra.CANCELADA);
        }
        compra.setFechaActualizacion(ahora);
        compraRepository.save(compra);
        return aDTO(compra);
    }

    private void liberarPublicacionReventa(
            CompraItem item, LocalDateTime ahora) {
        if (item.getIdPublicacionReventa() == null) {
            return;
        }
        publicacionReventaRepository
            .findByIdPublicacionReventaForUpdate(item.getIdPublicacionReventa())
            .filter(publicacion ->
                publicacion.getEstado() == EstadoPublicacionReventa.RESERVADA)
            .ifPresent(publicacion -> {
                publicacion.setEstado(EstadoPublicacionReventa.PUBLICADA);
                publicacion.setFechaActualizacion(ahora);
                publicacionReventaRepository.save(publicacion);
            });
    }

    private List<CarritoItem> seleccionarItems(Carrito carrito, Long idArtista) {
        List<CarritoItem> items = new ArrayList<>(carrito.getItems());
        if (idArtista == null) {
            return items;
        }
        return items.stream()
            .filter(item -> {
                Usuario vendedor = vendedorDe(item);
                return vendedor != null
                    && idArtista.equals(vendedor.getIdUsuario());
            })
            .toList();
    }

    private Map<Long, List<CarritoItem>> agruparPorVendedor(
            List<CarritoItem> items) {
        Map<Long, List<CarritoItem>> grupos = new LinkedHashMap<>();
        for (CarritoItem item : items) {
            Usuario vendedor = vendedorDe(item);
            if (vendedor == null) {
                throw new IllegalStateException("El producto no tiene vendedor.");
            }
            grupos.computeIfAbsent(vendedor.getIdUsuario(),
                clave -> new ArrayList<>()).add(item);
        }
        return grupos;
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

    private CuentaPagoUsuario validarCuentaPago(Usuario vendedor) {
        if (!"mercadopago".equalsIgnoreCase(proveedorPago)) {
            return null;
        }
        CuentaPagoUsuario cuenta = cuentaPagoRepository
            .findByUsuarioIdUsuarioAndProveedor(vendedor.getIdUsuario(), PROVEEDOR_MP)
            .orElseThrow(() -> new ConflictoException(
                "El vendedor @" + vendedor.getNombreUsuario()
                    + " todavía no conectó su cuenta de MercadoPago."));
        if (cuenta.getEstado() != EstadoCuentaPago.ACTIVA) {
            throw new ConflictoException(
                "La cuenta de MercadoPago del vendedor @"
                    + vendedor.getNombreUsuario() + " no está activa.");
        }
        if (cuenta.getAccessToken() == null || cuenta.getAccessToken().isBlank()) {
            throw new ConflictoException(
                "La cuenta de MercadoPago del vendedor @"
                    + vendedor.getNombreUsuario() + " no tiene un token válido.");
        }
        return cuenta;
    }

    private CompraItem crearItem(
            CarritoItem item,
            CompraGrupo grupo,
            Moneda ars,
            ConfiguracionComision comisionConfig,
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
        PublicacionReventa publicacion = item.getIdPublicacionReventa() != null
            ? publicacionReventaRepository
                .findByIdPublicacionReventaForUpdate(item.getIdPublicacionReventa())
                .orElseThrow(() -> new ConflictoException(
                    "La publicación de reventa ya no existe."))
            : null;
        boolean reventa = publicacion != null;

        if (!reventa) {
            if (producto.getEstado() != EstadoProducto.DISPONIBLE) {
                throw new ConflictoException(
                    "El producto “" + producto.getTitulo()
                        + "” ya no está disponible.");
            }
            if (!variante.estaDisponible()) {
                throw new ConflictoException(
                    "La variante “" + variante.getNombre()
                        + "” ya no está disponible.");
            }
            validarStock(variante, item.getCantidad());
        } else {
            if (publicacion.getEstado() != EstadoPublicacionReventa.PUBLICADA) {
                throw new ConflictoException(
                    "La publicación de reventa ya no está disponible.");
            }
            if (publicacion.getVendedor() == null
                    || grupo.getArtista() == null
                    || !publicacion.getVendedor().getIdUsuario()
                        .equals(grupo.getArtista().getIdUsuario())) {
                throw new ConflictoException(
                    "La publicación de reventa no pertenece a ese vendedor.");
            }
            if (publicacion.getVariante() == null
                    || !publicacion.getVariante().getIdVariante()
                        .equals(variante.getIdVariante())) {
                throw new ConflictoException(
                    "La publicación de reventa no corresponde a esa variante.");
            }
        }

        CompraItem origenReventa = reventa
            ? publicacion.getCompraItemOrigen() : null;
        Moneda monedaOriginal = reventa
            ? publicacion.getMoneda() : producto.getMoneda();
        if (monedaOriginal == null) {
            throw new ConflictoException(
                "La pieza no tiene una moneda configurada.");
        }
        BigDecimal factor = conversionMonedaService.factor(
            monedaOriginal, ars, ahora);
        if (factor == null) {
            throw new ConflictoException(
                "No hay cotización de " + monedaOriginal.getCodigo()
                    + " a " + ARS + ".");
        }
        BigDecimal precioBase = reventa
            ? publicacion.getPrecio() : variante.getPrecio();
        if (precioBase == null) {
            throw new ConflictoException(
                "La pieza no tiene precio.");
        }
        BigDecimal porcentaje = reventa
            ? comisionConfig.getPorcentajeReventa()
            : comisionConfig.getPorcentajeProducto();
        BigDecimal precioCobro = precioBase.multiply(factor)
            .setScale(2, RoundingMode.HALF_UP);
        BigDecimal comision = comisionService.calcularComision(
            precioCobro.multiply(BigDecimal.valueOf(item.getCantidad())), porcentaje);
        TipoEntrega tipoEntrega = reventa
            ? (origenReventa != null
                ? origenReventa.getTipoEntrega()
                : (producto.getMedio() != null
                    ? producto.getMedio().getTipoEntrega() : null))
            : (producto.getMedio() != null
                ? producto.getMedio().getTipoEntrega() : null);
        if (tipoEntrega == null) {
            throw new ConflictoException(
                "La pieza no tiene un medio de entrega configurado.");
        }
        String titulo = reventa && origenReventa != null
            ? origenReventa.getTituloProducto() : producto.getTitulo();
        String nombreVariante = reventa && origenReventa != null
            ? origenReventa.getNombreVariante() : variante.getNombre();
        String licencia = reventa && origenReventa != null
            ? origenReventa.getLicenciaSnapshot()
            : licenciaJson(producto.licenciaVigente());

        CompraItem comprado = new CompraItem();
        comprado.setProducto(producto);
        comprado.setVariante(variante);
        comprado.setTituloProducto(titulo);
        comprado.setNombreVariante(nombreVariante);
        comprado.setTipoEntrega(tipoEntrega);
        comprado.setCantidad(item.getCantidad());
        comprado.setPrecioOriginal(precioBase);
        comprado.setMonedaOriginal(monedaOriginal);
        comprado.setCotizacion(factor);
        comprado.setPrecioCobro(precioCobro);
        comprado.setComisionPorcentaje(porcentaje);
        comprado.setComisionMonto(comision);
        comprado.setSha256Archivo(variante.getSha256());
        comprado.setLicenciaSnapshot(licencia);
        comprado.setEstadoEntrega(EstadoEntregaItem.PENDIENTE);
        comprado.setGrupo(grupo);
        comprado.setOrigen(reventa
            ? OrigenCompraItem.REVENTA : OrigenCompraItem.PRIMERA_VENTA);
        comprado.setIdPublicacionReventa(reventa
            ? publicacion.getIdPublicacionReventa() : null);
        if (reventa) {
            publicacion.setEstado(EstadoPublicacionReventa.RESERVADA);
            publicacion.setFechaActualizacion(ahora);
            publicacionReventaRepository.save(publicacion);
        }
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
        List<CompraItem> items = compra.getGrupos().stream()
            .flatMap(grupo -> grupo.getItems().stream())
            .toList();
        int cantidadItems = items.stream()
            .mapToInt(CompraItem::getCantidad)
            .sum();
        boolean puedePagar = compra.getEstado() != EstadoCompra.VENCIDA
            && compra.getEstado() != EstadoCompra.CANCELADA
            && compra.getEstado() != EstadoCompra.PAGA
            && compra.getGrupos().stream()
                .anyMatch(grupo -> grupo.getEstado() == EstadoCompraGrupo.PENDIENTE);

        Set<Long> productos = new HashSet<>();
        Map<Long, CompraPreviewDTO> previews = new LinkedHashMap<>();
        boolean puedeDescargar = false;
        for (CompraItem item : items) {
            if (item.getProducto() != null) {
                productos.add(item.getProducto().getIdProducto());
            }
            boolean descargable = puedeDescargar(item);
            puedeDescargar = puedeDescargar || descargable;
            Long clave = item.getVariante() != null
                ? item.getVariante().getIdVariante()
                : item.getIdCompraItem();
            if (previews.size() < 3 && clave != null
                    && !previews.containsKey(clave)) {
                previews.put(clave, CompraPreviewDTO.builder()
                    .idCompraItem(item.getIdCompraItem())
                    .idProducto(item.getProducto() != null
                        ? item.getProducto().getIdProducto() : null)
                    .idVariante(item.getVariante() != null
                        ? item.getVariante().getIdVariante() : null)
                    .tituloProducto(item.getTituloProducto())
                    .nombreVariante(item.getNombreVariante())
                    .imagenUrl(imagenItem(item))
                    .cantidad(item.getCantidad())
                    .puedeDescargar(descargable)
                    .motivoNoDescarga(motivoNoDescarga(item))
                    .build());
            }
        }

        return CompraResumenDTO.builder()
            .idCompra(compra.getIdCompra())
            .estado(compra.getEstado())
            .total(compra.getTotal())
            .moneda(compra.getMoneda() != null
                ? compra.getMoneda().getCodigo() : ARS)
            .fechaCreacion(compra.getFechaCreacion())
            .cantidadItems(cantidadItems)
            .cantidadGrupos(compra.getGrupos().size())
            .cantidadProductos(productos.size())
            .puedePagar(puedePagar)
            .puedeDescargar(puedeDescargar)
            .itemsPreview(new ArrayList<>(previews.values()))
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
            .total(compra.getTotal())
            .moneda(compra.getMoneda() != null
                ? compra.getMoneda().getCodigo() : ARS)
            .fechaCreacion(compra.getFechaCreacion())
            .fechaActualizacion(compra.getFechaActualizacion())
            .puedeDescargar(compra.getGrupos().stream()
                .flatMap(grupo -> grupo.getItems().stream())
                .anyMatch(this::puedeDescargar))
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

        List<ReservaStock> reservasActivas = reservaStockRepository
            .findByCompraItemGrupoIdCompraGrupoAndEstado(
                grupo.getIdCompraGrupo(), EstadoReserva.ACTIVA);
        LocalDateTime reservaExpiraEn = reservasActivas.stream()
            .map(ReservaStock::getExpiraEn)
            .filter(fecha -> fecha != null)
            .min(LocalDateTime::compareTo)
            .orElse(null);
        boolean pagoIniciado = intentoCobroGrupoRepository
            .findFirstByGrupoIdCompraGrupoAndEstadoOrderByIntentoCobroFechaCreacionDesc(
                grupo.getIdCompraGrupo(), EstadoIntentoCobro.PENDIENTE)
            .isPresent();
        boolean puedeCancelar = grupo.getEstado() == EstadoCompraGrupo.PENDIENTE
            && !pagoIniciado
            && grupo.getCompra().getEstado() != EstadoCompra.VENCIDA
            && grupo.getCompra().getEstado() != EstadoCompra.CANCELADA
            && grupo.getCompra().getEstado() != EstadoCompra.PAGA;
        String motivoNoCancelar = grupo.getEstado() == EstadoCompraGrupo.PENDIENTE
            && pagoIniciado
                ? "El pago ya fue iniciado. La reserva se libera automáticamente cuando vence."
                : null;

        return CompraGrupoDTO.builder()
            .idCompraGrupo(grupo.getIdCompraGrupo())
            .idArtista(artista != null ? artista.getIdUsuario() : null)
            .nombreArtista(artista != null ? artista.getNombreUsuario() : null)
            .nombreVendedor(artista != null ? artista.getNombreUsuario() : null)
            .esReventa(items.stream()
                .anyMatch(i -> i.getOrigen() == OrigenCompraItem.REVENTA))
            .urlFotoArtista(artista != null ? fotoArtista(artista) : null)
            .subtotal(grupo.getSubtotal())
            .moneda(grupo.getMoneda() != null
                ? grupo.getMoneda().getCodigo() : ARS)
            .estado(grupo.getEstado())
            .fechaPago(grupo.getFechaPago())
            .puedePagar(puedePagar)
            .mensajePago(mensaje)
            .reservaExpiraEn(reservaExpiraEn)
            .pagoIniciado(pagoIniciado)
            .puedeCancelar(puedeCancelar)
            .motivoNoCancelar(motivoNoCancelar)
            .items(items)
            .build();
    }

    private CompraItemDTO aItemDTO(CompraItem item) {
        boolean descargable = puedeDescargar(item);
        Usuario artista = item.getProducto() != null
            ? item.getProducto().getArtista() : null;
        BigDecimal totalCobro = item.getPrecioCobro() != null
            ? item.getPrecioCobro().multiply(BigDecimal.valueOf(item.getCantidad()))
            : null;

        return CompraItemDTO.builder()
            .idCompraItem(item.getIdCompraItem())
            .idProducto(item.getProducto() != null
                ? item.getProducto().getIdProducto() : null)
            .idVariante(item.getVariante() != null
                ? item.getVariante().getIdVariante() : null)
            .tituloProducto(item.getTituloProducto())
            .nombreVariante(item.getNombreVariante())
            .tipoEntrega(item.getTipoEntrega())
            .origen(item.getOrigen())
            .idPublicacionReventa(item.getIdPublicacionReventa())
            .cantidad(item.getCantidad())
            .precioOriginal(item.getPrecioOriginal())
            .monedaOriginal(item.getMonedaOriginal() != null
                ? item.getMonedaOriginal().getCodigo() : null)
            .precioCobro(item.getPrecioCobro())
            .precioTotalCobro(totalCobro)
            .estadoEntrega(item.getEstadoEntrega())
            .puedeDescargar(descargable)
            .motivoNoDescarga(motivoNoDescarga(item))
            .idArtistaOriginal(artista != null ? artista.getIdUsuario() : null)
            .nombreArtistaOriginal(artista != null
                ? artista.getNombreUsuario() : null)
            .nombreDisplayArtistaOriginal(artista != null
                ? artista.getNombreDisplay() : null)
            .urlFotoArtistaOriginal(artista != null ? fotoArtista(artista) : null)
            .imagenUrl(imagenItem(item))
            .sha256Archivo(item.getSha256Archivo())
            .fechaEntrega(item.getFechaEntrega())
            .tieneLedger(item.getVariante() != null
                && eventoLedgerRepository.existsByVarianteIdVariante(
                    item.getVariante().getIdVariante()))
            .licencia(parseLicencia(item.getLicenciaSnapshot()))
            .build();
    }

    private boolean puedeDescargar(CompraItem item) {
        if (item.getEstadoEntrega() != EstadoEntregaItem.ENTREGADO) {
            return false;
        }
        if (item.getTipoEntrega() == TipoEntrega.DIGITAL) {
            return item.getVariante() != null
                && item.getVariante().getClaveArchivoOriginal() != null
                && !item.getVariante().getClaveArchivoOriginal().isBlank();
        }
        return imagenItem(item) != null;
    }

    private String motivoNoDescarga(CompraItem item) {
        if (item.getEstadoEntrega() != EstadoEntregaItem.ENTREGADO) {
            return "El archivo se habilita cuando se acredita el pago.";
        }
        if (item.getTipoEntrega() == TipoEntrega.DIGITAL) {
            if (item.getVariante() == null
                    || item.getVariante().getClaveArchivoOriginal() == null
                    || item.getVariante().getClaveArchivoOriginal().isBlank()) {
                return "El archivo original no está disponible.";
            }
            return null;
        }
        if (imagenItem(item) == null) {
            return "La imagen de la pieza no está disponible.";
        }
        return null;
    }

    /**
     * Imagen exacta que se compró: resuelve la versión de la variante por el
     * hash del archivo, no la portada actual del producto.
     */
    public String imagenDeItem(CompraItem item) {
        return imagenItem(item);
    }

    private String imagenItem(CompraItem item) {
        ProductoVariante variante = item.getVariante();
        if (variante != null) {
            if (item.getSha256Archivo() != null
                    && !item.getSha256Archivo().isBlank()) {
                var version = varianteImagenRepository
                    .findFirstByVarianteIdVarianteAndSha256OrderByNumeroDesc(
                        variante.getIdVariante(), item.getSha256Archivo());
                if (version.isPresent() && version.get().getUrl() != null) {
                    return version.get().getUrl();
                }
            }
            if (variante.getImagenUrl() != null
                    && !variante.getImagenUrl().isBlank()) {
                return variante.getImagenUrl();
            }
        }
        Producto producto = item.getProducto();
        if (producto != null) {
            if (producto.getImagenPortada() != null
                    && !producto.getImagenPortada().isBlank()) {
                return producto.getImagenPortada();
            }
            if (producto.getImagenes() != null) {
                return producto.getImagenes().stream()
                    .filter(imagen -> imagen != null && imagen.getUrl() != null)
                    .sorted(Comparator.comparing(ProductoImagen::getOrden,
                        Comparator.nullsLast(Integer::compareTo)))
                    .map(ProductoImagen::getUrl)
                    .findFirst()
                    .orElse(null);
            }
        }
        return null;
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
