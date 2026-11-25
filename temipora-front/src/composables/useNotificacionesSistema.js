import { ref } from 'vue'
import {
  cargarContadorNotificaciones,
  cargarNotificacionesRecientes,
  listarNotificaciones,
  marcarNotificacionLeida,
  marcarTodasNotificacionesLeidas,
} from '../servicios/notificacionesSistema'

const INTERVALO_MS = 20000
export const TAMANIO_PAGINA = 20
export const LIMITE_RECIENTES = 8

const noLeidas = ref(0)
const porCategoria = ref({})

const notificaciones = ref([])
const pagina = ref(0)
const hayMas = ref(false)
const total = ref(0)
const cargando = ref(false)
const cargandoMas = ref(false)
const error = ref('')
const categoria = ref(null)
const soloNoLeidas = ref(false)

const recientes = ref([])
const cargandoRecientes = ref(false)
const errorRecientes = ref('')

let intervalo = null
let secuencia = 0
let cargaRecientesEnCurso = null
let usuarioActual = localStorage.getItem('idUsuario')
let secuenciaRecientes = 0
let secuenciaContador = 0

function haySesion() {
  return Boolean(localStorage.getItem('token'))
}

function sincronizarSesion() {
  const id = localStorage.getItem('idUsuario')
  if (id !== usuarioActual) {
    usuarioActual = id
    limpiar()
  }
}

function aplicarContador(data) {
  noLeidas.value = Number(data?.noLeidas || 0)
  porCategoria.value = data?.porCategoria || {}
}

async function refrescarContador(silencioso = false) {
  sincronizarSesion()
  if (!haySesion()) {
    noLeidas.value = 0
    porCategoria.value = {}
    return
  }
  const turno = ++secuenciaContador
  try {
    const res = await cargarContadorNotificaciones({
      meta: { silenciosa: silencioso },
    })
    if (turno !== secuenciaContador) return
    aplicarContador(res.data)
  } catch (e) {
    if (turno !== secuenciaContador) return
    if (e.response?.status === 401) {
      noLeidas.value = 0
      porCategoria.value = {}
    }
  }
}

function paramsDeLista(paginaPedida) {
  const params = { pagina: paginaPedida, tamanio: TAMANIO_PAGINA }
  if (categoria.value) params.categoria = categoria.value
  if (soloNoLeidas.value) params.soloNoLeidas = true
  return params
}

async function cargar({ reset = true } = {}) {
  sincronizarSesion()
  if (!haySesion()) {
    limpiar()
    return
  }
  if (!reset && (cargandoMas.value || cargando.value || !hayMas.value)) {
    return
  }

  const paginaPedida = reset ? 0 : pagina.value + 1
  const turno = ++secuencia
  if (reset) {
    notificaciones.value = []
    pagina.value = 0
    hayMas.value = false
    total.value = 0
    cargando.value = true
  } else {
    cargandoMas.value = true
  }
  error.value = ''

  try {
    const res = await listarNotificaciones(paramsDeLista(paginaPedida))
    if (turno !== secuencia) return
    const contenido = res.data?.contenido || []
    notificaciones.value = reset
      ? contenido
      : [...notificaciones.value, ...contenido]
    pagina.value = paginaPedida
    hayMas.value = Boolean(res.data?.hayMas)
    total.value = Number(res.data?.totalElementos || 0)
  } catch (e) {
    if (turno !== secuencia) return
    if (e.response?.status === 401) {
      limpiar()
      return
    }
    error.value =
      e.response?.data?.message || 'No se pudieron cargar las notificaciones.'
  } finally {
    if (turno === secuencia) {
      cargando.value = false
      cargandoMas.value = false
    }
  }
}

function cargarMas() {
  return cargar({ reset: false })
}

function cambiarCategoria(nueva) {
  if (categoria.value === nueva) return
  categoria.value = nueva
  void cargar({ reset: true })
}

function alternarSoloNoLeidas() {
  soloNoLeidas.value = !soloNoLeidas.value
  void cargar({ reset: true })
}

function establecerFiltros({
  categoria: nuevaCategoria = null,
  soloNoLeidas: nuevasSoloNoLeidas = false,
} = {}) {
  categoria.value = nuevaCategoria
  soloNoLeidas.value = Boolean(nuevasSoloNoLeidas)
}

