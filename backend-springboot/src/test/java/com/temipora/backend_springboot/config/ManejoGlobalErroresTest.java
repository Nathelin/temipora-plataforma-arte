package com.temipora.backend_springboot.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import java.util.NoSuchElementException;

import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import com.temipora.backend_springboot.modelo.ERol;
import com.temipora.backend_springboot.modelo.RegistroUsuarioDTO;

class ManejoGlobalErroresTest {

    private final ManejoGlobalErrores manejo = new ManejoGlobalErrores();

    @Test
    void argumentoInvalido_devuelve400ConMensaje() {
        ResponseEntity<Map<String, String>> respuesta = manejo.manejarArgumentoInvalido(
                new IllegalArgumentException("No se puede auto-registrar un usuario con rol ADMIN"));

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(respuesta.getBody())
                .containsEntry("message", "No se puede auto-registrar un usuario con rol ADMIN");
    }

    @Test
    void noEncontrado_devuelve404ConMensaje() {
        ResponseEntity<Map<String, String>> respuesta = manejo.manejarNoEncontrado(
                new NoSuchElementException("Usuario no encontrado: 99"));

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(respuesta.getBody()).containsEntry("message", "Usuario no encontrado: 99");
    }

    @Test
    void validacion_devuelve400ConElPrimerErrorDeCampo() throws NoSuchMethodException {
        RegistroUsuarioDTO dto = new RegistroUsuarioDTO();
        BeanPropertyBindingResult binding = new BeanPropertyBindingResult(dto, "registroUsuarioDTO");
        binding.addError(new FieldError("registroUsuarioDTO", "correo", "El correo no es válido"));
        binding.addError(new FieldError("registroUsuarioDTO", "contrasenia", "La contraseña debe tener al menos 8 caracteres"));
        MethodArgumentNotValidException excepcion = new MethodArgumentNotValidException(parametroDummy(), binding);

        ResponseEntity<Map<String, String>> respuesta = manejo.manejarValidacion(excepcion);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(respuesta.getBody()).containsEntry("message", "El correo no es válido");
    }

    @Test
    void validacion_sinErrores_devuelveMensajePorDefecto() throws NoSuchMethodException {
        RegistroUsuarioDTO dto = new RegistroUsuarioDTO();
        BeanPropertyBindingResult binding = new BeanPropertyBindingResult(dto, "registroUsuarioDTO");
        MethodArgumentNotValidException excepcion = new MethodArgumentNotValidException(parametroDummy(), binding);

        ResponseEntity<Map<String, String>> respuesta = manejo.manejarValidacion(excepcion);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(respuesta.getBody()).containsEntry("message", "Datos inválidos.");
    }

    @Test
    void tipoDeParametroInvalido_devuelve400IndicandoElNombreDelParametro() throws NoSuchMethodException {
        MethodArgumentTypeMismatchException excepcion = new MethodArgumentTypeMismatchException(
                "PEPE", ERol.class, "rol", parametroDummy(), null);

        ResponseEntity<Map<String, String>> respuesta = manejo.manejarTipoInvalido(excepcion);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(respuesta.getBody()).containsEntry("message", "Parámetro inválido: rol");
    }

    @Test
    void violacionDeIntegridad_devuelve409ConMensajeGenerico() {
        ResponseEntity<Map<String, String>> respuesta = manejo.manejarIntegridadDatos(
                new DataIntegrityViolationException("could not execute statement"));

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(respuesta.getBody()).containsEntry("message",
                "Los datos no se pudieron guardar porque no son compatibles con la base de datos.");
    }

    // Spring 7 exige un MethodParameter real en los constructores de estas excepciones
    private org.springframework.core.MethodParameter parametroDummy() throws NoSuchMethodException {
        return new org.springframework.core.MethodParameter(
                ManejoGlobalErrores.class.getDeclaredMethod("manejarTipoInvalido",
                        MethodArgumentTypeMismatchException.class),
                0);
    }
}
