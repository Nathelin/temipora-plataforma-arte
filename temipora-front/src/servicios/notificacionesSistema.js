import api from './api'

export function listarNotificaciones(params) {
  return api.get('/notificaciones', { params })
}

export function cargarNotificacionesRecientes(limite, opciones = {}) {
  return api.get('/notificaciones/recientes', {
    ...opciones,
    params: { limite },
  })
}

export function cargarContadorNotificaciones(opciones = {}) {
  return api.get('/notificaciones/contador', opciones)
}

export function marcarNotificacionLeida(idNotificacionUsuario) {
  return api.post(`/notificaciones/${idNotificacionUsuario}/leer`)
}

export function marcarTodasNotificacionesLeidas() {
  return api.post('/notificaciones/leer-todas')
}
