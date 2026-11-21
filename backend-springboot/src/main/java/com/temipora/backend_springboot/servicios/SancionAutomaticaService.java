package com.temipora.backend_springboot.servicios;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.temipora.backend_springboot.modelo.AlcanceSancion;
import com.temipora.backend_springboot.modelo.EstadoProducto;
import com.temipora.backend_springboot.modelo.EstadoPublicacionReventa;
import com.temipora.backend_springboot.modelo.EstadoServicio;
import com.temipora.backend_springboot.modelo.EstadoSancion;
import com.temipora.backend_springboot.modelo.HitoPedido;
import com.temipora.backend_springboot.modelo.OrigenSancion;
import com.temipora.backend_springboot.modelo.Pedido;
import com.temipora.backend_springboot.modelo.Producto;
import com.temipora.backend_springboot.modelo.PublicacionReventa;
import com.temipora.backend_springboot.modelo.SancionCuenta;
import com.temipora.backend_springboot.modelo.Servicio;
import com.temipora.backend_springboot.modelo.TipoSancion;
import com.temipora.backend_springboot.repositorios.ProductoRepository;
import com.temipora.backend_springboot.repositorios.PublicacionReventaRepository;
import com.temipora.backend_springboot.repositorios.SancionCuentaRepository;
import com.temipora.backend_springboot.repositorios.ServicioRepository;

/**
 * Orquesta las sanciones automáticas por incumplimiento de entrega: aplica el
 * Nivel 1, escala al Nivel 2, cura cuando el cliente aprueba y pausa o
 * restaura el catálogo del artista.
 */
@Service
public class SancionAutomaticaService {

    private static final String NOTIF_VENTAS = "CUENTA_SUSPENDIDA_VENTAS";
    private static final String NOTIF_ENTREGAS = "CUENTA_RESTRINGIDA_ENTREGAS";
    private static final String TIPO_REFERENCIA = "PEDIDO";
    private static final String ENTIDAD = "SancionCuenta";
    private static final DateTimeFormatter FECHA =
        DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final SancionService sancionService;
    private final SancionCuentaRepository sancionRepository;
    private final ServicioRepository servicioRepository;
    private final ProductoRepository productoRepository;
    private final PublicacionReventaRepository reventaRepository;
    private final EntregaPendienteService entregaPendienteService;
    private final NotificacionService notificacionService;

    public SancionAutomaticaService(
            SancionService sancionService,
            SancionCuentaRepository sancionRepository,
            ServicioRepository servicioRepository,
            ProductoRepository productoRepository,
            PublicacionReventaRepository reventaRepository,
            EntregaPendienteService entregaPendienteService,
            NotificacionService notificacionService) {
        this.sancionService = sancionService;
        this.sancionRepository = sancionRepository;
        this.servicioRepository = servicioRepository;
        this.productoRepository = productoRepository;
        this.reventaRepository = reventaRepository;
        this.entregaPendienteService = entregaPendienteService;
        this.notificacionService = notificacionService;
    }

    @Transactional
    public SancionCuenta aplicarNivel1(
            Pedido pedido, int diasSancionVentas) {
        LocalDateTime ahora = LocalDateTime.now();
        LocalDateTime inicio = ahora;
        // Nunca pausar el catálogo si ya hay una sanción activa: el registro de
        // lo pausado quedaría asociado a una sanción ajena.
        var activa = sancionRepository.findFirstByUsuarioIdUsuarioAndEstado(
            pedido.getArtista().getIdUsuario(), EstadoSancion.ACTIVA);
        if (activa.isPresent()) {
            return activa.get();
        }
        LocalDateTime fin = inicio.plusDays(Math.max(1, diasSancionVentas));
        String clave = "AUTO:ENTREGA:" + pedido.getIdPedido() + ":NIVEL1";

        SancionCuenta sancion = sancionService.aplicarSistema(
            pedido.getArtista().getIdUsuario(),
            TipoSancion.SUSPENSION,
            AlcanceSancion.VENTAS,
            motivoNivel1(pedido, inicio),
            fin,
            clave,
            pedido,
            hitoDe(pedido),
            inicio);
        if (sancion.getOrigen() != OrigenSancion.AUTOMATICA) {
            return sancion;
        }
        String afectado = pausarVentas(
            pedido.getArtista().getIdUsuario(), ahora);
        if (afectado != null) {
            sancion.setAlcanceAfectado(afectado);
            sancion = sancionRepository.save(sancion);
        }
        notificacionService.crearParaUsuario(
            pedido.getArtista().getIdUsuario(),
            NOTIF_VENTAS,
            "Ventas suspendidas",
            "No entregaste a tiempo un encargo. Tus ventas quedan suspendidas "
                + "hasta el " + fin.format(FECHA)
                + ". Podés terminar los pedidos que tenés abiertos.",
            pedido.getIdPedido(),
            TIPO_REFERENCIA);
        return sancion;
    }

