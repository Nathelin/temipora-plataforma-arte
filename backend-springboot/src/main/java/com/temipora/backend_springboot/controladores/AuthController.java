package com.temipora.backend_springboot.controladores;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.temipora.backend_springboot.modelo.ReactivarCuentaRequest;
import com.temipora.backend_springboot.modelo.RegistroUsuarioDTO;
import com.temipora.backend_springboot.modelo.Usuario;
import com.temipora.backend_springboot.modelo.UsuarioDTO;
import com.temipora.backend_springboot.servicios.BajaCuentaService;
import com.temipora.backend_springboot.servicios.LimpiezaArchivosService;
import com.temipora.backend_springboot.servicios.StorageService;
import com.temipora.backend_springboot.servicios.UsuarioService;
import com.temipora.backend_springboot.servicios.FormatoArchivoInvalidoException;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api")
public class AuthController {

    private final UsuarioService usuarioService;
    private final StorageService storageService;
    private final LimpiezaArchivosService limpiezaArchivosService;
    private final BajaCuentaService bajaCuentaService;

    public AuthController(UsuarioService usuarioService, StorageService storageService,
            LimpiezaArchivosService limpiezaArchivosService,
            BajaCuentaService bajaCuentaService) {
        this.usuarioService = usuarioService;
        this.storageService = storageService;
        this.limpiezaArchivosService = limpiezaArchivosService;
        this.bajaCuentaService = bajaCuentaService;
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

    @PostMapping("/auth/reactivar-cuenta")
    public ResponseEntity<Map<String, Object>> reactivarCuenta(
            @Valid @RequestBody ReactivarCuentaRequest dto) {
        return ResponseEntity.ok(
            bajaCuentaService.reactivarConCredenciales(
                dto.getNombreUsuario(), dto.getContrasenia()));
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

    @DeleteMapping("/upload")
    public ResponseEntity<Map<String, Object>> eliminarArchivos(@RequestBody List<String> urls) {
        if (urls == null || urls.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "Debes indicar al menos una imagen."));
        }
        if (urls.size() > 100) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "Demasiadas imágenes en una sola petición."));
        }
        int eliminados = limpiezaArchivosService.eliminar(urls);
        return ResponseEntity.ok(Map.of("eliminados", eliminados));
    }

    @GetMapping("/checkNombreUsuario")
    public ResponseEntity<Map<String, Boolean>> chequearNombreUsuario(
            @RequestParam("nombreUsuario") String nombreUsuario) {
        boolean existe = usuarioService.existeNombreUsuario(nombreUsuario);
        return ResponseEntity.ok(Map.of("disponible", !existe));
    }
}