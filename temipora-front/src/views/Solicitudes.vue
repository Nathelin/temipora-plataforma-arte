<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import api from '../servicios/api'
import { formatearPrecio } from '../servicios/moneda'
import {
  GRUPOS_ESTADO,
  GRUPOS_RECIBIDAS,
  contraparteDe,
  datosContraparte,
  estadoDe,
  formatearFechaSolicitud,
  solicitudesEnGrupo,
} from '../servicios/solicitudes'
import { estadoServicioDe } from '../servicios/servicios'
import ReputacionChip from '../components/ReputacionChip.vue'
import PageHeader from '../components/PageHeader.vue'

const route = useRoute()
const router = useRouter()

const bandejaActiva = computed(() => (route.path.startsWith('/panel')
  ? 'recibidas'
  : 'enviadas'))
const solicitudes = ref([])
const cargando = ref(true)
const error = ref('')
const grupoActivo = ref('todas')

const esBandejaRecibidas = computed(() => bandejaActiva.value === 'recibidas')

const grupos = computed(() =>
  (esBandejaRecibidas.value ? GRUPOS_RECIBIDAS : GRUPOS_ESTADO).map((grupo) => ({
    ...grupo,
    cantidad: solicitudesEnGrupo(solicitudes.value, grupo).length,
  })),
)

watch(
  () => route.query.grupo,
  (clave) => {
    grupoActivo.value = grupos.value.some((grupo) => grupo.clave === clave)
      ? clave
      : 'todas'
  },
  { immediate: true },
)

const solicitudesFiltradas = computed(() => {
  const grupo = grupos.value.find((item) => item.clave === grupoActivo.value)
  return solicitudesEnGrupo(solicitudes.value, grupo)
})

const total = computed(() => solicitudes.value.length)
const porResponder = computed(
  () => solicitudes.value.filter((s) => s.estado === 'PENDIENTE' || s.estado === 'EN_LISTA_ESPERA').length,
)

function contraparte(solicitud) {
  return datosContraparte(contraparteDe(solicitud, esBandejaRecibidas.value))
}

async function cargar() {
  cargando.value = true
  error.value = ''
  try {
    const endpoint = esBandejaRecibidas.value
      ? '/solicitudes-servicio/recibidas'
      : '/solicitudes-servicio/enviadas'
    const res = await api.get(endpoint)
    solicitudes.value = res.data || []
  } catch (e) {
    if (e.response?.status === 401) {
      router.push({ name: 'login' })
      return
    }
    error.value = e.response?.data?.message || 'No se pudieron cargar tus solicitudes.'
  } finally {
    cargando.value = false
  }
}

onMounted(cargar)
</script>

