package com.temipora.backend_springboot.servicios;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import com.temipora.backend_springboot.modelo.ArchivoEntregaRequest;
import com.temipora.backend_springboot.modelo.EntregaArchivo;
import com.temipora.backend_springboot.modelo.EntregaHito;
import com.temipora.backend_springboot.modelo.EntregaHitoMultipartRequest;
import com.temipora.backend_springboot.modelo.EstadoPagoHito;
import com.temipora.backend_springboot.modelo.HitoPedido;
import com.temipora.backend_springboot.modelo.ItemSolicitud;
import com.temipora.backend_springboot.modelo.Pedido;
import com.temipora.backend_springboot.modelo.Solicitud;
import com.temipora.backend_springboot.modelo.Usuario;
import com.temipora.backend_springboot.repositorios.EntregaArchivoRepository;
import com.temipora.backend_springboot.repositorios.EntregaHitoRepository;

@ExtendWith(MockitoExtension.class)
class EntregaServiceTest {

    @Mock
    private EntregaHitoRepository entregaRepository;

    @Mock
    private EntregaArchivoRepository archivoRepository;

    @Mock
    private AlmacenPrivadoService almacen;

    @Mock
    private PreviewService previewService;

    @InjectMocks
    private EntregaService entregaService;

    @Test
    void crear_conVariosArchivosYVariosDibujos_guardaTodos() {
        Pedido pedido = pedido();
        HitoPedido hito = hito(pedido);
        prepararPrimeraEntrega(10L);
        prepararPreview();
        when(almacen.guardarOriginal(any(), anyString())).thenReturn(
            new AlmacenPrivadoService.ArchivoGuardado(
                "clave.png", "archivo.png", "image/png", 10, 10, 100,
                "h".repeat(64)));
        when(entregaRepository.save(any())).thenAnswer(
            invocacion -> invocacion.getArgument(0));

        EntregaHito entrega = entregaService.crear(
            pedido,
            hito,
            request(1L, 2L),
            List.of(archivo("a.png"), archivo("b.png")));

        assertThat(entrega.getNumero()).isEqualTo(1);
        assertThat(entrega.isVigente()).isTrue();
        assertThat(entrega.getArchivos()).hasSize(2);
        assertThat(entrega.getArchivos())
            .extracting(EntregaArchivo::getItemSolicitud)
            .containsExactly(
                pedido.getSolicitud().getItems().get(0),
                pedido.getSolicitud().getItems().get(1));
        assertThat(entrega.getArchivos())
            .extracting(EntregaArchivo::getClavePreview)
            .containsOnly("preview.webp");
        verify(almacen, times(2)).guardarOriginal(any(), anyString());
        verify(almacen, times(2)).guardarPreview(any(), anyString());
    }

    @Test
    void crear_conArchivosSinDibujo_permiteEntregaGeneral() {
        Pedido pedido = pedido();
        HitoPedido hito = hito(pedido);
        prepararPrimeraEntrega(10L);
        prepararPreview();
        when(almacen.guardarOriginal(any(), anyString())).thenReturn(
            new AlmacenPrivadoService.ArchivoGuardado(
                "clave.png", "archivo.png", "image/png", 10, 10, 100,
                "h".repeat(64)));
        when(entregaRepository.save(any())).thenAnswer(
            invocacion -> invocacion.getArgument(0));

        EntregaHito entrega = entregaService.crear(
            pedido,
            hito,
            request((Long) null),
            List.of(archivo("general.png")));

        assertThat(entrega.getArchivos()).hasSize(1);
        assertThat(entrega.getArchivos().get(0).getItemSolicitud()).isNull();
        assertThat(entrega.getArchivos().get(0).getClavePreview())
            .isEqualTo("preview.webp");
    }

    @Test
    void crear_conCantidadDeMetadatosDistinta_lanzaError() {
        Pedido pedido = pedido();
        HitoPedido hito = hito(pedido);

        assertThatThrownBy(() -> entregaService.crear(
            pedido,
            hito,
            request(1L),
            List.of(archivo("a.png"), archivo("b.png"))))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("no coincide");
    }

    @Test
    void crear_conDibujoDeOtroPedido_lanzaError() {
        Pedido pedido = pedido();
        HitoPedido hito = hito(pedido);

        assertThatThrownBy(() -> entregaService.crear(
            pedido,
            hito,
            request(99L),
            List.of(archivo("a.png"))))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("no pertenece");
    }

