package com.temipora.backend_springboot.servicios;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.awt.image.ConvolveOp;
import java.awt.image.Kernel;
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
    private static final float OPACIDAD = 0.38f;

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

        BufferedImage preview = desenfocar(escalar(fuente));
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

    private BufferedImage desenfocar(BufferedImage imagen) {
        int radio = Math.max(3,
            Math.min(imagen.getWidth(), imagen.getHeight()) / 160);
        radio = Math.min(radio, 10);
        int tam = radio * 2 + 1;
        float sigma = Math.max(1f, radio / 2f);
        float[] valores = new float[tam * tam];
        float suma = 0f;

        for (int y = -radio; y <= radio; y++) {
            for (int x = -radio; x <= radio; x++) {
                float valor = (float) Math.exp(
                    -(x * x + y * y) / (2.0 * sigma * sigma));
                valores[(y + radio) * tam + (x + radio)] = valor;
                suma += valor;
            }
        }
        for (int i = 0; i < valores.length; i++) {
            valores[i] /= suma;
        }

        BufferedImage destino = new BufferedImage(
            imagen.getWidth(), imagen.getHeight(), imagen.getType());
        new ConvolveOp(
            new Kernel(tam, tam, valores),
            ConvolveOp.EDGE_NO_OP,
            null).filter(imagen, destino);
        return destino;
    }

    private void dibujarMarca(BufferedImage imagen, String marca) {
        if (marca == null || marca.isBlank()) {
            return;
        }
        String texto = "PREVIEW · " + marca;

        Graphics2D grafico = imagen.createGraphics();
        try {
            grafico.setRenderingHint(
                RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);
            grafico.setRenderingHint(
                RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            int base = Math.max(15,
                Math.min(imagen.getWidth(), imagen.getHeight()) / 14);
            grafico.setFont(new Font(Font.SANS_SERIF, Font.BOLD, base));
            FontMetrics metricas = grafico.getFontMetrics();
            int anchoTexto = metricas.stringWidth(texto);
            int altoTexto = metricas.getHeight();
            int pasoX = Math.max(anchoTexto + base * 3, 280);
            int pasoY = Math.max(altoTexto + base * 4, 150);

            grafico.setComposite(AlphaComposite.getInstance(
                AlphaComposite.SRC_OVER, OPACIDAD));
            grafico.rotate(Math.toRadians(-30));

            int limiteX = imagen.getWidth() * 2;
            int limiteY = imagen.getHeight() * 2;
            for (int y = -imagen.getHeight(); y < limiteY; y += pasoY) {
                for (int x = -imagen.getWidth(); x < limiteX; x += pasoX) {
                    grafico.setColor(new Color(0, 0, 0, 170));
                    grafico.drawString(texto, x + 1, y + 1);
                    grafico.setColor(Color.WHITE);
                    grafico.drawString(texto, x, y);
                }
            }
        } finally {
            grafico.dispose();
        }
    }
}
