<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import api from '../servicios/api'
import { formatearPrecio } from '../servicios/moneda'
import {
  contraparteDe,
  datosContraparte,
  estadoDe,
  formatearFechaSolicitud,
  gruposDeRespuestas,
} from '../servicios/solicitudes'
import { estadoServicioDe } from '../servicios/servicios'
import VisorImagenes from '../components/VisorImagenes.vue'
import ModalRechazo from '../components/ModalRechazo.vue'
import ModalCancelacion from '../components/ModalCancelacion.vue'
import ModalConfirmacion from '../components/ModalConfirmacion.vue'
import TarjetaPresupuesto from '../components/TarjetaPresupuesto.vue'
import ReputacionCliente from '../components/ReputacionCliente.vue'
import ValoracionEstrellas from '../components/ValoracionEstrellas.vue'
import { cargarResumenReseniasArtista, formatearPromedio } from '../servicios/resenias'

const route = useRoute()
const router = useRouter()

const solicitud = ref(null)
const pestanaActiva = ref('presupuesto')
const contenidoRef = ref(null)
const tabsRef = ref(null)
const cargando = ref(true)
const errorCarga = ref('')
const resumenArtista = ref(null)

const idActual = Number(localStorage.getItem('idUsuario'))

const esArtistaDeLaSolicitud = computed(() => {
  const idArtista = solicitud.value?.artista?.idUsuario
  if (!idArtista || !idActual) return false
  return idArtista === idActual
})

const contraparte = computed(() =>
  contraparteDe(solicitud.value, esArtistaDeLaSolicitud.value),
)
const datosPersona = computed(() => datosContraparte(contraparte.value))
const tituloContraparte = computed(() =>
  esArtistaDeLaSolicitud.value ? 'Cliente' : 'Artista',
)
const nombreContraparte = computed(() => datosPersona.value?.nombre || 'No disponible')
const usuarioContraparte = computed(() => datosPersona.value?.usuario || '')
const fotoContraparte = computed(() => datosPersona.value?.foto || '')
const inicialContraparte = computed(() => datosPersona.value?.inicial || '?')
const estadoInfo = computed(() => estadoDe(solicitud.value?.estado))
const tienePresupuesto = computed(() => Boolean(solicitud.value?.presupuesto))
const nombreRutaPedido = computed(() => (
  esArtistaDeLaSolicitud.value ? 'panel-pedido-detalle' : 'pedido-detalle'
))

const ayudaContacto = computed(() => {
  if (!contraparte.value) return ''
  const estado = solicitud.value?.estado
  if (esArtistaDeLaSolicitud.value) {
    if (estado === 'PENDIENTE' || estado === 'EN_LISTA_ESPERA') {
      return 'Si el formulario no alcanza para presupuestar, escribile al cliente.'
    }
    if (estado === 'PRESUPUESTADA') {
      return 'Si querés ajustar algo del presupuesto, hablá con el cliente antes de que decida.'
    }
    return '¿Tenés dudas sobre esta etapa? Podés escribirle al cliente.'
  }
  if (estado === 'PENDIENTE' || estado === 'EN_LISTA_ESPERA') {
    return 'Si te falta contar algo para el presupuesto, escribile al artista.'
  }
  if (estado === 'PRESUPUESTADA') {
    return 'Si tenés dudas sobre el presupuesto, escribile al artista antes de aceptar.'
  }
  return '¿Tenés dudas sobre esta etapa? Podés escribirle al artista.'
})

const avisoServicio = computed(() => {
  const estado = solicitud.value?.estadoServicio
  if (!estado || estado === 'PUBLICADO') return null
  return estado === 'ARCHIVADO'
    ? 'Este servicio fue dado de baja definitivamente. El historial de tu solicitud sigue disponible.'
    : 'Este servicio está pausado: no recibe solicitudes nuevas, pero esta solicitud sigue vigente.'
})
const avisoServicioClase = computed(() => (
  solicitud.value?.estadoServicio === 'ARCHIVADO'
    ? 'border-rojizo/25 bg-rojizo/5 text-rojizo'
    : 'border-durazno bg-durazno/30 text-granate'
))
const estadoServicioInfo = computed(() => estadoServicioDe(solicitud.value?.estadoServicio))

const modalRechazoAbierto = ref(false)
const rechazando = ref(false)
const errorRechazo = ref('')
const avisoExito = ref('')
const modalRetiroAbierto = ref(false)
const retirando = ref(false)
const errorRetiro = ref('')
const modalCancelacionAbierto = ref(false)
const cancelando = ref(false)
const errorCancelacion = ref('')