    @Test
    void registrarRevision_marcaLaEntregaVigente() {
        HitoPedido hito = hito(pedido());
        EntregaHito entrega = new EntregaHito();
        entrega.setIdEntrega(7L);
        entrega.setHitoPedido(hito);
        when(entregaRepository
            .findFirstByHitoPedidoIdHitoPedidoAndVigenteTrue(10L))
            .thenReturn(Optional.of(entrega));

        entregaService.registrarRevision(hito, hito.getPedido().getCliente(), "Cambiar la pose");

        assertThat(entrega.getRevisionMotivo()).isEqualTo("Cambiar la pose");
        assertThat(entrega.getRevisionFecha()).isNotNull();
        verify(entregaRepository).save(entrega);
    }

    @Test
    void puedeVerOriginal_clienteSoloConHitoPagado() {
        HitoPedido hito = hito(pedido());
        hito.setEstadoPago(EstadoPagoHito.PENDIENTE);
        EntregaArchivo archivo = archivoDe(hito);

        assertThat(entregaService.puedeVerOriginal(archivo, false, false)).isFalse();
        assertThat(entregaService.puedeVerOriginal(archivo, true, false)).isTrue();
        assertThat(entregaService.puedeVerOriginal(archivo, false, true)).isTrue();

        hito.setEstadoPago(EstadoPagoHito.PAGADO);
        assertThat(entregaService.puedeVerOriginal(archivo, false, false)).isTrue();
    }

    private void prepararPreview() {
        when(almacen.leer(anyString())).thenReturn(new byte[] { 1, 2, 3 });
        when(previewService.generar(any(), anyString()))
            .thenReturn(new byte[] { 4, 5, 6 });
        when(almacen.guardarPreview(any(), anyString()))
            .thenReturn("preview.webp");
    }

    private void prepararPrimeraEntrega(Long idHito) {
        when(entregaRepository
            .findFirstByHitoPedidoIdHitoPedidoAndVigenteTrue(idHito))
            .thenReturn(Optional.empty());
        when(entregaRepository
            .findFirstByHitoPedidoIdHitoPedidoOrderByNumeroDesc(idHito))
            .thenReturn(Optional.empty());
    }

    private Pedido pedido() {
        Usuario artista = new Usuario();
        artista.setIdUsuario(5L);
        Usuario cliente = new Usuario();
        cliente.setIdUsuario(6L);

        List<ItemSolicitud> items = new ArrayList<>();
        items.add(item(1L, "Dibujo 1", 0));
        items.add(item(2L, "Dibujo 2", 1));

        Solicitud solicitud = new Solicitud();
        solicitud.setItems(items);

        Pedido pedido = new Pedido();
        pedido.setIdPedido(1L);
        pedido.setArtista(artista);
        pedido.setCliente(cliente);
        pedido.setSolicitud(solicitud);
        return pedido;
    }

    private HitoPedido hito(Pedido pedido) {
        HitoPedido hito = new HitoPedido();
        hito.setIdHitoPedido(10L);
        hito.setPedido(pedido);
        return hito;
    }

    private ItemSolicitud item(Long id, String nombre, int orden) {
        ItemSolicitud item = new ItemSolicitud();
        item.setIdItemSolicitud(id);
        item.setNombre(nombre);
        item.setOrden(orden);
        return item;
    }

    private EntregaHitoMultipartRequest request(Long... idsDibujo) {
        List<ArchivoEntregaRequest> archivos = new ArrayList<>();
        for (Long id : idsDibujo) {
            ArchivoEntregaRequest meta = new ArchivoEntregaRequest();
            meta.setIdItemSolicitud(id);
            archivos.add(meta);
        }
        EntregaHitoMultipartRequest request = new EntregaHitoMultipartRequest();
        request.setDescripcion("Bocetos");
        request.setArchivos(archivos);
        return request;
    }

    private MultipartFile archivo(String nombre) {
        return new MockMultipartFile(
            "archivos", nombre, "image/png", new byte[] { 1, 2, 3 });
    }

    private EntregaArchivo archivoDe(HitoPedido hito) {
        EntregaHito entrega = new EntregaHito();
        entrega.setHitoPedido(hito);
        EntregaArchivo archivo = new EntregaArchivo();
        archivo.setEntrega(entrega);
        return archivo;
    }
}
