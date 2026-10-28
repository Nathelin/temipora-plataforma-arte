package com.temipora.backend_springboot.servicios;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

class AlmacenPrivadoServiceTest {

    @TempDir
    Path directorio;

    @Test
    void guardarOriginal_conservaLosBytesYCalculaHash() throws Exception {
        byte[] original = imagen("png", 40, 30);
        MockMultipartFile archivo = new MockMultipartFile(
            "archivos", "obra.png", "image/png", original);

        var guardado = servicio().guardarOriginal(
            archivo, "pedidos/1/hitos/2/entregas/1");

        assertThat(guardado.clave())
            .startsWith("pedidos/1/hitos/2/entregas/1/")
            .endsWith(".png");
        assertThat(Files.readAllBytes(directorio.resolve(guardado.clave())))
            .isEqualTo(original);
        assertThat(guardado.sha256()).hasSize(64);
        assertThat(guardado.ancho()).isEqualTo(40);
        assertThat(guardado.alto()).isEqualTo(30);
        assertThat(guardado.mime()).isEqualTo("image/png");
        assertThat(guardado.nombreOriginal()).isEqualTo("obra.png");
    }

    @Test
    void guardarTextoPlano_lanzaError() {
        MockMultipartFile texto = new MockMultipartFile(
            "archivos", "nota.txt", "text/plain", "hola".getBytes());

        assertThatThrownBy(() -> servicio().guardarOriginal(texto, "pedidos/1"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("imagen");
    }

    @Test
    void guardarImagenConLadoExcesivo_lanzaError() throws Exception {
        MockMultipartFile archivo = new MockMultipartFile(
            "archivos", "grande.png", "image/png", imagen("png", 12_001, 1));

        assertThatThrownBy(() -> servicio().guardarOriginal(archivo, "pedidos/1"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("12.000");
    }

    @Test
    void guardarArchivoMayorA5MB_lanzaError() {
        MultipartFile grande = mock(MultipartFile.class);
        when(grande.isEmpty()).thenReturn(false);
        when(grande.getSize()).thenReturn(AlmacenPrivadoService.MAX_BYTES + 1);

        assertThatThrownBy(() -> servicio().guardarOriginal(grande, "pedidos/1"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("5 MB");
    }

    @Test
    void guardarPreview_conservaLosBytesEnUnArchivoWebp() {
        byte[] preview = new byte[] { 9, 8, 7, 6 };

        String clave = servicio().guardarPreview(preview, "pedidos/1");

        assertThat(clave).startsWith("pedidos/1/").endsWith(".webp");
        assertThat(servicio().leer(clave)).isEqualTo(preview);
    }

    @Test
    void abrir_claveConTraversal_lanzaError() {
        assertThatThrownBy(() -> servicio().abrir("../secreto.png"))
            .isInstanceOf(IllegalArgumentException.class);
    }

    private AlmacenPrivadoService servicio() {
        return new AlmacenPrivadoService(directorio.toString());
    }

    private byte[] imagen(String formato, int ancho, int alto) throws Exception {
        BufferedImage imagen = new BufferedImage(
            ancho, alto, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream salida = new ByteArrayOutputStream();
        ImageIO.write(imagen, formato, salida);
        return salida.toByteArray();
    }
}
