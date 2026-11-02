package com.temipora.backend_springboot.servicios;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class EmailPlantillaService {

    @Value("${temipora.mail.api-url:http://localhost:8080}")
    private String apiUrl;

    public String reactivacion(
            String nombre,
            List<OfertaEmail> ofertas,
            String tokenBaja) {
        StringBuilder items = new StringBuilder();
        for (OfertaEmail oferta : ofertas) {
            items.append(bloqueOferta(oferta));
        }

        String urlBaja = apiUrl + "/api/emails/baja/" + url(tokenBaja);
        String nombreSeguro = escapar(nombre);

        return """
            <!doctype html>
            <html lang="es">
            <head>
              <meta charset="utf-8">
              <meta name="viewport" content="width=device-width, initial-scale=1">
              <title>Temipora</title>
            </head>
            <body style="margin:0;padding:0;background:#fff6f1;font-family:Arial,Helvetica,sans-serif;color:#4a1d2e;">
              <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" style="background:#fff6f1;">
                <tr>
                  <td align="center" style="padding:24px 12px;">
                    <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" style="max-width:620px;background:#ffffff;border:1px solid #ffc2d1;border-radius:24px;overflow:hidden;">
                      <tr>
                        <td style="background:#4a1d2e;padding:26px 32px;color:#fff6f1;">
                          <div style="font-size:22px;font-weight:800;letter-spacing:-0.02em;">Temipora</div>
                          <div style="font-size:13px;opacity:.78;margin-top:4px;">Comisiones de arte</div>
                        </td>
                      </tr>
                      <tr>
                        <td style="padding:32px;">
                          <h1 style="margin:0 0 12px;font-size:26px;line-height:1.2;color:#4a1d2e;">Hola, %s</h1>
                          <p style="margin:0 0 8px;font-size:15px;line-height:1.6;color:#6e3b4d;">
                            Hace un tiempo no te vemos por Temipora. Preparamos algunas propuestas de artistas
                            que pueden interesarte, con un descuento especial.
                          </p>
                          <p style="margin:0 0 22px;font-size:13px;line-height:1.6;color:#6e3b4d;">
                            El descuento lo aplica Temipora sobre el presupuesto final del servicio.
                          </p>
                          %s
                          <p style="margin:22px 0 0;font-size:12px;line-height:1.6;color:#6e3b4d;">
                            Las ofertas tienen vencimiento. Si ya no te interesan, podés ignorar este correo.
                          </p>
                        </td>
                      </tr>
                      <tr>
                        <td style="padding:18px 32px;background:#fff0e9;font-size:12px;line-height:1.6;color:#6e3b4d;">
                          Recibís este correo porque tenés una cuenta en Temipora.
                          <a href="%s" style="color:#c2185b;font-weight:700;">Darme de baja de las promociones</a>.
                        </td>
                      </tr>
                    </table>
                  </td>
                </tr>
              </table>
            </body>
            </html>
            """.formatted(nombreSeguro, items.toString(), urlBaja);
    }

    public String bajaConfirmada(String nombre) {
        return paginaSimple(
            "Listo",
            "Damos de baja las promociones",
            "Hola, " + escapar(nombre) + ". No vas a recibir más correos promocionales de Temipora. "
                + "Vas a seguir recibiendo los avisos importantes de tus pedidos. "
                + "Podés volver a activarlos desde Configuración → Privacidad y contenido.");
    }

    public String bajaInvalida() {
        return paginaSimple(
            "Enlace no válido",
            "No pudimos procesar la baja",
            "El enlace de baja no es válido o ya fue utilizado. Si querés dejar de recibir "
                + "promociones, escribinos desde tu cuenta.");
    }

    private String bloqueOferta(OfertaEmail oferta) {
        String imagen = oferta.imagen() != null && !oferta.imagen().isBlank()
            ? "<img src=\"" + escapar(urlImagen(oferta.imagen()))
                + "\" alt=\"\" width=\"120\" style=\"display:block;width:120px;height:120px;object-fit:cover;border-radius:14px;\">"
            : "<div style=\"width:120px;height:120px;border-radius:14px;background:#ffc2d1;color:#4a1d2e;font-size:28px;font-weight:800;text-align:center;line-height:120px;\">"
                + escapar(inicial(oferta.nombreArtista())) + "</div>";

        String motivo = oferta.motivo() != null && !oferta.motivo().isBlank()
            ? "<div style=\"margin-top:6px;font-size:12px;color:#6e3b4d;\">" + escapar(oferta.motivo()) + "</div>"
            : "";

        return """
            <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" style="margin:14px 0;border:1px solid #ffc2d1;border-radius:18px;">
              <tr>
                <td width="132" style="padding:12px;vertical-align:top;">%s</td>
                <td style="padding:12px 12px 12px 0;vertical-align:top;">
                  <div style="font-size:16px;font-weight:800;color:#4a1d2e;">%s</div>
                  <div style="font-size:13px;color:#6e3b4d;">%s</div>
                  <div style="display:inline-block;margin-top:8px;background:#ffe8a3;color:#4a1d2e;border-radius:999px;padding:4px 10px;font-size:12px;font-weight:700;">
                    %s%% de descuento
                  </div>
                  <div style="margin-top:8px;font-size:13px;color:#6e3b4d;">Desde %s %s</div>
                  %s
                  <a href="%s" style="display:inline-block;margin-top:12px;background:#c2185b;color:#ffffff;border-radius:999px;padding:10px 18px;text-decoration:none;font-size:13px;font-weight:700;">
                    Ver y solicitar
                  </a>
                </td>
              </tr>
            </table>
            """.formatted(
                imagen,
                escapar(oferta.nombreServicio()),
                escapar(oferta.nombreArtista()),
                escapar(porcentaje(oferta)),
                escapar(oferta.precioDesde()),
                escapar(oferta.moneda()),
                motivo,
                escapar(oferta.url()));
    }

    private String paginaSimple(String kicker, String titulo, String texto) {
        return """
            <!doctype html>
            <html lang="es">
            <head><meta charset="utf-8"><meta name="viewport" content="width=device-width, initial-scale=1"><title>%s</title></head>
            <body style="margin:0;background:#fff6f1;font-family:Arial,Helvetica,sans-serif;color:#4a1d2e;">
              <div style="max-width:560px;margin:64px auto;background:#ffffff;border:1px solid #ffc2d1;border-radius:24px;padding:32px;">
                <div style="font-size:12px;font-weight:700;letter-spacing:.12em;text-transform:uppercase;color:#c2185b;">%s</div>
                <h1 style="margin:10px 0 12px;font-size:26px;line-height:1.2;">%s</h1>
                <p style="margin:0;font-size:15px;line-height:1.6;color:#6e3b4d;">%s</p>
              </div>
            </body>
            </html>
            """.formatted(escapar(titulo), escapar(kicker), escapar(titulo), escapar(texto));
    }

    private String porcentaje(OfertaEmail oferta) {
        if (oferta.porcentajeDescuento() == null) {
            return "0";
        }
        return oferta.porcentajeDescuento()
            .stripTrailingZeros()
            .toPlainString();
    }

    private String inicial(String nombre) {
        return nombre == null || nombre.isBlank()
            ? "?"
            : nombre.substring(0, 1).toUpperCase();
    }

    private String urlImagen(String imagen) {
        if (imagen.startsWith("http://") || imagen.startsWith("https://")) {
            return imagen;
        }
        return apiUrl + (imagen.startsWith("/") ? imagen : "/" + imagen);
    }

    private String url(String valor) {
        return valor == null ? "" : valor;
    }

    private String escapar(String valor) {
        if (valor == null) {
            return "";
        }
        return valor
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&#39;");
    }
}
