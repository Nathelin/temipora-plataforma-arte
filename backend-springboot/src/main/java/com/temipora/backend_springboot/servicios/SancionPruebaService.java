package com.temipora.backend_springboot.servicios;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.temipora.backend_springboot.modelo.AlcanceSancion;
import com.temipora.backend_springboot.modelo.CatalogoPausadoDTO;
import com.temipora.backend_springboot.modelo.ConfiguracionPedido;
import com.temipora.backend_springboot.modelo.ERol;
import com.temipora.backend_springboot.modelo.EstadoEntregaHito;
import com.temipora.backend_springboot.modelo.EstadoPagoHito;
import com.temipora.backend_springboot.modelo.EstadoPedido;
import com.temipora.backend_springboot.modelo.EstadoSancion;
import com.temipora.backend_springboot.modelo.HitoPedido;
import com.temipora.backend_springboot.modelo.EstadoProrroga;
import com.temipora.backend_springboot.modelo.OrigenSancion;
import com.temipora.backend_springboot.modelo.Pedido;
import com.temipora.backend_springboot.modelo.ProrrogaPedido;
import com.temipora.backend_springboot.modelo.SancionCuenta;
import com.temipora.backend_springboot.modelo.SancionCuentaDTO;
import com.temipora.backend_springboot.modelo.TipoHito;
import com.temipora.backend_springboot.modelo.Usuario;
import com.temipora.backend_springboot.repositorios.PedidoRepository;
import com.temipora.backend_springboot.repositorios.SancionCuentaRepository;
import com.temipora.backend_springboot.repositorios.UsuarioRepository;

/**
 * Herramientas de prueba para recorrer el ciclo de sanciones por entrega sin
 * esperar a los jobs: prepara un pedido en mora, aplica Nivel 1, fuerza el
 * Nivel 2, simula la aprobación del cliente y revierte todo.
 *
 * <p>Solo opera sobre cuentas ARTISTA y sobre sanciones automáticas. No crea
 * pedidos nuevos: reutiliza un pedido en curso del artista y lo reabre.</p>
 */
@Service
public class SancionPruebaService {

    private static final String USUARIO_POR_DEFECTO = "ArtistaMoroso";
    private static final int DIAS_PLAZO_SANO = 7;

    public record HitoPrueba(
            Long idHitoPedido,
            String nombre,
            String tipo,
            Integer orden,
            String estadoPago,
            String estadoEntrega,
            boolean requiereEntrega,
            boolean exigible,
            LocalDateTime plazoVigente) {
    }

    public record PedidoPrueba(
            Long idPedido,
            String nombreServicio,
            String cliente,
            String estado,
            LocalDate fechaInicio,
            LocalDate limiteEntrega,
            LocalDateTime moraDesde,
            boolean entregaAtrasada,
            List<HitoPrueba> hitos) {
    }

    public record EstadoPrueba(
            String usuario,
            Long idUsuario,
            String rol,
            SancionCuentaDTO sancion,
            List<PedidoPrueba> pedidos,
            List<CatalogoPausadoDTO> pausados,
            Integer diasGraciaEntrega,
            Integer diasSancionVentas,
            Integer diasCierreTrasBloqueo) {
    }

    public record ResumenCiclo(
            int morasRegistradas,
            int sancionesCuradas,
            int sancionesEscaladas,
            int nivel1Aplicados,
            int pedidosCerrados) {
    }

    public record ResultadoPrueba(
            String mensaje,
            ResumenCiclo ciclo,
            EstadoPrueba estado) {
    }

    private final UsuarioRepository usuarioRepository;
    private final PedidoRepository pedidoRepository;
    private final SancionCuentaRepository sancionRepository;
    private final MoraService moraService;
    private final SancionIncumplimientoService sancionIncumplimientoService;
    private final SancionAutomaticaService sancionAutomaticaService;
    private final ConfiguracionPedidoService configuracionPedidoService;
    private final EntregaPendienteService entregaPendienteService;
    private final SaludCuentaService saludCuentaService;

