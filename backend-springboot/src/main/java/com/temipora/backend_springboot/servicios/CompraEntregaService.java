package com.temipora.backend_springboot.servicios;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.HexFormat;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.temipora.backend_springboot.modelo.Compra;
import com.temipora.backend_springboot.modelo.CompraGrupo;
import com.temipora.backend_springboot.modelo.CompraItem;
import com.temipora.backend_springboot.modelo.EstadoCompra;
import com.temipora.backend_springboot.modelo.EstadoCompraGrupo;
import com.temipora.backend_springboot.modelo.EstadoEntregaItem;
import com.temipora.backend_springboot.modelo.EstadoIntentoCobro;
import com.temipora.backend_springboot.modelo.EstadoPublicacionReventa;
import com.temipora.backend_springboot.modelo.EstadoReserva;
import com.temipora.backend_springboot.modelo.EstadoVariante;
import com.temipora.backend_springboot.modelo.IntentoCobro;
import com.temipora.backend_springboot.modelo.IntentoCobroGrupo;
import com.temipora.backend_springboot.modelo.OrigenCompraItem;
import com.temipora.backend_springboot.modelo.ProductoVariante;
import com.temipora.backend_springboot.modelo.PublicacionReventa;
import com.temipora.backend_springboot.modelo.ReservaStock;
import com.temipora.backend_springboot.modelo.TipoTopeReventa;
import com.temipora.backend_springboot.modelo.Usuario;
import com.temipora.backend_springboot.repositorios.CompraGrupoRepository;
import com.temipora.backend_springboot.repositorios.CompraRepository;
import com.temipora.backend_springboot.repositorios.ProductoVarianteRepository;
import com.temipora.backend_springboot.repositorios.PublicacionReventaRepository;
import com.temipora.backend_springboot.repositorios.ReservaStockRepository;

@Service
public class CompraEntregaService {

    private final CompraRepository compraRepository;
    private final CompraGrupoRepository compraGrupoRepository;
    private final ProductoVarianteRepository varianteRepository;
    private final ReservaStockRepository reservaStockRepository;
    private final PublicacionReventaRepository publicacionReventaRepository;
    private final LedgerService ledgerService;

    public CompraEntregaService(
            CompraRepository compraRepository,
            CompraGrupoRepository compraGrupoRepository,
            ProductoVarianteRepository varianteRepository,
            ReservaStockRepository reservaStockRepository,
            PublicacionReventaRepository publicacionReventaRepository,
            LedgerService ledgerService) {
        this.compraRepository = compraRepository;
        this.compraGrupoRepository = compraGrupoRepository;
        this.varianteRepository = varianteRepository;
        this.reservaStockRepository = reservaStockRepository;
        this.publicacionReventaRepository = publicacionReventaRepository;
        this.ledgerService = ledgerService;
    }

