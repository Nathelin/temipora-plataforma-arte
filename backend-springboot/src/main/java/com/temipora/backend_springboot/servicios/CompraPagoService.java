package com.temipora.backend_springboot.servicios;

import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.temipora.backend_springboot.modelo.Compra;
import com.temipora.backend_springboot.modelo.CompraDTO;
import com.temipora.backend_springboot.modelo.CompraGrupo;
import com.temipora.backend_springboot.modelo.CompraItem;
import com.temipora.backend_springboot.modelo.EstadoCompra;
import com.temipora.backend_springboot.modelo.EstadoCompraGrupo;
import com.temipora.backend_springboot.modelo.EstadoIntentoCobro;
import com.temipora.backend_springboot.modelo.EstadoPago;
import com.temipora.backend_springboot.modelo.EstadoReserva;
import com.temipora.backend_springboot.modelo.IntentoCobro;
import com.temipora.backend_springboot.modelo.IntentoCobroGrupo;
import com.temipora.backend_springboot.modelo.PagoCompraDTO;
import com.temipora.backend_springboot.modelo.ReservaStock;
import com.temipora.backend_springboot.modelo.Usuario;
import com.temipora.backend_springboot.repositorios.CompraGrupoRepository;
import com.temipora.backend_springboot.repositorios.CompraRepository;
import com.temipora.backend_springboot.repositorios.IntentoCobroGrupoRepository;
import com.temipora.backend_springboot.repositorios.IntentoCobroRepository;
import com.temipora.backend_springboot.repositorios.ReservaStockRepository;
import com.temipora.backend_springboot.servicios.pagos.EstadoCobroProveedor;
import com.temipora.backend_springboot.servicios.pagos.ResultadoCobro;
import com.temipora.backend_springboot.servicios.pagos.producto.ProveedorPagoProducto;

@Service
public class CompraPagoService {

    private final CompraRepository compraRepository;
    private final CompraGrupoRepository compraGrupoRepository;
    private final ReservaStockRepository reservaStockRepository;
    private final IntentoCobroRepository intentoCobroRepository;
    private final IntentoCobroGrupoRepository intentoCobroGrupoRepository;
    private final CompraEntregaService compraEntregaService;
    private final CompraService compraService;
    private final UsuarioService usuarioService;
    private final ProveedorPagoProducto proveedorPago;
    private final int reservaMinutos;

    public CompraPagoService(
            CompraRepository compraRepository,
            CompraGrupoRepository compraGrupoRepository,
            ReservaStockRepository reservaStockRepository,
            IntentoCobroRepository intentoCobroRepository,
            IntentoCobroGrupoRepository intentoCobroGrupoRepository,
            CompraEntregaService compraEntregaService,
            CompraService compraService,
            UsuarioService usuarioService,
            ProveedorPagoProducto proveedorPago,
            @Value("${compra.reserva-minutos:30}") int reservaMinutos) {
        this.compraRepository = compraRepository;
        this.compraGrupoRepository = compraGrupoRepository;
        this.reservaStockRepository = reservaStockRepository;
        this.intentoCobroRepository = intentoCobroRepository;
        this.intentoCobroGrupoRepository = intentoCobroGrupoRepository;
        this.compraEntregaService = compraEntregaService;
        this.compraService = compraService;
        this.usuarioService = usuarioService;
        this.proveedorPago = proveedorPago;
        this.reservaMinutos = reservaMinutos;
    }

