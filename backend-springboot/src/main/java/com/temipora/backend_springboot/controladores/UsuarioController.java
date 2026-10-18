package com.temipora.backend_springboot.controladores;

import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.temipora.backend_springboot.modelo.ActualizarContraseniaDTO;
import com.temipora.backend_springboot.modelo.ActualizarCorreoDTO;
import com.temipora.backend_springboot.modelo.ActualizarPerfilDTO;
import com.temipora.backend_springboot.modelo.ERol;
import com.temipora.backend_springboot.modelo.Usuario;
import com.temipora.backend_springboot.modelo.UsuarioDTO;
import com.temipora.backend_springboot.servicios.UsuarioService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api")
public class UsuarioController {

	private final UsuarioService usuarioService;

	public UsuarioController(UsuarioService usuarioService) {
		this.usuarioService = usuarioService;
	}

	@GetMapping("/usuarios")
	@PreAuthorize("hasRole('ADMIN')")
	public Page<UsuarioDTO> obtenerUsuarios(
			@RequestParam(name = "nombreUsuario", required = false) String nombreUsuario,
			@RequestParam(name = "correo", required = false) String correo,
			@RequestParam(name = "estado", required = false) Boolean estado,
			@RequestParam(name = "rol", required = false) ERol rol,
			Pageable pageable) {
		return usuarioService.obtenerUsuariosPaginados(
				nombreUsuario, correo, estado, rol, pageable);
	}

	@GetMapping("/perfil/{nombreUsuario}")
	public UsuarioDTO obtenerPerfil(@PathVariable String nombreUsuario) {
		Usuario usuario = usuarioService.getByNombreUsuario(nombreUsuario);
		return UsuarioDTO.desdeUsuario(usuario);
	}

    @PutMapping("/usuarios/{idUsuario}/perfil")
    public ResponseEntity<UsuarioDTO> actualizarPerfil(
            @PathVariable Long idUsuario,
            @Valid @RequestBody ActualizarPerfilDTO dto,
            Authentication authentication) {
        if (esOtroUsuario(idUsuario, authentication)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.ok(usuarioService.actualizarPerfil(idUsuario, dto));
    }

    @PutMapping("/usuarios/{idUsuario}/correo")
    public ResponseEntity<UsuarioDTO> cambiarCorreo(
            @PathVariable Long idUsuario,
            @Valid @RequestBody ActualizarCorreoDTO dto,
            Authentication authentication) {
        if (esOtroUsuario(idUsuario, authentication)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.ok(usuarioService.cambiarCorreo(idUsuario, dto));
    }

    @PutMapping("/usuarios/{idUsuario}/contrasenia")
    public ResponseEntity<Map<String, String>> cambiarContrasenia(
            @PathVariable Long idUsuario,
            @Valid @RequestBody ActualizarContraseniaDTO dto,
            Authentication authentication) {
        if (esOtroUsuario(idUsuario, authentication)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        usuarioService.cambiarContrasenia(idUsuario, dto);
        return ResponseEntity.ok(Map.of("message", "Contraseña actualizada correctamente."));
    }

    private boolean esOtroUsuario(Long idUsuario, Authentication authentication) {
        if (authentication == null) {
            return true;
        }
        return !authentication.getName().equalsIgnoreCase(usuarioService.nombreUsuario(idUsuario));
    }
}