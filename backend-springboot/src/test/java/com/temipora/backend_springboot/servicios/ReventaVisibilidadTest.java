package com.temipora.backend_springboot.servicios;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.temipora.backend_springboot.modelo.EstadoPublicacionReventa;
import com.temipora.backend_springboot.modelo.Producto;
import com.temipora.backend_springboot.modelo.ProductoVariante;
import com.temipora.backend_springboot.modelo.PublicacionReventa;
import com.temipora.backend_springboot.modelo.Usuario;
import com.temipora.backend_springboot.repositorios.PublicacionReventaRepository;

@ExtendWith(MockitoExtension.class)
class ReventaVisibilidadTest {

    @Mock
    private PublicacionReventaRepository publicacionRepository;

    @InjectMocks
    private ReventaService reventaService;

    @Test
    void detallePublico_vendedorInactivo_lanzaServicioNoDisponible() {
        PublicacionReventa publicacion = publicacion(
            usuario(2L, false), usuario(3L, true));
        when(publicacionRepository.findByIdPublicacionReventa(1L))
            .thenReturn(Optional.of(publicacion));

        assertThatThrownBy(() -> reventaService.detallePublico(1L, null))
            .isInstanceOf(ServicioNoDisponibleException.class)
            .hasMessage("Esta publicación no está disponible.");
    }

    @Test
    void detallePublico_artistaOriginalInactivo_lanzaServicioNoDisponible() {
        PublicacionReventa publicacion = publicacion(
            usuario(2L, true), usuario(3L, false));
        when(publicacionRepository.findByIdPublicacionReventa(1L))
            .thenReturn(Optional.of(publicacion));

        assertThatThrownBy(() -> reventaService.detallePublico(1L, null))
            .isInstanceOf(ServicioNoDisponibleException.class)
            .hasMessage("Esta publicación no está disponible.");
    }

    private PublicacionReventa publicacion(Usuario vendedor, Usuario artista) {
        Producto producto = new Producto();
        producto.setIdProducto(4L);
        producto.setArtista(artista);

        ProductoVariante variante = new ProductoVariante();
        variante.setIdVariante(5L);
        variante.setProducto(producto);

        PublicacionReventa publicacion = new PublicacionReventa();
        publicacion.setIdPublicacionReventa(1L);
        publicacion.setVendedor(vendedor);
        publicacion.setVariante(variante);
        publicacion.setEstado(EstadoPublicacionReventa.PUBLICADA);
        return publicacion;
    }

    private Usuario usuario(Long id, boolean activo) {
        return Usuario.builder()
            .idUsuario(id)
            .nombreUsuario("usuario" + id)
            .estadoCuenta(activo)
            .build();
    }
}
