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
  }

  function usuarioActual() {
    return localStorage.getItem('nombreUsuario') || ''
  }

  function estaAutenticado() {
    return Boolean(localStorage.getItem('token'))
  }

  return { guardarSesion, usuarioActual, estaAutenticado }
}
