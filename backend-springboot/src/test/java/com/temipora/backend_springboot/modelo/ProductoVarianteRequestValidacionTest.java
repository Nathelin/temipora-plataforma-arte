package com.temipora.backend_springboot.modelo;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

/**
 * Un borrador puede tener una variante sin imagen todavía. La huella vacía
 * debe pasar la validación del request (el borrador no exige archivos) sin
 * abrir la puerta a huellas con formato inválido.
 */
class ProductoVarianteRequestValidacionTest {

    private static ValidatorFactory fabrica;
    private static Validator validador;

    @BeforeAll
    static void preparar() {
        fabrica = Validation.buildDefaultValidatorFactory();
        validador = fabrica.getValidator();
    }

    @AfterAll
    static void cerrar() {
        fabrica.close();
    }

    @Test
    void sha256VacioEsValidoParaUnBorrador() {
        ProductoVarianteRequest request = variante();
        request.setSha256("");

        assertThat(validador.validate(request)).isEmpty();
    }

    @Test
    void sha256NuloEsValidoParaUnBorrador() {
        ProductoVarianteRequest request = variante();
        request.setSha256(null);

        assertThat(validador.validate(request)).isEmpty();
    }

    @Test
    void sha256ValidoPasa() {
        ProductoVarianteRequest request = variante();
        request.setSha256("b".repeat(64));

        assertThat(validador.validate(request)).isEmpty();
    }

    @Test
    void sha256ConFormatoInvalidoSigueRechazado() {
        ProductoVarianteRequest request = variante();
        request.setSha256("no-es-un-hash");

        Set<ConstraintViolation<ProductoVarianteRequest>> errores =
            validador.validate(request);

        assertThat(errores).hasSize(1);
        assertThat(errores.iterator().next().getMessage())
            .isEqualTo("La huella SHA-256 de la imagen no es válida");
    }

    @Test
    void listaDeEntregablesVaciaEsValida() {
        ProductoVarianteRequest request = variante();
        request.setEntregablesSha256(java.util.List.of());

        assertThat(validador.validate(request)).isEmpty();
    }

    @Test
    void listaDeEntregablesConHashInvalidoSeRechaza() {
        ProductoVarianteRequest request = variante();
        request.setEntregablesSha256(java.util.List.of("no-es-un-hash"));

        Set<ConstraintViolation<ProductoVarianteRequest>> errores =
            validador.validate(request);

        assertThat(errores).hasSize(1);
    }

    @Test
    void listaDeEntregablesConVariosHasgosValidosPasa() {
        ProductoVarianteRequest request = variante();
        request.setEntregablesSha256(java.util.List.of(
            "b".repeat(64), "c".repeat(64), "d".repeat(64)));

        assertThat(validador.validate(request)).isEmpty();
    }

    @Test
    void precioNuloEsValidoParaUnBorrador() {
        ProductoVarianteRequest request = variante();
        request.setPrecio(null);

        assertThat(validador.validate(request)).isEmpty();
    }

    private ProductoVarianteRequest variante() {
        ProductoVarianteRequest request = new ProductoVarianteRequest();
        request.setNombre("Única");
        request.setOrden(0);
        request.setStockDisponible(null);
        return request;
    }
}