const puedeRechazar = computed(
  () => esArtistaDeLaSolicitud.value
    && ['PENDIENTE', 'EN_LISTA_ESPERA'].includes(solicitud.value?.estado),
)
const puedePresupuestar = computed(
  () => esArtistaDeLaSolicitud.value
    && ['PENDIENTE', 'EN_LISTA_ESPERA'].includes(solicitud.value?.estado)
    && solicitud.value?.puedePresupuestar === true,
)
const bloqueoPresupuesto = computed(() => {
  if (!esArtistaDeLaSolicitud.value) return ''
  if (!['PENDIENTE', 'EN_LISTA_ESPERA'].includes(solicitud.value?.estado)) return ''
  if (solicitud.value?.puedePresupuestar) return ''
  return solicitud.value?.motivoNoPresupuestable
    || 'No podés presupuestar esta solicitud todavía.'
})
const puedeEditarServicio = computed(
  () => solicitud.value?.estadoServicio !== 'ARCHIVADO',
)
const textoMotivoRechazo = computed(() => {
  if (solicitud.value?.motivoRechazo) return solicitud.value.motivoRechazo
  return esArtistaDeLaSolicitud.value
    ? 'No especificaste un motivo.'
    : 'El artista decidió no mencionar el motivo.'
})
const puedeCancelar = computed(
  () => !esArtistaDeLaSolicitud.value && solicitud.value?.puedeCancelar === true,
)
const textoMotivoCancelacion = computed(() => {
  if (solicitud.value?.motivoCancelacion) return solicitud.value.motivoCancelacion
  return esArtistaDeLaSolicitud.value
    ? 'El cliente decidió no mencionar el motivo.'
    : 'Decidiste no mencionar el motivo.'
})
const mensajeCancelacion = computed(() => {
  if (solicitud.value?.estado === 'PRESUPUESTADA') {
    return 'El presupuesto quedará sin efecto y el cupo reservado se va a liberar. '
      + 'El artista recibirá un aviso y esta acción no se puede deshacer.'
  }
  return 'La solicitud saldrá de la cola del artista y recibirá un aviso. '
    + 'Esta acción no se puede deshacer.'
})

const dibujosAbiertos = ref(new Set())
const visor = ref({ abierto: false, imagenes: [], indice: 0, etiqueta: '', descripcion: '' })

function resumenDeGrupos(grupos) {
  if (!grupos.length) return 'Este dibujo no tiene respuestas registradas.'
  const primero = grupos[0]
  if (primero.codigoTipo === 'SUBIR_IMAGEN') {
    const cantidad = primero.valores.length
    return `${primero.enunciado}: ${cantidad} ${cantidad === 1 ? 'imagen adjunta' : 'imágenes adjuntas'}`
  }
  const texto = primero.valores.join(' ').replace(/\s+/g, ' ').trim()
  return `${primero.enunciado}: ${texto}`
}

const dibujos = computed(() =>
  (solicitud.value?.items || []).map((item, posicion) => {
    const grupos = gruposDeRespuestas(item)
    return { ...item, posicion, grupos, resumen: resumenDeGrupos(grupos) }
  }),
)

function alternarDibujo(idItem) {
  const siguiente = new Set(dibujosAbiertos.value)
  if (siguiente.has(idItem)) {
    siguiente.delete(idItem)
  } else {
    siguiente.add(idItem)
  }
  dibujosAbiertos.value = siguiente
}

function cambiarPestana(pestana) {
  if (pestanaActiva.value === pestana) return

  const tabs = tabsRef.value
  const alturaHeader = window.innerWidth >= 640 ? 64 : 56
  const estabaFijo = tabs && tabs.getBoundingClientRect().top <= alturaHeader + 1

  pestanaActiva.value = pestana

  if (!estabaFijo || !contenidoRef.value) return

  const top = contenidoRef.value.getBoundingClientRect().top
    + window.scrollY
    - alturaHeader
    - 12
  window.scrollTo({ top: Math.max(0, top), behavior: 'smooth' })
}

function abrirVisor(dibujo, grupo, posicionImagen) {
  visor.value = {
    abierto: true,
    imagenes: grupo.valores,
    indice: posicionImagen,
    etiqueta: `Dibujo ${dibujo.posicion + 1}`,
    descripcion: grupo.enunciado,
  }
}

function cerrarVisor() {
  visor.value = { ...visor.value, abierto: false }
}

function abrirModalRechazo() {
  errorRechazo.value = ''
  modalRechazoAbierto.value = true
}

function cerrarModalRechazo() {
  if (!rechazando.value) modalRechazoAbierto.value = false
}

async function confirmarRechazo(motivo) {
  if (rechazando.value) return
  rechazando.value = true
  errorRechazo.value = ''
  try {
    const res = await api.post(
      `/solicitudes-servicio/${solicitud.value.idSolicitud}/rechazar`,
      { motivo: motivo || null },
    )
    solicitud.value = res.data
    modalRechazoAbierto.value = false
    avisoExito.value = 'La solicitud fue rechazada y el cliente recibió el aviso.'
  } catch (e) {
    if (e.response?.status === 401) {
      router.push({ name: 'login' })
      return
    }
    errorRechazo.value = e.response?.data?.message || 'No se pudo rechazar la solicitud.'
  } finally {
    rechazando.value = false
  }
}

function irAPresupuestar() {
  if (!solicitud.value?.idSolicitud) return
  router.push({
    name: 'presupuesto-crear',
    params: { idSolicitud: solicitud.value.idSolicitud },
  })
}

function irAPagar() {
  if (!solicitud.value?.idSolicitud) return
  router.push({
    name: 'pago-presupuesto',
    params: { idSolicitud: solicitud.value.idSolicitud },
  })
}

