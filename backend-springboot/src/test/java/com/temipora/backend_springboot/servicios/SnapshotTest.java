package com.temipora.backend_springboot.servicios;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.temipora.backend_springboot.modelo.EtiquetaEstado;
import com.temipora.backend_springboot.modelo.Medio;

class SnapshotTest {

    @Test
    void valorDe_leeGetterYGetterBooleano() {
        Medio medio = Medio.builder().idMedio(1L).nombre("Digital").activo(true).build();

        assertThat(Snapshot.valorDe(medio, "nombre")).isEqualTo("Digital");
        assertThat(Snapshot.valorDe(medio, "activo")).isEqualTo(true);
    }

    @Test
    void valorDe_propiedadInexistente_devuelveNull() {
        Medio medio = Medio.builder().nombre("Digital").build();

        assertThat(Snapshot.valorDe(medio, "inexistente")).isNull();
        assertThat(Snapshot.valorDe(null, "nombre")).isNull();
    }

    @Test
    void valorLegible_traduceEscalaresYEnums() {
        assertThat(Snapshot.valorLegible(null)).isNull();
        assertThat(Snapshot.valorLegible("texto")).isEqualTo("texto");
        assertThat(Snapshot.valorLegible(7)).isEqualTo(7);
        assertThat(Snapshot.valorLegible(false)).isEqualTo(false);
        assertThat(Snapshot.valorLegible(EtiquetaEstado.PENDIENTE)).isEqualTo("PENDIENTE");
    }

    @Test
    void valorLegible_deEntidadConNombre_usaElNombre() {
        Medio medio = Medio.builder().nombre("Digital").build();

        assertThat(Snapshot.valorLegible(medio)).isEqualTo("Digital");
    }

    @Test
    void de_guardaSoloLosCamposPedidos() {
        Medio medio = Medio.builder().nombre("Digital").descripcion("d").activo(true).build();

        Snapshot snapshot = Snapshot.de(medio, List.of(CampoAuditado.de("nombre", "Nombre")));

        assertThat(snapshot.get("nombre")).isEqualTo("Digital");
        assertThat(snapshot.get("descripcion")).isNull();
    }
}
