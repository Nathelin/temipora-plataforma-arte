package com.temipora.backend_springboot.servicios;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import com.temipora.backend_springboot.modelo.ERol;
import com.temipora.backend_springboot.modelo.EventoAuditoria;
import com.temipora.backend_springboot.modelo.EventoAuditoriaDTO;
import com.temipora.backend_springboot.modelo.FiltroAuditoria;
import com.temipora.backend_springboot.modelo.Medio;
import com.temipora.backend_springboot.modelo.PaginaDTO;
import com.temipora.backend_springboot.modelo.Rol;
import com.temipora.backend_springboot.modelo.Usuario;
import com.temipora.backend_springboot.repositorios.EventoAuditoriaRepository;
import com.temipora.backend_springboot.repositorios.UsuarioRepository;

@ExtendWith(MockitoExtension.class)
class AuditoriaServiceTest {

    private static final List<CampoAuditado> CAMPOS = List.of(
        CampoAuditado.de("nombre", "Nombre"),
        CampoAuditado.de("descripcion", "Descripción"),
        CampoAuditado.de("activo", "Activo"));

    @Mock
    private EventoAuditoriaRepository eventoRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @InjectMocks
    private AuditoriaService auditoriaService;

    @AfterEach
    void limpiarContexto() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void registrar_sinAutenticacion_guardaOrigenSistema() {
        Medio medio = medio("Digital", true);
        Snapshot antes = Snapshot.de(medio, CAMPOS);
        medio.setNombre("Digital actualizado");

        auditoriaService.registrar(
            "MEDIO_MODIFICAR", "Medio", 5L, "Modificó el medio", antes, medio, CAMPOS);

        EventoAuditoria evento = capturarGuardado();
        assertThat(evento.getOrigen()).isEqualTo("SISTEMA");
        assertThat(evento.getActorNombre()).isNull();
        assertThat(evento.getActorId()).isNull();
        assertThat(evento.getEntidad()).isEqualTo("Medio");
        assertThat(evento.getEntidadId()).isEqualTo("5");
        assertThat(evento.getCambios()).contains("\"campo\":\"nombre\"");
        assertThat(evento.getCambios()).contains("Digital actualizado");
    }

    @Test
    void registrar_sinCambios_guardaListaVacia() {
        Medio medio = medio("Digital", true);
        Snapshot antes = Snapshot.de(medio, CAMPOS);

        auditoriaService.registrar(
            "MEDIO_EDITAR", "Medio", 5L, "Sin cambios", antes, medio, CAMPOS);

        assertThat(capturarGuardado().getCambios()).isEqualTo("[]");
    }

    @Test
    void registrar_creacion_guardaValoresNuevos() {
        Medio medio = medio("Digital", true);

        auditoriaService.registrar(
            "MEDIO_CREAR", "Medio", 5L, "Creó el medio", null, medio, CAMPOS);

        String cambios = capturarGuardado().getCambios();
        assertThat(cambios).contains("\"anterior\":null");
        assertThat(cambios).contains("Digital");
    }

