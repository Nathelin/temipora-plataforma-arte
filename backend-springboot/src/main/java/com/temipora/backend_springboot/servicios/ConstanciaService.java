package com.temipora.backend_springboot.servicios;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import com.temipora.backend_springboot.modelo.EntregaArchivoDTO;
import com.temipora.backend_springboot.modelo.EntregaHitoDTO;
import com.temipora.backend_springboot.modelo.HitoPedidoDTO;
import com.temipora.backend_springboot.modelo.PagoDTO;
import com.temipora.backend_springboot.modelo.PedidoDTO;
import com.temipora.backend_springboot.modelo.UsuarioResumenDTO;
import com.temipora.backend_springboot.servicios.informes.DocumentoPdf;
import com.temipora.backend_springboot.servicios.informes.FormatoPdf;
import com.temipora.backend_springboot.servicios.informes.TablaPdf;

@Service
public class ConstanciaService {

    private static final String TIPO = "Constancia del pedido";
    private static final String PIE =
        "Documento contractual del pedido · generado automáticamente por "
            + "Temipora";

    public byte[] generar(PedidoDTO pedido) {
        String referencia = "Pedido #" + pedido.getIdPedido();
        String moneda = FormatoPdf.moneda(pedido.getMonedaMostrada());

        DocumentoPdf documento = DocumentoPdf.vertical(TIPO, referencia, PIE);
        documento.abrir();
        documento.cabecera(referencia,
            "Refleja el estado del pedido al momento de generarse.");
        documento.metadatos(
            "Cliente", "@" + usuario(pedido.getCliente()),
            "Artista", "@" + usuario(pedido.getArtista()),
            "Servicio", FormatoPdf.texto(pedido.getNombreServicio()),
            "Estado", FormatoPdf.texto(pedido.getEstado()),
            "Inicio", FormatoPdf.fecha(pedido.getFechaInicio()),
            "Límite de entrega", FormatoPdf.fecha(pedido.getLimiteEntrega()),
            "Finalizado", FormatoPdf.fechaHora(pedido.getFinalizadoEn()),
            "Términos de la plataforma", "versión "
                + FormatoPdf.numero(pedido.getTerminosPlataformaVersion()),
            "Moneda", moneda,
            "Generado", FormatoPdf.fechaHora(LocalDateTime.now()));

        importes(documento, pedido, moneda);
        planDePagos(documento, pedido, moneda);
        entregas(documento, pedido);
        pagos(documento, pedido);

        return documento.cerrar();
    }

    private void importes(
            DocumentoPdf documento, PedidoDTO pedido, String moneda) {
        documento.seccion("1", "Importes");

        List<String> conceptos = new ArrayList<>();
        List<String> valores = new ArrayList<>();
        int destacada;

        if (pedido.getComisionMonto() == null) {
            conceptos.add("Total a pagar");
            valores.add(FormatoPdf.monto(
                pedido.getPrecioFinalClienteMostrado()));
            destacada = 0;
            if (pedido.getDescuentoMostrado() != null
                    && pedido.getDescuentoMostrado().signum() > 0) {
                conceptos.add("Precio original");
                valores.add(FormatoPdf.monto(
                    pedido.getPrecioFinalMostrado()));
                conceptos.add("Descuento de reactivación");
                valores.add(FormatoPdf.monto(pedido.getDescuentoMostrado()));
            }
        } else {
            conceptos.add("Precio del pedido");
            valores.add(FormatoPdf.monto(pedido.getPrecioFinalMostrado()));
            conceptos.add("Comisión Temipora ("
                + FormatoPdf.numero(pedido.getComisionPorcentaje()) + "%)");
            valores.add(FormatoPdf.monto(pedido.getComisionMontoMostrado()));
            conceptos.add("Tu neto");
            valores.add(FormatoPdf.monto(
                pedido.getMontoNetoArtistaMostrado()));
            destacada = 2;
            if (pedido.getArancelMpEstimadoMostrado() != null) {
                conceptos.add("Arancel MercadoPago (estimado)");
                valores.add(FormatoPdf.monto(
                    pedido.getArancelMpEstimadoMostrado()));
                conceptos.add("Recibís (estimado)");
                valores.add(FormatoPdf.monto(
                    pedido.getMontoNetoArtistaConArancelMostrado()));
            }
            if (pedido.getDescuentoMostrado() != null
                    && pedido.getDescuentoMostrado().signum() > 0) {
                conceptos.add(
                    "Descuento del cliente (lo financia Temipora)");
                valores.add(FormatoPdf.monto(pedido.getDescuentoMostrado()));
            }
        }

        documento.resumen(
            conceptos.toArray(String[]::new),
            valores.toArray(String[]::new),
            destacada, "Concepto", "Importe (" + moneda + ")");
    }