    public SancionPruebaService(
            UsuarioRepository usuarioRepository,
            PedidoRepository pedidoRepository,
            SancionCuentaRepository sancionRepository,
            MoraService moraService,
            SancionIncumplimientoService sancionIncumplimientoService,
            SancionAutomaticaService sancionAutomaticaService,
            ConfiguracionPedidoService configuracionPedidoService,
            EntregaPendienteService entregaPendienteService,
            SaludCuentaService saludCuentaService) {
        this.usuarioRepository = usuarioRepository;
        this.pedidoRepository = pedidoRepository;
        this.sancionRepository = sancionRepository;
        this.moraService = moraService;
        this.sancionIncumplimientoService = sancionIncumplimientoService;
        this.sancionAutomaticaService = sancionAutomaticaService;
        this.configuracionPedidoService = configuracionPedidoService;
        this.entregaPendienteService = entregaPendienteService;
        this.saludCuentaService = saludCuentaService;
    }

    @Transactional(readOnly = true)
    public EstadoPrueba estado(String nombreUsuario) {
        Usuario usuario = artista(nombreUsuario);
        LocalDateTime ahora = LocalDateTime.now();
        ConfiguracionPedido config = configuracionPedidoService.obtener();
        SancionCuenta sancion = sancionRepository
            .findFirstByUsuarioIdUsuarioAndEstado(
                usuario.getIdUsuario(), EstadoSancion.ACTIVA)
            .orElse(null);

        List<PedidoPrueba> pedidos = new ArrayList<>();
        for (Pedido pedido : pedidosEnCurso(usuario)) {
            pedidos.add(pedidoPrueba(pedido));
        }

        List<CatalogoPausadoDTO> pausados = sancion != null
            ? saludCuentaService.salud(usuario.getIdUsuario()).dto().getPausados()
            : List.of();

        return new EstadoPrueba(
            usuario.getNombreUsuario(),
            usuario.getIdUsuario(),
            usuario.getRol() != null && usuario.getRol().getNombre() != null
                ? usuario.getRol().getNombre().name()
                : null,
            sancion != null ? SancionCuentaDTO.desde(sancion, ahora) : null,
            pedidos,
            pausados != null ? pausados : List.of(),
            config.getDiasGraciaEntrega(),
            config.getDiasSancionVentas(),
            config.getDiasCierreTrasBloqueo());
    }

    /**
     * Ejecuta el ciclo real de mora e incumplimiento (el mismo que corre el
     * job) y devuelve el estado del artista indicado.
     */
    @Transactional
    public ResultadoPrueba procesar(String nombreUsuario) {
        ResumenCiclo ciclo = ejecutarCiclo();
        return new ResultadoPrueba(
            "Ciclo de incumplimientos ejecutado.",
            ciclo,
            estadoSeguro(nombreUsuario));
    }

    /**
     * Deja al artista en Nivel 1: revierte cualquier sanción automática activa,
     * reabre un entregable y atrasa el plazo más allá de la gracia. Después
     * ejecuta el ciclo real para que aplique la sanción, notificación y correo.
     */
    @Transactional
    public ResultadoPrueba prepararNivel1(String nombreUsuario) {
        Usuario usuario = artista(nombreUsuario);
        revertirActiva(usuario, "Laboratorio: reinicio para aplicar Nivel 1");

        Pedido pedido = pedidoDePrueba(usuario);
        prepararEntregable(pedido);
        ConfiguracionPedido config = configuracionPedidoService.obtener();
        int gracia = config.getDiasGraciaEntrega() != null
            ? config.getDiasGraciaEntrega()
            : ConfiguracionPedido.DIAS_GRACIA_ENTREGA_POR_DEFECTO;
        LocalDate limite = LocalDate.now()
            .minusDays(Math.max(1, gracia) + 2L);
        pedido.setEstado(EstadoPedido.EN_CURSO);
        pedido.setFechaInicio(limite.minusDays(10));
        pedido.setLimiteEntrega(limite);
        pedido.setMoraDesde(null);
        pedido.setIncumplimientoEn(null);
        pedido.setCierrePorIncumplimiento(false);
        // Una prórroga pendiente pausaría el reloj de la mora.
        for (ProrrogaPedido prorroga : pedido.getProrrogas()) {
            if (prorroga.getEstado() == EstadoProrroga.PENDIENTE) {
                prorroga.setFechaLimiteRespuesta(
                    LocalDateTime.now().minusMinutes(1));
            }
        }
        pedidoRepository.save(pedido);

        LocalDateTime plazo = entregaPendienteService.plazoVigente(pedido);
        if (plazo == null) {
            throw new IllegalStateException(
                "No se pudo determinar el plazo del entregable del pedido #"
                    + pedido.getIdPedido() + ".");
        }
        // Se registra la mora directamente para que la prueba no dependa del
        // corte de sanciones automáticas del pedido.
        pedido.setMoraDesde(plazo);
        pedidoRepository.save(pedido);

        ResumenCiclo ciclo = ejecutarCiclo();
        EstadoPrueba estado = estadoSeguro(usuario.getNombreUsuario());
        boolean aplicada = estado.sancion() != null
            && estado.sancion().getAlcance() == AlcanceSancion.ENTREGAS;
        return new ResultadoPrueba(
            aplicada
                ? "Nivel 1 aplicado: cuenta limitada a entregas y catálogo pausado."
                : "No se pudo aplicar el Nivel 1. Revisá el estado del pedido.",
            ciclo,
            estado);
    }

