package com.temipora.backend_springboot.servicios;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Root;

import com.temipora.backend_springboot.modelo.CategoriaEtiqueta;
import com.temipora.backend_springboot.modelo.ContenidoSensible;
import com.temipora.backend_springboot.modelo.Etiqueta;
import com.temipora.backend_springboot.modelo.EtiquetaEstado;
import com.temipora.backend_springboot.modelo.FacetasServicioDTO;
import com.temipora.backend_springboot.modelo.FiltroServicioRequest;
import com.temipora.backend_springboot.modelo.Medio;
import com.temipora.backend_springboot.modelo.ModoRecepcionSolicitud;
import com.temipora.backend_springboot.modelo.Moneda;
import com.temipora.backend_springboot.modelo.Servicio;
import com.temipora.backend_springboot.modelo.ServicioEtiqueta;
import com.temipora.backend_springboot.modelo.ServicioEtiquetaId;
import com.temipora.backend_springboot.modelo.TipoServicio;
import com.temipora.backend_springboot.repositorios.EtiquetaRepository;
import com.temipora.backend_springboot.repositorios.MedioRepository;
import com.temipora.backend_springboot.repositorios.ServicioRepository;
import com.temipora.backend_springboot.repositorios.SolicitudRepository;
import com.temipora.backend_springboot.repositorios.TipoServicioRepository;
import com.temipora.backend_springboot.repositorios.UsuarioRepository;

/**
 * Conteos por faceta del catálogo de servicios.
 *
 * Lo que se protege acá es la regla central: cada faceta se cuenta con todos
 * los filtros activos salvo el de su propia dimensión.
 */
@ExtendWith(MockitoExtension.class)
class ServicioServiceFacetasTest {

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
    @Mock
    private ReseniaPublicaService reseniaPublicaService;
    @Mock
    private OfertaReactivacionService ofertaReactivacionService;
    @Mock
    private RecomendacionService recomendacionService;
    @Mock
    private VisitaService visitaService;

    @InjectMocks
    private ServicioService servicioService;

    private TipoServicio ilustracion;
    private TipoServicio emote;
    private Medio digital;
    private Medio tradicional;

    @BeforeEach
    void prepararCatalogos() {
        ilustracion = TipoServicio.builder().idTipoServicio(1L).nombre("Ilustración").build();
        emote = TipoServicio.builder().idTipoServicio(2L).nombre("Emote").build();
        digital = Medio.builder().idMedio(10L).nombre("Digital").build();
        tradicional = Medio.builder().idMedio(20L).nombre("Tradicional").build();
    }

    @Test
    void facetas_cuentaCadaOpcionConLosFiltrosDeLasOtrasDimensiones() {
        Servicio a = servicio(1L, ilustracion, digital, true);
        Servicio b = servicio(2L, ilustracion, tradicional, true);
        Servicio c = servicio(3L, emote, digital, true);

        when(servicioRepository.findAll(any(Specification.class), any(Sort.class)))
            .thenReturn(List.of(a, b, c));

        FacetasServicioDTO facetas = servicioService.facetas(new FiltroServicioRequest(), null);

        assertThat(facetas.getTipos())
            .containsEntry("1", 2L)
            .containsEntry("2", 1L);
        assertThat(facetas.getMedios())
            .containsEntry("10", 2L)
            .containsEntry("20", 1L);
    }

    @Test
    void facetas_alFiltrarPorUnMedio_losDemasMediosSiguenMostrandosuConteo() {
        Servicio a = servicio(1L, ilustracion, digital, true);
        Servicio b = servicio(2L, ilustracion, digital, true);
        Servicio c = servicio(3L, ilustracion, tradicional, true);

        when(servicioRepository.findAll(any(Specification.class), any(Sort.class)))
            .thenReturn(List.of(a, b, c));

        FiltroServicioRequest filtro = new FiltroServicioRequest();
        filtro.setIdsMedios(List.of(10L));

        FacetasServicioDTO facetas = servicioService.facetas(filtro, null);

        // La dimensión filtrada (medios) se cuenta sin su propio filtro: el
        // medio 20 sigue apareciendo con 1, no con 0.
        assertThat(facetas.getMedios())
            .containsEntry("10", 2L)
            .containsEntry("20", 1L);
    }

    @Test
    void facetas_dosEtiquetasDeLaMismaCategoria_seCuentanLasDosEtiquetas() {
        CategoriaEtiqueta estilo = categoria(100L, "Estilo");
        Etiqueta gotico = etiqueta(1000L, "Gótico", estilo);
        Etiqueta oscuro = etiqueta(1001L, "Oscuro", estilo);
        Etiqueta acuarela = etiqueta(1002L, "Acuarela", estilo);

        Servicio conDos = servicio(1L, ilustracion, digital, true);
        conDos.setEtiquetas(new LinkedHashSet<>(List.of(asignacion(conDos, gotico),
            asignacion(conDos, oscuro))));
        Servicio conUna = servicio(2L, ilustracion, digital, true);
        conUna.setEtiquetas(new LinkedHashSet<>(List.of(asignacion(conUna, acuarela))));

        when(servicioRepository.findAll(any(Specification.class), any(Sort.class)))
            .thenReturn(List.of(conDos, conUna));

        FacetasServicioDTO facetas = servicioService.facetas(new FiltroServicioRequest(), null);

        // El conteo es por etiqueta: cada servicio aporta a cada una de sus etiquetas.
        assertThat(facetas.getEtiquetas())
            .containsEntry("1000", 1L)
            .containsEntry("1001", 1L)
            .containsEntry("1002", 1L);
    }