function irAlPerfil() {
  const usuario = contraparte.value?.nombreUsuario
  if (!usuario) return
  router.push({ name: 'perfil', params: { nombreUsuario: usuario } })
}

function irAlChat() {
  const idConversacion = solicitud.value?.idConversacion
  if (!idConversacion) {
    irAlPerfil()
    return
  }
  router.push({
    name: 'conversacion',
    params: { idConversacion },
    query: { volverA: 'solicitud', idSolicitud: solicitud.value.idSolicitud },
  })
}

async function cargarResumenArtista() {
  resumenArtista.value = null
  const nombre = solicitud.value?.artista?.nombreUsuario
  if (!nombre || esArtistaDeLaSolicitud.value) return
  try {
    const res = await cargarResumenReseniasArtista(nombre)
    resumenArtista.value = res.data || null
  } catch {
    resumenArtista.value = null
  }
}

function abrirModalRetiro() {
  errorRetiro.value = ''
  modalRetiroAbierto.value = true
}

function cerrarModalRetiro() {
  if (!retirando.value) modalRetiroAbierto.value = false
}

async function confirmarRetiro() {
  if (retirando.value) return
  retirando.value = true
  errorRetiro.value = ''
  try {
    const res = await api.post(
      `/solicitudes-servicio/${solicitud.value.idSolicitud}/retirar-presupuesto`,
    )
    solicitud.value = res.data
    modalRetiroAbierto.value = false
    avisoExito.value = 'El presupuesto fue retirado y el cupo quedó liberado.'
  } catch (e) {
    if (e.response?.status === 401) {
      router.push({ name: 'login' })
      return
    }
    errorRetiro.value = e.response?.data?.message || 'No se pudo retirar el presupuesto.'
  } finally {
    retirando.value = false
  }
}

function abrirModalCancelacion() {
  errorCancelacion.value = ''
  modalCancelacionAbierto.value = true
}

function cerrarModalCancelacion() {
  if (!cancelando.value) modalCancelacionAbierto.value = false
}

async function confirmarCancelacion(motivo) {
  if (cancelando.value) return
  cancelando.value = true
  errorCancelacion.value = ''
  try {
    const res = await api.post(
      `/solicitudes-servicio/${solicitud.value.idSolicitud}/cancelar`,
      { motivo: motivo || null },
    )
    solicitud.value = { ...solicitud.value, ...res.data }
    modalCancelacionAbierto.value = false
    avisoExito.value = 'La solicitud fue cancelada y el artista recibió el aviso.'
  } catch (e) {
    if (e.response?.status === 401) {
      router.push({ name: 'login' })
      return
    }
    errorCancelacion.value = e.response?.data?.message || 'No se pudo cancelar la solicitud.'
  } finally {
    cancelando.value = false
  }
}

function volver() {
  const destino = localStorage.getItem('rol') === 'ARTISTA' ? 'panel-solicitudes' : 'solicitudes'
  router.push({ name: destino })
}

async function cargar() {
  cargando.value = true
  errorCarga.value = ''
  pestanaActiva.value = 'presupuesto'
  try {
    const res = await api.get(`/solicitudes-servicio/${route.params.idSolicitud}`)
    solicitud.value = res.data
    void cargarResumenArtista()
  } catch (e) {
    if (e.response?.status === 401) {
      router.push({ name: 'login' })
      return
    }
    if (e.response?.status === 403 || e.response?.status === 404) {
      errorCarga.value = 'No podés ver esta solicitud o ya no existe.'
    } else {
      errorCarga.value = e.response?.data?.message || 'No se pudo cargar la solicitud.'
    }
  } finally {
    cargando.value = false
  }
}

onMounted(cargar)
</script>

