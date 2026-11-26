export function useSesion() {
  function guardarSesion(datos) {
    if (!datos) return
    localStorage.setItem('token', datos.token ?? '')
    localStorage.setItem('rol', datos.rol ?? '')
    localStorage.setItem(
      'nombreUsuario',
      datos.NombreUsuario ?? datos.nombreUsuario ?? '',
    )
    localStorage.setItem('idUsuario', datos.idUsuario ?? '')
    localStorage.setItem('urlFotoPerfil', datos.urlFotoPerfil ?? '')
    localStorage.setItem('restringido', datos.restringido ? '1' : '0')
    localStorage.setItem('alcance', datos.alcance ?? '')
    if (datos.monedaPreferida) {
      localStorage.setItem('monedaPreferida', datos.monedaPreferida)
    }
  }

  function usuarioActual() {
    return localStorage.getItem('nombreUsuario') || ''
  }

  function estaAutenticado() {
    return Boolean(localStorage.getItem('token'))
  }

  function estaRestringido() {
    return localStorage.getItem('restringido') === '1'
  }

  function marcarRestringido(valor) {
    localStorage.setItem('restringido', valor ? '1' : '0')
  }

  function limpiarSesionLocal() {
    localStorage.removeItem('token')
    localStorage.removeItem('rol')
    localStorage.removeItem('nombreUsuario')
    localStorage.removeItem('idUsuario')
    localStorage.removeItem('urlFotoPerfil')
    localStorage.removeItem('monedaPreferida')
    localStorage.removeItem('restringido')
    localStorage.removeItem('alcance')
  }

  return {
    guardarSesion,
    usuarioActual,
    estaAutenticado,
    estaRestringido,
    marcarRestringido,
    limpiarSesionLocal,
  }
}
