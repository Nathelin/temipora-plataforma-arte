<script setup>
import {
  computed,
  nextTick,
  onBeforeUnmount,
  onMounted,
  ref,
  watch,
} from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  aceptarConversacion,
  archivarConversacion,
  cancelarConversacion,
  cargarConfiguracionMensajeria,
  cargarMensajes,
  consultarEscribiendo,
  enviarMensaje,
  estadoConversacionDe,
  formatearFechaHora,
  formatearFechaMensaje,
  inicialDe,
  marcarConversacionLeida,
  marcarEscribiendo,
  obtenerConversacion,
  rechazarConversacion,
} from '../servicios/mensajes'
import { useMensajes } from '../composables/useMensajes'
import { useMensajeriaSocket } from '../composables/useMensajeriaSocket'
import { useNotificaciones } from '../composables/useNotificaciones'
import ModalRechazoConversacion from '../components/ModalRechazoConversacion.vue'
import ModalConfirmacion from '../components/ModalConfirmacion.vue'

const route = useRoute()
const router = useRouter()
const { cargarContador } = useMensajes()
const {
  conectado: socketConectado,
  enviarEscribiendo,
  suscribir: suscribirSocket,
} = useMensajeriaSocket()
const { exito } = useNotificaciones()

const INTERVALO_MS = 5000
const TAMANIO_HISTORIAL = 30

const idConversacion = computed(() => Number(route.params.idConversacion))
const conversacion = ref(null)
const mensajes = ref([])
const cursor = ref(null)
const hayMas = ref(false)
const cargando = ref(true)
const cargandoMas = ref(false)
const error = ref('')
const errorEnvio = ref('')
const texto = ref('')
const enviando = ref(false)
const accionEnCurso = ref(false)
const errorAccion = ref('')
const config = ref({
  longitudMaximaMensaje: 2000,
  longitudMaximaMotivo: 200,
})
const modalRechazo = ref(false)
const modalCancelar = ref(false)
const refLista = ref(null)
const campoMensaje = ref(null)
const contraparteEscribiendo = ref(false)

let intervalo = null
let intervaloEscritura = null
let ultimoLatido = 0
let consultandoEscritura = false
let quitarSuscripcionSocket = null
let temporizadorEscritura = null

const enPanel = computed(() => route.path.startsWith('/panel'))
const nombreBandeja = computed(() => (enPanel.value ? 'panel-mensajes' : 'mensajes'))
const contraparte = computed(() => conversacion.value?.contraparte || null)
const estadoInfo = computed(() =>
  estadoConversacionDe(conversacion.value?.estado))
const maxMensaje = computed(
  () => Number(config.value?.longitudMaximaMensaje || 2000))
const maxMotivo = computed(
  () => Number(config.value?.longitudMaximaMotivo || 200))
const esPendiente = computed(
  () => conversacion.value?.estado === 'PENDIENTE')
const esAceptada = computed(
  () => conversacion.value?.estado === 'ACEPTADA')
const esRechazada = computed(
  () => conversacion.value?.estado === 'RECHAZADA')
const esCancelada = computed(
  () => conversacion.value?.estado === 'CANCELADA')
const puedoResponderSolicitud = computed(() =>
  Boolean(conversacion.value?.puedeAceptar || conversacion.value?.puedeRechazar))
const primerMensaje = computed(() => {
  if (!mensajes.value.length) return null
  const inicio = conversacion.value?.fechaSolicitud
  if (!inicio) return mensajes.value[0]
  const desde = new Date(inicio).getTime()
  return (
    mensajes.value.find(
      (mensaje) => new Date(mensaje.fechaEnvio).getTime() >= desde,
    ) || mensajes.value[0]
  )
})
const enviarDeshabilitado = computed(
  () => enviando.value || !conversacion.value?.puedeEnviar || !texto.value.trim())
