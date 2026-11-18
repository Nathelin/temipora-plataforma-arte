package com.temipora.backend_springboot.servicios;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.temipora.backend_springboot.modelo.EstadoProducto;
import com.temipora.backend_springboot.modelo.EstadoVariante;
import com.temipora.backend_springboot.modelo.Producto;
import com.temipora.backend_springboot.modelo.ProductoDTO;
import com.temipora.backend_springboot.modelo.ProductoResumenEstadoDTO;
import com.temipora.backend_springboot.modelo.ProductoVariante;
import com.temipora.backend_springboot.modelo.Usuario;
import com.temipora.backend_springboot.repositorios.EventoLedgerRepository;
import com.temipora.backend_springboot.repositorios.ProductoRepository;
import com.temipora.backend_springboot.repositorios.UsuarioRepository;

@ExtendWith(MockitoExtension.class)
class ProductoBajaServiceTest {

    @Mock private ProductoRepository productoRepository;
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private EventoLedgerRepository eventoLedgerRepository;
    @Mock private ProductoService productoService;
    @Mock private AuditoriaService auditoriaService;

    @InjectMocks private ProductoBajaService productoBajaService;

    @Test
    void resumenEstado_cuentaVariantes() {
        Producto producto = producto();
        producto.getVariantes().add(variante(1L, EstadoVariante.DISPONIBLE, 1));
        producto.getVariantes().add(variante(2L, EstadoVariante.AGOTADO, 0));
        producto.getVariantes().add(variante(3L, EstadoVariante.ARCHIVADO, 1));
        when(productoRepository.findByIdProducto(10L))
            .thenReturn(Optional.of(producto));
        when(eventoLedgerRepository.findVariantesConLedger(List.of(10L)))
            .thenReturn(Set.of(1L));

        ProductoResumenEstadoDTO resumen = productoBajaService.resumenEstado(10L, 5L);

        assertThat(resumen.getTotalVariantes()).isEqualTo(3);
        assertThat(resumen.getVariantesDisponibles()).isEqualTo(1);
        assertThat(resumen.getVariantesAgotadas()).isEqualTo(1);
        assertThat(resumen.getVariantesArchivadas()).isEqualTo(1);
        assertThat(resumen.getVariantesConLedger()).isEqualTo(1);
    }

    @Test
    void archivar_cambiaEstadoYRegistraAuditoria() {
        Producto producto = producto();
        Usuario artista = Usuario.builder()
            .idUsuario(5L)
            .nombreUsuario("artista")
            .build();
        ProductoDTO dto = ProductoDTO.builder()
            .idProducto(10L)
            .estado(EstadoProducto.ARCHIVADO)
            .build();

        when(productoRepository.findByIdProductoForUpdate(10L))
            .thenReturn(Optional.of(producto));
        when(usuarioRepository.findByIdUsuario(5L))
            .thenReturn(Optional.of(artista));
        when(productoRepository.save(any(Producto.class)))
            .thenAnswer(inv -> inv.getArgument(0));
        when(productoService.aDTO(producto)).thenReturn(dto);

        ProductoDTO resultado = productoBajaService.archivar(10L, 5L);

        assertThat(producto.getEstado()).isEqualTo(EstadoProducto.ARCHIVADO);
        assertThat(producto.getArchivadoPor()).isSameAs(artista);
        assertThat(producto.getArchivadoEn()).isNotNull();
        assertThat(resultado.getEstado()).isEqualTo(EstadoProducto.ARCHIVADO);
        verify(auditoriaService).registrar(
            eq(AccionesAdmin.PRODUCTO_ARCHIVAR), eq("Producto"), eq(10L),
            anyString(), any(Snapshot.class), any(Producto.class), anyList());
    }

    @Test
    void archivar_yaArchivado_lanzaConflicto() {
        Producto producto = producto();
        producto.setEstado(EstadoProducto.ARCHIVADO);
        when(productoRepository.findByIdProductoForUpdate(10L))
            .thenReturn(Optional.of(producto));

        assertThatThrownBy(() -> productoBajaService.archivar(10L, 5L))
            .isInstanceOf(ConflictoException.class)
            .hasMessage("El producto ya está archivado.");

        verify(productoRepository, never()).save(any(Producto.class));
    }

    private Producto producto() {
        Producto producto = new Producto();
        producto.setIdProducto(10L);
        producto.setArtista(Usuario.builder().idUsuario(5L).nombreUsuario("artista").build());
        producto.setTitulo("Plancha de otoño");
        producto.setEstado(EstadoProducto.DISPONIBLE);
        return producto;
    }

    private ProductoVariante variante(
            Long id, EstadoVariante estado, int stock) {
        ProductoVariante variante = new ProductoVariante();
        variante.setIdVariante(id);
        variante.setNombre("Variante " + id);
        variante.setPrecio(new BigDecimal("100.00"));
        variante.setEstado(estado);
        variante.setStockDisponible(stock);
        return variante;
    }
}
