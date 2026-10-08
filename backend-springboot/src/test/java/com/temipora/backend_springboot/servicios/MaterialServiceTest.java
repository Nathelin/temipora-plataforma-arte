package com.temipora.backend_springboot.servicios;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.temipora.backend_springboot.modelo.Material;
import com.temipora.backend_springboot.modelo.MaterialDTO;
import com.temipora.backend_springboot.modelo.MaterialRequest;
import com.temipora.backend_springboot.modelo.Medio;
import com.temipora.backend_springboot.modelo.TipoArte;
import com.temipora.backend_springboot.repositorios.MaterialRepository;
import com.temipora.backend_springboot.repositorios.TipoArteRepository;

@ExtendWith(MockitoExtension.class)
class MaterialServiceTest {

    @Mock
    private MaterialRepository materialRepository;

    @Mock
    private TipoArteRepository tipoArteRepository;

    @InjectMocks
    private MaterialService materialService;

    // ---------------------------------------------------------------
    // listar / selector
    // ---------------------------------------------------------------

    @Test
    void listar_mapeaEntidadesADtos() {
        when(materialRepository.findAllByOrderByNombreAsc())
                .thenReturn(List.of(material(1L, "Procreate", true), material(2L, "Lápiz", false)));

        List<MaterialDTO> resultado = materialService.listar();

        assertThat(resultado).hasSize(2);
        assertThat(resultado).extracting(MaterialDTO::getNombre)
                .containsExactly("Procreate", "Lápiz");
        assertThat(resultado.get(0).getNombreTipoArte()).isEqualTo("Digital");
        assertThat(resultado.get(1).isActivo()).isFalse();
    }

    @Test
    void selector_pasaElIdMedioYmapeaDtos() {
        when(materialRepository.buscarSelector(1L))
                .thenReturn(List.of(material(1L, "Procreate", true)));

        List<MaterialDTO> resultado = materialService.selector(1L);

        verify(materialRepository).buscarSelector(1L);
        assertThat(resultado).extracting(MaterialDTO::getNombre).containsExactly("Procreate");
    }

    @Test
    void selector_sinMedio_pasaNulo() {
        when(materialRepository.buscarSelector(isNull())).thenReturn(List.of());

        List<MaterialDTO> resultado = materialService.selector(null);

        assertThat(resultado).isEmpty();
        verify(materialRepository).buscarSelector(null);
    }

    // ---------------------------------------------------------------
    // crear
    // ---------------------------------------------------------------

    @Test
    void crear_nombreNoRepetidoYTipoActivo_guardaActivo() {
        MaterialRequest request = request("  Procreate  ", "Ilustracion digital", 1L, null);
        when(materialRepository.existsByNombreIgnoreCase("Procreate")).thenReturn(false);
        when(tipoArteRepository.findByIdTipoArte(1L)).thenReturn(Optional.of(tipoArte(1L, true)));
        when(materialRepository.save(any(Material.class))).thenAnswer(inv -> inv.getArgument(0));

        MaterialDTO resultado = materialService.crear(request);

        ArgumentCaptor<Material> captor = ArgumentCaptor.forClass(Material.class);
        verify(materialRepository).save(captor.capture());
        Material guardado = captor.getValue();
        assertThat(guardado.getNombre()).isEqualTo("Procreate");
        assertThat(guardado.getTipoArte().getIdTipoArte()).isEqualTo(1L);
        assertThat(guardado.isActivo()).isTrue();
        assertThat(resultado.getIdMedio()).isEqualTo(1L);
        assertThat(resultado.getNombreMedio()).isEqualTo("Digital");
    }

