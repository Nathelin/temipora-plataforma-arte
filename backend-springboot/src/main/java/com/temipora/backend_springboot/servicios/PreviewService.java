package com.temipora.backend_springboot.servicios;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import javax.imageio.ImageIO;

import org.springframework.stereotype.Service;

/**
 * Genera la versión protegida de un entregable: reduce el lado mayor,
 * marca la imagen con pedido, fecha y versión, y descarta los metadatos
 * originales al re-encodear.
 */
@Service
public class PreviewService {

    static final int MAX_LADO = 1200;
    private static final float OPACIDAD = 0.22f;

    public byte[] generar(byte[] original, String marca) {
        if (original == null || original.length == 0) {
            throw new IllegalArgumentException("La imagen está vacía.");
        }

        BufferedImage fuente;
        try {
            fuente = ImageIO.read(new ByteArrayInputStream(original));
        } catch (IOException e) {
            throw new IllegalArgumentException(
                "No se pudo leer la imagen para generar la preview.", e);
        }
        if (fuente == null) {
            throw new IllegalArgumentException(
                "No se pudo leer la imagen para generar la preview.");
        }

        BufferedImage preview = escalar(fuente);
        dibujarMarca(preview, marca);

        try (ByteArrayOutputStream salida = new ByteArrayOutputStream()) {
            if (!ImageIO.write(preview, "webp", salida)) {
                throw new IllegalStateException(
                    "No hay un codificador WebP disponible.");
            }
            return salida.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException(
                "No se pudo generar la preview.", e);
        }
    }

    private BufferedImage escalar(BufferedImage fuente) {
        int ancho = fuente.getWidth();
        int alto = fuente.getHeight();
        int ladoMayor = Math.max(ancho, alto);
        if (ladoMayor <= MAX_LADO) {
            return copiar(fuente, ancho, alto);
        }

        double escala = (double) MAX_LADO / ladoMayor;
        int nuevoAncho = Math.max(1, (int) Math.round(ancho * escala));
        int nuevoAlto = Math.max(1, (int) Math.round(alto * escala));
        BufferedImage reducida = nuevaImagen(
            nuevoAncho, nuevoAlto, fuente);
        Graphics2D grafico = reducida.createGraphics();
        try {
            grafico.setRenderingHint(
                RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            grafico.setRenderingHint(
                RenderingHints.KEY_RENDERING,
                RenderingHints.VALUE_RENDER_QUALITY);
            grafico.drawImage(fuente, 0, 0, nuevoAncho, nuevoAlto, null);
        } finally {
            grafico.dispose();
        }
        return reducida;
    }

    private BufferedImage copiar(
            BufferedImage fuente, int ancho, int alto) {
        BufferedImage copia = nuevaImagen(ancho, alto, fuente);
        Graphics2D grafico = copia.createGraphics();
        try {
            grafico.drawImage(fuente, 0, 0, null);
        } finally {
            grafico.dispose();
        }
        return copia;
    }

    private BufferedImage nuevaImagen(
            int ancho, int alto, BufferedImage fuente) {
        int tipo = fuente.getColorModel().hasAlpha()
            ? BufferedImage.TYPE_INT_ARGB
            : BufferedImage.TYPE_INT_RGB;
        return new BufferedImage(ancho, alto, tipo);
    }

    private void dibujarMarca(BufferedImage imagen, String marca) {
        if (marca == null || marca.isBlank()) {
            return;
        }

        Graphics2D grafico = imagen.createGraphics();
        try {
            grafico.setRenderingHint(
                RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);
            grafico.setRenderingHint(
                RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            int base = Math.max(14,
                Math.min(imagen.getWidth(), imagen.getHeight()) / 16);
            grafico.setFont(new Font(Font.SANS_SERIF, Font.BOLD, base));
            FontMetrics metricas = grafico.getFontMetrics();
            int anchoTexto = metricas.stringWidth(marca);
            int altoTexto = metricas.getHeight();
            int pasoX = Math.max(anchoTexto + base * 3, 260);
            int pasoY = Math.max(altoTexto + base * 4, 140);

            grafico.setComposite(AlphaComposite.getInstance(
                AlphaComposite.SRC_OVER, OPACIDAD));
            grafico.rotate(Math.toRadians(-30));

            int limiteX = imagen.getWidth() * 2;
            int limiteY = imagen.getHeight() * 2;
            for (int y = -imagen.getHeight(); y < limiteY; y += pasoY) {
                for (int x = -imagen.getWidth(); x < limiteX; x += pasoX) {
                    grafico.setColor(new Color(0, 0, 0, 140));
                    grafico.drawString(marca, x + 1, y + 1);
                    grafico.setColor(Color.WHITE);
                    grafico.drawString(marca, x, y);
                }
            }
        } finally {
            grafico.dispose();
        }
    }
}
