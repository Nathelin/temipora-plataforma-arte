package com.temipora.backend_springboot.config;

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
            .body(Map.of("message", "Los datos no se pudieron guardar porque no son compatibles con la base de datos."));
    }
}