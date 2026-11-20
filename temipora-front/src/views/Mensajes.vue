<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  estadoConversacionDe,
  formatearFechaMensaje,
  inicialDe,
  listarConversaciones,
  previewMensaje,
} from '../servicios/mensajes'
import { useMensajes } from '../composables/useMensajes'
import PageHeader from '../components/PageHeader.vue'

const route = useRoute()
const router = useRouter()
const {
  noLeidos,
  solicitudesPendientes,
  cargarContador,
} = useMensajes()

const TAMANIO = 15

const VISTAS = [
  {
    clave: 'conversaciones',
    texto: 'Conversaciones',
    estado: 'ACEPTADA',
    bandeja: 'TODAS',
    archivadas: false,
  },
  {
    clave: 'recibidas',
    texto: 'Solicitudes recibidas',
    estado: 'PENDIENTE',
    bandeja: 'RECIBIDAS',
    archivadas: false,
  },
  {
    clave: 'enviadas',
    texto: 'Enviadas',
    estado: 'PENDIENTE',
    bandeja: 'ENVIADAS',
    archivadas: false,
  },
  {
    clave: 'vencidas',
    texto: 'Vencidas',
    estado: 'VENCIDA',
    bandeja: 'TODAS',
    archivadas: false,
  },
  {
    clave: 'archivadas',
    texto: 'Archivadas',
    estado: null,
    bandeja: 'TODAS',
    archivadas: true,
  },
]

function vistaInicial() {
  const pedida = String(route.query.vista || '')
  return VISTAS.some((vista) => vista.clave === pedida) ? pedida : 'conversaciones'
}

const vistaActiva = ref(vistaInicial())
const conversaciones = ref([])
const pagina = ref(0)
const hayMas = ref(false)
const total = ref(0)
const cargando = ref(true)
const cargandoMas = ref(false)
const error = ref('')

const enPanel = computed(() => route.path.startsWith('/panel'))
const vistaInfo = computed(
  () => VISTAS.find((vista) => vista.clave === vistaActiva.value) || VISTAS[0],
)

function badgeDe(clave) {
  if (clave === 'recibidas') return solicitudesPendientes.value
  if (clave === 'conversaciones') return noLeidos.value
  return 0
}

function nombreDe(conversacion) {
  return (
    conversacion.contraparte?.nombreDisplay ||
    conversacion.contraparte?.nombreUsuario ||
    'Usuario'
  )
}

function previewDe(conversacion) {
  if (conversacion.estado === 'PENDIENTE' || conversacion.estado === 'VENCIDA') {
    return `Motivo: ${conversacion.motivoContacto || 'Sin motivo'}`
  }
  return conversacion.ultimoMensajePreview || 'Todavía no hay mensajes.'
}

function fechaDe(conversacion) {
  return formatearFechaMensaje(
    conversacion.fechaUltimoMensaje || conversacion.fechaSolicitud,
  )
}

async function cargar(reset = true) {
  if (reset) {
    pagina.value = 0
    conversaciones.value = []
    cargando.value = true
  } else {
    cargandoMas.value = true
  }
  error.value = ''

  const vista = vistaInfo.value
  const params = {
    pagina: pagina.value,
    tamanio: TAMANIO,
    archivadas: vista.archivadas,
    bandeja: vista.bandeja,
  }
  if (vista.estado) params.estado = vista.estado

  try {
    const res = await listarConversaciones(params)
    const contenido = res.data?.contenido || []
    conversaciones.value = reset
      ? contenido
      : [...conversaciones.value, ...contenido]
    hayMas.value = Boolean(res.data?.hayMas)
    total.value = Number(res.data?.totalElementos || 0)
  } catch (e) {
    if (e.response?.status === 401) {
      router.push({ name: 'login' })
      return
    }
    error.value =
      e.response?.data?.message || 'No se pudieron cargar tus mensajes.'
  } finally {
    cargando.value = false
    cargandoMas.value = false
  }
}

function cargarMas() {
  pagina.value += 1
  cargar(false)
}

function cambiarVista(clave) {
  if (vistaActiva.value === clave) return
  vistaActiva.value = clave
  router.replace({ query: { ...route.query, vista: clave } })
  cargar(true)
}

function abrirConversacion(conversacion) {
  const usuario = conversacion.contraparte?.nombreUsuario
  if (!usuario) return
  router.push({
    name: enPanel.value ? 'panel-conversacion' : 'conversacion',
    params: { nombreUsuario: `@${usuario}` },
  })
}