    /**
     * Fuerza el vencimiento de la ventana del Nivel 1 y ejecuta el ciclo real
     * para escalar la sanción a Nivel 2 (cuenta limitada a entregas).
     */
    @Transactional
    public ResultadoPrueba forzarNivel2(String nombreUsuario) {
        Usuario usuario = artista(nombreUsuario);
        SancionCuenta sancion = activa(usuario);
        if (sancion == null) {
            throw new IllegalStateException(
                "No hay una sanción automática activa. Aplicá el Nivel 1 primero.");
        }
        if (sancion.getAlcance() == AlcanceSancion.ACCESO) {
            return new ResultadoPrueba(
                "La cuenta ya está en Nivel 2 (inhabilitada).",
                null,
                estadoSeguro(nombreUsuario));
        }
        if (sancion.getAlcance() != AlcanceSancion.ENTREGAS) {
            throw new IllegalStateException(
                "La sanción activa no es una restricción a entregas.");
        }
        sancion.setFechaFin(LocalDateTime.now().minusSeconds(1));
        sancionRepository.save(sancion);

        ResumenCiclo ciclo = ejecutarCiclo();
        EstadoPrueba estado = estadoSeguro(nombreUsuario);
        boolean escalada = estado.sancion() != null
            && estado.sancion().getAlcance() == AlcanceSancion.ACCESO;
        return new ResultadoPrueba(
            escalada
                ? "Nivel 2 aplicado: la cuenta quedó inhabilitada."
                : "No se pudo escalar: el pedido ya no está en mora.",
            ciclo,
            estado);
    }

    /**
     * Simula que el cliente aprueba el entregable pendiente, ejecuta el ciclo
     * real y reincorpora al artista. En Nivel 2, además revierte la sanción
     * como lo haría un administrador.
     */
    @Transactional
    public ResultadoPrueba simularAprobacion(String nombreUsuario) {
        Usuario usuario = artista(nombreUsuario);
        SancionCuenta sancion = activa(usuario);
        if (sancion == null) {
            throw new IllegalStateException(
                "No hay una sanción activa para curar.");
        }
        Pedido pedido = sancion.getPedido() != null
            ? sancion.getPedido()
            : pedidoDePrueba(usuario);
        HitoPedido hito = sancion.getHitoPedido() != null
            ? sancion.getHitoPedido()
            : entregaPendienteService.hitoExigible(pedido);
        if (hito == null) {
            throw new IllegalStateException(
                "No se encontró un hito pendiente para aprobar.");
        }
        hito.setEstadoEntrega(EstadoEntregaHito.APROBADO);
        hito.setFechaAprobacion(LocalDateTime.now());
        hito.setFechaLimiteAprobacion(null);
        pedidoRepository.save(pedido);

        ResumenCiclo ciclo = ejecutarCiclo();
        SancionCuenta restante = activa(usuario);
        boolean reincorporada = false;
        if (restante != null
                && restante.getAlcance() == AlcanceSancion.ACCESO) {
            sancionAutomaticaService.revertirYRestaurar(
                restante.getIdSancion(),
                "Laboratorio: aprobación simulada en Nivel 2");
            reincorporada = true;
        }
        return new ResultadoPrueba(
            reincorporada
                ? "Aprobación simulada: se revirtió el Nivel 2 y el artista quedó reincorporado."
                : "Aprobación simulada: mora curada y catálogo restaurado.",
            ciclo,
            estadoSeguro(nombreUsuario));
    }