const nombreContraparte = computed(
  () =>
    contraparte.value?.nombreDisplay ||
    contraparte.value?.nombreUsuario ||
    'Usuario')

function cargarConfiguracion() {
  return cargarConfiguracionMensajeria()
    .then((res) => {
      if (res.data) config.value = res.data
    })
    .catch(() => {})
}

async function cargarDetalle({ silencioso = false } = {}) {
  try {
    const res = await obtenerConversacion(idConversacion.value)
    conversacion.value = res.data
  } catch (e) {
    if (e.response?.status === 401) {
      router.push({ name: 'login' })
      return
    }
    if (!silencioso) throw e
  }
}

async function cargarHistorial({ reset = true, mostrarCargando = true } = {}) {
  if (reset) {
    if (mostrarCargando) cargando.value = true
    mensajes.value = []
    cursor.value = null
    hayMas.value = false
  } else {
    cargandoMas.value = true
  }
  try {
    const params = { tamanio: TAMANIO_HISTORIAL }
    if (!reset && cursor.value) params.antesDe = cursor.value
    const res = await cargarMensajes(idConversacion.value, params)
    const nuevos = res.data?.mensajes || []
    if (reset) {
      mensajes.value = nuevos
    } else {
      mensajes.value = [...nuevos, ...mensajes.value]
    }
    cursor.value = res.data?.idCursorAnterior || null
    hayMas.value = Boolean(res.data?.hayMas)
    if (conversacion.value) {
      conversacion.value = {
        ...conversacion.value,
        contraparteUltimoLeidoEn:
          res.data?.contraparteUltimoLeidoEn ??
          conversacion.value.contraparteUltimoLeidoEn,
      }
    }
  } finally {
    cargando.value = false
    cargandoMas.value = false
  }
}

async function sincronizarNuevos() {
  try {
    const res = await cargarMensajes(idConversacion.value, {
      tamanio: TAMANIO_HISTORIAL,
    })
    const nuevos = res.data?.mensajes || []
    if (!nuevos.length) return

    if (nuevos.some((mensaje) => !mensaje.esPropio)) {
      contraparteEscribiendo.value = false
    }

    const mapa = new Map()
    for (const mensaje of mensajes.value) {
      mapa.set(mensaje.idMensaje, mensaje)
    }
    for (const mensaje of nuevos) {
      mapa.set(mensaje.idMensaje, mensaje)
    }
    mensajes.value = [...mapa.values()].sort(
      (a, b) => a.idMensaje - b.idMensaje,
    )
    if (!cursor.value && res.data?.idCursorAnterior) {
      cursor.value = res.data.idCursorAnterior
    }
    hayMas.value = hayMas.value || Boolean(res.data?.hayMas)
    if (conversacion.value) {
      conversacion.value = {
        ...conversacion.value,
        contraparteUltimoLeidoEn:
          res.data?.contraparteUltimoLeidoEn ??
          conversacion.value.contraparteUltimoLeidoEn,
      }
    }
  } catch (e) {
    if (e.response?.status === 401) {
      router.push({ name: 'login' })
    }
  }
}

async function marcarLeida() {
  if (!esAceptada.value) return
  try {
    await marcarConversacionLeida(idConversacion.value)
    await cargarContador({ silencioso: true })
  } catch (e) {
    if (e.response?.status === 401) {
      router.push({ name: 'login' })
    }
  }
}

async function inicializar() {
  cargando.value = true
  error.value = ''
  try {
    await Promise.all([cargarDetalle(), cargarConfiguracion()])
    await cargarHistorial({ reset: true })
    await marcarLeida()
    await nextTick()
    scrollAlFinal()
  } catch (e) {
    if (e.response?.status === 401) {
      router.push({ name: 'login' })
      return
    }
    error.value =
      e.response?.data?.message || 'No se pudo cargar la conversación.'
  } finally {
    cargando.value = false
  }
}