    @Transactional
    public void confirmarPago(IntentoCobro intento) {
        Compra compra = intento.getCompra();
        LocalDateTime ahora = LocalDateTime.now();

        for (IntentoCobroGrupo asignacion : intento.getGrupos()) {
            CompraGrupo grupo = compraGrupoRepository
                .findByIdCompraGrupoForUpdate(asignacion.getGrupo().getIdCompraGrupo())
                .orElseThrow();
            asignacion.setEstado(EstadoIntentoCobro.APROBADO);
            if (grupo.getEstado() == EstadoCompraGrupo.PAGADO) {
                continue;
            }

            for (CompraItem item : grupo.getItems()) {
                ProductoVariante variante = varianteRepository
                    .findByIdVarianteForUpdate(item.getVariante().getIdVariante())
                    .orElseThrow();
                confirmarReserva(item, variante);

                if (item.getOrigen() == OrigenCompraItem.REVENTA) {
                    if (item.getIdPublicacionReventa() == null) {
                        throw new ConflictoException(
                            "La compra de reventa no tiene una publicación asociada.");
                    }
                    PublicacionReventa publicacion = publicacionReventaRepository
                        .findByIdPublicacionReventaForUpdate(
                            item.getIdPublicacionReventa())
                        .orElseThrow(() -> new ConflictoException(
                            "La publicación de reventa ya no existe."));
                    if (publicacion.getEstado() == EstadoPublicacionReventa.VENDIDA
                            && (variante.getPropietarioActual() == null
                                || !variante.getPropietarioActual().getIdUsuario()
                                    .equals(compra.getCliente().getIdUsuario()))) {
                        throw new ConflictoException("La pieza ya fue vendida.");
                    }
                    Usuario vendedor = variante.getPropietarioActual();
                    if (vendedor == null && variante.getProducto() != null) {
                        vendedor = variante.getProducto().getArtista();
                    }
                    ledgerService.registrarReventa(
                        variante,
                        vendedor,
                        compra.getCliente(),
                        item.getIdCompraItem(),
                        compra.getIdCompra(),
                        item.getPrecioCobro()
                            .multiply(java.math.BigDecimal.valueOf(item.getCantidad())),
                        grupo.getMoneda(),
                        hashLicencia(item.getLicenciaSnapshot()),
                        publicacion.getIdPublicacionReventa(),
                        publicacion.getCompraItemOrigen() != null
                            ? publicacion.getCompraItemOrigen().getIdCompraItem()
                            : null);
                    variante.setStockDisponible(0);
                    variante.setEstado(EstadoVariante.AGOTADO);
                    variante.setPropietarioActual(compra.getCliente());
                    publicacion.setEstado(EstadoPublicacionReventa.VENDIDA);
                    publicacion.setFechaCierre(ahora);
                    publicacion.setFechaActualizacion(ahora);
                    publicacionReventaRepository.save(publicacion);
                } else if (variante.esUnica()) {
                    Usuario vendedor = variante.getPropietarioActual();
                    if (vendedor == null && variante.getProducto() != null) {
                        vendedor = variante.getProducto().getArtista();
                    }
                    ledgerService.registrarVenta(
                        variante,
                        vendedor,
                        compra.getCliente(),
                        item.getIdCompraItem(),
                        compra.getIdCompra(),
                        item.getPrecioCobro()
                            .multiply(java.math.BigDecimal.valueOf(item.getCantidad())),
                        grupo.getMoneda(),
                        hashLicencia(item.getLicenciaSnapshot()));
                    variante.setStockDisponible(0);
                    variante.setEstado(EstadoVariante.AGOTADO);
                    variante.setPropietarioActual(compra.getCliente());
                    if (variante.getPrecioTopeReventa() == null) {
                        variante.setTipoTopeReventa(TipoTopeReventa.PRECIO_PUBLICACION);
                        variante.setPrecioTopeReventa(item.getPrecioOriginal());
                    }
                } else if (variante.getStockDisponible() != null) {
                    int nuevoStock = variante.getStockDisponible() - item.getCantidad();
                    variante.setStockDisponible(Math.max(0, nuevoStock));
                    if (nuevoStock <= 0) {
                        variante.setEstado(EstadoVariante.AGOTADO);
                    }
                }
                variante.setFechaActualizacion(ahora);
                varianteRepository.save(variante);

                item.setEstadoEntrega(EstadoEntregaItem.ENTREGADO);
                item.setFechaEntrega(ahora);
            }

            grupo.setEstado(EstadoCompraGrupo.PAGADO);
            grupo.setFechaPago(ahora);
            grupo.setFechaActualizacion(ahora);
            compraGrupoRepository.save(grupo);
        }

        long pagados = compra.getGrupos().stream()
            .filter(grupo -> grupo.getEstado() == EstadoCompraGrupo.PAGADO)
            .count();
        if (pagados == compra.getGrupos().size()) {
            compra.setEstado(EstadoCompra.PAGA);
        } else if (pagados > 0) {
            compra.setEstado(EstadoCompra.PARCIALMENTE_PAGA);
        }
        compra.setFechaActualizacion(ahora);
        compraRepository.save(compra);
    }

    private void confirmarReserva(CompraItem item, ProductoVariante variante) {
        ReservaStock reserva = reservaStockRepository
            .findByCompraItemIdCompraItem(item.getIdCompraItem())
            .orElse(null);
        if (reserva != null && reserva.getEstado() == EstadoReserva.ACTIVA) {
            reserva.setEstado(EstadoReserva.CONFIRMADA);
            reserva.setFechaActualizacion(LocalDateTime.now());
            reservaStockRepository.save(reserva);
            return;
        }
        if (reserva != null && reserva.getEstado() == EstadoReserva.CONFIRMADA) {
            return;
        }
        // La reserva venció o se liberó: solo continuamos si la pieza sigue
        // disponible. Si ya no está, no se vende dos veces.
        if (!variante.estaDisponible()) {
            throw new ConflictoException(
                "La pieza “" + variante.getNombre()
                    + "” ya no está disponible para completar la compra.");
        }
        if (variante.getStockDisponible() != null
                && variante.getStockDisponible() < item.getCantidad()) {
            throw new ConflictoException(
                "No hay stock suficiente para “" + variante.getNombre() + "”.");
        }
    }

    private String hashLicencia(String licenciaSnapshot) {
        if (licenciaSnapshot == null) {
            return null;
        }
        try {
            return HexFormat.of().formatHex(
                MessageDigest.getInstance("SHA-256")
                    .digest(licenciaSnapshot.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("SHA-256 no disponible.", e);
        }
    }
}
