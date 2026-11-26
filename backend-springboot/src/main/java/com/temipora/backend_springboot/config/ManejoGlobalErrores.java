package com.temipora.backend_springboot.config;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.NoSuchElementException;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import com.temipora.backend_springboot.servicios.AccesoDenegadoException;
import com.temipora.backend_springboot.servicios.ConflictoException;
import com.temipora.backend_springboot.servicios.CredencialesInvalidasException;
import com.temipora.backend_springboot.servicios.ReactivacionVencidaException;
import com.temipora.backend_springboot.servicios.ServicioNoDisponibleException;
import com.temipora.backend_springboot.servicios.pagos.mercadopago.MercadoPagoException;

@RestControllerAdvice
public class ManejoGlobalErrores {

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> manejarArgumentoInvalido(IllegalArgumentException e) {
        return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
    }

    @ExceptionHandler(AccesoDenegadoException.class)
    public ResponseEntity<Map<String, String>> manejarAccesoDenegado(AccesoDenegadoException e) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", e.getMessage()));
    }

    @ExceptionHandler(ConflictoException.class)
    public ResponseEntity<Map<String, String>> manejarConflicto(ConflictoException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("message", e.getMessage()));
    }

    @ExceptionHandler(CredencialesInvalidasException.class)
    public ResponseEntity<Map<String, String>> manejarCredencialesInvalidas(
            CredencialesInvalidasException e) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
            .body(Map.of("message", e.getMessage()));
    }

    @ExceptionHandler(ReactivacionVencidaException.class)
    public ResponseEntity<Map<String, Object>> manejarReactivacionVencida(
            ReactivacionVencidaException e) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("codigo", "REACTIVACION_VENCIDA");
        body.put("message", e.getMessage());
        if (e.getContacto() != null) {
            body.put("contacto", e.getContacto());
        }
        if (e.getReactivarHasta() != null) {
            body.put("reactivarHasta", e.getReactivarHasta().toString());
        }
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(body);
    }

    @ExceptionHandler(ServicioNoDisponibleException.class)
    public ResponseEntity<Map<String, String>> manejarServicioNoDisponible(
            ServicioNoDisponibleException e) {
        return ResponseEntity.status(HttpStatus.GONE).body(Map.of("message", e.getMessage()));
    }

    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<Map<String, String>> manejarNoEncontrado(NoSuchElementException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", e.getMessage()));
    }

    @ExceptionHandler(MercadoPagoException.class)
    public ResponseEntity<Map<String, String>> manejarMercadoPago(MercadoPagoException e) {
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
            .body(Map.of("message", e.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> manejarValidacion(MethodArgumentNotValidException e) {
        String mensaje = e.getBindingResult().getFieldErrors().stream()
            .findFirst()
            .map(err -> err.getDefaultMessage())
            .orElse("Datos inválidos.");
        return ResponseEntity.badRequest().body(Map.of("message", mensaje));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Map<String, String>> manejarTipoInvalido(MethodArgumentTypeMismatchException e) {
        return ResponseEntity.badRequest().body(Map.of("message", "Parámetro inválido: " + e.getName()));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, String>> manejarIntegridadDatos(DataIntegrityViolationException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
            .body(Map.of("message", mensajeDeIntegridad(e)));
    }

    /**
     * Traduce las violaciones de constraint más frecuentes a un mensaje que el
     * usuario pueda accionar, en lugar de un 409 genérico que no dice nada.
     */
    private String mensajeDeIntegridad(DataIntegrityViolationException e) {
        String detalle = causasDe(e).toLowerCase(Locale.ROOT);

        if (detalle.contains("uk_terminos_plataforma_vigente")) {
            return "No se pudo publicar la nueva versión de los términos. "
                + "Volvé a intentarlo.";
        }
        if (detalle.contains("uk_entregable_variante")) {
            return "No se pudieron guardar los archivos entregables de la variante. "
                + "Volvé a intentarlo; si el problema sigue, quitá y volvé a marcar "
                + "los archivos.";
        }
        if (detalle.contains("uk_entregable_comun")) {
            return "No se pudieron guardar los archivos comunes. Volvé a intentarlo; "
                + "si el problema sigue, quitá y volvé a agregarlos.";
        }
        if (detalle.contains("uk_variante_sha_unica")) {
            return "Esa imagen ya está registrada como pieza única en otro producto. "
                + "Una pieza única no puede publicarse dos veces.";
        }
        if (detalle.contains("uk_licencia_producto_vigente")) {
            return "El producto ya tiene una licencia vigente. Volvé a intentarlo.";
        }
        if (detalle.contains("uk_producto_artista_titulo_tipo")) {
            return "Ya tenés otro producto publicado con ese título y tipo.";
        }
        if (detalle.contains("ck_variante_precio")) {
            return "El precio de la variante no es válido.";
        }
        if (detalle.contains("ck_producto_estado")) {
            return "El estado del producto no es válido.";
        }
        if (detalle.contains("producto_variante_precio")
                || (detalle.contains("null value in column \"precio\""))) {
            return "Falta el precio de la variante.";
        }
        if (detalle.contains("foreign key")) {
            return "El producto hace referencia a un dato que ya no existe.";
        }
        return "Los datos no se pudieron guardar porque no son compatibles con la base de datos.";
    }

    private String causasDe(Throwable error) {
        StringBuilder texto = new StringBuilder();
        Throwable actual = error;
        int profundidad = 0;
        while (actual != null && profundidad < 10) {
            if (actual.getMessage() != null) {
                texto.append(actual.getMessage()).append(' ');
            }
            actual = actual.getCause();
            profundidad++;
        }
        return texto.toString();
    }
}