function reiniciar() {
  conversacion.value = null
  mensajes.value = []
  cursor.value = null
  hayMas.value = false
  texto.value = ''
  error.value = ''
  errorEnvio.value = ''
  errorAccion.value = ''
  modalRechazo.value = false
  modalCancelar.value = false
  detenerTemporizadorEscritura()
  ultimoLatido = 0
  inicializar()
}

function estaCercaDelFinal() {
  const el = refLista.value
  if (!el) return true
  return el.scrollHeight - el.scrollTop - el.clientHeight < 140
}

function scrollAlFinal() {
  const el = refLista.value
  if (el) el.scrollTop = el.scrollHeight
}

function cargarAnteriores() {
  if (cargandoMas.value || !hayMas.value) return
  const alturaAnterior = refLista.value?.scrollHeight || 0
  cargarHistorial({ reset: false }).then(() => {
    nextTick(() => {
      const el = refLista.value
      if (!el) return
      el.scrollTop = el.scrollHeight - alturaAnterior
    })
  })
}

async function enviar() {
  const contenido = texto.value.trim()
  if (!contenido || enviando.value || !conversacion.value?.puedeEnviar) return
  if (contenido.length > maxMensaje.value) {
    errorEnvio.value = `El mensaje no puede superar los ${maxMensaje.value} caracteres.`
    return
  }

  enviando.value = true
  errorEnvio.value = ''
  try {
    const res = await enviarMensaje(idConversacion.value, contenido)
    if (res.data && !mensajes.value.some(
      (mensaje) => mensaje.idMensaje === res.data.idMensaje)) {
      mensajes.value = [...mensajes.value, res.data]
    }
    texto.value = ''
    await nextTick()
    scrollAlFinal()
    await cargarContador({ silencioso: true })
  } catch (e) {
    if (e.response?.status === 401) {
      router.push({ name: 'login' })
      return
    }
    errorEnvio.value =
      e.response?.data?.message || 'No se pudo enviar el mensaje.'
  } finally {
    enviando.value = false
    ultimoLatido = 0
    await nextTick()
    campoMensaje.value?.focus()
  }
}

function avisarEscribiendo() {
  if (!conversacion.value?.puedeEnviar) return
  if (!texto.value.trim()) return
  const ahora = Date.now()
  if (ahora - ultimoLatido < 2000) return
  ultimoLatido = ahora
  if (socketConectado.value && enviarEscribiendo(idConversacion.value)) {
    return
  }
  marcarEscribiendo(idConversacion.value).catch(() => {})
}

function detenerTemporizadorEscritura() {
  if (temporizadorEscritura) {
    window.clearTimeout(temporizadorEscritura)
    temporizadorEscritura = null
  }
  contraparteEscribiendo.value = false
}

function manejarEventoSocket(evento) {
  if (!evento || Number(evento.idConversacion) !== idConversacion.value) {
    return
  }

  if (evento.tipo === 'MENSAJE' && evento.mensaje) {
    const mensaje = evento.mensaje
    if (mensajes.value.some((m) => m.idMensaje === mensaje.idMensaje)) {
      return
    }
    const estabaCerca = estaCercaDelFinal()
    mensajes.value = [...mensajes.value, mensaje]
      .sort((a, b) => a.idMensaje - b.idMensaje)
    if (!mensaje.esPropio) {
      detenerTemporizadorEscritura()
      if (document.visibilityState === 'visible' && esAceptada.value) {
        void marcarLeida()
      }
      void cargarContador({ silencioso: true })
    }
    if (estabaCerca) {
      void nextTick().then(scrollAlFinal)
    }
    return
  }

  if (evento.tipo === 'ESCRITURA') {
    if (!esAceptada.value) return
    contraparteEscribiendo.value = true
    if (temporizadorEscritura) {
      window.clearTimeout(temporizadorEscritura)
    }
    temporizadorEscritura = window.setTimeout(() => {
      contraparteEscribiendo.value = false
      temporizadorEscritura = null
    }, 6000)
    return
  }

  if (evento.tipo === 'LEIDO' && evento.fechaLectura) {
    const fecha = new Date(evento.fechaLectura)
    if (conversacion.value) {
      conversacion.value = {
        ...conversacion.value,
        contraparteUltimoLeidoEn: evento.fechaLectura,
      }
    }
    mensajes.value = mensajes.value.map((mensaje) => mensaje.esPropio
      ? { ...mensaje, leido: new Date(mensaje.fechaEnvio) <= fecha }
      : mensaje)
    return
  }

  if (evento.tipo === 'CONVERSACION') {
    void recargarPorEvento()
  }
}

