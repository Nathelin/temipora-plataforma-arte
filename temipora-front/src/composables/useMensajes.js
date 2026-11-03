import { ref } from 'vue'
import { cargarContadorMensajes } from '../servicios/mensajes'

const INTERVALO_MS = 20000

const noLeidos = ref(0)
const solicitudesPendientes = ref(0)
const cargando = ref(false)
const error = ref('')

let intervalo = null
let cargaEnCurso = null

function haySesion() {
  if (!localStorage.getItem('token')) {
    return false
  }
  const rol = localStorage.getItem('rol')
  return !rol || rol === 'CLIENTE' || rol === 'ARTISTA'
}

async function cargarContador({ silencioso = false } = {}) {
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
      const res = await cargarContadorMensajes()
      noLeidos.value = Number(res.data?.noLeidos || 0)
      solicitudesPendientes.value = Number(res.data?.solicitudesPendientes || 0)
    } catch (e) {
      if (e.response?.status === 401) {
        limpiar()
        return
      }
      if (!silencioso) {
        error.value =
          e.response?.data?.message || 'No se pudieron cargar los mensajes.'
      }
    } finally {
      cargando.value = false
      cargaEnCurso = null
    }
  })()

  return cargaEnCurso
}

function iniciarPolling(ms = INTERVALO_MS) {
  detenerPolling()
  if (!haySesion()) {
    return
  }
  cargarContador({ silencioso: true })
  intervalo = window.setInterval(() => {
    if (document.visibilityState === 'visible') {
      cargarContador({ silencioso: true })
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
  detenerPolling()
  noLeidos.value = 0
  solicitudesPendientes.value = 0
  error.value = ''
  cargando.value = false
}

export function useMensajes() {
  return {
    noLeidos,
    solicitudesPendientes,
    cargando,
    error,
    cargarContador,
    iniciarPolling,
    detenerPolling,
    limpiar,
  }
}