    @Transactional
    public SancionCuenta escalarNivel2(SancionCuenta sancion, Pedido pedido) {
        String clave = "AUTO:ENTREGA:" + pedido.getIdPedido() + ":NIVEL2";
        SancionCuenta escalada = sancionService.escalarSistema(
            sancion.getIdSancion(),
            motivoNivel2(pedido),
            clave,
            pedido,
            hitoDe(pedido),
            pedido.getMoraDesde());
        notificacionService.crearParaUsuario(
            escalada.getUsuario().getIdUsuario(),
            NOTIF_ENTREGAS,
            "Cuenta restringida",
            "Tu cuenta quedó limitada a entregar los pedidos pendientes. "
                + "Podés apelar desde la pantalla de cuenta restringida.",
            pedido.getIdPedido(),
            TIPO_REFERENCIA);
        return escalada;
    }

    @Transactional
    public void curar(SancionCuenta sancion) {
        SancionCuenta revertida = sancionService.revertirSistema(
            sancion.getIdSancion(),
            "Se curó la mora: el cliente aprobó los entregables pendientes");
        restaurarEfectos(revertida);
    }

    @Transactional
    public SancionCuenta revertirYRestaurar(
            Long idSancion, String observacion) {
        SancionCuenta revertida = sancionService.revertirSistema(
            idSancion, observacion);
        restaurarEfectos(revertida);
        return revertida;
    }

    /**
     * Reactiva solo lo que la sanción pausó. Un servicio que el artista había
     * pausado antes de la sanción no estaba en el registro y no se toca.
     */
    @Transactional
    public void restaurarEfectos(SancionCuenta sancion) {
        String valor = sancion.getAlcanceAfectado();
        if (valor == null || valor.isBlank()) {
            return;
        }
        for (String parte : valor.split(";")) {
            if (parte.isBlank()) {
                continue;
            }
            String[] par = parte.split("=", 2);
            if (par.length != 2) {
                continue;
            }
            List<Long> ids = ids(par[1]);
            switch (par[0]) {
                case "servicios" -> ids.forEach(this::reactivarServicio);
                case "productos" -> ids.forEach(this::reactivarProducto);
                case "reventas" -> ids.forEach(this::reactivarReventa);
                default -> { }
            }
        }
        sancion.setAlcanceAfectado(null);
        sancionRepository.save(sancion);
    }

