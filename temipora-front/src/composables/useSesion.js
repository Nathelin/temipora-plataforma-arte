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

  function limpiarSesionLocal() {
    localStorage.removeItem('token')
    localStorage.removeItem('rol')
    localStorage.removeItem('nombreUsuario')
    localStorage.removeItem('idUsuario')
    localStorage.removeItem('urlFotoPerfil')
    localStorage.removeItem('monedaPreferida')
  }

  return { guardarSesion, usuarioActual, estaAutenticado, limpiarSesionLocal }
}
