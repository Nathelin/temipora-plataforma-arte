package com.temipora.backend_springboot.controladores;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.temipora.backend_springboot.modelo.RegistroUsuarioDTO;
import com.temipora.backend_springboot.modelo.Usuario;
import com.temipora.backend_springboot.modelo.UsuarioDTO;
import com.temipora.backend_springboot.servicios.StorageService;
import com.temipora.backend_springboot.servicios.UsuarioService;
import com.temipora.backend_springboot.servicios.FormatoArchivoInvalidoException;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api")
public class AuthController {

    private final UsuarioService usuarioService;
    private final StorageService storageService;

    public AuthController(UsuarioService usuarioService, StorageService storageService) {
        this.usuarioService = usuarioService;
        this.storageService = storageService;
    }

    @PostMapping("/auth/registrarse")
    public ResponseEntity<?> registrarse(@Valid @RequestBody RegistroUsuarioDTO dto) {
        if (!dto.getContrasenia().equals(dto.getConfirmarContrasenia())) {
            return ResponseEntity.badRequest().body(Map.of("message", "Las contraseñas no coinciden."));
        }

        if (usuarioService.existeNombreUsuario(dto.getNombreUsuario())) {
            return ResponseEntity.badRequest().body(Map.of("message", "El nombre de usuario ya está en uso."));
        }

        Usuario usuario = usuarioService.registrarUsuario(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(UsuarioDTO.desdeUsuario(usuario));
    }

    @PostMapping("/upload")
    public ResponseEntity<Map<String, String>> subirArchivo(
            @RequestPart("archivo") MultipartFile archivo) {
        try {
            String url = storageService.guardar(archivo);
            return ResponseEntity.ok(Map.of("url", url == null ? "" : url));
        } catch (FormatoArchivoInvalidoException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @GetMapping("/checkNombreUsuario")
    public ResponseEntity<Map<String, Boolean>> chequearNombreUsuario(
            @RequestParam("nombreUsuario") String nombreUsuario) {
        boolean existe = usuarioService.existeNombreUsuario(nombreUsuario);
        return ResponseEntity.ok(Map.of("disponible", !existe));
    }
}