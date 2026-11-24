package com.temipora.backend_springboot.servicios;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.temipora.backend_springboot.modelo.AlcanceSancion;
import com.temipora.backend_springboot.modelo.CatalogoPausadoDTO;
import com.temipora.backend_springboot.modelo.EstadoApelacion;
import com.temipora.backend_springboot.modelo.PublicacionReventa;
import com.temipora.backend_springboot.modelo.SancionCuenta;
import com.temipora.backend_springboot.modelo.SaludCuentaDTO;
import com.temipora.backend_springboot.repositorios.ApelacionSancionRepository;
import com.temipora.backend_springboot.repositorios.ProductoRepository;
import com.temipora.backend_springboot.repositorios.PublicacionReventaRepository;
import com.temipora.backend_springboot.repositorios.ServicioRepository;

/**
 * Calcula la salud de la cuenta del artista para el panel: estado de la
 * sanción, catálogo pausado y si tiene una apelación pendiente.
 */
@Service
public class SaludCuentaService {

    private final SancionService sancionService;
    private final ApelacionSancionRepository apelacionSancionRepository;
    private final ServicioRepository servicioRepository;
    private final ProductoRepository productoRepository;
    private final PublicacionReventaRepository publicacionReventaRepository;

    public SaludCuentaService(
            SancionService sancionService,
            ApelacionSancionRepository apelacionSancionRepository,
            ServicioRepository servicioRepository,
            ProductoRepository productoRepository,
            PublicacionReventaRepository publicacionReventaRepository) {
        this.sancionService = sancionService;
        this.apelacionSancionRepository = apelacionSancionRepository;
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
                .diasGraciaEntrega(null)
                .diasGraciaRestantes(null)
                .apelacionPendiente(apelacion)
                .pausados(pausados(actual.getAlcanceAfectado()))
                .build(), Map.of());
        }

        return new Salud(SaludCuentaDTO.builder()
            .estado("AL_DIA")
            .diasGraciaEntrega(null)
            .diasGraciaRestantes(null)
            .apelacionPendiente(false)
            .pausados(List.of())
            .build(), Map.of());
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
