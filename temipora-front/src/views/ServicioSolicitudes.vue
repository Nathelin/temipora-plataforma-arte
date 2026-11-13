<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import api from '../servicios/api'
import { formatearPrecio } from '../servicios/moneda'
import {
  GRUPOS_ESTADO,
  datosContraparte,
  estadoDe,
  formatearFechaSolicitud,
  solicitudesEnGrupo,
} from '../servicios/solicitudes'
import { estadoServicioDe } from '../servicios/servicios'
import ModalRechazo from '../components/ModalRechazo.vue'
import ModalConfirmacion from '../components/ModalConfirmacion.vue'
import { useNotificaciones } from '../composables/useNotificaciones'

const route = useRoute()
const router = useRouter()
const { exito, error: notificarError } = useNotificaciones()

const idServicio = computed(() => Number(route.params.id))

const servicio = ref(null)
const solicitudes = ref([])
const cargando = ref(true)
const errorCarga = ref('')
const grupoActivo = ref('todas')

const modalRechazoAbierto = ref(false)
const solicitudARechazar = ref(null)
const rechazando = ref(false)
const errorRechazo = ref('')

const solicitudARetirar = ref(null)
const retirando = ref(false)

const nombreServicio = computed(
  () => servicio.value?.nombre || solicitudes.value[0]?.nombreServicio || `Servicio #${idServicio.value}`,
)
const estadoInfo = computed(() => estadoServicioDe(servicio.value?.estado))

const grupos = computed(() =>
  GRUPOS_ESTADO.map((grupo) => ({
    ...grupo,
    cantidad: solicitudesEnGrupo(solicitudes.value, grupo).length,
  })),
)

const solicitudesFiltradas = computed(() => {
  const grupo = GRUPOS_ESTADO.find((item) => item.clave === grupoActivo.value)
  return solicitudesEnGrupo(solicitudes.value, grupo)
})

const conteo = computed(() => ({
  pendientes: solicitudes.value.filter((s) => s.estado === 'PENDIENTE').length,
  enEspera: solicitudes.value.filter((s) => s.estado === 'EN_LISTA_ESPERA').length,
  presupuestadas: solicitudes.value.filter((s) => s.estado === 'PRESUPUESTADA').length,
  pagadas: solicitudes.value.filter((s) => s.estado === 'PAGADA').length,
}))

const puedeRechazar = (solicitud) =>
  ['PENDIENTE', 'EN_LISTA_ESPERA'].includes(solicitud.estado)

async function cargar() {
  cargando.value = true
  errorCarga.value = ''
  try {
    const [resSolicitudes, resServicio] = await Promise.all([
      api.get(`/servicios/${idServicio.value}/solicitudes`),
      api.get(`/servicios/${idServicio.value}`).catch(() => null),
    ])
    solicitudes.value = resSolicitudes.data || []
    servicio.value = resServicio?.data || null
  } catch (e) {
    if (e.response?.status === 401) {
      router.push({ name: 'login' })
      return
    }
    errorCarga.value = e.response?.data?.message || 'No se pudieron cargar las solicitudes del servicio.'
  } finally {
    cargando.value = false
  }
}

function abrirRechazo(solicitud) {
  solicitudARechazar.value = solicitud
  errorRechazo.value = ''
  modalRechazoAbierto.value = true
}

async function confirmarRechazo(motivo) {
  if (!solicitudARechazar.value) return
  rechazando.value = true
  errorRechazo.value = ''
  try {
    await api.post(`/solicitudes-servicio/${solicitudARechazar.value.idSolicitud}/rechazar`, { motivo })
    modalRechazoAbierto.value = false
    solicitudARechazar.value = null
    exito('Solicitud rechazada. El cliente ya recibió la notificación.')
    await cargar()
  } catch (e) {
    errorRechazo.value = e.response?.data?.message || 'No se pudo rechazar la solicitud.'
  } finally {
    rechazando.value = false
  }
}

