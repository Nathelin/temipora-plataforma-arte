<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import {
  destinoDe as destinoCompartido,
  textoDestino as textoCompartido,
} from '../servicios/notificaciones'
import { useNotificacionesSistema } from '../composables/useNotificacionesSistema'

const router = useRouter()
const {
  notificaciones,
  noLeidas,
  cargando,
  error,
  cargar,
  marcarLeida,
  marcarTodasLeidas,
} = useNotificacionesSistema()

const marcandoTodas = ref(false)
const esAdmin = localStorage.getItem('rol') === 'ADMIN'
const esArtista = localStorage.getItem('rol') === 'ARTISTA'
const hayNotificaciones = computed(() => notificaciones.value.length > 0)
const hayNoLeidas = computed(() => noLeidas.value > 0)

const rolActual = localStorage.getItem('rol') || ''

function destinoDe(notificacion) {
  return destinoCompartido(notificacion, {
    rol: rolActual,
    pathActual: router.currentRoute.value.path,
  })
}

function textoDestino(notificacion) {
  return textoCompartido(notificacion, rolActual)
}

async function abrir(notificacion) {
  const destino = destinoDe(notificacion)
  await marcarLeida(notificacion)
  if (destino) router.push(destino)
}

async function marcarTodas() {
  if (marcandoTodas.value || !hayNoLeidas.value) return
  marcandoTodas.value = true
  try {
    await marcarTodasLeidas()
  } finally {
    marcandoTodas.value = false
  }
}

function formatearFecha(valor) {
  if (!valor) return ''
  const fecha = new Date(valor)
  if (Number.isNaN(fecha.getTime())) return ''
  return new Intl.DateTimeFormat('es', {
    dateStyle: 'medium',
    timeStyle: 'short',
  }).format(fecha)
}

onMounted(() => {
  cargar({ silencioso: hayNotificaciones.value })
})
</script>

<template>
  <div class="mx-auto max-w-4xl px-4 py-8 sm:px-8 sm:py-12">
    <button
      type="button"
      class="text-sm font-medium text-cafe/70 transition hover:text-coral"
      @click="router.back()"
    >
      ← Volver
    </button>

    <header class="mt-6 flex flex-col gap-5 sm:flex-row sm:items-end sm:justify-between">
      <div>
        <p class="text-sm font-semibold uppercase tracking-[0.18em] text-coral">Tu actividad</p>
        <h1 class="mt-1 font-display text-3xl font-extrabold tracking-[-0.02em] text-granate sm:text-4xl">
          Notificaciones
        </h1>
        <p class="mt-2 text-sm text-cafe/65">
          {{ hayNoLeidas ? `${noLeidas} sin leer` : 'Estás al día.' }}
        </p>
      </div>

      <div class="flex flex-wrap items-center gap-3 self-start sm:self-auto">
        <router-link
          v-if="!esAdmin"
          :to="esArtista ? '/panel/solicitudes' : '/solicitudes'"
          class="text-sm font-semibold text-coral transition hover:text-rojizo"
        >
          Mis solicitudes
        </router-link>

        <button
          v-if="hayNoLeidas"
          type="button"
          class="inline-flex min-h-10 items-center justify-center rounded-full border border-rosa bg-blanco px-5 text-sm font-semibold text-granate transition hover:border-coral/50 hover:bg-rosa/20 disabled:cursor-not-allowed disabled:opacity-50"
          :disabled="marcandoTodas"
          @click="marcarTodas"
        >
          {{ marcandoTodas ? 'Marcando...' : 'Marcar todas como leídas' }}
        </button>
      </div>
    </header>

    <div v-if="cargando && !hayNotificaciones" class="mt-8 rounded-2xl border border-rosa/50 bg-blanco px-6 py-12 text-center text-sm text-cafe/60">
      Cargando notificaciones...
    </div>

    <div v-else-if="error && !hayNotificaciones" class="mt-8 rounded-2xl border border-rosa/50 bg-blanco px-6 py-12 text-center">
      <p class="text-sm text-cafe/70">{{ error }}</p>
      <button
        type="button"
        class="mt-4 rounded-full border border-rosa bg-blanco px-4 py-2 text-sm font-semibold text-granate transition hover:bg-rosa/20"
        @click="cargar()"
      >
        Reintentar
      </button>
    </div>

    <div v-else-if="!hayNotificaciones" class="mt-8 rounded-3xl border border-dashed border-rosa/70 bg-blanco px-6 py-16 text-center">
      <span class="mx-auto flex h-12 w-12 items-center justify-center rounded-full bg-rosa/25 text-coral" aria-hidden="true">
        <svg class="h-6 w-6" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
          <path d="M14.857 17.082a23.848 23.848 0 0 0 5.454-1.31A8.967 8.967 0 0 1 18 9.75V9A6 6 0 0 0 6 9v.75a8.967 8.967 0 0 1-2.312 6.022c1.733.64 3.56 1.085 5.455 1.31m5.714 0a24.255 24.255 0 0 1-5.714 0m5.714 0a3 3 0 1 1-5.714 0" />
        </svg>
      </span>
      <p class="mt-4 text-base font-semibold text-granate">No tenés notificaciones.</p>
      <p class="mx-auto mt-1 max-w-sm text-sm leading-relaxed text-cafe/60">
        Cuando recibas una solicitud, un pedido o una novedad importante,
        aparecerá en esta página.
      </p>
    </div>

    <ul v-else class="mt-8 space-y-3">
      <li v-for="notificacion in notificaciones" :key="notificacion.idNotificacionUsuario">
        <button
          type="button"
          class="group flex w-full items-start gap-4 rounded-2xl border px-4 py-4 text-left transition sm:px-5"
          :class="notificacion.leida
            ? 'border-rosa/40 bg-blanco hover:border-coral/30 hover:bg-rosa/10'
            : 'border-coral/25 bg-rosa/10 hover:border-coral/40 hover:bg-rosa/20'"
          @click="abrir(notificacion)"
        >
          <span
            class="mt-0.5 flex h-10 w-10 shrink-0 items-center justify-center rounded-full"
            :class="notificacion.leida ? 'bg-rosa/20 text-cafe/60' : 'bg-coral/15 text-coral'"
            aria-hidden="true"
          >
            <svg class="h-5 w-5" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
              <path d="M9 12h6m-6 4h6m2 5H7a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h5.586a1 1 0 0 1 .707.293l5.414 5.414a1 1 0 0 1 .293.707V19a2 2 0 0 1-2 2Z" />
            </svg>
          </span>

          <span class="min-w-0 flex-1">
            <span class="flex flex-wrap items-start justify-between gap-x-3 gap-y-1">
              <span class="text-sm font-semibold text-granate sm:text-base">{{ notificacion.nombre }}</span>
              <span v-if="!notificacion.leida" class="inline-flex items-center gap-1.5 text-[11px] font-semibold uppercase tracking-wide text-coral">
                <span class="h-2 w-2 rounded-full bg-coral" aria-hidden="true"></span>
                Nueva
              </span>
            </span>
            <span class="mt-1 block text-sm leading-relaxed text-cafe/75">
              {{ notificacion.descripcion }}
            </span>
            <span class="mt-2 flex flex-wrap items-center gap-x-3 gap-y-1 text-xs text-cafe/50">
              <span>{{ formatearFecha(notificacion.fechaCreacion) }}</span>
              <span v-if="destinoDe(notificacion)" class="font-semibold text-coral transition group-hover:text-rojizo">
                {{ textoDestino(notificacion) }}
              </span>
            </span>
          </span>
        </button>
      </li>
    </ul>
  </div>
</template>
