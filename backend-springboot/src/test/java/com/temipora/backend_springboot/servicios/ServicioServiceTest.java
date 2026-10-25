package com.temipora.backend_springboot.servicios;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Root;

import com.temipora.backend_springboot.modelo.ETiempoEntregaUnidad;
import com.temipora.backend_springboot.modelo.EstadoServicio;
import com.temipora.backend_springboot.modelo.FiltroServicioRequest;
import com.temipora.backend_springboot.modelo.Medio;
import com.temipora.backend_springboot.modelo.ModoRecepcionSolicitud;
import com.temipora.backend_springboot.modelo.Moneda;
import com.temipora.backend_springboot.modelo.Servicio;
import com.temipora.backend_springboot.modelo.ServicioDTO;
import com.temipora.backend_springboot.modelo.ServicioRequest;
import com.temipora.backend_springboot.modelo.TipoServicio;
import com.temipora.backend_springboot.modelo.Usuario;
import com.temipora.backend_springboot.repositorios.EtiquetaRepository;
import com.temipora.backend_springboot.repositorios.MedioRepository;
import com.temipora.backend_springboot.repositorios.ServicioRepository;
import com.temipora.backend_springboot.repositorios.SolicitudRepository;
import com.temipora.backend_springboot.repositorios.TipoServicioRepository;
import com.temipora.backend_springboot.repositorios.UsuarioRepository;

@ExtendWith(MockitoExtension.class)
class ServicioServiceTest {

    @Mock
    private ServicioRepository servicioRepository;

    @Mock
    private SolicitudRepository solicitudRepository;

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
        when(servicioRepository.findByArtistaIdUsuarioAndEstadoNotOrderByNombreAsc(
            5L, EstadoServicio.ARCHIVADO))
            .thenReturn(List.of(servicio(1L, 5L)));

