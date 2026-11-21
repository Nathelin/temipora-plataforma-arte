package com.temipora.backend_springboot.servicios;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.temipora.backend_springboot.modelo.AlcanceSancion;
import com.temipora.backend_springboot.modelo.CatalogoPausadoDTO;
import com.temipora.backend_springboot.modelo.ConfiguracionPedido;
import com.temipora.backend_springboot.modelo.EstadoApelacion;
import com.temipora.backend_springboot.modelo.EstadoPedido;
import com.temipora.backend_springboot.modelo.Pedido;
import com.temipora.backend_springboot.modelo.PublicacionReventa;
import com.temipora.backend_springboot.modelo.SancionCuenta;
import com.temipora.backend_springboot.modelo.SaludCuentaDTO;
import com.temipora.backend_springboot.repositorios.ApelacionSancionRepository;
import com.temipora.backend_springboot.repositorios.PedidoRepository;
import com.temipora.backend_springboot.repositorios.ProductoRepository;
import com.temipora.backend_springboot.repositorios.PublicacionReventaRepository;
import com.temipora.backend_springboot.repositorios.ServicioRepository;

/**
 * Calcula la salud de la cuenta del artista para el panel: estado de la
 * sanción, días de gracia que le quedan, catálogo pausado y si tiene una
 * apelación pendiente. También devuelve la gracia por pedido para que el
 * resumen pueda marcar el riesgo sin duplicar la regla de fechas.
 */
@Service
public class SaludCuentaService {

    private static final List<EstadoPedido> ESTADOS_ACTIVOS = List.of(
        EstadoPedido.PENDIENTE_PAGO,
        EstadoPedido.EN_CURSO);
    private static final int MAX_PEDIDOS = 20;

    private final SancionService sancionService;
    private final ConfiguracionPedidoService configuracionPedidoService;
    private final EntregaPendienteService entregaPendienteService;
    private final ApelacionSancionRepository apelacionSancionRepository;
    private final PedidoRepository pedidoRepository;
    private final ServicioRepository servicioRepository;
    private final ProductoRepository productoRepository;
    private final PublicacionReventaRepository publicacionReventaRepository;

    public SaludCuentaService(
            SancionService sancionService,
            ConfiguracionPedidoService configuracionPedidoService,
            EntregaPendienteService entregaPendienteService,
            ApelacionSancionRepository apelacionSancionRepository,
            PedidoRepository pedidoRepository,
            ServicioRepository servicioRepository,
            ProductoRepository productoRepository,
            PublicacionReventaRepository publicacionReventaRepository) {
        this.sancionService = sancionService;
        this.configuracionPedidoService = configuracionPedidoService;
        this.entregaPendienteService = entregaPendienteService;
        this.apelacionSancionRepository = apelacionSancionRepository;
        this.pedidoRepository = pedidoRepository;
        this.servicioRepository = servicioRepository;
        this.productoRepository = productoRepository;
        this.publicacionReventaRepository = publicacionReventaRepository;
    }

    public record Salud(
            SaludCuentaDTO dto,
            Map<Long, Integer> diasGraciaPorPedido) {
    }

    @Transactional(readOnly = true)
    public Salud salud(Long idArtista) {
        int gracia = diasGracia();
        Map<Long, Integer> dias = diasGraciaPorPedido(idArtista, gracia);
        Optional<SancionCuenta> sancion =
            sancionService.obtenerVigente(idArtista);

        if (sancion.isPresent()) {
            SancionCuenta actual = sancion.get();
            boolean apelacion = actual.getIdSancion() != null
                && apelacionSancionRepository
                    .findFirstBySancionIdSancionAndEstado(
                        actual.getIdSancion(), EstadoApelacion.PENDIENTE)
                    .isPresent();
            return new Salud(SaludCuentaDTO.builder()
                .estado(estado(actual))
                .idSancion(actual.getIdSancion())
                .motivo(actual.getMotivo())
                .alcance(actual.getAlcance() != null
                    ? actual.getAlcance().name()
                    : null)
                .fechaFin(actual.getFechaFin())
                .fechaEscalada(actual.getFechaEscalada())
                .diasGraciaEntrega(gracia)
                .diasGraciaRestantes(menor(dias))
                .apelacionPendiente(apelacion)
                .pausados(pausados(actual.getAlcanceAfectado()))
                .build(), dias);
        }

        return new Salud(SaludCuentaDTO.builder()
            .estado(dias.isEmpty() ? "AL_DIA" : "EN_GRACIA")
            .diasGraciaEntrega(gracia)
            .diasGraciaRestantes(menor(dias))
            .apelacionPendiente(false)
            .pausados(List.of())
            .build(), dias);
    }