    @Test
    void registrar_conAdmin_completaActor() {
        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken(
                "admin", null, List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))));
        when(usuarioRepository.findByNombreUsuarioIgnoreCase("admin"))
            .thenReturn(Optional.of(Usuario.builder()
                .idUsuario(1L)
                .nombreUsuario("admin")
                .rol(Rol.builder().nombre(ERol.ADMIN).build())
                .build()));

        auditoriaService.registrar("ACCION", "Entidad", 1L, "Resumen", null);

        EventoAuditoria evento = capturarGuardado();
        assertThat(evento.getOrigen()).isEqualTo("ADMIN");
        assertThat(evento.getActorId()).isEqualTo(1L);
        assertThat(evento.getActorNombre()).isEqualTo("admin");
        assertThat(evento.getActorRol()).isEqualTo("ADMIN");
    }

    @Test
    void registrar_conMotivo_loGuarda() {
        auditoriaService.registrar("ACCION", "Entidad", 1L, "Resumen", "Por reclamo");

        assertThat(capturarGuardado().getMotivo()).isEqualTo("Por reclamo");
    }

    @Test
    void listar_mapeaPaginaYCambios() {
        EventoAuditoria evento = new EventoAuditoria();
        evento.setIdEventoAuditoria(9L);
        evento.setOcurridoEn(LocalDateTime.now());
        evento.setAccion("ETIQUETA_APROBAR");
        evento.setEntidad("Etiqueta");
        evento.setEntidadId("3");
        evento.setResumen("Aprobó la etiqueta");
        evento.setCambios("[{\"campo\":\"estado\",\"etiqueta\":\"Estado\",\"anterior\":\"PENDIENTE\",\"nuevo\":\"VALIDA\"}]");
        evento.setOrigen("ADMIN");
        evento.setResultado("OK");

        when(eventoRepository.findAll(
                org.mockito.ArgumentMatchers.<Specification<EventoAuditoria>>any(),
                any(Pageable.class)))
            .thenReturn(new PageImpl<>(List.of(evento)));

        PaginaDTO<EventoAuditoriaDTO> pagina = auditoriaService.listar(
            new FiltroAuditoria(
                "admin", null, "Etiqueta", null, "ETIQUETA_APROBAR",
                null, null, null, null, null, null),
            0, 20);

        assertThat(pagina.getContenido()).hasSize(1);
        EventoAuditoriaDTO dto = pagina.getContenido().get(0);
        assertThat(dto.categoria()).isEqualTo("CATALOGO");
        assertThat(dto.critica()).isFalse();
        assertThat(dto.cambios()).hasSize(1);
        assertThat(dto.cambios().get(0).etiqueta()).isEqualTo("Estado");
        assertThat(dto.cambios().get(0).anterior()).isEqualTo("PENDIENTE");
        assertThat(dto.cambios().get(0).nuevo()).isEqualTo("VALIDA");
        assertThat(pagina.getTotalElementos()).isEqualTo(1);
    }

    @Test
    void listar_conCambiosInvalidos_devuelveListaVacia() {
        EventoAuditoria evento = new EventoAuditoria();
        evento.setCambios("no-es-json");
        when(eventoRepository.findAll(
                org.mockito.ArgumentMatchers.<Specification<EventoAuditoria>>any(),
                any(Pageable.class)))
            .thenReturn(new PageImpl<>(List.of(evento)));

        PaginaDTO<EventoAuditoriaDTO> pagina = auditoriaService.listar(
            new FiltroAuditoria(
                null, null, null, null, null, null, null, null, null, null, null),
            0, 20);

        assertThat(pagina.getContenido().get(0).cambios()).isEmpty();
    }

    @Test
    void registrarIntentoFallido_guardaElUsuarioIntentado() {
        auditoriaService.registrarIntentoFallido("  artista777  ");

        EventoAuditoria evento = capturarGuardado();
        assertThat(evento.getAccion()).isEqualTo(AccionesAdmin.LOGIN_FALLIDO);
        assertThat(evento.getActorNombre()).isEqualTo("artista777");
        assertThat(evento.getOrigen()).isEqualTo("SISTEMA");
        assertThat(evento.getResumen()).contains("artista777");
    }

    @Test
    void listar_cuentaLosEventosDeLaMismaOperacion() {
        EventoAuditoria uno = new EventoAuditoria();
        uno.setIdEventoAuditoria(1L);
        uno.setRequestId("req-1");
        uno.setAccion("PEDIDO_CREAR");
        uno.setCambios("[]");
        EventoAuditoria dos = new EventoAuditoria();
        dos.setIdEventoAuditoria(2L);
        dos.setRequestId("req-1");
        dos.setAccion("PAGO_INICIAR");
        dos.setCambios("[]");
        when(eventoRepository.findAll(
                org.mockito.ArgumentMatchers.<Specification<EventoAuditoria>>any(),
                any(Pageable.class)))
            .thenReturn(new PageImpl<>(List.of(uno, dos)));
        when(eventoRepository.contarPorRequestIds(any()))
            .thenReturn(List.<Object[]>of(new Object[] { "req-1", 2L }));

        var pagina = auditoriaService.listar(
            new FiltroAuditoria(
                null, null, null, null, null, null, null, null, null, null, null),
            0, 20);

        assertThat(pagina.getContenido())
            .allSatisfy(dto -> assertThat(dto.eventosOperacion()).isEqualTo(2));
    }

    @Test
    void opciones_devuelveLosValoresDelRepositorio() {
        when(eventoRepository.entidadesRegistradas()).thenReturn(List.of("Etiqueta"));
        when(eventoRepository.accionesRegistradas()).thenReturn(List.of("ETIQUETA_APROBAR"));

        var opciones = auditoriaService.opciones();

        assertThat(opciones.entidades()).containsExactly("Etiqueta");
        assertThat(opciones.acciones()).containsExactly("ETIQUETA_APROBAR");
        assertThat(opciones.categorias())
            .anySatisfy(categoria -> {
                assertThat(categoria.codigo()).isEqualTo("COMERCIO");
                assertThat(categoria.etiqueta()).isEqualTo("Comercio");
            });
    }

    @Test
    void obtener_devuelveElEventoPorId() {
        EventoAuditoria evento = new EventoAuditoria();
        evento.setIdEventoAuditoria(9L);
        evento.setAccion("PAGO_CONFIRMAR");
        evento.setOrigen("CLIENTE");
        evento.setResultado("OK");
        evento.setCambios("[]");
        when(eventoRepository.findById(9L)).thenReturn(Optional.of(evento));

        EventoAuditoriaDTO dto = auditoriaService.obtener(9L);

        assertThat(dto.idEventoAuditoria()).isEqualTo(9L);
        assertThat(dto.categoria()).isEqualTo("COMERCIO");
        assertThat(dto.critica()).isTrue();
    }

    @Test
    void resumen_devuelveLosConteosDelRango() {
        LocalDateTime desde = LocalDateTime.now().minusHours(24);
        LocalDateTime hasta = LocalDateTime.now();
        when(eventoRepository.countByOcurridoEnBetween(desde, hasta))
            .thenReturn(10L);
        when(eventoRepository.countByAccionInAndOcurridoEnBetween(
            any(), eq(desde), eq(hasta))).thenReturn(4L);
        when(eventoRepository.countByAccionAndOcurridoEnBetween(
            AccionesAdmin.LOGIN_FALLIDO, desde, hasta)).thenReturn(2L);

        var resumen = auditoriaService.resumen(desde, hasta);

        assertThat(resumen.total()).isEqualTo(10);
        assertThat(resumen.criticas()).isEqualTo(4);
        assertThat(resumen.loginFallido()).isEqualTo(2);
    }

    private EventoAuditoria capturarGuardado() {
        ArgumentCaptor<EventoAuditoria> captor = ArgumentCaptor.forClass(EventoAuditoria.class);
        org.mockito.Mockito.verify(eventoRepository, org.mockito.Mockito.atLeastOnce()).save(captor.capture());
        return captor.getValue();
    }

    private Medio medio(String nombre, boolean activo) {
        return Medio.builder()
            .idMedio(5L)
            .nombre(nombre)
            .descripcion("descripcion")
            .activo(activo)
            .build();
    }
}