async function recargarPorEvento() {
  await cargarDetalle({ silencioso: true })
  if (esAceptada.value) {
    await cargarHistorial({ reset: true, mostrarCargando: false })
    await marcarLeida()
    await nextTick()
    scrollAlFinal()
  }
}

function manejarEnter(event) {
  if (event.shiftKey) return
  event.preventDefault()
  enviar()
}

async function aceptar() {
  if (accionEnCurso.value) return
  accionEnCurso.value = true
  errorAccion.value = ''
  try {
    const res = await aceptarConversacion(idConversacion.value)
    conversacion.value = res.data
    await cargarHistorial({ reset: true, mostrarCargando: false })
    await marcarLeida()
    await nextTick()
    scrollAlFinal()
    campoMensaje.value?.focus()
    exito('Aceptaste la solicitud. Ya pueden escribirle.')
  } catch (e) {
    if (e.response?.status === 401) {
      router.push({ name: 'login' })
      return
    }
    errorAccion.value =
      e.response?.data?.message || 'No se pudo aceptar la solicitud.'
  } finally {
    accionEnCurso.value = false
  }
}

async function rechazar(motivo) {
  if (accionEnCurso.value) return
  accionEnCurso.value = true
  errorAccion.value = ''
  try {
    const res = await rechazarConversacion(idConversacion.value, motivo)
    conversacion.value = res.data
    modalRechazo.value = false
    exito('Rechazaste la solicitud de mensaje.')
  } catch (e) {
    if (e.response?.status === 401) {
      router.push({ name: 'login' })
      return
    }
    errorAccion.value =
      e.response?.data?.message || 'No se pudo rechazar la solicitud.'
  } finally {
    accionEnCurso.value = false
  }
}

async function cancelar() {
  if (accionEnCurso.value) return
  accionEnCurso.value = true
  errorAccion.value = ''
  try {
    const res = await cancelarConversacion(idConversacion.value)
    conversacion.value = res.data
    modalCancelar.value = false
    exito('Cancelaste la solicitud de mensaje.')
  } catch (e) {
    if (e.response?.status === 401) {
      router.push({ name: 'login' })
      return
    }
    errorAccion.value =
      e.response?.data?.message || 'No se pudo cancelar la solicitud.'
  } finally {
    accionEnCurso.value = false
  }
}

async function alternarArchivado() {
  if (accionEnCurso.value) return
  accionEnCurso.value = true
  errorAccion.value = ''
  const archivar = !conversacion.value?.archivada
  try {
    const res = await archivarConversacion(idConversacion.value, archivar)
    conversacion.value = res.data
    exito(archivar ? 'Archivaste la conversación.' : 'Desarchivaste la conversación.')
    if (archivar) router.push({ name: nombreBandeja.value, query: { vista: 'archivadas' } })
  } catch (e) {
    if (e.response?.status === 401) {
      router.push({ name: 'login' })
      return
    }
    errorAccion.value =
      e.response?.data?.message || 'No se pudo archivar la conversación.'
  } finally {
    accionEnCurso.value = false
  }
}

function irAlPerfil() {
  if (!contraparte.value?.nombreUsuario) return
  router.push({
    name: 'perfil',
    params: { nombreUsuario: contraparte.value.nombreUsuario },
  })
}