    @Test
    void facetas_ignoraEtiquetasNoValidas() {
        CategoriaEtiqueta estilo = categoria(100L, "Estilo");
        Etiqueta valida = etiqueta(1000L, "Gótico", estilo);
        Etiqueta pendiente = etiqueta(1001L, "Pendiente", estilo);
        pendiente.setEstado(EtiquetaEstado.PENDIENTE);

        Servicio s = servicio(1L, ilustracion, digital, true);
        s.setEtiquetas(new LinkedHashSet<>(List.of(asignacion(s, valida),
            asignacion(s, pendiente))));

        when(servicioRepository.findAll(any(Specification.class), any(Sort.class)))
            .thenReturn(List.of(s));

        FacetasServicioDTO facetas = servicioService.facetas(new FiltroServicioRequest(), null);

        assertThat(facetas.getEtiquetas()).containsEntry("1000", 1L);
        assertThat(facetas.getEtiquetas()).doesNotContainKey("1001");
    }

    @Test
    void facetas_cuentaContenidosSensibles() {
        Servicio s = servicio(1L, ilustracion, digital, true);
        s.setContenidosSensibles(new HashSet<>(
            Set.of(ContenidoSensible.VIOLENCIA, ContenidoSensible.DESNUDOS)));

        when(servicioRepository.findAll(any(Specification.class), any(Sort.class)))
            .thenReturn(List.of(s));

        FacetasServicioDTO facetas = servicioService.facetas(new FiltroServicioRequest(), null);

        assertThat(facetas.getContenidosSensibles())
            .containsEntry("VIOLENCIA", 1L)
            .containsEntry("DESNUDOS", 1L);
    }

    @Test
    void facetas_conUnaSolaMoneda_noDevuelveLaFacetaDeMoneda() {
        Servicio s = servicio(1L, ilustracion, digital, true);
        when(servicioRepository.findAll(any(Specification.class), any(Sort.class)))
            .thenReturn(List.of(s));

        FacetasServicioDTO facetas = servicioService.facetas(new FiltroServicioRequest(), null);

        assertThat(facetas.getMonedas()).isEmpty();
    }

    @Test
    void facetas_conDosMonedas_devuelveLaFacetaDeMoneda() {
        Servicio enArs = servicio(1L, ilustracion, digital, true);
        Servicio enUsd = servicio(2L, ilustracion, digital, true);
        enUsd.setMoneda(Moneda.builder().idMoneda(2L).codigo("USD").build());

        when(servicioRepository.findAll(any(Specification.class), any(Sort.class)))
            .thenReturn(List.of(enArs, enUsd));

        FacetasServicioDTO facetas = servicioService.facetas(new FiltroServicioRequest(), null);

        assertThat(facetas.getMonedas())
            .containsEntry("ARS", 1L)
            .containsEntry("USD", 1L);
    }

    @Test
    void facetas_separaDisponiblesDeSinCupos() {
        Servicio libre = servicio(1L, ilustracion, digital, true);
        Servicio lleno = servicio(2L, ilustracion, digital, true);
        lleno.setCupos(0);

        when(servicioRepository.findAll(any(Specification.class), any(Sort.class)))
            .thenReturn(List.of(libre, lleno));

        FacetasServicioDTO facetas = servicioService.facetas(new FiltroServicioRequest(), null);

        assertThat(facetas.getDisponibilidad())
            .containsEntry("abiertos", 1L)
            .containsEntry("sinCupos", 1L);
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private Servicio servicio(Long id, TipoServicio tipo, Medio medio, boolean abierto) {
        Servicio s = new Servicio();
        s.setIdServicio(id);
        s.setTipoServicio(tipo);
        s.setMedio(medio);
        s.setMoneda(Moneda.builder().idMoneda(1L).codigo("ARS").build());
        s.setEstado(com.temipora.backend_springboot.modelo.EstadoServicio.PUBLICADO);
        s.setModoRecepcion(abierto
            ? ModoRecepcionSolicitud.ABIERTA
            : ModoRecepcionSolicitud.SOLO_ESPERA);
        s.setCupos(3);
        s.setContenidosSensibles(new HashSet<>());
        s.setEtiquetas(new LinkedHashSet<>());
        return s;
    }

    private CategoriaEtiqueta categoria(Long id, String nombre) {
        return CategoriaEtiqueta.builder()
            .idCategoriaEtiqueta(id)
            .nombre(nombre)
            .descripcion(nombre)
            .build();
    }

    private Etiqueta etiqueta(Long id, String nombre, CategoriaEtiqueta categoria) {
        return Etiqueta.builder()
            .idEtiqueta(id)
            .nombre(nombre)
            .descripcion(nombre)
            .categoria(categoria)
            .estado(EtiquetaEstado.VALIDA)
            .build();
    }

    private ServicioEtiqueta asignacion(Servicio servicio, Etiqueta etiqueta) {
        ServicioEtiqueta se = new ServicioEtiqueta();
        se.setId(new ServicioEtiquetaId());
        se.setServicio(servicio);
        se.setEtiqueta(etiqueta);
        return se;
    }
}