    private String pausarVentas(Long idArtista, LocalDateTime ahora) {
        List<Long> servicios = new ArrayList<>();
        for (Servicio servicio : servicioRepository
                .findByArtistaIdUsuarioAndEstado(idArtista, EstadoServicio.PUBLICADO)) {
            servicio.setEstado(EstadoServicio.PAUSADO);
            servicio.setPausadoEn(ahora);
            servicioRepository.save(servicio);
            servicios.add(servicio.getIdServicio());
        }
        List<Long> productos = new ArrayList<>();
        for (Producto producto : productoRepository
                .findByArtistaIdUsuarioAndEstadoOrderByTituloAsc(
                    idArtista, EstadoProducto.DISPONIBLE)) {
            producto.setEstado(EstadoProducto.PAUSADO);
            producto.setPausadoEn(ahora);
            productoRepository.save(producto);
            productos.add(producto.getIdProducto());
        }
        List<Long> reventas = new ArrayList<>();
        for (PublicacionReventa reventa : reventaRepository
                .findByVendedorIdUsuarioAndEstado(
                    idArtista, EstadoPublicacionReventa.PUBLICADA)) {
            reventa.setEstado(EstadoPublicacionReventa.SUSPENDIDA);
            reventaRepository.save(reventa);
            reventas.add(reventa.getIdPublicacionReventa());
        }
        return formato(servicios, productos, reventas);
    }

    private void reactivarServicio(Long idServicio) {
        servicioRepository.findByIdServicio(idServicio).ifPresent(servicio -> {
            if (servicio.getEstado() == EstadoServicio.PAUSADO) {
                servicio.setEstado(EstadoServicio.PUBLICADO);
                servicio.setPausadoEn(null);
                servicioRepository.save(servicio);
            }
        });
    }

    private void reactivarProducto(Long idProducto) {
        productoRepository.findById(idProducto).ifPresent(producto -> {
            if (producto.getEstado() == EstadoProducto.PAUSADO) {
                producto.setEstado(EstadoProducto.DISPONIBLE);
                producto.setPausadoEn(null);
                productoRepository.save(producto);
            }
        });
    }

    private void reactivarReventa(Long idPublicacion) {
        reventaRepository.findById(idPublicacion).ifPresent(reventa -> {
            if (reventa.getEstado() == EstadoPublicacionReventa.SUSPENDIDA) {
                reventa.setEstado(EstadoPublicacionReventa.PUBLICADA);
                reventaRepository.save(reventa);
            }
        });
    }

    private HitoPedido hitoDe(Pedido pedido) {
        HitoPedido exigible = entregaPendienteService.hitoExigible(pedido);
        if (exigible != null) {
            return exigible;
        }
        return pedido.getHitos().stream()
            .filter(hito -> hito.getHitoPresupuesto() != null
                && hito.getHitoPresupuesto().isRequiereEntrega())
            .findFirst()
            .orElse(null);
    }

    private String motivoNivel1(Pedido pedido, LocalDateTime plazo) {
        return "No entregó '" + nombreHito(pedido) + "' del pedido #"
            + pedido.getIdPedido() + " en el plazo previsto (vencido el "
            + plazo.format(FECHA) + ").";
    }

    private String motivoNivel2(Pedido pedido) {
        return "No entregó '" + nombreHito(pedido) + "' del pedido #"
            + pedido.getIdPedido()
            + " dentro de la ventana de la suspensión de ventas.";
    }

    private String nombreHito(Pedido pedido) {
        HitoPedido hito = hitoDe(pedido);
        return hito != null && hito.getHitoPresupuesto() != null
                && hito.getHitoPresupuesto().getNombre() != null
            ? hito.getHitoPresupuesto().getNombre()
            : "el entregable pendiente";
    }

    private String formato(
            List<Long> servicios, List<Long> productos, List<Long> reventas) {
        StringBuilder texto = new StringBuilder();
        agregar(texto, "servicios", servicios);
        agregar(texto, "productos", productos);
        agregar(texto, "reventas", reventas);
        return texto.length() == 0 ? null : texto.toString();
    }

    private void agregar(StringBuilder texto, String clave, List<Long> ids) {
        if (ids.isEmpty()) {
            return;
        }
        texto.append(clave).append('=')
            .append(String.join(",", ids.stream().map(String::valueOf).toList()))
            .append(';');
    }

    private List<Long> ids(String valor) {
        List<Long> ids = new ArrayList<>();
        for (String parte : valor.split(",")) {
            try {
                ids.add(Long.valueOf(parte.trim()));
            } catch (NumberFormatException e) {
                // Ignorar entradas corruptas del registro.
            }
        }
        return ids;
    }
}