    @Transactional
    public PagoCompraDTO iniciarPago(
            String nombreUsuario, Long idCompra, Long idCompraGrupo) {
        Usuario cliente = usuarioService.getByNombreUsuario(nombreUsuario);
        Compra compra = compraRepository
            .findByIdCompraAndClienteIdUsuario(idCompra, cliente.getIdUsuario())
            .orElseThrow(() -> new NoSuchElementException(
                "Compra no encontrada: " + idCompra));
        CompraGrupo grupo = compraGrupoRepository
            .findByIdCompraGrupoAndCompraIdCompra(idCompraGrupo, idCompra)
            .orElseThrow(() -> new NoSuchElementException(
                "Grupo de compra no encontrado: " + idCompraGrupo));

        if (compra.getEstado() == EstadoCompra.PAGA
                || compra.getEstado() == EstadoCompra.VENCIDA
                || compra.getEstado() == EstadoCompra.CANCELADA) {
            throw new ConflictoException(
                "La compra ya no admite un pago nuevo.");
        }
        if (grupo.getEstado() != EstadoCompraGrupo.PENDIENTE) {
            throw new ConflictoException(
                "El grupo ya está pagado o no admite un pago nuevo.");
        }
        validarReservas(grupo);

        IntentoCobroGrupo existente = intentoCobroGrupoRepository
            .findFirstByGrupoIdCompraGrupoAndEstadoOrderByIntentoCobroFechaCreacionDesc(
                idCompraGrupo, EstadoIntentoCobro.PENDIENTE)
            .orElse(null);
        if (existente != null && existente.getIntentoCobro().getInstruccion() != null) {
            return aDTO(existente.getIntentoCobro());
        }

        IntentoCobro intento = new IntentoCobro();
        intento.setCompra(compra);
        intento.setProveedor(proveedorPago.nombre());
        intento.setExternalReference("COMPRA-" + compra.getIdCompra() + "-"
            + UUID.randomUUID().toString().replace("-", "").substring(0, 10));
        intento.setClaveIdempotencia("compra-grupo-" + grupo.getIdCompraGrupo()
            + "-" + UUID.randomUUID());
        intento.setMonto(grupo.getSubtotal());
        intento.setMoneda(grupo.getMoneda());
        intento.setEstado(EstadoIntentoCobro.CREADO);
        intento.setFechaCreacion(LocalDateTime.now());

        IntentoCobroGrupo asignacion = new IntentoCobroGrupo();
        asignacion.setGrupo(grupo);
        asignacion.setMonto(grupo.getSubtotal());
        asignacion.setComision(grupo.getComision());
        asignacion.setNeto(grupo.getNetoArtista());
        asignacion.setEstado(EstadoIntentoCobro.PENDIENTE);
        intento.agregarGrupo(asignacion);
        intentoCobroRepository.save(intento);

        ResultadoCobro resultado = proveedorPago.crearCobro(
            intento, List.of(grupo));
        intento.setIdPreferenciaExterna(resultado.idExterno());
        intento.setInstruccion(resultado.instruccion());
        intento.setEstado(EstadoIntentoCobro.PENDIENTE);
        intento.setFechaActualizacion(LocalDateTime.now());
        intentoCobroRepository.save(intento);
        return aDTO(intento, resultado.expiraEn());
    }

    @Transactional
    public CompraDTO sincronizar(String nombreUsuario, Long idCompra) {
        Usuario cliente = usuarioService.getByNombreUsuario(nombreUsuario);
        Compra compra = compraRepository
            .findByIdCompraAndClienteIdUsuario(idCompra, cliente.getIdUsuario())
            .orElseThrow(() -> new NoSuchElementException(
                "Compra no encontrada: " + idCompra));

        for (CompraGrupo grupo : compra.getGrupos()) {
            if (grupo.getEstado() != EstadoCompraGrupo.PENDIENTE) {
                continue;
            }
            IntentoCobroGrupo asignacion = intentoCobroGrupoRepository
                .findFirstByGrupoIdCompraGrupoAndEstadoOrderByIntentoCobroFechaCreacionDesc(
                    grupo.getIdCompraGrupo(), EstadoIntentoCobro.PENDIENTE)
                .orElse(null);
            if (asignacion == null) {
                continue;
            }
            IntentoCobro intento = asignacion.getIntentoCobro();
            proveedorPago.consultarEstado(intento).ifPresent(estado ->
                aplicarEstadoProveedor(intento.getIdIntentoCobro(), estado));
        }
        return compraService.obtener(nombreUsuario, idCompra);
    }

    @Transactional
    public CompraDTO confirmarSimulado(String nombreUsuario, Long idIntento) {
        IntentoCobro intento = intentoDelCliente(nombreUsuario, idIntento);
        aplicarEstadoProveedor(intento.getIdIntentoCobro(),
            new EstadoCobroProveedor(
                EstadoPago.APROBADO,
                "SIM-" + intento.getIdIntentoCobro(),
                "approved",
                "Pago simulado confirmado.",
                null));
        return compraService.obtener(
            nombreUsuario, intento.getCompra().getIdCompra());
    }

    @Transactional
    public CompraDTO rechazarSimulado(String nombreUsuario, Long idIntento) {
        IntentoCobro intento = intentoDelCliente(nombreUsuario, idIntento);
        aplicarEstadoProveedor(intento.getIdIntentoCobro(),
            new EstadoCobroProveedor(
                EstadoPago.RECHAZADO,
                "SIM-" + intento.getIdIntentoCobro(),
                "rejected",
                "Pago simulado rechazado.",
                null));
        return compraService.obtener(
            nombreUsuario, intento.getCompra().getIdCompra());
    }