    /**
     * Revierte la sanción activa, restaura el catálogo y deja el pedido del
     * artista con un plazo futuro para que no vuelva a sancionarse solo.
     */
    @Transactional
    public ResultadoPrueba revertirTodo(String nombreUsuario) {
        Usuario usuario = artista(nombreUsuario);
        revertirActiva(usuario, "Laboratorio: reversión total");

        for (Pedido pedido : pedidosEnCurso(usuario)) {
            pedido.setMoraDesde(null);
            pedido.setIncumplimientoEn(null);
            pedido.setCierrePorIncumplimiento(false);
            if (pedido.getEstado() == EstadoPedido.EN_CURSO) {
                pedido.setLimiteEntrega(
                    LocalDate.now().plusDays(DIAS_PLAZO_SANO));
                HitoPedido hito = primerEntregable(pedido);
                if (hito != null) {
                    hito.setEstadoEntrega(EstadoEntregaHito.NO_INICIADO);
                    hito.setFechaEntrega(null);
                    hito.setFechaLimiteReentrega(null);
                    hito.setFechaLimiteAprobacion(null);
                    hito.setFechaAprobacion(null);
                }
            }
            pedidoRepository.save(pedido);
        }
        return new ResultadoPrueba(
            "Sanción revertida, catálogo restaurado y pedido reabierto con "
                + DIAS_PLAZO_SANO + " días de plazo.",
            null,
            estadoSeguro(nombreUsuario));
    }

    private ResumenCiclo ejecutarCiclo() {
        int moras = moraService.detectar();
        SancionIncumplimientoService.Resumen resumen =
            sancionIncumplimientoService.procesar();
        return new ResumenCiclo(
            moras,
            resumen.curados(),
            resumen.escalados(),
            resumen.nivel1Aplicados(),
            resumen.pedidosCerrados());
    }

    private EstadoPrueba estadoSeguro(String nombreUsuario) {
        try {
            return estado(nombreUsuario);
        } catch (RuntimeException e) {
            return null;
        }
    }

    private Usuario artista(String nombreUsuario) {
        String buscado = nombreUsuario == null || nombreUsuario.isBlank()
            ? USUARIO_POR_DEFECTO
            : nombreUsuario.trim();
        Usuario usuario = usuarioRepository
            .findByNombreUsuarioIgnoreCase(buscado)
            .orElseThrow(() -> new NoSuchElementException(
                "Usuario no encontrado: " + buscado));
        if (usuario.getRol() == null
                || usuario.getRol().getNombre() != ERol.ARTISTA) {
            throw new IllegalArgumentException(
                "El laboratorio solo opera sobre cuentas ARTISTA.");
        }
        return usuario;
    }

    private List<Pedido> pedidosEnCurso(Usuario usuario) {
        return pedidoRepository
            .findByArtistaIdUsuarioAndEstadoInOrderByFechaCreacionDesc(
                usuario.getIdUsuario(),
                List.of(EstadoPedido.EN_CURSO),
                PageRequest.of(0, 10));
    }

    private Pedido pedidoDePrueba(Usuario usuario) {
        for (Pedido pedido : pedidosEnCurso(usuario)) {
            if (primerEntregable(pedido) != null) {
                return pedido;
            }
        }
        throw new IllegalStateException(
            "El artista no tiene pedidos en curso con un entregable pendiente.");
    }

