import api from './api'

export function listarNotificaciones(params) {
  return api.get('/notificaciones', { params })
}

export function cargarNotificacionesRecientes(limite) {
  return api.get('/notificaciones/recientes', { params: { limite } })
}

export function cargarContadorNotificaciones() {
  return api.get('/notificaciones/contador')
}

export function marcarNotificacionLeida(idNotificacionUsuario) {
  return api.post(`/notificaciones/${idNotificacionUsuario}/leer`)
}

export function marcarTodasNotificacionesLeidas() {
  return api.post('/notificaciones/leer-todas')
}