    @Test
    void crear_nombreRepetido_lanzaError() {
        MaterialRequest request = request("Procreate", "desc", 1L, null);
        when(materialRepository.existsByNombreIgnoreCase("Procreate")).thenReturn(true);

        assertThatThrownBy(() -> materialService.crear(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Ya existe un material con ese nombre.");

        verify(materialRepository, never()).save(any(Material.class));
    }

    @Test
    void crear_tipoInexistente_lanzaNoSuchElement() {
        MaterialRequest request = request("Procreate", "desc", 99L, null);
        when(materialRepository.existsByNombreIgnoreCase("Procreate")).thenReturn(false);
        when(tipoArteRepository.findByIdTipoArte(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> materialService.crear(request))
                .isInstanceOf(NoSuchElementException.class)
                .hasMessage("Tipo de arte no encontrado: 99");
    }

    @Test
    void crear_tipoDadoDeBaja_lanzaError() {
        MaterialRequest request = request("Procreate", "desc", 1L, null);
        when(materialRepository.existsByNombreIgnoreCase("Procreate")).thenReturn(false);
        when(tipoArteRepository.findByIdTipoArte(1L)).thenReturn(Optional.of(tipoArte(1L, false)));

        assertThatThrownBy(() -> materialService.crear(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("El tipo de arte 'Digital' fue dado de baja y no puede recibir materiales nuevos.");

        verify(materialRepository, never()).save(any(Material.class));
    }

    // ---------------------------------------------------------------
    // modificar
    // ---------------------------------------------------------------

    @Test
    void modificar_ok_actualizaCampos() {
        Material existente = material(1L, "Procreate", true);
        when(materialRepository.findByIdMaterial(1L)).thenReturn(Optional.of(existente));
        when(materialRepository.existsByNombreIgnoreCaseAndIdMaterialNot("Procreate 2", 1L))
                .thenReturn(false);
        when(tipoArteRepository.findByIdTipoArte(1L)).thenReturn(Optional.of(tipoArte(1L, true)));
        when(materialRepository.save(any(Material.class))).thenAnswer(inv -> inv.getArgument(0));

        MaterialDTO resultado = materialService
                .modificar(1L, request("Procreate 2", "Nueva desc", 1L, "obs"));

        assertThat(resultado.getNombre()).isEqualTo("Procreate 2");
        assertThat(resultado.getDescripcion()).isEqualTo("Nueva desc");
        assertThat(resultado.getObservaciones()).isEqualTo("obs");
    }

    @Test
    void modificar_alTipoInactivoDelMismoMaterial_loPermite() {
        Material existente = material(1L, "Procreate", true);
        existente.setTipoArte(tipoArte(1L, false));
        when(materialRepository.findByIdMaterial(1L)).thenReturn(Optional.of(existente));
        when(materialRepository.existsByNombreIgnoreCaseAndIdMaterialNot("Procreate", 1L))
                .thenReturn(false);
        when(tipoArteRepository.findByIdTipoArte(1L)).thenReturn(Optional.of(tipoArte(1L, false)));
        when(materialRepository.save(any(Material.class))).thenAnswer(inv -> inv.getArgument(0));

        MaterialDTO resultado = materialService
                .modificar(1L, request("Procreate", "desc", 1L, null));

        assertThat(resultado.getNombre()).isEqualTo("Procreate");
    }

    @Test
    void modificar_aOtroTipoInactivo_lanzaError() {
        Material existente = material(1L, "Procreate", true);
        when(materialRepository.findByIdMaterial(1L)).thenReturn(Optional.of(existente));
        when(materialRepository.existsByNombreIgnoreCaseAndIdMaterialNot("Procreate", 1L))
                .thenReturn(false);
        when(tipoArteRepository.findByIdTipoArte(2L)).thenReturn(Optional.of(tipoArteInactivo()));

        assertThatThrownBy(() -> materialService.modificar(1L, request("Procreate", "desc", 2L, null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("El tipo de arte 'Tradicional' fue dado de baja y no puede asignarse.");

        verify(materialRepository, never()).save(any(Material.class));
    }

    @Test
    void modificar_materialInexistente_lanzaNoSuchElement() {
        when(materialRepository.findByIdMaterial(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> materialService.modificar(99L, request("X", "desc", 1L, null)))
                .isInstanceOf(NoSuchElementException.class)
                .hasMessage("Material no encontrado: 99");
    }

    // ---------------------------------------------------------------
    // darDeBaja / darDeAlta
    // ---------------------------------------------------------------

    @Test
    void darDeBaja_enUso_desactivaConservandoElRegistro() {
        when(materialRepository.findByIdMaterial(1L)).thenReturn(Optional.of(material(1L, "Procreate", true)));
        when(materialRepository.save(any(Material.class))).thenAnswer(inv -> inv.getArgument(0));

        MaterialDTO resultado = materialService.darDeBaja(1L);

        assertThat(resultado.isActivo()).isFalse();
    }

    @Test
    void darDeBaja_materialInexistente_lanzaNoSuchElement() {
        when(materialRepository.findByIdMaterial(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> materialService.darDeBaja(99L))
                .isInstanceOf(NoSuchElementException.class);
    }

    @Test
    void darDeAlta_tipoActivo_reactiva() {
        Material existente = material(1L, "Procreate", false);
        when(materialRepository.findByIdMaterial(1L)).thenReturn(Optional.of(existente));
        when(materialRepository.save(any(Material.class))).thenAnswer(inv -> inv.getArgument(0));

        MaterialDTO resultado = materialService.darDeAlta(1L);

        assertThat(resultado.isActivo()).isTrue();
    }

    @Test
    void darDeAlta_tipoDadoDeBaja_lanzaError() {
        Material existente = material(1L, "Procreate", false);
        existente.setTipoArte(tipoArte(1L, false));
        when(materialRepository.findByIdMaterial(1L)).thenReturn(Optional.of(existente));

        assertThatThrownBy(() -> materialService.darDeAlta(1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("El tipo de arte 'Digital' está dado de baja; reactívalo antes de dar de alta el material.");

        verify(materialRepository, never()).save(any(Material.class));
    }

    // ---------------------------------------------------------------
    // helpers
    // ---------------------------------------------------------------

    private Medio medio(Long id, String nombre) {
        return Medio.builder()
                .idMedio(id)
                .nombre(nombre)
                .activo(true)
                .build();
    }

    private TipoArte tipoArte(Long id, boolean activo) {
        return TipoArte.builder()
                .idTipoArte(id)
                .nombre("Digital")
                .medio(medio(1L, "Digital"))
                .activo(activo)
                .build();
    }

    private TipoArte tipoArteInactivo() {
        return TipoArte.builder()
                .idTipoArte(2L)
                .nombre("Tradicional")
                .medio(medio(2L, "Tradicional"))
                .activo(false)
                .build();
    }

    private Material material(Long id, String nombre, boolean activo) {
        return Material.builder()
                .idMaterial(id)
                .nombre(nombre)
                .descripcion("descripcion")
                .tipoArte(tipoArte(1L, true))
                .observaciones(null)
                .activo(activo)
                .build();
    }

    private MaterialRequest request(String nombre, String descripcion, Long idTipoArte, String observaciones) {
        MaterialRequest request = new MaterialRequest();
        request.setNombre(nombre);
        request.setDescripcion(descripcion);
        request.setIdTipoArte(idTipoArte);
        request.setObservaciones(observaciones);
        return request;
    }
}