<template>
  <div>
    <PageHeader
      eyebrow="Tu actividad"
      titulo="Solicitudes"
      :descripcion="esBandejaRecibidas
        ? 'Solicitudes que recibiste de tus clientes.'
        : 'Solicitudes que enviaste a otros artistas.'"
      :nivel="2"
    >
      <template #acciones>
        <span
          v-if="!cargando && !error && porResponder"
          class="inline-flex self-start rounded-full border border-coral/25 bg-coral/10 px-3 py-1.5 text-xs font-semibold text-coral sm:self-auto"
        >
          {{ porResponder }} {{ esBandejaRecibidas ? 'por responder' : 'esperando respuesta' }}
        </span>
      </template>
    </PageHeader>

    <div v-if="cargando" class="mt-8 space-y-3" role="status" aria-live="polite">
      <span class="sr-only">Cargando solicitudes...</span>
      <div
        v-for="n in 3"
        :key="n"
        class="h-24 animate-pulse rounded-2xl border border-rosa/40 bg-blanco/70"
        aria-hidden="true"
      ></div>
    </div>

    <div v-else-if="error" class="mt-8 rounded-2xl border border-rosa/50 bg-blanco px-6 py-12 text-center">
      <p class="text-sm text-cafe/70">{{ error }}</p>
      <button
        type="button"
        class="mt-4 rounded-full border border-rosa bg-blanco px-4 py-2 text-sm font-semibold text-granate transition hover:bg-rosa/20"
        @click="cargar"
      >
        Reintentar
      </button>
    </div>

    <div v-else>
      <div
        v-if="!total"
        class="mt-8 rounded-3xl border border-dashed border-rosa/70 bg-blanco px-6 py-16 text-center"
      >
        <span class="mx-auto flex h-12 w-12 items-center justify-center rounded-full bg-rosa/25 text-coral" aria-hidden="true">
          <svg class="h-6 w-6" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
            <path d="M9 12h6m-6 4h6m2 5H7a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h5.586a1 1 0 0 1 .707.293l5.414 5.414a1 1 0 0 1 .293.707V19a2 2 0 0 1-2 2Z" />
          </svg>
        </span>
        <p class="mt-4 text-base font-semibold text-granate">
          {{ esBandejaRecibidas ? 'Todavía no recibiste solicitudes.' : 'Todavía no enviaste solicitudes.' }}
        </p>
        <p class="mx-auto mt-1 max-w-sm text-sm leading-relaxed text-cafe/60">
          {{ esBandejaRecibidas
            ? 'Cuando alguien solicite uno de tus servicios, aparecerá en esta página.'
            : 'Explorá los servicios de otros artistas y enviá tu primera solicitud.' }}
        </p>
        <router-link
          v-if="!esBandejaRecibidas"
          to="/servicios"
          class="mt-5 inline-flex min-h-10 items-center justify-center rounded-full bg-coral px-5 text-sm font-semibold text-white shadow-[0_8px_20px_var(--sombra-acento)] transition hover:bg-rojizo"
        >
          Explorar servicios
        </router-link>
      </div>

      <template v-else>
        <div
          class="mt-5 flex gap-1 overflow-x-auto rounded-xl border border-rosa/60 bg-blanco p-1"
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

        <p class="mt-3 text-xs text-cafe/50">
          {{ solicitudesFiltradas.length }} de {{ total }}
          {{ total === 1 ? 'solicitud' : 'solicitudes' }}
        </p>

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
          <li v-for="solicitud in solicitudesFiltradas" :key="solicitud.idSolicitud">
          <router-link
            :to="{ name: esBandejaRecibidas ? 'panel-solicitud-detalle' : 'solicitud-detalle', params: { idSolicitud: solicitud.idSolicitud } }"
            class="group flex flex-col gap-4 rounded-2xl border border-rosa/50 bg-blanco p-4 transition hover:border-coral/40 hover:bg-rosa/10 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-coral/50 sm:flex-row sm:items-center sm:justify-between sm:p-5"
          >
            <div class="flex min-w-0 items-start gap-3">
              <img
                v-if="contraparte(solicitud)?.foto"
                :src="contraparte(solicitud).foto"
                alt=""
                class="h-11 w-11 shrink-0 rounded-full border-2 border-white object-cover shadow-sm ring-1 ring-rosa/70"
              />
              <span
                v-else
                class="flex h-11 w-11 shrink-0 items-center justify-center rounded-full bg-rosa text-base font-bold text-granate shadow-sm ring-1 ring-white/70"
                aria-hidden="true"
              >
                {{ contraparte(solicitud)?.inicial || '?' }}
              </span>

              <div class="min-w-0">
                <p class="flex flex-wrap items-baseline gap-x-2 gap-y-0.5">
                  <span class="text-sm font-semibold text-granate sm:text-base">
                    {{ contraparte(solicitud)?.nombre || 'Usuario no disponible' }}
                  </span>
                  <span v-if="contraparte(solicitud)?.usuario" class="text-xs text-cafe/55">
                    @{{ contraparte(solicitud).usuario }}
                  </span>
                  <ReputacionChip
                    v-if="esBandejaRecibidas && solicitud.reputacionCliente"
                    :reputacion="solicitud.reputacionCliente"
                  />
                </p>
                <p class="mt-1 flex flex-wrap items-center gap-2 text-sm text-cafe/75">
                  <span class="truncate">{{ solicitud.nombreServicio || 'Servicio' }}</span>
                  <span
                    v-if="solicitud.estadoServicio && solicitud.estadoServicio !== 'PUBLICADO'"
                    class="shrink-0 rounded-full border px-2 py-0.5 text-[11px] font-semibold"
                    :class="estadoServicioDe(solicitud.estadoServicio).clase"
                  >
                    Servicio {{ estadoServicioDe(solicitud.estadoServicio).texto.toLowerCase() }}
                  </span>
                </p>
                <p class="mt-1 flex flex-wrap items-center gap-x-2 gap-y-1 text-xs text-cafe/50">
                  <span>Solicitud #{{ solicitud.idSolicitud }}</span>
                  <span aria-hidden="true">·</span>
                  <span>{{ solicitud.cantidadDibujos }} {{ solicitud.cantidadDibujos === 1 ? 'dibujo' : 'dibujos' }}</span>
                  <span aria-hidden="true">·</span>
                  <span>{{ formatearFechaSolicitud(solicitud.fechaCreacion, { corta: true }) }}</span>
                </p>
              </div>
            </div>

            <div class="flex shrink-0 items-center justify-between gap-3 sm:justify-end">
              <div class="flex flex-col items-start gap-1 sm:items-end">
                <span
                  class="rounded-full border px-3 py-1 text-xs font-semibold"
                  :class="estadoDe(solicitud.estado).clase"
                >
                  {{ estadoDe(solicitud.estado).texto }}
                </span>
                <span
                  v-if="(solicitud.precioFinalClienteMostrado ?? solicitud.precioFinalMostrado ?? solicitud.precioFinal) != null"
                  class="text-xs font-semibold text-coral"
                >
                  Presupuesto: {{ formatearPrecio(
                    solicitud.precioFinalClienteMostrado
                      ?? solicitud.precioFinalMostrado
                      ?? solicitud.precioFinal,
                    solicitud.monedaMostrada ?? solicitud.moneda,
                  ) }}
                </span>
                <span
                  v-else-if="(solicitud.precioBaseMostrado ?? solicitud.precioBase) != null"
                  class="text-xs text-cafe/50"
                >
                  {{ formatearPrecio(
                    solicitud.precioBaseMostrado ?? solicitud.precioBase,
                    solicitud.monedaMostrada ?? solicitud.moneda,
                  ) }} por dibujo
                </span>
                <span
                  v-if="solicitud.fechaVencimiento && solicitud.estado === 'PRESUPUESTADA'"
                  class="text-xs text-cafe/50"
                >
                  Oferta vence {{ formatearFechaSolicitud(solicitud.fechaVencimiento, { corta: true }) }}
                </span>
              </div>

              <svg
                class="h-5 w-5 text-cafe/35 transition group-hover:translate-x-0.5 group-hover:text-coral"
                viewBox="0 0 24 24"
                fill="none"
                stroke="currentColor"
                stroke-width="2"
                aria-hidden="true"
              >
                <path stroke-linecap="round" stroke-linejoin="round" d="m9 5 7 7-7 7" />
              </svg>
            </div>
          </router-link>
          </li>
        </ul>
      </template>
    </div>
  </div>
</template>