<template>
  <div class="mx-auto max-w-6xl px-4 py-8 sm:px-8 sm:py-10">
    <button
      type="button"
      class="text-sm font-medium text-cafe/70 transition hover:text-coral"
      @click="volver"
    >
      ← Volver a solicitudes
    </button>

    <div v-if="cargando" class="mt-8 rounded-2xl border border-rosa/50 bg-blanco px-6 py-16 text-center text-sm text-cafe/60">
      Cargando solicitud...
    </div>

    <div v-else-if="errorCarga" class="mt-8 rounded-2xl border border-rosa/50 bg-blanco px-6 py-16 text-center">
      <p class="text-sm text-cafe/70">{{ errorCarga }}</p>
      <div class="mt-4 flex flex-wrap justify-center gap-2">
        <button
          type="button"
          class="rounded-full border border-rosa bg-blanco px-4 py-2 text-sm font-semibold text-granate transition hover:bg-rosa/20"
          @click="cargar"
        >
          Reintentar
        </button>
        <button
          type="button"
          class="rounded-full bg-coral px-4 py-2 text-sm font-semibold text-white transition hover:bg-rojizo"
          @click="volver"
        >
          Ir a mis solicitudes
        </button>
      </div>
    </div>

    <template v-else-if="solicitud">
      <header class="mt-6 flex flex-col gap-4 sm:flex-row sm:items-start sm:justify-between">
        <div>
          <p class="text-sm font-semibold uppercase tracking-[0.18em] text-coral">Solicitud de servicio</p>
          <h1 class="mt-1 font-display text-3xl font-extrabold tracking-[-0.02em] text-granate sm:text-4xl">
            Solicitud #{{ solicitud.idSolicitud }}
          </h1>
          <p class="mt-2 text-sm text-cafe/65">
            {{ solicitud.nombreServicio || 'Servicio' }}
            <span aria-hidden="true"> · </span>
            {{ formatearFechaSolicitud(solicitud.fechaCreacion) }}
          </p>
        </div>
        <span
          class="inline-flex self-start rounded-full border px-3 py-1.5 text-xs font-semibold"
          :class="estadoInfo.clase"
        >
          {{ estadoInfo.texto }}
        </span>
      </header>

      <p
        v-if="avisoServicio"
        class="mt-4 rounded-xl border px-4 py-3 text-sm leading-relaxed"
        :class="avisoServicioClase"
        role="status"
      >
        {{ avisoServicio }}
        <span
          v-if="solicitud.estadoServicio === 'ARCHIVADO'"
          class="ml-1 inline-flex rounded-full border px-2 py-0.5 text-[11px] font-semibold"
          :class="estadoServicioInfo.clase"
        >
          {{ estadoServicioInfo.texto }}
        </span>
      </p>

      <p
        v-if="solicitud.estado === 'VENCIDA'"
        class="mt-4 rounded-xl border border-cafe/25 bg-cafe/5 px-4 py-3 text-sm leading-relaxed text-cafe"
        role="status"
      >
        El presupuesto venció y el cupo quedó liberado. Si querés, podés volver a solicitar el servicio.
      </p>

      <div class="mt-8 grid gap-6 lg:grid-cols-[minmax(0,1fr)_20rem]">
        <div ref="contenidoRef" class="space-y-6">
          <section
            v-if="solicitud.estado === 'RECHAZADA'"
            class="rounded-2xl border border-rojizo/25 bg-rojizo/5 p-5 sm:p-6"
          >
            <p class="text-xs font-semibold uppercase tracking-[0.14em] text-rojizo">
              Solicitud rechazada
            </p>
            <p class="mt-2 text-sm leading-relaxed text-cafe/80">{{ textoMotivoRechazo }}</p>
            <p v-if="solicitud.fechaRechazo" class="mt-2 text-xs text-cafe/50">
              {{ formatearFechaSolicitud(solicitud.fechaRechazo) }}
            </p>
          </section>

          <section
            v-if="solicitud.estado === 'CANCELADA'"
            class="rounded-2xl border border-rojizo/25 bg-rojizo/5 p-5 sm:p-6"
          >
            <p class="text-xs font-semibold uppercase tracking-[0.14em] text-rojizo">
              Solicitud cancelada
            </p>
            <p class="mt-2 text-sm leading-relaxed text-cafe/80">{{ textoMotivoCancelacion }}</p>
            <p v-if="solicitud.fechaCancelacion" class="mt-2 text-xs text-cafe/50">
              {{ formatearFechaSolicitud(solicitud.fechaCancelacion) }}
            </p>
          </section>

          <template v-if="tienePresupuesto">
            <div
              ref="tabsRef"
              class="sticky top-14 z-20 flex flex-wrap items-center gap-2 border-b border-borde/50 bg-crema pb-1.5 pt-1.5 sm:top-16"
              role="tablist"
              aria-label="Secciones de la solicitud"
            >
              <button
                id="pestana-presupuesto"
                type="button"
                role="tab"
                aria-controls="panel-presupuesto"
                :aria-selected="pestanaActiva === 'presupuesto'"
                class="rounded-full px-5 py-2.5 text-sm font-semibold transition focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-coral/50 focus-visible:ring-offset-2 focus-visible:ring-offset-crema"
                :class="pestanaActiva === 'presupuesto' ? 'bg-coral text-white shadow-sm' : 'text-cafe/60 hover:bg-rosa/25 hover:text-granate'"
                @click="cambiarPestana('presupuesto')"
              >
                Presupuesto
              </button>
              <button
                id="pestana-solicitud"
                type="button"
                role="tab"
                aria-controls="panel-solicitud"
                :aria-selected="pestanaActiva === 'solicitud'"
                class="rounded-full px-5 py-2.5 text-sm font-semibold transition focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-coral/50 focus-visible:ring-offset-2 focus-visible:ring-offset-crema"
                :class="pestanaActiva === 'solicitud' ? 'bg-coral text-white shadow-sm' : 'text-cafe/60 hover:bg-rosa/25 hover:text-granate'"
                @click="cambiarPestana('solicitud')"
              >
                Solicitud
              </button>
            </div>

            <section
              v-show="pestanaActiva === 'presupuesto'"
              id="panel-presupuesto"
              role="tabpanel"
              aria-labelledby="pestana-presupuesto"
              tabindex="0"
              class="focus-visible:outline-none"
            >
              <TarjetaPresupuesto
                :presupuesto="solicitud.presupuesto"
                :estado="solicitud.estado"
                :es-artista="esArtistaDeLaSolicitud"
              />
            </section>
          </template>

          <div
            v-show="!tienePresupuesto || pestanaActiva === 'solicitud'"
            :id="tienePresupuesto ? 'panel-solicitud' : undefined"
            :role="tienePresupuesto ? 'tabpanel' : undefined"
            :aria-labelledby="tienePresupuesto ? 'pestana-solicitud' : undefined"
            :tabindex="tienePresupuesto ? 0 : undefined"
            class="space-y-6 focus-visible:outline-none"
          >
            <section class="rounded-2xl border border-rosa/50 bg-blanco p-5 sm:p-6">
              <p class="text-xs font-semibold uppercase tracking-[0.14em] text-cafe/45">
                {{ tituloContraparte }}
              </p>

              <div v-if="contraparte" class="mt-4 grid gap-5 md:grid-cols-[minmax(0,1fr)_auto] md:items-center">
                <div class="flex min-w-0 items-center gap-4">
                  <img
                    v-if="fotoContraparte"
                    :src="fotoContraparte"
                    alt=""
                    class="h-16 w-16 shrink-0 rounded-full border-2 border-white object-cover shadow-sm ring-1 ring-rosa/70"
                  />
                  <span
                    v-else
                    class="flex h-16 w-16 shrink-0 items-center justify-center rounded-full bg-rosa text-xl font-bold text-granate shadow-sm ring-1 ring-white/70"
                    aria-hidden="true"
                  >
                    {{ inicialContraparte }}
                  </span>

                  <div class="min-w-0">
                    <p class="text-lg font-semibold text-granate">{{ nombreContraparte }}</p>
                    <p v-if="usuarioContraparte" class="text-sm text-cafe/55">@{{ usuarioContraparte }}</p>
                    <p v-if="contraparte.nombrePais" class="mt-1 text-xs text-cafe/50">{{ contraparte.nombrePais }}</p>
                  </div>
                </div>

                <div class="flex flex-wrap gap-2 md:flex-col md:border-l md:border-rosa/40 md:pl-5">
                  <router-link
                    :to="{ name: 'perfil', params: { nombreUsuario: contraparte.nombreUsuario } }"
                    class="inline-flex min-h-9 items-center justify-center rounded-full border border-rosa bg-blanco px-4 text-sm font-semibold text-granate transition hover:bg-rosa/20"
                  >
                    Ver perfil
                  </router-link>
                  <button
                    type="button"
                    class="inline-flex min-h-9 items-center justify-center rounded-full border border-coral/40 bg-coral/5 px-4 text-sm font-semibold text-coral transition hover:bg-coral/10"
                    @click="irAlChat"
                  >
                    Enviar mensaje
                  </button>
                </div>
              </div>

              <p v-else class="mt-3 text-sm text-cafe/60">
                Los datos públicos de esta persona no están disponibles.
              </p>

              <p v-if="contraparte?.biografia" class="mt-4 border-t border-rosa/40 pt-4 text-sm leading-relaxed text-cafe/70">
                {{ contraparte.biografia }}
              </p>

              <div
                v-if="!esArtistaDeLaSolicitud && resumenArtista?.hayDatos"
                class="mt-3 flex flex-wrap items-center gap-2"
              >
                <ValoracionEstrellas
                  :model-value="Math.round(Number(resumenArtista.promedio))"
                  readonly
                  tamano="sm"
                  etiqueta="Promedio de reseñas del artista"
                />
                <span class="text-sm font-semibold text-granate">
                  {{ formatearPromedio(resumenArtista.promedio) }}
                </span>
                <span class="text-xs text-cafe/55">
                  ({{ resumenArtista.cantidad }}
                  {{ resumenArtista.cantidad === 1 ? 'reseña' : 'reseñas' }})
                </span>
              </div>
              <p
                v-else-if="!esArtistaDeLaSolicitud && resumenArtista"
                class="mt-3 text-xs text-cafe/50"
              >
                El artista todavía no tiene reseñas.
              </p>
            </section>

            <section>
              <div class="flex flex-wrap items-end justify-between gap-2">
                <div>
                  <h2 class="text-xl font-semibold text-granate">Dibujos solicitados</h2>
                  <p class="mt-1 text-sm text-cafe/60">
                    {{ solicitud.cantidadDibujos }} {{ solicitud.cantidadDibujos === 1 ? 'dibujo' : 'dibujos' }} en esta solicitud.
                  </p>
                </div>
                <span
                  v-if="solicitud.prioridadDefinidaPorCliente"
                  class="rounded-full bg-durazno/50 px-3 py-1 text-xs font-semibold text-granate"
                >
                  Prioridad definida
                </span>
              </div>

              <div class="mt-4 space-y-3">
                <article
                  v-for="dibujo in dibujos"
                  :key="dibujo.idItemSolicitud"
                  class="overflow-hidden rounded-2xl border border-rosa/50 bg-blanco"
                >
                  <h3>
                    <button
                      type="button"
                      class="flex w-full items-start justify-between gap-4 px-5 py-4 text-left transition hover:bg-rosa/10 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-inset focus-visible:ring-coral/50 sm:px-6"
                      :aria-expanded="dibujosAbiertos.has(dibujo.idItemSolicitud)"
                      :aria-controls="`panel-dibujo-${dibujo.idItemSolicitud}`"
                      @click="alternarDibujo(dibujo.idItemSolicitud)"
                    >
                      <span class="min-w-0">
                        <span class="flex flex-wrap items-center gap-2">
                          <span class="text-xs font-semibold uppercase tracking-[0.14em] text-cafe/45">
                            Dibujo {{ dibujo.posicion + 1 }}
                          </span>
                          <span
                            v-if="solicitud.prioridadDefinidaPorCliente && dibujo.ordenPrioridad"
                            class="rounded-full bg-coral/10 px-2.5 py-0.5 text-[11px] font-semibold text-coral"
                          >
                            Prioridad {{ dibujo.ordenPrioridad }}
                          </span>
                        </span>
                        <span class="mt-1 block text-lg font-semibold text-granate">{{ dibujo.nombre }}</span>
                        <span
                          v-if="!dibujosAbiertos.has(dibujo.idItemSolicitud)"
                          class="mt-1.5 line-clamp-2 block text-sm leading-relaxed text-cafe/60"
                        >
                          {{ dibujo.resumen }}
                        </span>
                      </span>

                      <svg
                        class="mt-1 h-5 w-5 shrink-0 text-cafe/50 transition-transform duration-300"
                        :class="dibujosAbiertos.has(dibujo.idItemSolicitud) ? 'rotate-180' : ''"
                        viewBox="0 0 24 24"
                        fill="none"
                        stroke="currentColor"
                        stroke-width="2"
                        aria-hidden="true"
                      >
                        <path stroke-linecap="round" stroke-linejoin="round" d="m6 9 6 6 6-6" />
                      </svg>
                    </button>
                  </h3>

                  <div
                    :id="`panel-dibujo-${dibujo.idItemSolicitud}`"
                    class="grid transition-[grid-template-rows] duration-300 ease-out"
                    :class="dibujosAbiertos.has(dibujo.idItemSolicitud) ? 'grid-rows-[1fr]' : 'grid-rows-[0fr]'"
                    :inert="!dibujosAbiertos.has(dibujo.idItemSolicitud)"
                  >
                    <div class="overflow-hidden">
                      <div class="space-y-5 border-t border-rosa/40 px-5 py-5 sm:px-6">
                        <div
                          v-for="grupo in dibujo.grupos"
                          :key="grupo.clave"
                          class="border-b border-rosa/30 pb-5 last:border-b-0 last:pb-0"
                        >
                          <p class="text-sm font-semibold text-cafe">{{ grupo.enunciado }}</p>

                          <div v-if="grupo.codigoTipo === 'SUBIR_IMAGEN'" class="mt-3 grid grid-cols-2 gap-3 sm:grid-cols-4">
                            <button
                              v-for="(url, urlIndice) in grupo.valores"
                              :key="`${url}-${urlIndice}`"
                              type="button"
                              class="group relative block aspect-square overflow-hidden rounded-xl bg-rosa/20 ring-1 ring-rosa/40 transition hover:ring-2 hover:ring-coral focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-coral"
                              :aria-label="`Abrir ${dibujo.nombre}: imagen ${urlIndice + 1} de ${grupo.valores.length}`"
                              @click="abrirVisor(dibujo, grupo, urlIndice)"
                            >
                              <img
                                :src="url"
                                :alt="`Imagen de referencia ${urlIndice + 1} de ${dibujo.nombre}`"
                                class="h-full w-full object-cover transition duration-300 group-hover:scale-105"
                                loading="lazy"
                              />
                              <span
                                class="absolute bottom-2 right-2 rounded-full bg-granate/75 px-2 py-0.5 text-[11px] font-semibold text-white"
                              >
                                {{ urlIndice + 1 }}/{{ grupo.valores.length }}
                              </span>
                            </button>
                          </div>

                          <p
                            v-else-if="grupo.codigoTipo === 'TEXTO'"
                            class="mt-2 whitespace-pre-line text-sm leading-relaxed text-cafe/75"
                          >
                            {{ grupo.valores.join('\n') }}
                          </p>

                          <div v-else class="mt-2 flex flex-wrap gap-2">
                            <span
                              v-for="(valor, valorIndice) in grupo.valores"
                              :key="`${valor}-${valorIndice}`"
                              class="rounded-full bg-rosa/20 px-3 py-1 text-sm text-cafe"
                            >
                              {{ valor }}
                            </span>
                          </div>
                        </div>

                        <p v-if="!dibujo.grupos.length" class="text-sm text-cafe/55">
                          Este dibujo no tiene respuestas registradas.
                        </p>
                      </div>
                    </div>
                  </div>
                </article>
              </div>
            </section>

          </div>
        </div>

        <aside class="space-y-4 lg:sticky lg:top-16 lg:h-fit lg:self-start">
          <ReputacionCliente
            v-if="esArtistaDeLaSolicitud && datosPersona?.idUsuario"
            :id-cliente="datosPersona.idUsuario"
            :nombre="nombreContraparte"
            :reputacion="solicitud.reputacionCliente"
          />

          <section class="rounded-2xl border border-rosa/50 bg-blanco p-5">
            <h2 class="text-base font-semibold text-granate">Resumen</h2>
            <dl class="mt-4 space-y-3 text-sm">
              <div class="flex items-start justify-between gap-4">
                <dt class="text-cafe/55">Servicio</dt>
                <dd class="text-right font-medium text-cafe">{{ solicitud.nombreServicio || '—' }}</dd>
              </div>
              <div class="flex items-start justify-between gap-4">
                <dt class="text-cafe/55">Cantidad</dt>
                <dd class="text-right font-medium text-cafe">{{ solicitud.cantidadDibujos }} dibujo(s)</dd>
              </div>
              <div class="flex items-start justify-between gap-4">
                <dt class="text-cafe/55">Precio base por dibujo</dt>
                <dd class="text-right font-medium text-cafe">
                  {{ (solicitud.precioBaseMostrado ?? solicitud.precioBase) != null
                    ? formatearPrecio(
                      solicitud.precioBaseMostrado ?? solicitud.precioBase,
                      solicitud.monedaMostrada ?? solicitud.moneda,
                    )
                    : 'A definir' }}
                </dd>
              </div>
              <div v-if="solicitud.terminosVersion" class="flex items-start justify-between gap-4">
                <dt class="text-cafe/55">Términos aceptados</dt>
                <dd class="text-right font-medium text-cafe">Versión {{ solicitud.terminosVersion }}</dd>
              </div>
              <div v-if="solicitud.posicionListaEspera" class="flex items-start justify-between gap-4">
                <dt class="text-cafe/55">Lista de espera</dt>
                <dd class="text-right font-medium text-cafe">Posición {{ solicitud.posicionListaEspera }}</dd>
              </div>
              <div
                v-if="solicitud.posicionCola && ['PENDIENTE', 'EN_LISTA_ESPERA'].includes(solicitud.estado)"
                class="flex items-start justify-between gap-4"
              >
                <dt class="text-cafe/55">Posición en la cola</dt>
                <dd class="text-right font-medium text-cafe">{{ solicitud.posicionCola }}</dd>
              </div>
            </dl>
          </section>

          <section class="rounded-2xl border border-rosa/50 bg-blanco p-5">
            <h2 class="text-base font-semibold text-granate">Acciones</h2>

            <div
              v-if="contraparte && ayudaContacto"
              class="mt-3 rounded-xl border border-coral/25 bg-coral/5 px-3 py-2.5"
            >
              <p class="text-xs leading-relaxed text-cafe/70">{{ ayudaContacto }}</p>
              <button
                type="button"
                class="mt-1.5 text-xs font-semibold text-coral transition hover:text-rojizo"
                @click="irAlChat"
              >
                Escribirle a {{ nombreContraparte }} →
              </button>
            </div>

            <p
              v-if="avisoExito"
              role="status"
              class="mt-3 rounded-xl border border-green-600/20 bg-green-500/10 px-3 py-2 text-xs leading-relaxed text-green-700"
            >
              {{ avisoExito }}
            </p>

            <p v-if="puedeRechazar" class="mt-2 text-xs leading-relaxed text-cafe/60">
              Podés rechazar la solicitud y, si querés, explicarle el motivo al cliente.
            </p>
            <p
              v-else-if="esArtistaDeLaSolicitud && solicitud.estado === 'RECHAZADA'"
              class="mt-2 text-xs leading-relaxed text-cafe/60"
            >
              Rechazaste esta solicitud.
            </p>
            <p
              v-else-if="esArtistaDeLaSolicitud && solicitud.estado === 'VENCIDA'"
              class="mt-2 text-xs leading-relaxed text-cafe/60"
            >
              El presupuesto venció y el cupo quedó liberado.
            </p>
            <p
              v-else-if="esArtistaDeLaSolicitud && solicitud.estado === 'CANCELADA'"
              class="mt-2 text-xs leading-relaxed text-cafe/60"
            >
              El cliente canceló esta solicitud.
            </p>
            <p
              v-else-if="esArtistaDeLaSolicitud && solicitud.presupuesto"
              class="mt-2 text-xs leading-relaxed text-cafe/60"
            >
              Ya enviaste el presupuesto. Podés retirarlo si necesitás cambiarlo.
            </p>
            <p v-else-if="esArtistaDeLaSolicitud" class="mt-2 text-xs leading-relaxed text-cafe/60">
              Esta solicitud ya no se puede rechazar.
            </p>
            <p
              v-else-if="solicitud.estado === 'CANCELADA'"
              class="mt-2 text-xs leading-relaxed text-cafe/60"
            >
              Cancelaste esta solicitud. Podés volver a solicitarla cuando quieras.
            </p>
            <p
              v-else-if="solicitud.estado === 'RECHAZADA'"
              class="mt-2 text-xs leading-relaxed text-cafe/60"
            >
              Esta solicitud fue rechazada. Podés volver a solicitar el servicio.
            </p>
            <p
              v-else-if="solicitud.estado === 'VENCIDA'"
              class="mt-2 text-xs leading-relaxed text-cafe/60"
            >
              El presupuesto venció. Podés volver a solicitar el servicio.
            </p>
            <p v-else-if="solicitud.presupuesto" class="mt-2 text-xs leading-relaxed text-cafe/60">
              El artista ya envió el presupuesto. Podés pagarlo cuando esté disponible o cancelar
              la solicitud si no te sirve.
            </p>
            <p v-else class="mt-2 text-xs leading-relaxed text-cafe/60">
              Cuando el artista envíe el presupuesto, vas a poder pagarlo desde acá.
            </p>

            <div v-if="esArtistaDeLaSolicitud" class="mt-4 grid gap-2">
              <button
                v-if="!solicitud.presupuesto"
                type="button"
                class="inline-flex min-h-10 items-center justify-center rounded-full border border-rojizo/40 px-4 text-sm font-semibold text-rojizo transition hover:bg-rojizo/10 disabled:cursor-not-allowed disabled:border-rosa disabled:text-cafe/40 disabled:hover:bg-transparent"
                :disabled="!puedeRechazar"
                @click="abrirModalRechazo"
              >
                Rechazar
              </button>
              <button
                v-if="solicitud.estado === 'PRESUPUESTADA' && solicitud.presupuesto"
                type="button"
                class="inline-flex min-h-10 items-center justify-center rounded-full border border-rojizo/40 px-4 text-sm font-semibold text-rojizo transition hover:bg-rojizo/10"
                @click="abrirModalRetiro"
              >
                Retirar presupuesto
              </button>
              <button
                v-if="puedePresupuestar"
                type="button"
                class="inline-flex min-h-10 items-center justify-center rounded-full bg-coral px-4 text-sm font-semibold text-white transition hover:bg-rojizo"
                @click="irAPresupuestar"
              >
                Presupuestar
              </button>
              <button
                v-else-if="solicitud.estado === 'PENDIENTE' || solicitud.estado === 'EN_LISTA_ESPERA'"
                type="button"
                disabled
                class="inline-flex min-h-10 cursor-not-allowed items-center justify-center rounded-full bg-coral/40 px-4 text-sm font-semibold text-white/70"
              >
                Presupuestar
              </button>
              <p v-if="bloqueoPresupuesto" class="text-xs leading-relaxed text-cafe/60">
                {{ bloqueoPresupuesto }}
              </p>
              <router-link
                v-if="bloqueoPresupuesto && puedeEditarServicio"
                :to="{ name: 'servicio-editar', params: { id: solicitud.idServicio } }"
                class="inline-flex text-xs font-semibold text-coral transition hover:text-rojizo"
              >
                Editar servicio para ampliar cupos →
              </router-link>
            </div>

            <div
              v-if="!esArtistaDeLaSolicitud && solicitud.estado === 'PRESUPUESTADA' && solicitud.presupuesto"
              class="mt-4 grid gap-2"
            >
              <button
                type="button"
                class="inline-flex min-h-10 items-center justify-center rounded-full bg-coral px-4 text-sm font-semibold text-white transition hover:bg-rojizo"
                @click="irAPagar"
              >
                Aceptar y pagar
              </button>
            </div>

            <div v-if="solicitud.idPedido" class="mt-4 grid gap-2">
              <router-link
                :to="{ name: nombreRutaPedido, params: { idPedido: solicitud.idPedido } }"
                class="inline-flex min-h-10 items-center justify-center rounded-full bg-coral px-4 text-sm font-semibold text-white transition hover:bg-rojizo"
              >
                Ver pedido
              </router-link>
            </div>

            <div v-if="puedeCancelar" class="mt-4 grid gap-2">
              <button
                type="button"
                class="inline-flex min-h-10 items-center justify-center rounded-full border border-rojizo/40 px-4 text-sm font-semibold text-rojizo transition hover:bg-rojizo/10"
                @click="abrirModalCancelacion"
              >
                Cancelar solicitud
              </button>
            </div>
          </section>
        </aside>
      </div>
    </template>

    <VisorImagenes
      :abierto="visor.abierto"
      :imagenes="visor.imagenes"
      :indice="visor.indice"
      :etiqueta="visor.etiqueta"
      :descripcion="visor.descripcion"
      @cerrar="cerrarVisor"
    />

    <ModalRechazo
      :abierto="modalRechazoAbierto"
      :cargando="rechazando"
      :error="errorRechazo"
      @cerrar="cerrarModalRechazo"
      @confirmar="confirmarRechazo"
    />

    <ModalCancelacion
      :abierto="modalCancelacionAbierto"
      :cargando="cancelando"
      :error="errorCancelacion"
      :mensaje="mensajeCancelacion"
      @cerrar="cerrarModalCancelacion"
      @confirmar="confirmarCancelacion"
    />

    <ModalConfirmacion
      :abierto="modalRetiroAbierto"
      titulo="¿Retirar el presupuesto?"
      mensaje="El cliente va a poder volver a solicitar el servicio. Si el presupuesto estaba reservando un cupo, se libera."
      :texto-confirmar="retirando ? 'Retirando...' : 'Retirar presupuesto'"
      texto-cancelar="Volver"
      :destructivo="true"
      @confirmar="confirmarRetiro"
      @cerrar="cerrarModalRetiro"
    >
      <p
        v-if="errorRetiro"
        class="mt-3 rounded-xl border border-rojizo/25 bg-rojizo/5 px-3 py-2 text-xs leading-relaxed text-rojizo"
        role="alert"
      >
        {{ errorRetiro }}
      </p>
    </ModalConfirmacion>
  </div>
</template>