    private void planDePagos(
            DocumentoPdf documento, PedidoDTO pedido, String moneda) {
        documento.seccion("2", "Plan de pagos", "importes en " + moneda);

        List<HitoPedidoDTO> hitos = pedido.getHitos() != null
            ? pedido.getHitos() : List.of();
        if (hitos.isEmpty()) {
            documento.parrafo(
                "El pedido no tiene hitos de pago registrados.");
            return;
        }

        TablaPdf tabla = new TablaPdf(
            new String[] { "#", "Hito", "Alcance", "Tipo", "Monto", "Pago",
                "Entrega" },
            new float[] { 0.35f, 1.6f, 3.1f, 0.9f, 1.2f, 1.05f, 1.15f },
            new boolean[] { true, false, false, false, true, false, false });

        for (HitoPedidoDTO hito : hitos) {
            tabla.fila(
                String.valueOf(
                    (hito.getOrden() == null ? 0 : hito.getOrden()) + 1),
                FormatoPdf.texto(hito.getNombre()),
                FormatoPdf.texto(hito.getDescripcion()),
                FormatoPdf.texto(hito.getTipo()),
                FormatoPdf.monto(hito.getMontoMostrado()),
                FormatoPdf.texto(hito.getEstadoPago()),
                FormatoPdf.texto(hito.getEstadoEntrega()));
        }
        documento.tabla(tabla);
    }

    private void entregas(DocumentoPdf documento, PedidoDTO pedido) {
        documento.seccion("3", "Entregas");

        List<HitoPedidoDTO> hitos = pedido.getHitos() != null
            ? pedido.getHitos() : List.of();
        boolean hayEntregas = false;

        for (HitoPedidoDTO hito : hitos) {
            if (hito.getEntregas() == null || hito.getEntregas().isEmpty()) {
                continue;
            }
            hayEntregas = true;
            documento.subtitulo(
                "Hito: " + FormatoPdf.texto(hito.getNombre()));

            for (EntregaHitoDTO entrega : hito.getEntregas()) {
                String estado = entrega.isVigente()
                    ? "Vigente" : "Reemplazada";
                if (entrega.isAprobada()) {
                    estado += " · Aprobada";
                }
                documento.parrafoChico("Entrega v" + entrega.getNumero()
                    + " — " + FormatoPdf.fechaHora(entrega.getFechaEntrega())
                    + " — @" + usuario(entrega.getAutor())
                    + " — " + estado);

                if (entrega.getDescripcion() != null) {
                    documento.parrafoChico("   " + entrega.getDescripcion());
                }
                if (entrega.getRevisionMotivo() != null) {
                    documento.parrafoChico("   Revisión solicitada: "
                        + entrega.getRevisionMotivo() + " ("
                        + FormatoPdf.fechaHora(entrega.getRevisionFecha())
                        + ")");
                }
                for (EntregaArchivoDTO archivo : entrega.getArchivos()) {
                    String dibujo = archivo.getDibujo() != null
                        ? "Dibujo " + ((archivo.getDibujo().getOrden() == null
                            ? 0 : archivo.getDibujo().getOrden()) + 1) + " — "
                        : "";
                    documento.parrafoChico("   · " + dibujo
                        + FormatoPdf.texto(archivo.getNombre())
                        + " · huella SHA-256: " + FormatoPdf.texto(
                            archivo.getSha256()));
                }
            }
        }

        if (!hayEntregas) {
            documento.parrafo("Todavía no hay entregas registradas.");
        }
    }

    private void pagos(DocumentoPdf documento, PedidoDTO pedido) {
        documento.seccion("4", "Pagos");

        List<HitoPedidoDTO> hitos = pedido.getHitos() != null
            ? pedido.getHitos() : List.of();
        List<String[]> filas = new ArrayList<>();

        for (HitoPedidoDTO hito : hitos) {
            if (hito.getPagos() == null) {
                continue;
            }
            for (PagoDTO pago : hito.getPagos()) {
                filas.add(new String[] {
                    FormatoPdf.texto(hito.getNombre()),
                    "#" + pago.getIdPago(),
                    FormatoPdf.texto(pago.getEstado()),
                    montoConMoneda(pago.getMonto(), pago.getMoneda()),
                    FormatoPdf.fechaHora(
                        pago.getFechaAcreditacion() != null
                            ? pago.getFechaAcreditacion()
                            : pago.getFechaCreacion()) });
            }
        }

        if (filas.isEmpty()) {
            documento.parrafo("Todavía no hay pagos registrados.");
            return;
        }

        TablaPdf tabla = new TablaPdf(
            new String[] { "Hito", "Pago", "Estado", "Monto", "Fecha" },
            new float[] { 1.7f, 0.7f, 1.1f, 1.4f, 1.5f },
            new boolean[] { false, true, false, true, true });
        for (String[] fila : filas) {
            tabla.fila(fila);
        }
        documento.tabla(tabla);
    }

    private String usuario(UsuarioResumenDTO usuario) {
        return usuario == null || usuario.getNombreUsuario() == null
            ? "—" : usuario.getNombreUsuario();
    }

    private static String montoConMoneda(BigDecimal valor, String moneda) {
        if (valor == null) {
            return "—";
        }
        String codigo = FormatoPdf.moneda(moneda);
        return "—".equals(codigo)
            ? FormatoPdf.monto(valor)
            : FormatoPdf.monto(valor) + " " + codigo;
    }
}
