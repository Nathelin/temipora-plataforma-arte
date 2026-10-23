import { ref } from 'vue'
import api from '../servicios/api'

const INTERVALO_MS = 20000

const notificaciones = ref([])
const noLeidas = ref(0)
const cargando = ref(false)
const error = ref('')

let intervalo = null
let cargaEnCurso = null

function haySesion() {
  return Boolean(localStorage.getItem('token'))
}

function recalcularNoLeidas() {
  noLeidas.value = notificaciones.value.filter((notificacion) => !notificacion.leida).length
}

async function cargar({ silencioso = false } = {}) {
  if (!haySesion()) {
    limpiar()
    return
  }

  if (cargaEnCurso) {
    return cargaEnCurso
  }

  if (!silencioso) {
    cargando.value = true
  }
  error.value = ''

  cargaEnCurso = (async () => {
    try {
      const res = await api.get('/notificaciones')
      notificaciones.value = res.data || []
      recalcularNoLeidas()
    } catch (e) {
      if (e.response?.status === 401) {
        notificaciones.value = []
        noLeidas.value = 0
        return
      }
      if (!silencioso) {
        error.value =
          e.response?.data?.message || 'No se pudieron cargar las notificaciones.'
      }
    } finally {
      cargando.value = false
      cargaEnCurso = null
    }
  })()

  return cargaEnCurso
}

async function marcarLeida(notificacion) {
  if (!notificacion || notificacion.leida) {
    return
  }

  const estadoAnterior = {
    leida: notificacion.leida,
    fechaLectura: notificacion.fechaLectura,
  }

  notificacion.leida = true
  notificacion.fechaLectura = new Date().toISOString()
  recalcularNoLeidas()

  try {
    const res = await api.post(
      `/notificaciones/${notificacion.idNotificacionUsuario}/leer`,
    )
    if (res.data) {
      Object.assign(notificacion, res.data)
    }
    recalcularNoLeidas()
  } catch (e) {
    Object.assign(notificacion, estadoAnterior)
    recalcularNoLeidas()
    if (e.response?.status !== 401) {
      error.value =
        e.response?.data?.message || 'No se pudo marcar la notificación como leída.'
    }
  }
}

async function marcarTodasLeidas() {
  const pendientes = notificaciones.value.filter((notificacion) => !notificacion.leida)
  if (!pendientes.length) {
    return
  }
  await Promise.all(pendientes.map((notificacion) => marcarLeida(notificacion)))
}

function iniciarPolling(ms = INTERVALO_MS) {
  detenerPolling()
  intervalo = window.setInterval(() => {
    if (document.visibilityState === 'visible') {
      cargar({ silencioso: true })
    }
  }, ms)
}

function detenerPolling() {
  if (intervalo) {
    window.clearInterval(intervalo)
    intervalo = null
  }
}

function limpiar() {
  notificaciones.value = []
  noLeidas.value = 0
  error.value = ''
  cargando.value = false
}

export function useNotificacionesSistema() {
  return {
    notificaciones,
    noLeidas,
    cargando,
    error,
    cargar,
    marcarLeida,
    marcarTodasLeidas,
    iniciarPolling,
    detenerPolling,
    limpiar,
  }
}
