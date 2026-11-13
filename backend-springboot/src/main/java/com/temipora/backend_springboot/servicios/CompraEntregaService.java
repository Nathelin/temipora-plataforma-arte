package com.temipora.backend_springboot.servicios;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.temipora.backend_springboot.modelo.CambioAuditoriaDTO;
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
import com.temipora.backend_springboot.modelo.Moneda;
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
    private final NotificacionService notificacionService;
    private final AuditoriaService auditoria;

    public CompraEntregaService(
            CompraRepository compraRepository,
            CompraGrupoRepository compraGrupoRepository,
            ProductoVarianteRepository varianteRepository,
            ReservaStockRepository reservaStockRepository,
            PublicacionReventaRepository publicacionReventaRepository,
            LedgerService ledgerService,
            NotificacionService notificacionService,
            AuditoriaService auditoria) {
        this.compraRepository = compraRepository;
        this.compraGrupoRepository = compraGrupoRepository;
        this.varianteRepository = varianteRepository;
        this.reservaStockRepository = reservaStockRepository;
        this.publicacionReventaRepository = publicacionReventaRepository;
        this.ledgerService = ledgerService;
        this.notificacionService = notificacionService;
        this.auditoria = auditoria;
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
            EstadoCompraGrupo estadoAnterior = grupo.getEstado();
            List<CambioAuditoriaDTO> itemsAuditados = new ArrayList<>();
            int numeroItem = 1;

            for (CompraItem item : grupo.getItems()) {
                ProductoVariante variante = varianteRepository
                    .findByIdVarianteForUpdate(item.getVariante().getIdVariante())
                    .orElseThrow();
                String vendedorItem = vendedorDeItem(variante);
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
                    notificarReventaAlArtista(variante, vendedor, compra, item);
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

                itemsAuditados.add(new CambioAuditoriaDTO(
                    "item" + numeroItem, "Producto " + numeroItem, null,
                    describirItem(item, vendedorItem)));
                numeroItem++;
            }

            grupo.setEstado(EstadoCompraGrupo.PAGADO);
            grupo.setFechaPago(ahora);
            grupo.setFechaActualizacion(ahora);
            compraGrupoRepository.save(grupo);
            registrarAuditoriaCompra(compra, grupo, estadoAnterior, itemsAuditados);
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

    /**
     * Una compra puede tener un grupo por artista y cada grupo se cobra aparte,
     * así que el evento se registra por grupo pagado. El origen de cada ítem
     * (primera venta o reventa) viaja como dato, no como una acción distinta.
     */
    private void registrarAuditoriaCompra(
            Compra compra,
            CompraGrupo grupo,
            EstadoCompraGrupo estadoAnterior,
            List<CambioAuditoriaDTO> items) {
        String artista = grupo.getArtista() != null
            ? grupo.getArtista().getNombreUsuario() : null;
        int total = grupo.getItems().size();
        String origenes = resumenOrigenes(grupo.getItems());

        String resumen = "Confirmó el pago de la compra #" + compra.getIdCompra()
            + (artista != null ? " de @" + artista : "")
            + " por " + montoTexto(grupo.getSubtotal())
            + " " + codigoMoneda(grupo.getMoneda())
            + " (" + total + (total == 1 ? " ítem" : " ítems")
            + (origenes.isBlank() ? "" : ": " + origenes) + ")";

        List<CambioAuditoriaDTO> cambios = new ArrayList<>();
        cambios.add(new CambioAuditoriaDTO("estado", "Estado",
            estadoAnterior != null ? estadoAnterior.name() : null,
            EstadoCompraGrupo.PAGADO.name()));
        cambios.add(new CambioAuditoriaDTO("monto", "Monto", null,
            montoTexto(grupo.getSubtotal()) + " " + codigoMoneda(grupo.getMoneda())));
        if (artista != null) {
            cambios.add(new CambioAuditoriaDTO("artista", "Artista", null, "@" + artista));
        }
        cambios.add(new CambioAuditoriaDTO("grupo", "Grupo de compra", null,
            "#" + grupo.getIdCompraGrupo()));
        cambios.add(new CambioAuditoriaDTO("items", "Ítems", null, String.valueOf(total)));
        cambios.addAll(items);

        auditoria.registrarCambios(
            AccionesAdmin.COMPRA_CONFIRMAR, "Compra", compra.getIdCompra(),
            resumen, cambios);
    }

    private String resumenOrigenes(List<CompraItem> items) {
        long reventas = items.stream()
            .filter(item -> item.getOrigen() == OrigenCompraItem.REVENTA)
            .count();
        long primeras = items.size() - reventas;
        List<String> partes = new ArrayList<>();
        if (primeras > 0) {
            partes.add(primeras + (primeras == 1 ? " primera venta" : " primeras ventas"));
        }
        if (reventas > 0) {
            partes.add(reventas + (reventas == 1 ? " reventa" : " reventas"));
        }
        return String.join(", ", partes);
    }

    private String describirItem(CompraItem item, String vendedor) {
        StringBuilder texto = new StringBuilder()
            .append("«").append(item.getTituloProducto()).append("»");
        if (item.getNombreVariante() != null) {
            texto.append(" · variante «").append(item.getNombreVariante()).append("»");
        }
        texto.append(" · ").append(montoTexto(item.getPrecioCobro()))
            .append(" ").append(codigoMoneda(item.getMonedaOriginal()));
        if (item.getCantidad() > 1) {
            texto.append(" × ").append(item.getCantidad());
        }
        if (item.getOrigen() == OrigenCompraItem.REVENTA) {
            texto.append(" · reventa");
            if (vendedor != null) {
                texto.append(" (vendida por @").append(vendedor).append(")");
            }
        } else {
            texto.append(" · primera venta");
        }
        return texto.toString();
    }

    private String vendedorDeItem(ProductoVariante variante) {
        if (variante.getPropietarioActual() != null
                && variante.getPropietarioActual().getNombreUsuario() != null) {
            return variante.getPropietarioActual().getNombreUsuario();
        }
        if (variante.getProducto() != null
                && variante.getProducto().getArtista() != null) {
            return variante.getProducto().getArtista().getNombreUsuario();
        }
        return null;
    }

    private String montoTexto(BigDecimal valor) {
        return valor != null
            ? valor.setScale(2, RoundingMode.HALF_UP).toPlainString()
            : "—";
    }

    private String codigoMoneda(Moneda moneda) {
        return moneda != null && moneda.getCodigo() != null
            ? moneda.getCodigo() : "";
    }

    private void notificarReventaAlArtista(
            ProductoVariante variante,
            Usuario vendedor,
            Compra compra,
            CompraItem item) {
        if (variante.getProducto() == null
                || variante.getProducto().getArtista() == null
                || compra.getCliente() == null) {
            return;
        }
        Usuario artista = variante.getProducto().getArtista();
        Long idArtista = artista.getIdUsuario();
        if (idArtista.equals(compra.getCliente().getIdUsuario())
                || (vendedor != null
                    && idArtista.equals(vendedor.getIdUsuario()))) {
            return;
        }

        String monto = item.getPrecioOriginal() != null
            ? item.getPrecioOriginal().stripTrailingZeros().toPlainString()
            : "";
        String moneda = item.getMonedaOriginal() != null
            && item.getMonedaOriginal().getCodigo() != null
            ? item.getMonedaOriginal().getCodigo()
            : "";
        StringBuilder descripcion = new StringBuilder()
            .append("«").append(variante.getProducto().getTitulo())
            .append("» · ").append(variante.getNombre())
            .append(" se revendió por ").append(monto);
        if (!moneda.isBlank()) {
            descripcion.append(" ").append(moneda);
        }
        descripcion.append(".");
        if (vendedor != null) {
            descripcion.append(" Vendedor: @")
                .append(vendedor.getNombreUsuario()).append(".");
        }

        notificacionService.crearParaUsuario(
            idArtista,
            "REVENTA_PRODUCTO",
            "Tu obra fue revendida",
            descripcion.toString(),
            variante.getProducto().getIdProducto(),
            "PRODUCTO");
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