function volver() {
  router.push({ name: nombreBandeja.value, query: { vista: 'conversaciones' } })
}

function iniciarPolling() {
  detenerPolling()
  intervalo = window.setInterval(async () => {
    if (document.visibilityState !== 'visible' || !conversacion.value) return
    const cerca = estaCercaDelFinal()
    await cargarDetalle({ silencioso: true })
    if (esAceptada.value) {
      await sincronizarNuevos()
      if ((conversacion.value?.noLeidos || 0) > 0) {
        await marcarLeida()
      }
      if (cerca) {
        await nextTick()
        scrollAlFinal()
      }
    }
  }, INTERVALO_MS)
}

function detenerPolling() {
  if (intervalo) {
    window.clearInterval(intervalo)
    intervalo = null
  }
}

async function consultarEstadoEscritura() {
  if (consultandoEscritura) return
  if (document.visibilityState !== 'visible') return
  if (!esAceptada.value) {
    contraparteEscribiendo.value = false
    return
  }
  consultandoEscritura = true
  try {
    const res = await consultarEscribiendo(idConversacion.value)
    contraparteEscribiendo.value = Boolean(res.data?.escribiendo)
  } catch {
    contraparteEscribiendo.value = false
  } finally {
    consultandoEscritura = false
  }
}

function iniciarPollingEscritura() {
  detenerPollingEscritura()
  intervaloEscritura = window.setInterval(consultarEstadoEscritura, 2000)
}

function detenerPollingEscritura() {
  if (intervaloEscritura) {
    window.clearInterval(intervaloEscritura)
    intervaloEscritura = null
  }
}

watch(idConversacion, (nuevo, anterior) => {
  if (nuevo && nuevo !== anterior) reiniciar()
})

onMounted(() => {
  inicializar()
  iniciarPolling()
  iniciarPollingEscritura()
  quitarSuscripcionSocket = suscribirSocket(manejarEventoSocket)
})

onBeforeUnmount(() => {
  quitarSuscripcionSocket?.()
  detenerTemporizadorEscritura()
  detenerPolling()
  detenerPollingEscritura()
})
</script>

