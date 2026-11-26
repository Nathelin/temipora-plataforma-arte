package com.temipora.backend_springboot.controladores;

import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.temipora.backend_springboot.modelo.ActualizarContraseniaDTO;
import com.temipora.backend_springboot.modelo.ActualizarCorreoDTO;
import com.temipora.backend_springboot.modelo.ActualizarPerfilDTO;
import com.temipora.backend_springboot.modelo.ActualizarTelefonoDTO;
import com.temipora.backend_springboot.modelo.BajaCuentaRequest;
import com.temipora.backend_springboot.modelo.BajaCuentaResultadoDTO;
import com.temipora.backend_springboot.modelo.ERol;
import com.temipora.backend_springboot.modelo.QuitarTelefonoDTO;
import com.temipora.backend_springboot.modelo.Usuario;
import com.temipora.backend_springboot.modelo.UsuarioDTO;
import com.temipora.backend_springboot.servicios.BajaCuentaService;
import com.temipora.backend_springboot.servicios.ServicioNoDisponibleException;
import com.temipora.backend_springboot.servicios.UsuarioService;
import com.temipora.backend_springboot.servicios.VisitaService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api")
public class UsuarioController {

	private final UsuarioService usuarioService;
	private final VisitaService visitaService;
	private final BajaCuentaService bajaCuentaService;

	public UsuarioController(
			UsuarioService usuarioService,
			VisitaService visitaService,
			BajaCuentaService bajaCuentaService) {
		this.usuarioService = usuarioService;
		this.visitaService = visitaService;
		this.bajaCuentaService = bajaCuentaService;
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
	public UsuarioDTO obtenerPerfil(
			@PathVariable String nombreUsuario,
			Authentication authentication) {
		Usuario usuario = usuarioService.getByNombreUsuario(nombreUsuario);
		if (!usuario.isEstadoCuenta()) {
			throw new ServicioNoDisponibleException(
				"Esta cuenta no está disponible.");
		}
		Long viewerId = idAutenticado(authentication);
		if (viewerId != null
				&& usuario.getRol() != null
				&& usuario.getRol().getNombre() == ERol.ARTISTA) {
			visitaService.registrarArtista(viewerId, usuario.getIdUsuario());
		}
		UsuarioDTO dto = UsuarioDTO.desdeUsuario(usuario);
		usuarioService.sancionVigente(usuario.getIdUsuario())
			.ifPresent(sancion -> {
				dto.setSancionado(true);
				dto.setSancionTipo(sancion.getTipo() != null
					? sancion.getTipo().name() : null);
				dto.setSancionAlcance(sancion.getAlcance() != null
					? sancion.getAlcance().name() : null);
			});
		return dto;
	}

	private Long idAutenticado(Authentication authentication) {
		if (authentication == null
				|| !authentication.isAuthenticated()
				|| authentication.getName() == null
				|| "anonymousUser".equals(authentication.getName())) {
			return null;
		}
		return usuarioService.getByNombreUsuario(authentication.getName())
			.getIdUsuario();
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

    @PutMapping("/usuarios/{idUsuario}/telefono")
    public ResponseEntity<UsuarioDTO> cambiarTelefono(
            @PathVariable Long idUsuario,
            @Valid @RequestBody ActualizarTelefonoDTO dto,
            Authentication authentication) {
        if (esOtroUsuario(idUsuario, authentication)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.ok(usuarioService.cambiarTelefono(idUsuario, dto));
    }

    @DeleteMapping("/usuarios/{idUsuario}/telefono")
    public ResponseEntity<UsuarioDTO> quitarTelefono(
            @PathVariable Long idUsuario,
            @Valid @RequestBody QuitarTelefonoDTO dto,
            Authentication authentication) {
        if (esOtroUsuario(idUsuario, authentication)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.ok(usuarioService.quitarTelefono(idUsuario, dto));
    }

    @PostMapping("/usuarios/{idUsuario}/baja")
    public ResponseEntity<BajaCuentaResultadoDTO> darDeBaja(
            @PathVariable Long idUsuario,
            @Valid @RequestBody BajaCuentaRequest dto,
            Authentication authentication) {
        if (esOtroUsuario(idUsuario, authentication)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.ok(
            bajaCuentaService.darDeBaja(idUsuario, dto.getContrasenia()));
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