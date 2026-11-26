import api from './api'

export function obtenerMiEstadoCuenta() {
  return api.get('/mi-estado-cuenta')
}
