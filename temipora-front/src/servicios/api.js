import axios from 'axios'

const api = axios.create({
  baseURL: '/api',
})

function limpiarSesion() {
  localStorage.removeItem('token')
  localStorage.removeItem('nombreUsuario')
  localStorage.removeItem('rol')
  localStorage.removeItem('idUsuario')
  localStorage.removeItem('urlFotoPerfil')
}

api.interceptors.request.use((config) => {
  const token = localStorage.getItem('token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

api.interceptors.response.use(
  (response) => response,
  (error) => {
    const status = error.response?.status
    const data = error.response?.data

    if (status === 401) {
      limpiarSesion()
    }
    if (status === 403 && data?.codigo === 'TOS_NO_ACEPTADOS') {
      window.dispatchEvent(new CustomEvent('tos-no-aceptados'))
    }
    if (status === 403
        && (data?.codigo === 'CUENTA_SANCIONADA'
          || data?.codigo === 'CUENTA_INACTIVA'
          || data?.codigo === 'CUENTA_DE_BAJA')) {
      try {
        sessionStorage.setItem('cuentaInhabilitada', JSON.stringify({
          codigo: data.codigo,
          motivo: data.motivo || '',
          contacto: data.contacto || '',
          fechaFin: data.fechaFin || null,
          fechaBaja: data.fechaBaja || null,
          reactivarHasta: data.reactivarHasta || null,
          puedeReactivar: data.puedeReactivar ?? null,
          message: data.message || '',
        }))
      } catch {
        // sessionStorage no disponible: el usuario igual será redirigido.
      }
      limpiarSesion()
      window.dispatchEvent(new CustomEvent('cuenta-inhabilitada'))
    }

    return Promise.reject(error)
  },
)

export default api