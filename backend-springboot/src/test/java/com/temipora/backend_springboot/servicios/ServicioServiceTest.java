package com.temipora.backend_springboot.servicios;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.temipora.backend_springboot.modelo.ETiempoEntregaUnidad;
import com.temipora.backend_springboot.modelo.Medio;
import com.temipora.backend_springboot.modelo.Moneda;
import com.temipora.backend_springboot.modelo.Servicio;
import com.temipora.backend_springboot.modelo.ServicioDTO;
import com.temipora.backend_springboot.modelo.ServicioRequest;
import com.temipora.backend_springboot.modelo.TipoServicio;
import com.temipora.backend_springboot.modelo.Usuario;
import com.temipora.backend_springboot.repositorios.EtiquetaRepository;
import com.temipora.backend_springboot.repositorios.MedioRepository;
import com.temipora.backend_springboot.repositorios.ServicioRepository;
import com.temipora.backend_springboot.repositorios.TipoServicioRepository;
import com.temipora.backend_springboot.repositorios.UsuarioRepository;

@ExtendWith(MockitoExtension.class)
class ServicioServiceTest {

    @Mock
    private ServicioRepository servicioRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private TipoServicioRepository tipoServicioRepository;

    @Mock
    private MedioRepository medioRepository;

    @Mock
    private EtiquetaRepository etiquetaRepository;

    @Mock
    private FormularioService formularioService;

    @Mock
    private TerminosService terminosService;

    @Mock
    private ConversionMonedaService conversionMonedaService;

    @Mock
    private MonedaService monedaService;

    @InjectMocks
    private ServicioService servicioService;

