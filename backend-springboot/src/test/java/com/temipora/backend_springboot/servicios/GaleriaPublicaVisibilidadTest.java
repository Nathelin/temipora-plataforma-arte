package com.temipora.backend_springboot.servicios;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.temipora.backend_springboot.modelo.GaleriaPedidosDTO;
import com.temipora.backend_springboot.modelo.Usuario;

@ExtendWith(MockitoExtension.class)
class GaleriaPublicaVisibilidadTest {

    @Mock
    private UsuarioService usuarioService;

    @InjectMocks
    private GaleriaPublicaService galeriaPublicaService;

    @Test
    void listar_duenioInactivo_devuelveGaleriaVacia() {
        when(usuarioService.getByNombreUsuario("baja"))
            .thenReturn(Usuario.builder()
                .idUsuario(9L)
                .nombreUsuario("baja")
                .estadoCuenta(false)
                .build());

        GaleriaPedidosDTO galeria = galeriaPublicaService
            .listar("baja", null, null, null, 0, 10);

        assertThat(galeria.isVisible()).isFalse();
        assertThat(galeria.getPiezas().getContenido()).isEmpty();
        assertThat(galeria.getServicios()).isEmpty();
        assertThat(galeria.getArtistas()).isEmpty();
    }
}