    @Transactional
    public void aplicarEstadoProveedor(
            Long idIntento, EstadoCobroProveedor estado) {
        IntentoCobro intento = intentoCobroRepository
            .findByIdIntentoCobroForUpdate(idIntento)
            .orElse(null);
        if (intento == null) {
            return;
        }
        if (intento.getEstado() == EstadoIntentoCobro.APROBADO) {
            return;
        }

        EstadoIntentoCobro nuevo = mapear(estado.estado());
        intento.setEstado(nuevo);
        intento.setEstadoExterno(estado.estadoExterno());
        intento.setIdPagoExterno(estado.idPagoExterno());
        intento.setFechaActualizacion(LocalDateTime.now());
        if (estado.datos() != null) {
            intento.setArancelMp(estado.datos().arancel());
            intento.setNetoRecibidoMp(estado.datos().netoRecibido());
        }
        for (IntentoCobroGrupo asignacion : intento.getGrupos()) {
            asignacion.setEstado(nuevo);
        }

        if (nuevo == EstadoIntentoCobro.APROBADO) {
            intento.setFechaAprobacion(LocalDateTime.now());
            intentoCobroRepository.save(intento);
            compraEntregaService.confirmarPago(intento);
        } else {
            intentoCobroRepository.save(intento);
        }
    }

    private void validarReservas(CompraGrupo grupo) {
        for (CompraItem item : grupo.getItems()) {
            ReservaStock reserva = reservaStockRepository
                .findByCompraItemIdCompraItem(item.getIdCompraItem())
                .orElse(null);
            if (reserva == null || reserva.getEstado() != EstadoReserva.ACTIVA) {
                throw new ConflictoException(
                    "La reserva de “" + item.getTituloProducto()
                        + "” venció. Volvé a comprar el producto.");
            }
        }
    }

    private IntentoCobro intentoDelCliente(String nombreUsuario, Long idIntento) {
        Usuario cliente = usuarioService.getByNombreUsuario(nombreUsuario);
        IntentoCobro intento = intentoCobroRepository.findById(idIntento)
            .orElseThrow(() -> new NoSuchElementException(
                "Intento de cobro no encontrado: " + idIntento));
        if (intento.getCompra() == null
                || intento.getCompra().getCliente() == null
                || !intento.getCompra().getCliente().getIdUsuario()
                    .equals(cliente.getIdUsuario())) {
            throw new AccesoDenegadoException(
                "No podés operar sobre este pago.");
        }
        return intento;
    }

    private EstadoIntentoCobro mapear(EstadoPago estado) {
        if (estado == null) {
            return EstadoIntentoCobro.PENDIENTE;
        }
        return switch (estado) {
            case APROBADO -> EstadoIntentoCobro.APROBADO;
            case RECHAZADO -> EstadoIntentoCobro.RECHAZADO;
            case CANCELADO, REEMBOLSADO -> EstadoIntentoCobro.CANCELADO;
            default -> EstadoIntentoCobro.PENDIENTE;
        };
    }

    private PagoCompraDTO aDTO(IntentoCobro intento) {
        return aDTO(intento, expiracion(intento));
    }

    private PagoCompraDTO aDTO(IntentoCobro intento, LocalDateTime expira) {
        Long idGrupo = intento.getGrupos().isEmpty()
            ? null : intento.getGrupos().get(0).getGrupo().getIdCompraGrupo();
        return PagoCompraDTO.builder()
            .idIntentoCobro(intento.getIdIntentoCobro())
            .idCompra(intento.getCompra() != null
                ? intento.getCompra().getIdCompra() : null)
            .idCompraGrupo(idGrupo)
            .proveedor(intento.getProveedor())
            .estado(intento.getEstado())
            .instruccion(intento.getInstruccion())
            .idPagoExterno(intento.getIdPagoExterno())
            .monto(intento.getMonto())
            .moneda(intento.getMoneda() != null
                ? intento.getMoneda().getCodigo() : "ARS")
            .expiraEn(expira)
            .build();
    }

    private LocalDateTime expiracion(IntentoCobro intento) {
        LocalDateTime base = intento.getFechaCreacion() != null
            ? intento.getFechaCreacion() : LocalDateTime.now();
        return base.plusMinutes(reservaMinutos);
    }
}
