package com.temipora.backend_springboot.servicios;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Duration;
import java.util.UUID;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

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
    void guardar_archivoMayorA5MB_lanzaError() {
        MultipartFile grande = mock(MultipartFile.class);
        when(grande.isEmpty()).thenReturn(false);
        when(grande.getSize()).thenReturn(
            LocalStorageService.MAX_BYTES_PUBLICO + 1);

        assertThatThrownBy(() -> servicio().guardar(grande))
            .isInstanceOf(FormatoArchivoInvalidoException.class)
            .hasMessageContaining("5 MB");
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

    @Test
    void limitarDimensiones_imagenGrande_reduceLadoMayorA2048() {
        BufferedImage grande = new BufferedImage(3000, 1500, BufferedImage.TYPE_INT_RGB);

        BufferedImage reducida = servicio().limitarDimensiones(grande);

        assertThat(reducida.getWidth()).isEqualTo(2048);
        assertThat(reducida.getHeight()).isEqualTo(1024);
    }

    @Test
    void limitarDimensiones_imagenVerticalGrande_reduceLadoMayorA2048() {
        BufferedImage grande = new BufferedImage(1500, 4000, BufferedImage.TYPE_INT_RGB);

        BufferedImage reducida = servicio().limitarDimensiones(grande);

        assertThat(reducida.getWidth()).isEqualTo(768);
        assertThat(reducida.getHeight()).isEqualTo(2048);
    }

    @Test
    void limitarDimensiones_imagenChica_noModificaDimensiones() {
        BufferedImage chica = new BufferedImage(800, 600, BufferedImage.TYPE_INT_RGB);

        BufferedImage resultante = servicio().limitarDimensiones(chica);

        assertThat(resultante.getWidth()).isEqualTo(800);
        assertThat(resultante.getHeight()).isEqualTo(600);
    }

    @Test
    void guardarImagenGrande_ladoMayorNoSuperaMaximo() throws IOException {
        MockMultipartFile grande = archivo("grande.jpg", imagenConTamano("jpg", 3000, 1500));

        String ruta = servicio().guardar(grande);

        assertThat(ruta).matches("/uploads/[0-9a-fA-F-]+\\.webp");
        Path guardado = directorio.resolve(ruta.substring("/uploads/".length()));
        assertThat(Files.exists(guardado)).isTrue();
        assertThat(Files.size(guardado)).isPositive();
    }

    // ---------------------------------------------------------------
    // eliminar
    // ---------------------------------------------------------------

    @Test
    void eliminar_urlValida_borraElArchivo() throws IOException {
        LocalStorageService svc = servicio();
        String ruta = svc.guardar(archivo("foto.png", imagenBytes("png")));
        Path guardado = directorio.resolve(ruta.substring("/uploads/".length()));

        boolean eliminado = svc.eliminar(ruta);

        assertThat(eliminado).isTrue();
        assertThat(Files.exists(guardado)).isFalse();
    }

    @Test
    void eliminar_archivoInexistente_devuelveFalse() {
        assertThat(servicio().eliminar("/uploads/" + UUID.randomUUID() + ".webp")).isFalse();
    }

    @Test
    void eliminar_urlsInvalidas_noTocanNingunArchivo() throws IOException {
        LocalStorageService svc = servicio();
        String ruta = svc.guardar(archivo("foto.png", imagenBytes("png")));
        Path guardado = directorio.resolve(ruta.substring("/uploads/".length()));

        assertThat(svc.eliminar(null)).isFalse();
        assertThat(svc.eliminar("/otra/ruta.webp")).isFalse();
        assertThat(svc.eliminar("/uploads/nota.txt")).isFalse();
        assertThat(svc.eliminar("/uploads/../" + guardado.getFileName())).isFalse();
        assertThat(Files.exists(guardado)).isTrue();
    }

    // ---------------------------------------------------------------
    // eliminarHuerfanos
    // ---------------------------------------------------------------

    @Test
    void eliminarHuerfanos_archivoViejoNoReferenciado_seBorra() throws IOException {
        Path viejo = archivoWebpAntiguo(25);

        int borrados = servicio().eliminarHuerfanos(url -> false, Duration.ofHours(24));

        assertThat(borrados).isEqualTo(1);
        assertThat(Files.exists(viejo)).isFalse();
    }

    @Test
    void eliminarHuerfanos_archivoReferenciado_noSeBorra() throws IOException {
        Path viejo = archivoWebpAntiguo(25);
        String url = "/uploads/" + viejo.getFileName();

        int borrados = servicio().eliminarHuerfanos(url::equals, Duration.ofHours(24));

        assertThat(borrados).isZero();
        assertThat(Files.exists(viejo)).isTrue();
    }

    @Test
    void eliminarHuerfanos_archivoReciente_noSeBorra() throws IOException {
        Path reciente = archivoWebpAntiguo(1);

        int borrados = servicio().eliminarHuerfanos(url -> false, Duration.ofHours(24));

        assertThat(borrados).isZero();
        assertThat(Files.exists(reciente)).isTrue();
    }

    @Test
    void eliminarHuerfanos_nombreNoUuid_noSeBorra() throws IOException {
        Path manual = directorio.resolve("manual.webp");
        Files.createFile(manual);
        Files.setLastModifiedTime(manual, FileTime.fromMillis(System.currentTimeMillis() - 25 * 3600_000L));

        int borrados = servicio().eliminarHuerfanos(url -> false, Duration.ofHours(24));

        assertThat(borrados).isZero();
        assertThat(Files.exists(manual)).isTrue();
    }

    @Test
    void eliminarHuerfanos_directorioInexistente_devuelveCero() {
        LocalStorageService svc = new LocalStorageService(directorio.resolve("no-existe").toString());

        assertThat(svc.eliminarHuerfanos(url -> false, Duration.ofHours(24))).isZero();
    }

    // ---------------------------------------------------------------
    // helpers
    // ---------------------------------------------------------------

    private Path archivoWebpAntiguo(int horasAtras) throws IOException {
        Path archivo = directorio.resolve(UUID.randomUUID() + ".webp");
        Files.createFile(archivo);
        Files.setLastModifiedTime(archivo,
                FileTime.fromMillis(System.currentTimeMillis() - horasAtras * 3600_000L));
        return archivo;
    }

    private LocalStorageService servicio() {
        return new LocalStorageService(directorio.toString());
    }

    private MockMultipartFile archivo(String nombre, byte[] bytes) {
        return new MockMultipartFile("archivo", nombre, "application/octet-stream", bytes);
    }

    private byte[] imagenBytes(String formato) throws IOException {
        return imagenConTamano(formato, 8, 8);
    }

    private byte[] imagenConTamano(String formato, int ancho, int alto) throws IOException {
        BufferedImage imagen = new BufferedImage(ancho, alto, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream salida = new ByteArrayOutputStream();
        ImageIO.write(imagen, formato, salida);
        return salida.toByteArray();
    }
}