        List<ServicioDTO> resultado = servicioService.listarMios(5L);

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).getIdServicio()).isEqualTo(1L);
    }

    @Test
    void listarMios_conviertePrecioAMonedaPreferidaDelArtista() {
        Servicio servicio = servicio(1L, 5L);
        servicio.setPrecioBase(new BigDecimal("100.00"));
        servicio.setMoneda(moneda(3L, "ARS"));

        Usuario artista = usuario(5L);
        artista.setMonedaPreferida(moneda(2L, "USD"));

        when(servicioRepository.findByArtistaIdUsuarioAndEstadoNotOrderByNombreAsc(
            5L, EstadoServicio.ARCHIVADO))
            .thenReturn(List.of(servicio));
        when(usuarioRepository.findByIdUsuario(5L)).thenReturn(Optional.of(artista));
        when(conversionMonedaService.factor(any(Moneda.class), any(Moneda.class), any(LocalDateTime.class)))
            .thenReturn(new BigDecimal("0.5"));

        List<ServicioDTO> resultado = servicioService.listarMios(5L);

        assertThat(resultado.get(0).getPrecioBaseMostrado()).isEqualByComparingTo("50.00");
        assertThat(resultado.get(0).getMonedaMostrada()).isEqualTo("USD");
    }

    @Test
    void listarActivos_soloIncluyeAbiertosConCuposLibres() {
        Servicio abierto = servicioLista(1L, ModoRecepcionSolicitud.ABIERTA, 2);
        Servicio lleno = servicioLista(2L, ModoRecepcionSolicitud.ABIERTA, 1);
        Servicio soloEspera = servicioLista(3L, ModoRecepcionSolicitud.SOLO_ESPERA, 5);

        when(servicioRepository.findAll(any(Specification.class), any(Sort.class)))
            .thenReturn(List.of(abierto, lleno, soloEspera));
        when(monedaService.predeterminada()).thenReturn(moneda(3L, "ARS"));
        when(conversionMonedaService.factor(any(Moneda.class), any(Moneda.class), any(LocalDateTime.class)))
            .thenReturn(BigDecimal.ONE);
        when(solicitudRepository.countByServicioIdServicioAndEstadoIn(any(Long.class), any()))
            .thenAnswer(inv -> ((Long) inv.getArgument(0)) == 2L ? 1L : 0L);

        List<ServicioDTO> resultado = servicioService.listarActivos(new FiltroServicioRequest(), null);

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).getIdServicio()).isEqualTo(1L);
        assertThat(resultado.get(0).getCuposDisponibles()).isEqualTo(2);
        assertThat(resultado.get(0).getModoRecepcion()).isEqualTo("ABIERTA");
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Test
    void listarActivos_excluyeLosServiciosDelPropioArtista() {
        when(servicioRepository.findAll(any(Specification.class), any(Sort.class)))
            .thenReturn(List.of());
        when(usuarioRepository.findByIdUsuario(5L)).thenReturn(Optional.empty());
        when(monedaService.predeterminada()).thenReturn(moneda(3L, "ARS"));

        servicioService.listarActivos(new FiltroServicioRequest(), 5L);

        ArgumentCaptor<Specification<Servicio>> captor = ArgumentCaptor.forClass(Specification.class);
        verify(servicioRepository).findAll(captor.capture(), any(Sort.class));

        Root<Servicio> root = mock(Root.class, withSettings().lenient());
        CriteriaQuery<?> query = mock(CriteriaQuery.class);
        CriteriaBuilder cb = mock(CriteriaBuilder.class);
        Path<Object> artista = mock(Path.class);
        Path<Object> idArtista = mock(Path.class);

        when(root.get("artista")).thenReturn((Path) artista);
        when(artista.get("idUsuario")).thenReturn(idArtista);

        captor.getValue().toPredicate(root, query, cb);

        verify(cb).notEqual(idArtista, 5L);
    }

    @Test
    void modificar_noPermiteBajarCuposPorDebajoDeOcupados() {
        Servicio servicio = servicioLista(1L, ModoRecepcionSolicitud.ABIERTA, 5);
        when(servicioRepository.findByIdServicio(1L)).thenReturn(Optional.of(servicio));
        when(servicioRepository.existsByArtistaIdUsuarioAndTipoServicioIdTipoServicioAndNombreIgnoreCaseAndIdServicioNotAndEstadoNot(
            5L, 1L, "Ilustración", 1L, EstadoServicio.ARCHIVADO)).thenReturn(false);
        when(solicitudRepository.countByServicioIdServicioAndEstadoIn(any(Long.class), any()))
            .thenReturn(3L);

        ServicioRequest request = request();
        request.setCupos(2);

        assertThatThrownBy(() -> servicioService.modificar(1L, 5L, request))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("No podés reducir los cupos");

        verify(servicioRepository, never()).save(any(Servicio.class));
    }

    @Test
    void crear_ok_calculaDiasYGuarda() {
        when(servicioRepository.existsByArtistaIdUsuarioAndTipoServicioIdTipoServicioAndNombreIgnoreCaseAndEstadoNot(5L, 1L, "Ilustración", EstadoServicio.ARCHIVADO))
            .thenReturn(false);
        when(usuarioRepository.findByIdUsuario(5L)).thenReturn(Optional.of(usuario(5L)));
        when(tipoServicioRepository.findByIdTipoServicio(1L)).thenReturn(Optional.of(tipoServicio(1L, "Ilustración digital")));
        when(medioRepository.findByIdMedio(2L)).thenReturn(Optional.of(medio(2L, "Digital")));
        when(monedaService.obtener(3L)).thenReturn(moneda(3L, "ARS"));
        when(terminosService.tieneGlobalValido(5L)).thenReturn(true);
        when(servicioRepository.save(any(Servicio.class))).thenAnswer(inv -> inv.getArgument(0));

        ServicioRequest request = request();
        ServicioDTO dto = servicioService.crear(5L, request);

        assertThat(dto.getTiempoEntregaDias()).isEqualTo(14);
        assertThat(dto.getTiempoEntregaUnidad()).isEqualTo("SEMANAS");
        assertThat(dto.getEstado()).isEqualTo(EstadoServicio.PUBLICADO);
        assertThat(dto.getImagenes()).containsExactly("a.png", "b.png");
        assertThat(dto.getNombreTipoServicio()).isEqualTo("Ilustración digital");
        assertThat(dto.getDescripcionTipoServicio()).isEqualTo("d");
    }

    @Test
    void crear_sinTerminosGlobales_lanzaError() {
        when(servicioRepository.existsByArtistaIdUsuarioAndTipoServicioIdTipoServicioAndNombreIgnoreCaseAndEstadoNot(5L, 1L, "Ilustración", EstadoServicio.ARCHIVADO))
            .thenReturn(false);
        when(usuarioRepository.findByIdUsuario(5L)).thenReturn(Optional.of(usuario(5L)));
        when(tipoServicioRepository.findByIdTipoServicio(1L)).thenReturn(Optional.of(tipoServicio(1L, "Ilustración digital")));
        when(medioRepository.findByIdMedio(2L)).thenReturn(Optional.of(medio(2L, "Digital")));
        when(monedaService.obtener(3L)).thenReturn(moneda(3L, "ARS"));
        when(terminosService.tieneGlobalValido(5L)).thenReturn(false);

        assertThatThrownBy(() -> servicioService.crear(5L, request()))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Completá tus términos de uso y servicio antes de publicar el servicio.");

        verify(servicioRepository, never()).save(any(Servicio.class));
    }

    @Test
    void crear_nombreTipoRepetido_lanzaError() {
        when(servicioRepository.existsByArtistaIdUsuarioAndTipoServicioIdTipoServicioAndNombreIgnoreCaseAndEstadoNot(5L, 1L, "Ilustración", EstadoServicio.ARCHIVADO))
            .thenReturn(true);

        assertThatThrownBy(() -> servicioService.crear(5L, request()))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Ya tenés un servicio con ese nombre y tipo.");

        verify(servicioRepository, never()).save(any(Servicio.class));
    }

    @Test
    void crear_sinImagenDePortada_lanzaError() {
        when(servicioRepository.existsByArtistaIdUsuarioAndTipoServicioIdTipoServicioAndNombreIgnoreCaseAndEstadoNot(5L, 1L, "Ilustración", EstadoServicio.ARCHIVADO))
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
        when(servicioRepository.existsByArtistaIdUsuarioAndTipoServicioIdTipoServicioAndNombreIgnoreCaseAndEstadoNot(5L, 1L, "Ilustración", EstadoServicio.ARCHIVADO))
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
        when(servicioRepository.existsByArtistaIdUsuarioAndTipoServicioIdTipoServicioAndNombreIgnoreCaseAndEstadoNot(5L, 1L, "Ilustración", EstadoServicio.ARCHIVADO))
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
    void crear_cuposCero_lanzaError() {
        when(servicioRepository.existsByArtistaIdUsuarioAndTipoServicioIdTipoServicioAndNombreIgnoreCaseAndEstadoNot(5L, 1L, "Ilustración", EstadoServicio.ARCHIVADO))
            .thenReturn(false);
        when(usuarioRepository.findByIdUsuario(5L)).thenReturn(Optional.of(usuario(5L)));

        ServicioRequest request = request();
        request.setCupos(0);

        assertThatThrownBy(() -> servicioService.crear(5L, request))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("La cantidad de cupos debe ser mayor a cero.");

        verify(servicioRepository, never()).save(any(Servicio.class));
    }

    @Test
    void crear_cuposMayorAlMaximo_lanzaError() {
        when(servicioRepository.existsByArtistaIdUsuarioAndTipoServicioIdTipoServicioAndNombreIgnoreCaseAndEstadoNot(5L, 1L, "Ilustración", EstadoServicio.ARCHIVADO))
            .thenReturn(false);
        when(usuarioRepository.findByIdUsuario(5L)).thenReturn(Optional.of(usuario(5L)));

        ServicioRequest request = request();
        request.setCupos(11);

        assertThatThrownBy(() -> servicioService.crear(5L, request))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("La cantidad de cupos no puede superar 10.");

        verify(servicioRepository, never()).save(any(Servicio.class));
    }

    @Test
    void obtener_servicioPausadoDeOtroUsuario_lanzaNoSuchElement() {
        Servicio servicio = servicio(1L, 5L);
        servicio.setEstado(EstadoServicio.PAUSADO);
        when(servicioRepository.findByIdServicio(1L)).thenReturn(Optional.of(servicio));

        assertThatThrownBy(() -> servicioService.obtener(1L, 99L))
            .isInstanceOf(NoSuchElementException.class)
            .hasMessage("Servicio no encontrado: 1");
    }

    @Test
    void obtener_servicioArchivadoDelDuenio_lanzaNoSuchElement() {
        Servicio servicio = servicio(1L, 5L);
        servicio.setEstado(EstadoServicio.ARCHIVADO);
        when(servicioRepository.findByIdServicio(1L)).thenReturn(Optional.of(servicio));

        assertThatThrownBy(() -> servicioService.obtener(1L, 5L))
            .isInstanceOf(NoSuchElementException.class)
            .hasMessage("Servicio no encontrado: 1");
    }

    @Test
    void crear_tipoInexistente_lanzaError() {
        when(servicioRepository.existsByArtistaIdUsuarioAndTipoServicioIdTipoServicioAndNombreIgnoreCaseAndEstadoNot(5L, 1L, "Ilustración", EstadoServicio.ARCHIVADO))
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
        when(servicioRepository.existsByArtistaIdUsuarioAndTipoServicioIdTipoServicioAndNombreIgnoreCaseAndIdServicioNotAndEstadoNot(5L, 1L, "Ilustración", 1L, EstadoServicio.ARCHIVADO))
            .thenReturn(false);
        when(tipoServicioRepository.findByIdTipoServicio(1L)).thenReturn(Optional.of(tipoServicio(1L, "Ilustración digital")));
        when(medioRepository.findByIdMedio(2L)).thenReturn(Optional.of(medio(2L, "Digital")));
        when(monedaService.obtener(3L)).thenReturn(moneda(3L, "ARS"));
        when(terminosService.tieneTerminosValidos(1L, 5L)).thenReturn(true);
        when(servicioRepository.save(any(Servicio.class))).thenAnswer(inv -> inv.getArgument(0));

        ServicioDTO dto = servicioService.modificar(1L, 5L, request());

        assertThat(dto.getNombre()).isEqualTo("Ilustración");
    }

    @Test
    void modificar_sinTerminosValidos_lanzaError() {
        when(servicioRepository.findByIdServicio(1L)).thenReturn(Optional.of(servicio(1L, 5L)));
        when(servicioRepository.existsByArtistaIdUsuarioAndTipoServicioIdTipoServicioAndNombreIgnoreCaseAndIdServicioNotAndEstadoNot(5L, 1L, "Ilustración", 1L, EstadoServicio.ARCHIVADO))
            .thenReturn(false);
        when(tipoServicioRepository.findByIdTipoServicio(1L)).thenReturn(Optional.of(tipoServicio(1L, "Ilustración digital")));
        when(medioRepository.findByIdMedio(2L)).thenReturn(Optional.of(medio(2L, "Digital")));
        when(monedaService.obtener(3L)).thenReturn(moneda(3L, "ARS"));
        when(terminosService.tieneTerminosValidos(1L, 5L)).thenReturn(false);

        assertThatThrownBy(() -> servicioService.modificar(1L, 5L, request()))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Completá tus términos de uso y servicio antes de publicar el servicio.");

        verify(servicioRepository, never()).save(any(Servicio.class));
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
    void pausar_marcaElServicioComoPausado() {
        when(servicioRepository.findByIdServicioForUpdate(1L))
            .thenReturn(Optional.of(servicio(1L, 5L)));
        when(servicioRepository.save(any(Servicio.class))).thenAnswer(inv -> inv.getArgument(0));

        ServicioDTO dto = servicioService.pausar(1L, 5L);

        assertThat(dto.getEstado()).isEqualTo(EstadoServicio.PAUSADO);
    }

    @Test
    void pausar_conSolicitudesPendientesYCompromisos_igualPausa() {
        Servicio servicio = servicio(1L, 5L);
        when(servicioRepository.findByIdServicioForUpdate(1L)).thenReturn(Optional.of(servicio));
        when(servicioRepository.save(any(Servicio.class))).thenAnswer(inv -> inv.getArgument(0));

        ServicioDTO dto = servicioService.pausar(1L, 5L);

        assertThat(dto.getEstado()).isEqualTo(EstadoServicio.PAUSADO);
        verify(solicitudRepository, never()).countByServicioIdServicioAndEstadoIn(any(), any());
    }

    @Test
    void reactivar_sinTerminosValidos_lanzaError() {
        Servicio servicio = servicio(1L, 5L);
        servicio.setEstado(EstadoServicio.PAUSADO);
        when(servicioRepository.findByIdServicioForUpdate(1L)).thenReturn(Optional.of(servicio));
        when(terminosService.tieneTerminosValidos(1L, 5L)).thenReturn(false);

        assertThatThrownBy(() -> servicioService.reactivar(1L, 5L))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Completá tus términos de uso y servicio antes de publicar el servicio.");

        verify(servicioRepository, never()).save(any(Servicio.class));
    }

    @Test
    void reactivar_archivado_lanzaConflicto() {
        Servicio servicio = servicio(1L, 5L);
        servicio.setEstado(EstadoServicio.ARCHIVADO);
        when(servicioRepository.findByIdServicioForUpdate(1L)).thenReturn(Optional.of(servicio));

        assertThatThrownBy(() -> servicioService.reactivar(1L, 5L))
            .isInstanceOf(ConflictoException.class);
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

    private Servicio servicioLista(Long id, ModoRecepcionSolicitud modo, int cupos) {
        Servicio s = servicio(id, 5L);
        s.setNombre("Servicio " + id);
        s.setPrecioBase(new BigDecimal("100.00"));
        s.setMoneda(moneda(3L, "ARS"));
        s.setEstado(EstadoServicio.PUBLICADO);
        s.setCupos(cupos);
        s.setModoRecepcion(modo);
        s.setMaxDibujosPorSolicitud(3);
        s.setContenidosSensibles(new HashSet<>());
        s.setEtiquetas(new HashSet<>());
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