    private HitoPedido prepararEntregable(Pedido pedido) {
        HitoPedido elegido = primerEntregable(pedido);
        if (elegido == null) {
            throw new IllegalStateException(
                "El pedido #" + pedido.getIdPedido()
                    + " no tiene hitos con entregable.");
        }
        LocalDateTime ahora = LocalDateTime.now();
        for (HitoPedido hito : pedido.getHitos()) {
            if (hito.getOrden() == null || elegido.getOrden() == null
                    || hito.getOrden() >= elegido.getOrden()) {
                continue;
            }
            if (hito.getHitoPresupuesto() != null && !estaAprobado(hito)) {
                // La entrega final exige que la etapa anterior esté aprobada,
                // aunque esa etapa no tenga entregable propio.
                hito.setEstadoEntrega(EstadoEntregaHito.APROBADO);
                hito.setFechaAprobacion(ahora);
                hito.setFechaLimiteAprobacion(null);
            }
        }
        if (elegido.getHitoPresupuesto().getTipo() == TipoHito.ENTREGA) {
            elegido.setEstadoPago(EstadoPagoHito.PENDIENTE);
        } else {
            elegido.setEstadoPago(EstadoPagoHito.PAGADO);
            if (elegido.getFechaPago() == null) {
                elegido.setFechaPago(ahora);
            }
        }
        elegido.setEstadoEntrega(EstadoEntregaHito.NO_INICIADO);
        elegido.setFechaEntrega(null);
        elegido.setFechaLimiteReentrega(null);
        elegido.setFechaLimiteAprobacion(null);
        elegido.setFechaAprobacion(null);
        elegido.setRevisiones(0);
        elegido.setEntregaDescripcion(null);
        elegido.setEntregaImagen(null);
        return elegido;
    }

    private HitoPedido primerEntregable(Pedido pedido) {
        for (HitoPedido hito : pedido.getHitos()) {
            if (hito.getHitoPresupuesto() != null
                    && hito.getHitoPresupuesto().isRequiereEntrega()) {
                return hito;
            }
        }
        return null;
    }

    private boolean estaAprobado(HitoPedido hito) {
        return hito.getEstadoEntrega() == EstadoEntregaHito.APROBADO
            || hito.getEstadoEntrega()
                == EstadoEntregaHito.APROBADO_AUTOMATICAMENTE;
    }

    private SancionCuenta activa(Usuario usuario) {
        SancionCuenta sancion = sancionRepository
            .findFirstByUsuarioIdUsuarioAndEstado(
                usuario.getIdUsuario(), EstadoSancion.ACTIVA)
            .orElse(null);
        if (sancion != null
                && sancion.getOrigen() != OrigenSancion.AUTOMATICA) {
            throw new IllegalStateException(
                "La cuenta tiene una sanción manual activa; revertila desde la tabla.");
        }
        return sancion;
    }

    private void revertirActiva(Usuario usuario, String observacion) {
        SancionCuenta sancion = sancionRepository
            .findFirstByUsuarioIdUsuarioAndEstado(
                usuario.getIdUsuario(), EstadoSancion.ACTIVA)
            .orElse(null);
        if (sancion == null) {
            return;
        }
        if (sancion.getOrigen() != OrigenSancion.AUTOMATICA) {
            throw new IllegalStateException(
                "La cuenta tiene una sanción manual activa; revertila desde la tabla.");
        }
        sancionAutomaticaService.revertirYRestaurar(
            sancion.getIdSancion(), observacion);
    }

    private PedidoPrueba pedidoPrueba(Pedido pedido) {
        List<HitoPrueba> hitos = new ArrayList<>();
        for (HitoPedido hito : pedido.getHitos()) {
            if (hito.getHitoPresupuesto() == null) {
                continue;
            }
            hitos.add(new HitoPrueba(
                hito.getIdHitoPedido(),
                hito.getHitoPresupuesto().getNombre(),
                hito.getHitoPresupuesto().getTipo() != null
                    ? hito.getHitoPresupuesto().getTipo().name()
                    : null,
                hito.getOrden(),
                hito.getEstadoPago() != null
                    ? hito.getEstadoPago().name()
                    : null,
                hito.getEstadoEntrega() != null
                    ? hito.getEstadoEntrega().name()
                    : null,
                hito.getHitoPresupuesto().isRequiereEntrega(),
                entregaPendienteService.esExigibleAlArtista(pedido, hito),
                entregaPendienteService.plazoVigente(pedido, hito)));
        }
        return new PedidoPrueba(
            pedido.getIdPedido(),
            pedido.getServicio() != null
                ? pedido.getServicio().getNombre()
                : "Pedido #" + pedido.getIdPedido(),
            pedido.getCliente() != null
                ? pedido.getCliente().getNombreUsuario()
                : "",
            pedido.getEstado() != null ? pedido.getEstado().name() : null,
            pedido.getFechaInicio(),
            pedido.getLimiteEntrega(),
            pedido.getMoraDesde(),
            PedidoFechas.entregaAtrasada(pedido, LocalDate.now()),
            hitos);
    }
}