    @Test
    void listarMios_mapeaEntidadesADtos() {
        when(servicioRepository.findByArtistaIdUsuarioOrderByNombreAsc(5L))
            .thenReturn(List.of(servicio(1L, 5L)));

        List<ServicioDTO> resultado = servicioService.listarMios(5L);

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).getIdServicio()).isEqualTo(1L);
    }

    @Test
    void crear_ok_calculaDiasYGuarda() {
        when(servicioRepository.existsByArtistaIdUsuarioAndTipoServicioIdTipoServicioAndNombreIgnoreCase(5L, 1L, "Ilustración"))
            .thenReturn(false);
        when(usuarioRepository.findByIdUsuario(5L)).thenReturn(Optional.of(usuario(5L)));
        when(tipoServicioRepository.findByIdTipoServicio(1L)).thenReturn(Optional.of(tipoServicio(1L, "Ilustración digital")));
        when(medioRepository.findByIdMedio(2L)).thenReturn(Optional.of(medio(2L, "Digital")));
        when(monedaService.obtener(3L)).thenReturn(moneda(3L, "ARS"));
        when(servicioRepository.save(any(Servicio.class))).thenAnswer(inv -> inv.getArgument(0));

        ServicioRequest request = request();
        ServicioDTO dto = servicioService.crear(5L, request);

        assertThat(dto.getTiempoEntregaDias()).isEqualTo(14);
        assertThat(dto.getTiempoEntregaUnidad()).isEqualTo("SEMANAS");
        assertThat(dto.isEstado()).isTrue();
        assertThat(dto.getImagenes()).containsExactly("a.png", "b.png");
        assertThat(dto.getNombreTipoServicio()).isEqualTo("Ilustración digital");
    }

    @Test
    void crear_nombreTipoRepetido_lanzaError() {
        when(servicioRepository.existsByArtistaIdUsuarioAndTipoServicioIdTipoServicioAndNombreIgnoreCase(5L, 1L, "Ilustración"))
            .thenReturn(true);

        assertThatThrownBy(() -> servicioService.crear(5L, request()))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Ya tenés un servicio con ese nombre y tipo.");

        verify(servicioRepository, never()).save(any(Servicio.class));
    }

    @Test
    void crear_sinImagenDePortada_lanzaError() {
        when(servicioRepository.existsByArtistaIdUsuarioAndTipoServicioIdTipoServicioAndNombreIgnoreCase(5L, 1L, "Ilustración"))
            .thenReturn(false);
        when(usuarioRepository.findByIdUsuario(5L)).thenReturn(Optional.of(usuario(5L)));

        ServicioRequest request = request();
        request.setImagenPortada(" ");

        assertThatThrownBy(() -> servicioService.crear(5L, request))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("La imagen de portada es obligatoria.");

        verify(servicioRepository, never()).save(any(Servicio.class));
    }

    @Test
    void crear_sinImagenesDeEjemplo_lanzaError() {
        when(servicioRepository.existsByArtistaIdUsuarioAndTipoServicioIdTipoServicioAndNombreIgnoreCase(5L, 1L, "Ilustración"))
            .thenReturn(false);
        when(usuarioRepository.findByIdUsuario(5L)).thenReturn(Optional.of(usuario(5L)));

        ServicioRequest request = request();
        request.setImagenes(List.of(" "));

        assertThatThrownBy(() -> servicioService.crear(5L, request))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Debés agregar al menos una imagen de ejemplo.");

        verify(servicioRepository, never()).save(any(Servicio.class));
    }

    @Test
    void crear_precioCero_lanzaError() {
        when(servicioRepository.existsByArtistaIdUsuarioAndTipoServicioIdTipoServicioAndNombreIgnoreCase(5L, 1L, "Ilustración"))
            .thenReturn(false);
        when(usuarioRepository.findByIdUsuario(5L)).thenReturn(Optional.of(usuario(5L)));

        ServicioRequest request = request();
        request.setPrecioBase(BigDecimal.ZERO);

        assertThatThrownBy(() -> servicioService.crear(5L, request))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("El precio base debe ser mayor a cero.");

        verify(servicioRepository, never()).save(any(Servicio.class));
    }

    @Test
    void obtener_servicioInactivoDeOtroUsuario_lanzaNoSuchElement() {
        Servicio servicio = servicio(1L, 5L);
        servicio.setEstado(false);
        when(servicioRepository.findByIdServicio(1L)).thenReturn(Optional.of(servicio));

        assertThatThrownBy(() -> servicioService.obtener(1L, 99L))
            .isInstanceOf(NoSuchElementException.class)
            .hasMessage("Servicio no encontrado: 1");
    }

    @Test
    void crear_tipoInexistente_lanzaError() {
        when(servicioRepository.existsByArtistaIdUsuarioAndTipoServicioIdTipoServicioAndNombreIgnoreCase(5L, 1L, "Ilustración"))
            .thenReturn(false);
        when(usuarioRepository.findByIdUsuario(5L)).thenReturn(Optional.of(usuario(5L)));
        when(tipoServicioRepository.findByIdTipoServicio(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicioService.crear(5L, request()))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Tipo de servicio no encontrado: 1");
    }

    @Test
    void modificar_ok() {
        when(servicioRepository.findByIdServicio(1L)).thenReturn(Optional.of(servicio(1L, 5L)));
        when(servicioRepository.existsByArtistaIdUsuarioAndTipoServicioIdTipoServicioAndNombreIgnoreCaseAndIdServicioNot(5L, 1L, "Ilustración", 1L))
            .thenReturn(false);
        when(tipoServicioRepository.findByIdTipoServicio(1L)).thenReturn(Optional.of(tipoServicio(1L, "Ilustración digital")));
        when(medioRepository.findByIdMedio(2L)).thenReturn(Optional.of(medio(2L, "Digital")));
        when(monedaService.obtener(3L)).thenReturn(moneda(3L, "ARS"));
        when(servicioRepository.save(any(Servicio.class))).thenAnswer(inv -> inv.getArgument(0));

        ServicioDTO dto = servicioService.modificar(1L, 5L, request());

        assertThat(dto.getNombre()).isEqualTo("Ilustración");
    }

    @Test
    void modificar_noDuenio_lanzaError() {
        when(servicioRepository.findByIdServicio(1L)).thenReturn(Optional.of(servicio(1L, 99L)));

        assertThatThrownBy(() -> servicioService.modificar(1L, 5L, request()))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("No podés modificar un servicio que no es tuyo.");
    }

    @Test
    void modificar_inexistente_lanzaNoSuchElement() {
        when(servicioRepository.findByIdServicio(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicioService.modificar(99L, 5L, request()))
            .isInstanceOf(NoSuchElementException.class)
            .hasMessage("Servicio no encontrado: 99");
    }

    @Test
    void darDeBaja_desactivaElServicio() {
        when(servicioRepository.findByIdServicio(1L)).thenReturn(Optional.of(servicio(1L, 5L)));
        when(servicioRepository.save(any(Servicio.class))).thenAnswer(inv -> inv.getArgument(0));

        ServicioDTO dto = servicioService.darDeBaja(1L, 5L);

        assertThat(dto.isEstado()).isFalse();
    }

    // ---------------------------------------------------------------
    // helpers
    // ---------------------------------------------------------------

    private Servicio servicio(Long id, Long idArtista) {
        Servicio s = new Servicio();
        s.setIdServicio(id);
        s.setArtista(usuario(idArtista));
        s.setImagenes(new ArrayList<>());
        return s;
    }

    private ServicioRequest request() {
        ServicioRequest r = new ServicioRequest();
        r.setIdTipoServicio(1L);
        r.setIdMedio(2L);
        r.setIdMoneda(3L);
        r.setNombre("Ilustración");
        r.setDescripcion("desc");
        r.setPrecioBase(new BigDecimal("100.00"));
        r.setTiempoEntregaValor(2);
        r.setTiempoEntregaUnidad(ETiempoEntregaUnidad.SEMANAS);
        r.setCupos(5);
        r.setImagenPortada("portada.png");
        r.setImagenes(List.of("a.png", "b.png"));
        return r;
    }

    private Moneda moneda(Long id, String codigo) {
        return Moneda.builder().idMoneda(id).codigo(codigo).nombre(codigo).build();
    }

    private TipoServicio tipoServicio(Long id, String nombre) {
        return TipoServicio.builder().idTipoServicio(id).nombre(nombre).descripcion("d").build();
    }

    private Medio medio(Long id, String nombre) {
        return Medio.builder().idMedio(id).nombre(nombre).build();
    }

    private Usuario usuario(Long id) {
        return Usuario.builder().idUsuario(id).nombreUsuario("artista").build();
    }
}