async function cargarRecientes({ silencioso = false } = {}) {
  sincronizarSesion()
  if (!haySesion()) {
    recientes.value = []
    return
  }
  if (cargaRecientesEnCurso) {
    return cargaRecientesEnCurso
  }
  const turno = ++secuenciaRecientes
  if (!silencioso) {
    cargandoRecientes.value = true
  }
  errorRecientes.value = ''

  cargaRecientesEnCurso = (async () => {
    try {
      const res = await cargarNotificacionesRecientes(LIMITE_RECIENTES, {
        meta: { silenciosa: silencioso },
      })
      if (turno !== secuenciaRecientes) return
      recientes.value = res.data || []
    } catch (e) {
      if (turno !== secuenciaRecientes) return
      if (e.response?.status === 401) {
        recientes.value = []
        return
      }
      if (!silencioso) {
        errorRecientes.value =
          e.response?.data?.message || 'No se pudieron cargar las notificaciones.'
      }
    } finally {
      if (turno === secuenciaRecientes) {
        cargandoRecientes.value = false
        cargaRecientesEnCurso = null
      }
    }
  })()

  return cargaRecientesEnCurso
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
  noLeidas.value = Math.max(0, noLeidas.value - 1)
  if (notificacion.categoria) {
    const actual = Number(porCategoria.value[notificacion.categoria] || 0)
    porCategoria.value = {
      ...porCategoria.value,
      [notificacion.categoria]: Math.max(0, actual - 1),
    }
  }

  if (soloNoLeidas.value) {
    notificaciones.value = notificaciones.value.filter(
      (item) => item.idNotificacionUsuario !== notificacion.idNotificacionUsuario,
    )
    total.value = Math.max(0, total.value - 1)
  }

  try {
    const res = await marcarNotificacionLeida(notificacion.idNotificacionUsuario)
    if (res.data) {
      Object.assign(notificacion, res.data)
    }
  } catch (e) {
    Object.assign(notificacion, estadoAnterior)
    noLeidas.value += 1
    if (notificacion.categoria) {
      const actual = Number(porCategoria.value[notificacion.categoria] || 0)
      porCategoria.value = {
        ...porCategoria.value,
        [notificacion.categoria]: actual + 1,
      }
    }
    if (soloNoLeidas.value) {
      await cargar({ reset: true })
    }
    if (e.response?.status !== 401) {
      error.value =
        e.response?.data?.message || 'No se pudo marcar la notificación como leída.'
    }
  }
}

async function marcarTodasLeidas() {
  sincronizarSesion()
  if (!haySesion()) {
    return 0
  }
  const res = await marcarTodasNotificacionesLeidas()
  const marcadas = Number(res.data?.marcadas || 0)
  noLeidas.value = 0
  porCategoria.value = {}
  notificaciones.value = notificaciones.value.map((notificacion) => ({
    ...notificacion,
    leida: true,
  }))
  recientes.value = recientes.value.map((notificacion) => ({
    ...notificacion,
    leida: true,
  }))
  if (soloNoLeidas.value) {
    await cargar({ reset: true })
  }
  return marcadas
}

function iniciarPolling(ms = INTERVALO_MS) {
  detenerPolling()
  intervalo = window.setInterval(() => {
    if (document.visibilityState === 'visible') {
      void refrescarContador(true)
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
  recientes.value = []
  pagina.value = 0
  hayMas.value = false
  total.value = 0
  noLeidas.value = 0
  porCategoria.value = {}
  error.value = ''
  errorRecientes.value = ''
  cargando.value = false
  cargandoMas.value = false
  cargandoRecientes.value = false
  categoria.value = null
  soloNoLeidas.value = false
  secuencia += 1
  secuenciaRecientes += 1
  secuenciaContador += 1
  cargaRecientesEnCurso = null
}

export function useNotificacionesSistema() {
  return {
    noLeidas,
    porCategoria,
    notificaciones,
    recientes,
    pagina,
    hayMas,
    total,
    cargando,
    cargandoMas,
    cargandoRecientes,
    error,
    errorRecientes,
    categoria,
    soloNoLeidas,
    cargar,
    cargarMas,
    cargarRecientes,
    refrescarContador,
    marcarLeida,
    marcarTodasLeidas,
    cambiarCategoria,
    alternarSoloNoLeidas,
    establecerFiltros,
    sincronizarSesion,
    iniciarPolling,
    detenerPolling,
    limpiar,
  }
}
