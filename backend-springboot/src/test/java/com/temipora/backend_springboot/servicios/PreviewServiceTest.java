package com.temipora.backend_springboot.servicios;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.Test;

class PreviewServiceTest {

    private final PreviewService previewService = new PreviewService();

    @Test
    void generar_reduceElLadoMayorA1200() throws Exception {
        byte[] preview = previewService.generar(
            imagen("png", 2000, 1000), "Pedido #1 · 28/10/2026 · v1");

        BufferedImage resultado = ImageIO.read(
            new ByteArrayInputStream(preview));

        assertThat(resultado).isNotNull();
        assertThat(resultado.getWidth()).isEqualTo(1200);
        assertThat(resultado.getHeight()).isEqualTo(600);
    }

    @Test
    void generar_imagenChicaMantieneSusDimensiones() throws Exception {
        byte[] preview = previewService.generar(
            imagen("png", 400, 300), "Pedido #1 · 28/10/2026 · v1");

        BufferedImage resultado = ImageIO.read(
            new ByteArrayInputStream(preview));

        assertThat(resultado.getWidth()).isEqualTo(400);
        assertThat(resultado.getHeight()).isEqualTo(300);
    }

    @Test
    void generar_archivoQueNoEsImagen_lanzaError() {
        assertThatThrownBy(() -> previewService.generar(
            "no soy una imagen".getBytes(), "Pedido #1"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("preview");
    }

    private byte[] imagen(String formato, int ancho, int alto) throws Exception {
        BufferedImage imagen = new BufferedImage(
            ancho, alto, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream salida = new ByteArrayOutputStream();
        ImageIO.write(imagen, formato, salida);
        return salida.toByteArray();
    }
}
