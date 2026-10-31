import api from './api'

export const ESTADOS_CONVERSACION = {
  PENDIENTE: {
    texto: 'Pendiente',
    clase: 'border-coral/30 bg-coral/10 text-coral',
  },
  ACEPTADA: {
    texto: 'Conversación activa',
    clase: 'border-granate/20 bg-rosa/30 text-granate',
  },
  RECHAZADA: {
    texto: 'Rechazada',
    clase: 'border-rojizo/25 bg-rojizo/5 text-rojizo',
  },
  CANCELADA: {
    texto: 'Cancelada',
    clase: 'border-borde/60 bg-superficie-alt text-cafe/60',
  },
}

export function estadoConversacionDe(estado) {
  return ESTADOS_CONVERSACION[estado] || {
    texto: estado || 'Sin estado',
    clase: 'border-borde/60 bg-superficie-alt text-cafe/60',
  }
}

export function obtenerEstadoConUsuario(nombreUsuario) {
  return api.get(`/conversaciones/con-usuario/${encodeURIComponent(nombreUsuario)}`)
}

export function enviarSolicitudMensaje(payload) {
  return api.post('/conversaciones', payload)
}

export function listarConversaciones(params) {
  return api.get('/conversaciones', { params })
}

export function obtenerConversacion(idConversacion) {
  return api.get(`/conversaciones/${idConversacion}`)
}

export function aceptarConversacion(idConversacion) {
  return api.post(`/conversaciones/${idConversacion}/aceptar`)
}

export function rechazarConversacion(idConversacion, motivo) {
  return api.post(`/conversaciones/${idConversacion}/rechazar`, {
    motivo: motivo || null,
  })
}

export function cancelarConversacion(idConversacion) {
  return api.post(`/conversaciones/${idConversacion}/cancelar`)
}

export function archivarConversacion(idConversacion, archivada) {
  return api.post(
    `/conversaciones/${idConversacion}/${archivada ? 'archivar' : 'desarchivar'}`,
  )
}

export function cargarMensajes(idConversacion, params = {}) {
  return api.get(`/conversaciones/${idConversacion}/mensajes`, { params })
}

export function enviarMensaje(idConversacion, contenido) {
  return api.post(`/conversaciones/${idConversacion}/mensajes`, { contenido })
}

export function marcarConversacionLeida(idConversacion) {
  return api.post(`/conversaciones/${idConversacion}/leer`)
}

export function marcarEscribiendo(idConversacion) {
  return api.post(`/conversaciones/${idConversacion}/escribiendo`)
}

export function consultarEscribiendo(idConversacion) {
  return api.get(`/conversaciones/${idConversacion}/escribiendo`)
}

export function cargarContadorMensajes() {
  return api.get('/conversaciones/contador')
}

export function cargarConfiguracionMensajeria() {
  return api.get('/conversaciones/configuracion')
}

export function inicialDe(nombre) {
  return (nombre || '?').charAt(0).toUpperCase()
}

export function previewMensaje(texto, maximo = 90) {
  if (!texto) return ''
  const limpio = String(texto).replace(/\s+/g, ' ').trim()
  return limpio.length <= maximo ? limpio : `${limpio.slice(0, maximo)}…`
}

export function formatearFechaMensaje(valor) {
  if (!valor) return ''
  const fecha = new Date(valor)
  if (Number.isNaN(fecha.getTime())) return ''
  const ahora = new Date()
  const mismoDia = fecha.toDateString() === ahora.toDateString()
  if (mismoDia) {
    return new Intl.DateTimeFormat('es', {
      hour: '2-digit',
      minute: '2-digit',
    }).format(fecha)
  }
  const mismoAnio = fecha.getFullYear() === ahora.getFullYear()
  return new Intl.DateTimeFormat('es', {
    day: '2-digit',
    month: 'short',
    ...(mismoAnio ? {} : { year: 'numeric' }),
  }).format(fecha)
}

export function formatearFechaHora(valor) {
  if (!valor) return ''
  const fecha = new Date(valor)
  if (Number.isNaN(fecha.getTime())) return ''
  return new Intl.DateTimeFormat('es', {
    dateStyle: 'medium',
    timeStyle: 'short',
  }).format(fecha)
}
