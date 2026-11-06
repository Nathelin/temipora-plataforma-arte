import { useRoute } from 'vue-router'
import api from '../servicios/api'
import { useSesion } from './useSesion'

const CLAVE_USUARIO = 'accesoEnlaceUsuario'
const CLAVE_ERROR = 'accesoEnlaceError'

/**
 * Consume el token de acceso que viene en los mails automáticos, inicia
 * sesión con la cuenta destinataria y limpia el parámetro de la URL.
 */
export function useAccesoEnlace() {
  const route = useRoute()
  const { guardarSesion } = useSesion()

  function tokenDeUrl() {
    const valor = route.query.acceso
    return typeof valor === 'string' ? valor : ''
  }

  function urlSinAcceso() {
    const params = new URLSearchParams(window.location.search)
    params.delete('acceso')
    const query = params.toString()
    return window.location.pathname + (query ? `?${query}` : '')
  }

  async function consumirSiCorresponde() {
    const token = tokenDeUrl()
    if (!token) {
      return { habiaEnlace: false, consumido: false, error: '' }
    }
    try {
      const { data } = await api.post('/auth/acceso', { token })
      guardarSesion(data)
      sessionStorage.setItem(
        CLAVE_USUARIO,
        data.NombreUsuario || data.nombreUsuario || '',
      )
      window.location.replace(urlSinAcceso())
      return { habiaEnlace: true, consumido: true, error: '' }
    } catch (e) {
      const error =
        e.response?.data?.message ||
        'El enlace venció o ya fue usado. Iniciá sesión manualmente.'
      // Si ya hay una sesión válida (por ejemplo, el cliente abrió otro link
      // del mismo mail), el token de un solo uso ya está gastado pero la
      // página sigue siendo accesible: no mostramos un error.
      const yaLogueado = Boolean(localStorage.getItem('token'))
      if (!yaLogueado) {
        sessionStorage.setItem(CLAVE_ERROR, error)
      }
      window.location.replace(urlSinAcceso())
      return {
        habiaEnlace: true,
        consumido: false,
        error: yaLogueado ? '' : error,
      }
    }
  }

  function avisoAcceso() {
    const usuario = sessionStorage.getItem(CLAVE_USUARIO) || ''
    sessionStorage.removeItem(CLAVE_USUARIO)
    return usuario
  }

  function errorAcceso() {
    const error = sessionStorage.getItem(CLAVE_ERROR) || ''
    sessionStorage.removeItem(CLAVE_ERROR)
    return error
  }

  return { consumirSiCorresponde, avisoAcceso, errorAcceso }
}
