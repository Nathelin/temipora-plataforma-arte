package com.temipora.backend_springboot.servicios;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
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

    private final String urlFrontend;

    public ConstanciaService(
            @Value("${app.frontend.url:}") String urlFrontend) {
        this.urlFrontend = urlFrontend == null
            ? ""
            : urlFrontend.replaceAll("/+$", "");
    }

    public byte[] generar(PedidoDTO pedido) {
        String referencia = "Pedido #" + pedido.getIdPedido();
        String moneda = FormatoPdf.moneda(pedido.getMonedaMostrada());

        DocumentoPdf documento = DocumentoPdf.vertical(TIPO, referencia, PIE);
        documento.abrir();
        documento.cabecera("Documento contractual", referencia,
            "Estado del pedido al momento de generarse · Cifras en " + moneda);
        documento.metadatos(
            "Cliente", "@" + usuario(pedido.getCliente()),
            "Artista", "@" + usuario(pedido.getArtista()),
            "Servicio", FormatoPdf.texto(pedido.getNombreServicio()),
            "Estado", estadoPedido(pedido.getEstado()),
            "Inicio", FormatoPdf.fecha(pedido.getFechaInicio()),
            "Límite de entrega", FormatoPdf.fecha(pedido.getLimiteEntrega()),
            "Finalizado", FormatoPdf.fechaHora(pedido.getFinalizadoEn()),
            "Moneda", moneda,
            "Generado", FormatoPdf.fechaHora(LocalDateTime.now()));

        importes(documento, pedido);
        planDePagos(documento, pedido);
        entregas(documento, pedido);
        pagos(documento, pedido);
        terminos(documento, pedido);

        return documento.cerrar();
    }

    private void importes(DocumentoPdf documento, PedidoDTO pedido) {
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
            destacada);
    }

    private void planDePagos(DocumentoPdf documento, PedidoDTO pedido) {
        documento.seccion("2", "Plan de pagos");

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
            new float[] { 0.35f, 1.6f, 2.55f, 0.95f, 1.2f, 1.3f, 1.4f },
            new boolean[] { true, false, false, false, true, false, false });

        for (HitoPedidoDTO hito : hitos) {
            tabla.fila(
                String.valueOf(
                    (hito.getOrden() == null ? 0 : hito.getOrden()) + 1),
                FormatoPdf.texto(hito.getNombre()),
                FormatoPdf.texto(hito.getDescripcion()),
                tipoHito(hito.getTipo()),
                FormatoPdf.monto(hito.getMontoMostrado()),
                estadoPago(hito.getEstadoPago()),
                estadoEntrega(hito.getEstadoEntrega()));
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
                    estadoPago(pago.getEstado()),
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

    private static String estadoPedido(String valor) {
        if (valor == null || valor.isBlank()) {
            return "—";
        }
        return switch (valor) {
            case "PENDIENTE_PAGO" -> "Pendiente de pago";
            case "EN_CURSO" -> "En curso";
            case "FINALIZADO" -> "Finalizado";
            case "CANCELADO" -> "Cancelado";
            case "IMPAGO" -> "Impago";
            default -> valor;
        };
    }

    private static String tipoHito(String valor) {
        if (valor == null || valor.isBlank()) {
            return "—";
        }
        return switch (valor) {
            case "SENA" -> "Seña";
            case "CUOTA" -> "Cuota";
            case "ENTREGA" -> "Entrega";
            default -> valor;
        };
    }

    private static String estadoEntrega(String valor) {
        if (valor == null || valor.isBlank()) {
            return "—";
        }
        return switch (valor) {
            case "NO_INICIADO" -> "No iniciado";
            case "EN_PROCESO" -> "En proceso";
            case "ENTREGADO" -> "Entregado";
            case "APROBADO" -> "Aprobado";
            case "APROBADO_AUTOMATICAMENTE" -> "Aprobado automáticamente";
            case "CON_REVISION" -> "Con revisión";
            default -> valor;
        };
    }

    private void terminos(DocumentoPdf documento, PedidoDTO pedido) {
        documento.seccion("5", "Términos aceptados");

        TablaPdf tabla = new TablaPdf(
            new String[] { "Documento", "Versión", "Alcance", "Aceptado",
                "Huella" },
            new float[] { 1.2f, 0.6f, 1.9f, 1.3f, 1.4f },
            new boolean[] { false, true, false, false, false });

        tabla.fila(
            "Plataforma",
            version(pedido.getTerminosPlataformaVersion()),
            "Uso de la plataforma",
            FormatoPdf.fechaHora(pedido.getTerminosPlataformaAceptadosEn()),
            huellaCorta(pedido.getTerminosPlataformaHuella()));

        boolean hayTerminosDelArtista =
            pedido.getTerminosArtistaVersion() != null;
        if (hayTerminosDelArtista) {
            tabla.fila(
                "Del artista",
                version(pedido.getTerminosArtistaVersion()),
                pedido.isTerminosArtistaDelServicio()
                    ? "Del servicio «"
                        + FormatoPdf.texto(pedido.getNombreServicio()) + "»"
                    : "Generales del artista",
                FormatoPdf.fechaHora(pedido.getTerminosArtistaAceptadosEn()),
                huellaCorta(pedido.getTerminosArtistaHuella()));
        }
        documento.tabla(tabla);

        documento.parrafoChico(
            "La huella identifica el texto exacto de los términos aceptados: "
                + "si se modifican después, la de este pedido no cambia. Los "
                + "términos del artista se aceptaron al enviar la solicitud.");
        if (pedido.getTerminosPlataformaHuella() != null) {
            documento.parrafoChico("Plataforma · huella SHA-256 completa: "
                + pedido.getTerminosPlataformaHuella());
        }
        if (hayTerminosDelArtista && pedido.getTerminosArtistaHuella() != null) {
            documento.parrafoChico("Artista · huella SHA-256 completa: "
                + pedido.getTerminosArtistaHuella());
            if (!urlFrontend.isBlank()) {
                documento.parrafoChico(
                    "Verificación pública del texto aceptado: "
                        + urlFrontend + "/terminos-aceptados/"
                        + CodigoTerminos.codificar(pedido.getIdSolicitud(),
                            pedido.getTerminosArtistaHuella()));
            }
        }
    }

    private static String version(Integer valor) {
        return valor == null ? "—" : "v" + valor;
    }

    private static String huellaCorta(String huella) {
        if (huella == null || huella.isBlank()) {
            return "—";
        }
        return huella.length() <= 16 ? huella : huella.substring(0, 16) + "…";
    }

    private static String estadoPago(String valor) {
        if (valor == null || valor.isBlank()) {
            return "—";
        }
        return switch (valor) {
            case "PENDIENTE" -> "Pendiente";
            case "PAGADO" -> "Pagado";
            case "APROBADO" -> "Aprobado";
            case "RECHAZADO" -> "Rechazado";
            case "REEMBOLSADO" -> "Reembolsado";
            case "VENCIDO" -> "Vencido";
            case "CANCELADO" -> "Cancelado";
            default -> valor;
        };
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