    private int diasGracia() {
        ConfiguracionPedido config = configuracionPedidoService.obtener();
        return config != null && config.getDiasGraciaEntrega() != null
            ? config.getDiasGraciaEntrega()
            : ConfiguracionPedido.DIAS_GRACIA_ENTREGA_POR_DEFECTO;
    }

    private Map<Long, Integer> diasGraciaPorPedido(
            Long idArtista, int gracia) {
        Map<Long, Integer> dias = new LinkedHashMap<>();
        LocalDate hoy = LocalDate.now();
        for (Pedido pedido : pedidoRepository
                .findByArtistaIdUsuarioAndEstadoInOrderByFechaCreacionDesc(
                    idArtista, ESTADOS_ACTIVOS, PageRequest.of(0, MAX_PEDIDOS))) {
            LocalDateTime plazo = entregaPendienteService.plazoVigente(pedido);
            if (plazo == null) {
                continue;
            }
            int transcurridos = (int) ChronoUnit.DAYS.between(
                hoy, plazo.toLocalDate());
            int restantes = gracia + transcurridos;
            if (restantes <= gracia) {
                dias.put(pedido.getIdPedido(), Math.max(0, restantes));
            }
        }
        return dias;
    }

    private String estado(SancionCuenta sancion) {
        if (sancion.getAlcance() == AlcanceSancion.VENTAS) {
            return "VENTAS_SUSPENDIDAS";
        }
        if (sancion.getAlcance() == AlcanceSancion.ENTREGAS) {
            return "LIMITADA_ENTREGAS";
        }
        return "LIMITADA_ACCESO";
    }

    private Integer menor(Map<Long, Integer> dias) {
        return dias.values().stream().min(Integer::compareTo).orElse(null);
    }

    private List<CatalogoPausadoDTO> pausados(String alcance) {
        if (alcance == null || alcance.isBlank()) {
            return List.of();
        }
        List<CatalogoPausadoDTO> items = new ArrayList<>();
        for (String bloque : alcance.split(";")) {
            String[] partes = bloque.split("=", 2);
            if (partes.length != 2) {
                continue;
            }
            String tipo = partes[0];
            for (String idTexto : partes[1].split(",")) {
                Long id = parseId(idTexto);
                if (id == null) {
                    continue;
                }
                switch (tipo) {
                    case "servicios" -> servicioRepository
                        .findByIdServicio(id)
                        .ifPresent(servicio -> items.add(
                            CatalogoPausadoDTO.builder()
                                .tipo("SERVICIO")
                                .id(id)
                                .nombre(texto(servicio.getNombre(),
                                    "Servicio #" + id))
                                .build()));
                    case "productos" -> productoRepository
                        .findById(id)
                        .ifPresent(producto -> items.add(
                            CatalogoPausadoDTO.builder()
                                .tipo("PRODUCTO")
                                .id(id)
                                .nombre(texto(producto.getTitulo(),
                                    "Producto #" + id))
                                .build()));
                    case "reventas" -> publicacionReventaRepository
                        .findById(id)
                        .ifPresent(reventa -> items.add(
                            CatalogoPausadoDTO.builder()
                                .tipo("REVENTA")
                                .id(id)
                                .nombre(nombreReventa(reventa, id))
                                .build()));
                    default -> { }
                }
            }
        }
        return items;
    }

    private Long parseId(String valor) {
        try {
            return Long.valueOf(valor.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String nombreReventa(PublicacionReventa reventa, Long id) {
        if (reventa.getVariante() != null
                && reventa.getVariante().getProducto() != null) {
            return texto(reventa.getVariante().getProducto().getTitulo(),
                "Publicación #" + id);
        }
        return "Publicación #" + id;
    }

    private String texto(String valor, String porDefecto) {
        return valor != null && !valor.isBlank() ? valor : porDefecto;
    }
}