function textoVacio() {
  switch (vistaActiva.value) {
    case 'recibidas':
      return 'Cuando alguien quiera contactarte, la solicitud va a aparecer acá.'
    case 'enviadas':
      return 'Todavía no enviaste solicitudes de mensaje.'
    case 'vencidas':
      return 'No tenés solicitudes de mensaje vencidas.'
    case 'archivadas':
      return 'No tenés conversaciones archivadas.'
    default:
      return 'Todavía no tenés conversaciones activas.'
  }
}

watch(
  () => route.query.vista,
  (valor) => {
    const clave = VISTAS.some((vista) => vista.clave === valor)
      ? valor
      : 'conversaciones'
    if (clave !== vistaActiva.value) {
      vistaActiva.value = clave
      cargar(true)
    }
  },
)

onMounted(() => {
  cargarContador({ silencioso: true })
  cargar(true)
})
</script>

<template>
  <div :class="enPanel ? '' : 'mx-auto max-w-5xl px-4 py-8 sm:px-8 sm:py-10'">
    <PageHeader
      :eyebrow="enPanel ? 'Tu espacio de trabajo' : 'Tu actividad'"
      titulo="Mensajes"
      descripcion="Solicitudes para contactar y conversaciones privadas entre artistas y clientes."
      :nivel="enPanel ? 2 : 1"
    >
      <template #acciones>
        <span
          v-if="!cargando && !error"
          class="inline-flex self-start rounded-full border border-coral/25 bg-coral/10 px-3 py-1.5 text-xs font-semibold text-coral sm:self-auto"
        >
          {{ total }} {{ total === 1 ? 'conversación' : 'conversaciones' }}
        </span>
      </template>
    </PageHeader>

    <div
      class="mt-5 overflow-x-auto pb-1"
      :class="enPanel ? '' : '-mx-4 px-4 sm:mx-0 sm:px-0'"
    >
      <div class="inline-flex min-w-full rounded-full border border-rosa bg-blanco p-1" role="tablist" aria-label="Bandejas de mensajes">
        <button
          v-for="vista in VISTAS"
          :key="vista.clave"
          type="button"
          role="tab"
          :aria-selected="vistaActiva === vista.clave"
          class="inline-flex shrink-0 items-center gap-2 rounded-full px-4 py-1.5 text-sm font-semibold transition"
          :class="vistaActiva === vista.clave
            ? 'bg-coral text-white shadow-sm'
            : 'text-cafe/65 hover:text-granate'"
          @click="cambiarVista(vista.clave)"
        >
          <span>{{ vista.texto }}</span>
          <span
            v-if="badgeDe(vista.clave)"
            class="inline-flex min-w-5 items-center justify-center rounded-full px-1.5 py-0.5 text-[10px] font-bold"
            :class="vistaActiva === vista.clave ? 'bg-white/25 text-white' : 'bg-coral/15 text-coral'"
          >
            {{ badgeDe(vista.clave) > 9 ? '9+' : badgeDe(vista.clave) }}
          </span>
        </button>
      </div>
    </div>

    <div v-if="cargando" class="mt-6 min-h-[calc(100vh-4rem)] space-y-3" role="status" aria-live="polite">
      <span class="sr-only">Cargando mensajes...</span>
      <div
        v-for="n in 3"
        :key="n"
        class="h-28 animate-pulse rounded-2xl border border-rosa/40 bg-blanco"
      ></div>
    </div>

    <div
      v-else-if="error"
      class="mt-8 rounded-2xl border border-rojizo/25 bg-rojizo/5 px-4 py-8 text-center"
    >
      <p class="text-sm text-rojizo">{{ error }}</p>
      <button
        type="button"
        class="mt-4 min-h-10 rounded-full border border-rosa bg-blanco px-4 text-sm font-semibold text-granate transition hover:bg-rosa/20"
        @click="cargar(true)"
      >
        Reintentar
      </button>
    </div>

    <div
      v-else-if="!conversaciones.length"
      class="mt-8 rounded-3xl border border-dashed border-rosa/70 bg-blanco px-6 py-16 text-center"
    >
      <span class="mx-auto flex h-12 w-12 items-center justify-center rounded-full bg-rosa/25 text-coral" aria-hidden="true">
        <svg class="h-6 w-6" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
          <path d="M8.625 12a.375.375 0 1 1-.75 0 .375.375 0 0 1 .75 0Zm0 0H8.25m4.125 0a.375.375 0 1 1-.75 0 .375.375 0 0 1 .75 0Zm0 0H12m4.125 0a.375.375 0 1 1-.75 0 .375.375 0 0 1 .75 0Zm0 0h-.375M21 12c0 4.556-4.03 8.25-9 8.25a9.764 9.764 0 0 1-2.555-.337A5.972 5.972 0 0 1 5.41 20.97a5.969 5.969 0 0 1-.474-.065 4.48 4.48 0 0 0 .978-2.025c.09-.457-.133-.901-.467-1.226C3.93 16.178 3 14.189 3 12c0-4.556 4.03-8.25 9-8.25s9 3.694 9 8.25Z" />
        </svg>
      </span>
      <p class="mt-4 text-base font-semibold text-granate">
        {{ vistaActiva === 'recibidas' ? 'No tenés solicitudes pendientes' : 'Nada por acá' }}
      </p>
      <p class="mx-auto mt-1 max-w-md text-sm leading-relaxed text-cafe/60">
        {{ textoVacio() }}
      </p>
      <p class="mt-4 text-xs text-cafe/45">
        Para iniciar una conversación, entrá al perfil de otro usuario y usá “Enviar mensaje”.
      </p>
    </div>

    <div v-else class="mt-6 space-y-3">
      <button
        v-for="conversacion in conversaciones"
        :key="conversacion.idConversacion"
        type="button"
        class="group flex w-full items-start gap-3 rounded-2xl border bg-blanco p-4 text-left transition hover:border-coral/45 hover:bg-rosa/5 sm:gap-4 sm:p-5"
        :class="conversacion.noLeidos > 0 ? 'border-coral/35' : 'border-rosa/50'"
        @click="abrirConversacion(conversacion)"
      >
        <img
          v-if="conversacion.contraparte?.urlFotoPerfil || conversacion.contraparte?.urlFotoPerfilMiniatura"
          :src="conversacion.contraparte?.urlFotoPerfilMiniatura || conversacion.contraparte?.urlFotoPerfil"
          alt=""
          class="h-11 w-11 shrink-0 rounded-full border-2 border-white object-cover shadow-sm ring-1 ring-rosa/70 sm:h-12 sm:w-12"
        />
        <span
          v-else
          class="flex h-11 w-11 shrink-0 items-center justify-center rounded-full bg-rosa text-sm font-bold text-granate ring-1 ring-white/70 sm:h-12 sm:w-12"
          aria-hidden="true"
        >
          {{ inicialDe(conversacion.contraparte?.nombreUsuario) }}
        </span>

        <span class="min-w-0 flex-1">
          <span class="flex flex-wrap items-start justify-between gap-x-3 gap-y-1">
            <span class="min-w-0">
              <span class="block truncate text-sm font-semibold text-granate sm:text-base">
                {{ nombreDe(conversacion) }}
              </span>
              <span class="block truncate text-xs text-cafe/55">
                @{{ conversacion.contraparte?.nombreUsuario }}
              </span>
            </span>
            <span class="shrink-0 text-[11px] text-cafe/45">
              {{ fechaDe(conversacion) }}
            </span>
          </span>

          <span class="mt-2 line-clamp-2 block text-sm leading-relaxed text-cafe/70">
            {{ previewMensaje(previewDe(conversacion), 140) }}
          </span>

          <span class="mt-2.5 flex flex-wrap items-center gap-2">
            <span
              class="rounded-full border px-2.5 py-0.5 text-[11px] font-semibold"
              :class="estadoConversacionDe(conversacion.estado).clase"
            >
              {{ estadoConversacionDe(conversacion.estado).texto }}
            </span>
            <span
              v-if="conversacion.noLeidos > 0"
              class="rounded-full bg-coral px-2.5 py-0.5 text-[11px] font-bold text-white"
            >
              {{ conversacion.noLeidos }} sin leer
            </span>
            <span
              v-if="conversacion.archivada"
              class="rounded-full border border-borde/60 bg-superficie-alt px-2.5 py-0.5 text-[11px] font-semibold text-cafe/55"
            >
              Archivada
            </span>
            <span
              v-if="conversacion.estado === 'PENDIENTE' && conversacion.soyIniciador"
              class="text-[11px] font-medium text-cafe/45"
            >
              Esperando respuesta
            </span>
          </span>
        </span>
      </button>

      <div v-if="hayMas" class="pt-2 text-center">
        <button
          type="button"
          class="min-h-10 rounded-full border border-rosa bg-blanco px-5 text-sm font-semibold text-granate transition hover:bg-rosa/20 disabled:cursor-wait disabled:opacity-60"
          :disabled="cargandoMas"
          @click="cargarMas"
        >
          {{ cargandoMas ? 'Cargando...' : 'Cargar más' }}
        </button>
      </div>
    </div>
  </div>
</template>