async function confirmarRetiro() {
  if (!solicitudARetirar.value || retirando.value) return
  retirando.value = true
  try {
    await api.post(`/solicitudes-servicio/${solicitudARetirar.value.idSolicitud}/retirar-presupuesto`)
    solicitudARetirar.value = null
    exito('Presupuesto retirado. Ya podés dar de baja definitiva el servicio.')
    await cargar()
  } catch (e) {
    notificarError(e.response?.data?.message || 'No se pudo retirar el presupuesto.')
  } finally {
    retirando.value = false
  }
}

onMounted(cargar)
</script>

<template>
  <div>
    <router-link
      to="/panel/servicios"
      class="inline-flex items-center gap-1.5 text-sm font-medium text-coral transition hover:text-rojizo"
    >
      <span aria-hidden="true">←</span> Mis servicios
    </router-link>

    <header class="mt-4 flex flex-col gap-4 sm:flex-row sm:items-end sm:justify-between">
      <div>
        <p class="text-sm font-semibold uppercase tracking-[0.18em] text-coral">Solicitudes del servicio</p>
        <h1 class="mt-1 font-display text-3xl font-extrabold tracking-[-0.02em] text-granate sm:text-4xl">
          {{ nombreServicio }}
        </h1>
        <p class="mt-2 text-sm text-cafe/65">
          Revisá las solicitudes y resolvé los presupuestos antes de dar de baja definitiva el servicio.
        </p>
      </div>

      <span
        v-if="servicio"
        class="inline-flex self-start rounded-full border px-3 py-1.5 text-xs font-semibold sm:self-auto"
        :class="estadoInfo.clase"
      >
        {{ estadoInfo.texto }}
      </span>
    </header>

    <div v-if="cargando" class="mt-8 space-y-3" role="status" aria-live="polite">
      <span class="sr-only">Cargando solicitudes...</span>
      <div
        v-for="n in 3"
        :key="n"
        class="h-24 animate-pulse rounded-2xl border border-rosa/40 bg-blanco/70"
        aria-hidden="true"
      ></div>
    </div>

    <div v-else-if="errorCarga" class="mt-8 rounded-2xl border border-rosa/50 bg-blanco px-6 py-12 text-center">
      <p class="text-sm text-cafe/70">{{ errorCarga }}</p>
      <button
        type="button"
        class="mt-4 rounded-full border border-rosa bg-blanco px-4 py-2 text-sm font-semibold text-granate transition hover:bg-rosa/20"
        @click="cargar"
      >
        Reintentar
      </button>
    </div>

    <template v-else>
      <div class="mt-6 grid gap-3 sm:grid-cols-2 lg:grid-cols-4">
        <div class="rounded-2xl border border-rosa/50 bg-blanco p-4">
          <p class="text-xs font-semibold uppercase tracking-[0.12em] text-cafe/45">Pendientes</p>
          <p class="mt-1 text-2xl font-semibold text-granate">{{ conteo.pendientes }}</p>
        </div>
        <div class="rounded-2xl border border-rosa/50 bg-blanco p-4">
          <p class="text-xs font-semibold uppercase tracking-[0.12em] text-cafe/45">En espera</p>
          <p class="mt-1 text-2xl font-semibold text-granate">{{ conteo.enEspera }}</p>
        </div>
        <div class="rounded-2xl border border-rosa/50 bg-blanco p-4">
          <p class="text-xs font-semibold uppercase tracking-[0.12em] text-cafe/45">Presupuestadas</p>
          <p class="mt-1 text-2xl font-semibold text-granate">{{ conteo.presupuestadas }}</p>
        </div>
        <div class="rounded-2xl border border-rosa/50 bg-blanco p-4">
          <p class="text-xs font-semibold uppercase tracking-[0.12em] text-cafe/45">Pagadas</p>
          <p class="mt-1 text-2xl font-semibold text-granate">{{ conteo.pagadas }}</p>
        </div>
      </div>

      <div v-if="conteo.presupuestadas || conteo.pagadas" class="mt-4 space-y-2">
        <p
          v-if="conteo.presupuestadas"
          class="rounded-xl border border-durazno bg-durazno/35 px-4 py-3 text-sm leading-relaxed text-granate"
        >
          Tenés {{ conteo.presupuestadas }} presupuesto(s) vigente(s): retiralo(s) para poder dar de baja
          definitiva el servicio.
        </p>
        <p
          v-if="conteo.pagadas"
          class="rounded-xl border border-durazno bg-durazno/35 px-4 py-3 text-sm leading-relaxed text-granate"
        >
          Tenés {{ conteo.pagadas }} pedido(s) en curso: la baja definitiva se habilita cuando se entreguen.
        </p>
      </div>

      <div
        v-if="!solicitudes.length"
        class="mt-6 rounded-3xl border border-dashed border-rosa/70 bg-blanco px-6 py-16 text-center"
      >
        <p class="text-base font-semibold text-granate">Este servicio no tiene solicitudes.</p>
        <p class="mx-auto mt-1 max-w-sm text-sm leading-relaxed text-cafe/60">
          Cuando un cliente envíe una, aparecerá en esta página.
        </p>
      </div>

      <template v-else>
        <div
          class="mt-6 flex gap-1 overflow-x-auto rounded-xl border border-rosa/60 bg-blanco p-1"
          role="group"
          aria-label="Filtrar solicitudes por estado"
        >
          <button
            v-for="grupo in grupos"
            :key="grupo.clave"
            type="button"
            class="flex flex-1 items-center justify-center gap-2 whitespace-nowrap rounded-lg px-2 py-1.5 text-xs font-medium transition sm:flex-none sm:px-3 sm:text-sm"
            :class="grupoActivo === grupo.clave
              ? 'bg-coral text-white shadow-sm'
              : 'text-cafe/70 hover:bg-rosa/20 hover:text-granate'"
            :aria-pressed="grupoActivo === grupo.clave"
            @click="grupoActivo = grupo.clave"
          >
            {{ grupo.texto }}
            <span
              class="rounded-full px-1.5 py-0.5 text-[11px] leading-none"
              :class="grupoActivo === grupo.clave ? 'bg-white/20 text-white' : 'bg-rosa/30 text-granate'"
            >
              {{ grupo.cantidad }}
            </span>
          </button>
        </div>

        <div v-if="!solicitudesFiltradas.length" class="mt-4 rounded-2xl border border-dashed border-rosa/60 px-4 py-12 text-center">
          <p class="text-sm font-medium text-granate">No hay solicitudes en este estado.</p>
          <button
            type="button"
            class="mt-2 text-sm font-medium text-coral transition hover:text-rojizo"
            @click="grupoActivo = 'todas'"
          >
            Ver todas las solicitudes
          </button>
        </div>

        <ul v-else class="mt-4 space-y-3">
          <li
            v-for="solicitud in solicitudesFiltradas"
            :key="solicitud.idSolicitud"
            class="rounded-2xl border border-rosa/50 bg-blanco p-4 sm:p-5"
          >
            <div class="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
              <router-link
                :to="{ name: 'panel-solicitud-detalle', params: { idSolicitud: solicitud.idSolicitud } }"
                class="group flex min-w-0 items-start gap-3"
              >
                <img
                  v-if="datosContraparte(solicitud.cliente)?.foto"
                  :src="datosContraparte(solicitud.cliente).foto"
                  alt=""
                  class="h-11 w-11 shrink-0 rounded-full border-2 border-white object-cover shadow-sm ring-1 ring-rosa/70"
                />
                <span
                  v-else
                  class="flex h-11 w-11 shrink-0 items-center justify-center rounded-full bg-rosa text-base font-bold text-granate shadow-sm ring-1 ring-white/70"
                  aria-hidden="true"
                >
                  {{ datosContraparte(solicitud.cliente)?.inicial || '?' }}
                </span>

                <div class="min-w-0">
                  <p class="flex flex-wrap items-baseline gap-x-2 gap-y-0.5">
                    <span class="text-sm font-semibold text-granate sm:text-base">
                      {{ datosContraparte(solicitud.cliente)?.nombre || 'Usuario no disponible' }}
                    </span>
                    <span v-if="datosContraparte(solicitud.cliente)?.usuario" class="text-xs text-cafe/55">
                      @{{ datosContraparte(solicitud.cliente).usuario }}
                    </span>
                  </p>
                  <p class="mt-1 flex flex-wrap items-center gap-x-2 gap-y-1 text-xs text-cafe/50">
                    <span>Solicitud #{{ solicitud.idSolicitud }}</span>
                    <span aria-hidden="true">·</span>
                    <span>{{ solicitud.cantidadDibujos }} {{ solicitud.cantidadDibujos === 1 ? 'dibujo' : 'dibujos' }}</span>
                    <span aria-hidden="true">·</span>
                    <span>{{ formatearFechaSolicitud(solicitud.fechaCreacion, { corta: true }) }}</span>
                    <template v-if="(solicitud.precioFinalMostrado ?? solicitud.precioFinal) != null">
                      <span aria-hidden="true">·</span>
                      <span class="font-semibold text-coral">
                        Presupuesto: {{ formatearPrecio(
                          solicitud.precioFinalMostrado ?? solicitud.precioFinal,
                          solicitud.monedaMostrada ?? solicitud.moneda,
                        ) }}
                      </span>
                    </template>
                    <template v-else-if="(solicitud.precioBaseMostrado ?? solicitud.precioBase) != null">
                      <span aria-hidden="true">·</span>
                      <span>{{ formatearPrecio(
                        solicitud.precioBaseMostrado ?? solicitud.precioBase,
                        solicitud.monedaMostrada ?? solicitud.moneda,
                      ) }} por dibujo</span>
                    </template>
                    <span
                      v-if="solicitud.fechaVencimiento && solicitud.estado === 'PRESUPUESTADA'"
                    >
                      <span aria-hidden="true">·</span>
                      Oferta vence {{ formatearFechaSolicitud(solicitud.fechaVencimiento, { corta: true }) }}
                    </span>
                  </p>
                </div>
              </router-link>

              <div class="flex flex-wrap items-center gap-2 sm:justify-end">
                <span
                  class="rounded-full border px-3 py-1 text-xs font-semibold"
                  :class="estadoDe(solicitud.estado).clase"
                >
                  {{ estadoDe(solicitud.estado).texto }}
                </span>
                <button
                  v-if="puedeRechazar(solicitud)"
                  type="button"
                  class="rounded-full border border-rosa bg-blanco px-3 py-1.5 text-xs font-semibold text-rojizo transition hover:bg-rojizo/5"
                  @click="abrirRechazo(solicitud)"
                >
                  Rechazar
                </button>
                <button
                  v-if="solicitud.estado === 'PRESUPUESTADA'"
                  type="button"
                  class="rounded-full border border-coral/40 bg-coral/5 px-3 py-1.5 text-xs font-semibold text-coral transition hover:bg-coral/10"
                  @click="solicitudARetirar = solicitud"
                >
                  Retirar presupuesto
                </button>
              </div>
            </div>
          </li>
        </ul>
      </template>
    </template>

    <ModalRechazo
      :abierto="modalRechazoAbierto"
      :cargando="rechazando"
      :error="errorRechazo"
      @confirmar="confirmarRechazo"
      @cerrar="modalRechazoAbierto = false"
    />

    <ModalConfirmacion
      :abierto="!!solicitudARetirar"
      titulo="¿Retirar el presupuesto?"
      mensaje="El cliente va a poder volver a solicitar el servicio. Si el presupuesto estaba reservando un cupo, se libera."
      texto-confirmar="Retirar presupuesto"
      texto-cancelar="Volver"
      :destructivo="true"
      @confirmar="confirmarRetiro"
      @cerrar="solicitudARetirar = null"
    />
  </div>
</template>
