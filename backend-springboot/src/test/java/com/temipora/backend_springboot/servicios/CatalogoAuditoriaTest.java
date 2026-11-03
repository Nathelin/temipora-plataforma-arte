package com.temipora.backend_springboot.servicios;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

import org.junit.jupiter.api.Test;

class CatalogoAuditoriaTest {

    @Test
    void todasLasAccionesTienenCategoria() throws Exception {
        for (Field campo : AccionesAdmin.class.getDeclaredFields()) {
            if (Modifier.isStatic(campo.getModifiers())
                    && campo.getType() == String.class) {
                String accion = (String) campo.get(null);
                assertThat(CatalogoAuditoria.categoriaDe(accion))
                    .as("categoría de %s", accion)
                    .isNotEqualTo(CategoriaAuditoria.OTROS);
            }
        }
    }

    @Test
    void dineroYCambiosDeSeguridadSonCriticos() {
        assertThat(CatalogoAuditoria.esCritica(
            AccionesAdmin.PAGO_CONFIRMAR)).isTrue();
        assertThat(CatalogoAuditoria.esCritica(
            AccionesAdmin.PEDIDO_CANCELAR)).isTrue();
        assertThat(CatalogoAuditoria.esCritica(
            AccionesAdmin.USUARIO_CAMBIAR_CONTRASENIA)).isTrue();
        assertThat(CatalogoAuditoria.esCritica(
            AccionesAdmin.ETIQUETA_CREAR)).isFalse();
        assertThat(CatalogoAuditoria.esCritica(
            AccionesAdmin.LOGIN_FALLIDO)).isFalse();
    }

    @Test
    void accionesDeUnaCategoriaSoloDevuelveLasSuyas() {
        assertThat(CatalogoAuditoria.accionesDe("COMERCIO"))
            .contains(AccionesAdmin.PAGO_CONFIRMAR)
            .doesNotContain(AccionesAdmin.ETIQUETA_CREAR);
        assertThat(CatalogoAuditoria.accionesDe("NO_EXISTE")).isEmpty();
    }
}
