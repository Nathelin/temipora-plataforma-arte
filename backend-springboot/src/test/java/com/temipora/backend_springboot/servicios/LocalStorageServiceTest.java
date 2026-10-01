package com.temipora.backend_springboot.servicios;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

class LocalStorageServiceTest {

    @TempDir
    Path directorio;

    @Test
    void guardar_archivoNulo_devuelveNull() {
        assertThat(servicio().guardar(null)).isNull();
    }

    @Test
    void guardar_archivoVacio_devuelveNull() {
        MockMultipartFile vacio = new MockMultipartFile(
                "archivo", "foto.png", "image/png", new byte[0]);

        assertThat(servicio().guardar(vacio)).isNull();
    }

    @Test
    void guardar_pngValido_convierteAWebpYDevuelveRuta() throws IOException {
        MockMultipartFile png = archivo("foto.png", imagenBytes("png"));

        String ruta = servicio().guardar(png);

        assertThat(ruta).matches("/uploads/[0-9a-fA-F-]+\\.webp");
        Path guardado = directorio.resolve(ruta.substring("/uploads/".length()));
        assertThat(Files.exists(guardado)).isTrue();
        assertThat(Files.size(guardado)).isPositive();
    }

    @Test
    void guardarJpgValido_convierteAWebpYDevuelveRuta() throws IOException {
        MockMultipartFile jpg = archivo("foto.jpg", imagenBytes("jpg"));

        String ruta = servicio().guardar(jpg);

        assertThat(ruta).endsWith(".webp");
        Path guardado = directorio.resolve(ruta.substring("/uploads/".length()));
        assertThat(Files.exists(guardado)).isTrue();
        assertThat(Files.size(guardado)).isPositive();
    }

    @Test
    void guardarTextoPlano_lanzaFormatoInvalido() {
        MockMultipartFile texto = archivo("nota.txt",
                "hola mundo".getBytes(StandardCharsets.UTF_8));

        assertThatThrownBy(() -> servicio().guardar(texto))
                .isInstanceOf(FormatoArchivoInvalidoException.class);
    }

    @Test
    void guardarGif_lanzaFormatoInvalidoPorFormatoNoPermitido() throws IOException {
        MockMultipartFile gif = archivo("anim.gif", imagenBytes("gif"));

        assertThatThrownBy(() -> servicio().guardar(gif))
                .isInstanceOf(FormatoArchivoInvalidoException.class);
    }

    // ---------------------------------------------------------------
    // helpers
    // ---------------------------------------------------------------

    private LocalStorageService servicio() {
        return new LocalStorageService(directorio.toString());
    }

    private MockMultipartFile archivo(String nombre, byte[] bytes) {
        return new MockMultipartFile("archivo", nombre, "application/octet-stream", bytes);
    }

    private byte[] imagenBytes(String formato) throws IOException {
        BufferedImage imagen = new BufferedImage(8, 8, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream salida = new ByteArrayOutputStream();
        ImageIO.write(imagen, formato, salida);
        return salida.toByteArray();
    }
}
