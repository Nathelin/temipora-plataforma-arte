package com.temipora.backend_springboot.servicios;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.temipora.backend_springboot.modelo.DescargaArchivo;
import com.temipora.backend_springboot.modelo.EntregaArchivo;
import com.temipora.backend_springboot.modelo.Pedido;
import com.temipora.backend_springboot.modelo.TipoDescarga;
import com.temipora.backend_springboot.modelo.Usuario;
import com.temipora.backend_springboot.repositorios.DescargaArchivoRepository;
import com.temipora.backend_springboot.repositorios.EntregaArchivoRepository;
import com.temipora.backend_springboot.repositorios.PedidoRepository;
import com.temipora.backend_springboot.repositorios.UsuarioRepository;

@ExtendWith(MockitoExtension.class)
class DescargaServiceTest {

    @Mock
    private DescargaArchivoRepository descargaRepository;

    @Mock
    private PedidoRepository pedidoRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private EntregaArchivoRepository archivoRepository;

    @InjectMocks
    private DescargaService descargaService;

    @Test
    void registrar_guardaElTipoElArchivoYLaIp() {
        Pedido pedido = new Pedido();
        Usuario usuario = new Usuario();
        EntregaArchivo archivo = new EntregaArchivo();
        when(pedidoRepository.getReferenceById(1L)).thenReturn(pedido);
        when(usuarioRepository.getReferenceById(2L)).thenReturn(usuario);
        when(archivoRepository.getReferenceById(3L)).thenReturn(archivo);

        descargaService.registrar(
            1L, 2L, 3L, TipoDescarga.ORIGINAL, "127.0.0.1");

        ArgumentCaptor<DescargaArchivo> captor =
            ArgumentCaptor.forClass(DescargaArchivo.class);
        verify(descargaRepository).save(captor.capture());

        DescargaArchivo descarga = captor.getValue();
        assertThat(descarga.getTipo()).isEqualTo(TipoDescarga.ORIGINAL);
        assertThat(descarga.getArchivo()).isSameAs(archivo);
        assertThat(descarga.getPedido()).isSameAs(pedido);
        assertThat(descarga.getUsuario()).isSameAs(usuario);
        assertThat(descarga.getIp()).isEqualTo("127.0.0.1");
    }

    @Test
    void registrar_sinArchivo_guardaLaConstanciaIgual() {
        Pedido pedido = new Pedido();
        Usuario usuario = new Usuario();
        when(pedidoRepository.getReferenceById(1L)).thenReturn(pedido);
        when(usuarioRepository.getReferenceById(2L)).thenReturn(usuario);

        descargaService.registrar(
            1L, 2L, null, TipoDescarga.CONSTANCIA, null);

        ArgumentCaptor<DescargaArchivo> captor =
            ArgumentCaptor.forClass(DescargaArchivo.class);
        verify(descargaRepository).save(captor.capture());

        assertThat(captor.getValue().getTipo())
            .isEqualTo(TipoDescarga.CONSTANCIA);
        assertThat(captor.getValue().getArchivo()).isNull();
        assertThat(captor.getValue().getIp()).isNull();
    }
}