<template>
  <div class="mx-auto max-w-4xl px-4 py-8 sm:px-8 sm:py-10">
    <button
      type="button"
      class="text-sm font-medium text-cafe/70 transition hover:text-coral"
      @click="volver"
    >
      ← Volver a mensajes
    </button>

    <div v-if="cargando" class="mt-6 space-y-3" role="status" aria-live="polite">
      <span class="sr-only">Cargando conversación...</span>
      <div class="h-20 animate-pulse rounded-2xl border border-rosa/40 bg-blanco"></div>
      <div class="h-64 animate-pulse rounded-2xl border border-rosa/40 bg-blanco"></div>
    </div>

    <div
      v-else-if="error"
      class="mt-8 rounded-2xl border border-rojizo/25 bg-rojizo/5 px-4 py-8 text-center"
    >
      <p class="text-sm text-rojizo">{{ error }}</p>
      <button
        type="button"
        class="mt-4 min-h-10 rounded-full border border-rosa bg-blanco px-4 text-sm font-semibold text-granate transition hover:bg-rosa/20"
        @click="inicializar"
      >
        Reintentar
      </button>
    </div>

    <template v-else-if="conversacion">
      <header class="mt-5 flex flex-col gap-4 rounded-3xl border border-rosa/50 bg-blanco p-4 sm:flex-row sm:items-center sm:justify-between sm:p-5">
        <div class="flex min-w-0 items-center gap-3">
          <img
            v-if="contraparte?.urlFotoPerfil || contraparte?.urlFotoPerfilMiniatura"
            :src="contraparte?.urlFotoPerfilMiniatura || contraparte?.urlFotoPerfil"
            alt=""
            class="h-12 w-12 shrink-0 rounded-full border-2 border-white object-cover shadow-sm ring-1 ring-rosa/70"
          />
          <span
            v-else
            class="flex h-12 w-12 shrink-0 items-center justify-center rounded-full bg-rosa text-base font-bold text-granate ring-1 ring-white/70"
            aria-hidden="true"
          >
            {{ inicialDe(contraparte?.nombreUsuario) }}
          </span>
          <div class="min-w-0">
            <button
              type="button"
              class="block truncate text-left font-display text-lg font-bold text-granate transition hover:text-coral"
              @click="irAlPerfil"
            >
              {{ nombreContraparte }}
            </button>
            <p class="truncate text-xs text-cafe/55">@{{ contraparte?.nombreUsuario }}</p>
            <span
              class="mt-1 inline-flex rounded-full border px-2.5 py-0.5 text-[11px] font-semibold"
              :class="estadoInfo.clase"
            >
              {{ estadoInfo.texto }}
            </span>
          </div>
        </div>

        <div class="flex shrink-0 flex-wrap items-center gap-2">
          <button
            type="button"
            class="inline-flex min-h-9 items-center gap-2 rounded-full border border-rosa bg-blanco px-3.5 text-xs font-semibold text-cafe transition hover:bg-rosa/20 disabled:opacity-50"
            :disabled="accionEnCurso"
            @click="alternarArchivado"
          >
            {{ conversacion.archivada ? 'Desarchivar' : 'Archivar' }}
          </button>
        </div>
      </header>

      <section
        v-if="esPendiente"
        class="mt-5 rounded-3xl border border-coral/30 bg-rosa/10 p-5"
      >
        <p class="text-xs font-semibold uppercase tracking-[0.16em] text-coral">
          Solicitud de mensaje
        </p>
        <p class="mt-2 text-sm text-cafe/80">
          <span class="font-semibold text-granate">Motivo:</span>
          {{ conversacion.motivoContacto }}
        </p>
        <div
          v-if="primerMensaje"
          class="mt-3 whitespace-pre-line rounded-2xl border border-rosa/50 bg-blanco px-4 py-3 text-sm leading-relaxed text-cafe"
        >
          {{ primerMensaje.contenido }}
        </div>
        <p class="mt-2 text-[11px] text-cafe/50">
          {{ formatearFechaHora(primerMensaje?.fechaEnvio || conversacion.fechaSolicitud) }}
        </p>

        <div v-if="puedoResponderSolicitud" class="mt-4 flex flex-wrap gap-2">
          <button
            type="button"
            class="min-h-10 rounded-full bg-coral px-5 text-sm font-bold text-white shadow-sm transition hover:bg-rojizo disabled:cursor-not-allowed disabled:opacity-60"
            :disabled="accionEnCurso"
            @click="aceptar"
          >
            {{ accionEnCurso ? 'Aceptando...' : 'Aceptar y abrir conversación' }}
          </button>
          <button
            type="button"
            class="min-h-10 rounded-full border border-rojizo/40 bg-blanco px-5 text-sm font-semibold text-rojizo transition hover:bg-rojizo/5 disabled:opacity-60"
            :disabled="accionEnCurso"
            @click="modalRechazo = true"
          >
            Rechazar
          </button>
        </div>
        <div v-else class="mt-4 flex flex-wrap items-center gap-3">
          <span class="text-sm text-cafe/60">
            Esperando que {{ nombreContraparte }} responda.
          </span>
          <button
            v-if="conversacion.puedeCancelar"
            type="button"
            class="min-h-9 rounded-full border border-rosa bg-blanco px-4 text-xs font-semibold text-cafe transition hover:bg-rosa/20 disabled:opacity-50"
            :disabled="accionEnCurso"
            @click="modalCancelar = true"
          >
            Cancelar solicitud
          </button>
        </div>
      </section>

      <section
        v-else-if="esRechazada"
        class="mt-5 rounded-3xl border border-rojizo/25 bg-rojizo/5 p-5"
      >
        <p class="text-sm font-semibold text-rojizo">
          {{ conversacion.soyIniciador
            ? 'La solicitud fue rechazada.'
            : 'Rechazaste esta solicitud.' }}
        </p>
        <p v-if="conversacion.motivoRechazo" class="mt-2 text-sm text-cafe/75">
          <span class="font-semibold">Motivo:</span>
          {{ conversacion.motivoRechazo }}
        </p>
        <p v-if="conversacion.fechaRechazo" class="mt-1 text-xs text-cafe/55">
          {{ formatearFechaHora(conversacion.fechaRechazo) }}
        </p>
        <p
          v-if="conversacion.soyIniciador && conversacion.fechaProximoEnvio"
          class="mt-3 text-sm text-cafe/70"
        >
          Vas a poder volver a enviar una solicitud el
          <strong>{{ formatearFechaHora(conversacion.fechaProximoEnvio) }}</strong>.
        </p>
      </section>

      <section
        v-else-if="esCancelada"
        class="mt-5 rounded-3xl border border-borde/60 bg-superficie-alt p-5"
      >
        <p class="text-sm font-semibold text-cafe">
          {{ conversacion.soyIniciador
            ? 'Cancelaste esta solicitud.'
            : 'La solicitud fue cancelada.' }}
        </p>
        <p v-if="conversacion.fechaRespuesta" class="mt-1 text-xs text-cafe/55">
          {{ formatearFechaHora(conversacion.fechaRespuesta) }}
        </p>
      </section>

      <div
        v-if="esAceptada"
        ref="refLista"
        class="mt-5 max-h-[58vh] space-y-3 overflow-y-auto rounded-3xl border border-rosa/50 bg-superficie px-4 py-5 sm:px-6"
        aria-live="polite"
      >
        <div v-if="hayMas" class="pb-1 text-center">
          <button
            type="button"
            class="min-h-9 rounded-full border border-rosa bg-blanco px-4 text-xs font-semibold text-granate transition hover:bg-rosa/20 disabled:cursor-wait disabled:opacity-60"
            :disabled="cargandoMas"
            @click="cargarAnteriores"
          >
            {{ cargandoMas ? 'Cargando...' : 'Cargar mensajes anteriores' }}
          </button>
        </div>

        <p
          v-if="!mensajes.length"
          class="py-10 text-center text-sm text-cafe/55"
        >
          Todavía no hay mensajes. Escribí el primero.
        </p>

        <div
          v-for="mensaje in mensajes"
          :key="mensaje.idMensaje"
          class="flex"
          :class="mensaje.esPropio ? 'justify-end' : 'justify-start'"
        >
          <div class="max-w-[85%] sm:max-w-[75%]">
            <div
              class="whitespace-pre-line rounded-2xl px-4 py-2.5 text-sm leading-relaxed shadow-sm"
              :class="mensaje.esPropio
                ? 'rounded-br-md bg-coral text-white'
                : 'rounded-bl-md border border-rosa/50 bg-blanco text-cafe'"
            >
              {{ mensaje.contenido }}
            </div>
            <p
              class="mt-1 text-[10px] text-cafe/45"
              :class="mensaje.esPropio ? 'text-right' : 'text-left'"
            >
              {{ formatearFechaMensaje(mensaje.fechaEnvio) }}
              <span
                v-if="mensaje.esPropio && mensaje.leido"
                class="font-semibold text-coral"
              >
                · Leído
              </span>
            </p>
          </div>
        </div>
      </div>

      <div
        v-if="esAceptada"
        class="mt-3 min-h-5 px-1 text-xs text-cafe/55"
        role="status"
        aria-live="polite"
      >
        <span v-if="contraparteEscribiendo" class="inline-flex items-center gap-1.5">
          <span class="inline-flex items-center gap-0.5" aria-hidden="true">
            <span class="h-1 w-1 animate-bounce rounded-full bg-coral motion-reduce:animate-none" style="animation-delay: -0.3s"></span>
            <span class="h-1 w-1 animate-bounce rounded-full bg-coral motion-reduce:animate-none" style="animation-delay: -0.15s"></span>
            <span class="h-1 w-1 animate-bounce rounded-full bg-coral motion-reduce:animate-none"></span>
          </span>
          {{ nombreContraparte }} está escribiendo…
        </span>
      </div>

      <form
        v-if="esAceptada"
        class="mt-4 rounded-3xl border border-rosa/50 bg-blanco p-3 sm:p-4"
        @submit.prevent="enviar"
      >
        <label for="cuerpo-mensaje" class="sr-only">Escribí un mensaje</label>
        <textarea
          id="cuerpo-mensaje"
          ref="campoMensaje"
          v-model="texto"
          rows="3"
          :maxlength="maxMensaje"
          :disabled="!conversacion.puedeEnviar || enviando"
          aria-describedby="contador-mensaje"
          placeholder="Escribí un mensaje..."
          class="w-full resize-none rounded-2xl border border-rosa/70 bg-blanco px-3.5 py-2.5 text-sm text-cafe transition placeholder:text-cafe/40 focus:border-coral focus:outline-none focus:ring-2 focus:ring-coral/20 disabled:cursor-not-allowed disabled:opacity-60"
          @input="avisarEscribiendo"
          @keydown.enter.exact="manejarEnter"
        ></textarea>

        <div class="mt-2 flex flex-wrap items-center justify-between gap-3">
          <p id="contador-mensaje" class="text-[11px] text-cafe/50">
            {{ texto.length }}/{{ maxMensaje }} · Enter envía, Shift+Enter hace un salto de línea.
          </p>
          <button
            type="submit"
            class="inline-flex min-h-10 items-center gap-2 rounded-full bg-coral px-5 text-sm font-bold text-white shadow-sm transition hover:bg-rojizo disabled:cursor-not-allowed disabled:opacity-50"
            :disabled="enviarDeshabilitado"
          >
            <svg class="h-4 w-4" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true">
              <path stroke-linecap="round" stroke-linejoin="round" d="m4.5 19.5 15-7.5-15-7.5 0 6 9 1.5-9 1.5v6Z" />
            </svg>
            {{ enviando ? 'Enviando...' : 'Enviar' }}
          </button>
        </div>

        <p
          v-if="errorEnvio"
          class="mt-2 rounded-xl border border-rojizo/25 bg-rojizo/5 px-3 py-2 text-xs text-rojizo"
          role="alert"
        >
          {{ errorEnvio }}
        </p>
        <p
          v-else-if="conversacion.motivoNoEnviable"
          class="mt-2 text-xs text-cafe/55"
        >
          {{ conversacion.motivoNoEnviable }}
        </p>
      </form>

      <p
        v-if="errorAccion"
        class="mt-4 rounded-xl border border-rojizo/25 bg-rojizo/5 px-3.5 py-2.5 text-sm text-rojizo"
        role="alert"
      >
        {{ errorAccion }}
      </p>
    </template>

    <ModalRechazoConversacion
      :abierto="modalRechazo"
      :cargando="accionEnCurso"
      :error="errorAccion"
      :maximo="maxMotivo"
      @cerrar="modalRechazo = false"
      @confirmar="rechazar"
    />

    <ModalConfirmacion
      :abierto="modalCancelar"
      titulo="¿Cancelar la solicitud?"
      mensaje="La persona va a ver que retiraste la solicitud. No se va a abrir la conversación."
      texto-confirmar="Cancelar solicitud"
      texto-cancelar="Volver"
      destructivo
      @cerrar="modalCancelar = false"
      @confirmar="cancelar"
    />
  </div>
</template>
