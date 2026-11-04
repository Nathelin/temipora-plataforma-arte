import { computed, ref } from 'vue'
import api from '../servicios/api'

const INTERVALO_MS = 60000

const METRICAS_VACIAS = {
  pedidosEnCurso: 0,
  pedidosPendientesPago: 0,
  pedidosImpagos: 0,
  entregasPendientes: 0,
  entregasConRevision: 0,
  solicitudesPorResponder: 0,
  solicitudesEnEspera: 0,
  presupuestosPorVencer: 0,
  hitosPorVencer: 0,
}

const DINERO_VACIO = {
  porCobrar: 0,
  cobradoMes: 0,
  cobradoTotal: 0,
  pendienteAcreditar: 0,
  monedaMostrada: null,
}

const resumen = ref(null)
const cargando = ref(true)
const error = ref('')

let intervalo = null
let cargaEnCurso = null

const metricas = computed(() => resumen.value?.metricas || METRICAS_VACIAS)
const dinero = computed(() => resumen.value?.dinero || DINERO_VACIO)
const pedidosActivos = computed(() => resumen.value?.pedidosActivos || [])
const hitosAtencion = computed(() => resumen.value?.hitosAtencion || [])
const solicitudesRecientes = computed(() => resumen.value?.solicitudesRecientes || [])
const solicitudesPorResponder = computed(() => resumen.value?.solicitudesPorResponder || [])
const presupuestosPorVencer = computed(() => resumen.value?.presupuestosPorVencer || [])
const mensajesNoLeidos = computed(() => Number(resumen.value?.mensajesNoLeidos || 0))
const solicitudesMensajePendientes = computed(
  () => Number(resumen.value?.solicitudesMensajePendientes || 0),
)
const cuentaCobrosConectada = computed(() => resumen.value?.cuentaCobrosConectada !== false)

function haySesionArtista() {
  return Boolean(localStorage.getItem('token'))
    && localStorage.getItem('rol') === 'ARTISTA'
}

async function cargar({ silencioso = false } = {}) {
  if (!haySesionArtista()) {
    resumen.value = null
    cargando.value = false
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
      const res = await api.get('/panel-artista/resumen')
      resumen.value = res.data || null
    } catch (e) {
      if (e.response?.status === 401) {
        resumen.value = null
        return
      }
      if (!silencioso) {
        error.value = e.response?.data?.message
          || 'No se pudo cargar el resumen del panel.'
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
  if (!haySesionArtista()) {
    return
  }
  cargar({ silencioso: true })
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

export function usePanelArtista() {
  return {
    resumen,
    metricas,
    dinero,
    pedidosActivos,
    hitosAtencion,
    solicitudesRecientes,
    solicitudesPorResponder,
    presupuestosPorVencer,
    mensajesNoLeidos,
    solicitudesMensajePendientes,
    cuentaCobrosConectada,
    cargando,
    error,
    cargar,
    iniciarPolling,
    detenerPolling,
  }
}
