<script setup>
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useNotificacionesSistema } from '../composables/useNotificacionesSistema'

const router = useRouter()
const rutaNotificaciones = localStorage.getItem('rol') === 'ADMIN'
  ? { name: 'admin-notificaciones' }
  : { name: 'notificaciones' }

const {
  notificaciones,
  noLeidas,
  cargando,
  error,
  cargar,
  marcarLeida,
  marcarTodasLeidas,
  iniciarPolling,
  detenerPolling,
} = useNotificacionesSistema()

const abierto = ref(false)
const marcandoTodas = ref(false)
const contenedor = ref(null)

const hayNotificaciones = computed(() => notificaciones.value.length > 0)
const hayNoLeidas = computed(() => noLeidas.value > 0)
const etiquetaContador = computed(() => (noLeidas.value > 9 ? '9+' : String(noLeidas.value)))

function alternar() {
  abierto.value = !abierto.value
  if (abierto.value) {
    cargar({ silencioso: hayNotificaciones.value })
  }
}

function cerrar() {
  abierto.value = false
}

function manejarClickExterior(event) {
  if (contenedor.value && !contenedor.value.contains(event.target)) {
    cerrar()
  }
}

function manejarTecla(event) {
  if (event.key === 'Escape') {
    cerrar()
  }
}

function destinoDe(notificacion) {
  const tipo = notificacion?.tipoReferencia
  if (tipo !== 'SOLICITUD' && tipo !== 'PEDIDO' && tipo !== 'RESENIA'
    && tipo !== 'CONVERSACION' && tipo !== 'OFERTA_REACTIVACION') return null

  const rol = localStorage.getItem('rol')
  if (rol !== 'ARTISTA' && rol !== 'CLIENTE' && rol !== 'ADMIN') return null

  if (tipo === 'RESENIA') {
    return rol === 'ADMIN' ? { name: 'admin-resenias-reportadas' } : null
  }

  if (tipo === 'OFERTA_REACTIVACION') {
    return rol === 'ADMIN' ? null : { name: 'ofertas' }
  }

  const id = Number(notificacion.idReferencia)
  if (!Number.isInteger(id) || id <= 0) return null

  if (tipo === 'CONVERSACION') {
    if (rol === 'ADMIN') return null
    return router.currentRoute.value.path.startsWith('/panel')
      ? { name: 'panel-conversacion', params: { idConversacion: id } }
      : { name: 'conversacion', params: { idConversacion: id } }
  }

  if (tipo === 'SOLICITUD') {
    if (rol === 'ADMIN') return null
    return { name: 'solicitud-detalle', params: { idSolicitud: id } }
  }

  if (rol === 'ADMIN') {
    return { name: 'admin-pedido-detalle', params: { idPedido: id } }
  }
  if (rol === 'ARTISTA'
    && router.currentRoute.value.path.startsWith('/panel')) {
    return { name: 'panel-pedido-detalle', params: { idPedido: id } }
  }
  return { name: 'pedido-detalle', params: { idPedido: id } }
}

function textoDestino(notificacion) {
  if (notificacion?.tipoReferencia === 'PEDIDO') return 'Ver pedido →'
  if (notificacion?.tipoReferencia === 'SOLICITUD') return 'Ver solicitud →'
  if (notificacion?.tipoReferencia === 'RESENIA') return 'Ver reportes →'
  if (notificacion?.tipoReferencia === 'CONVERSACION') return 'Ver conversación →'
  if (notificacion?.tipoReferencia === 'OFERTA_REACTIVACION') return 'Ver ofertas →'
  return ''
}

async function seleccionar(notificacion) {
  const destino = destinoDe(notificacion)
  await marcarLeida(notificacion)
  cerrar()
  if (destino) router.push(destino)
}

async function marcarTodas() {
  if (marcandoTodas.value || !hayNoLeidas.value) {
    return
  }
  marcandoTodas.value = true
  try {
    await marcarTodasLeidas()
  } finally {
    marcandoTodas.value = false
  }
}

function formatearFecha(valor) {
  if (!valor) {
    return ''
  }
  const fecha = new Date(valor)
  if (Number.isNaN(fecha.getTime())) {
    return ''
  }
  return new Intl.DateTimeFormat('es', {
    dateStyle: 'medium',
    timeStyle: 'short',
  }).format(fecha)
}

onMounted(() => {
  cargar()
  iniciarPolling()
  document.addEventListener('click', manejarClickExterior)
  document.addEventListener('keydown', manejarTecla)
})

onBeforeUnmount(() => {
  detenerPolling()
  document.removeEventListener('click', manejarClickExterior)
  document.removeEventListener('keydown', manejarTecla)
})
</script>

<template>
  <div ref="contenedor" class="relative">
    <button
      type="button"
      class="relative inline-flex h-10 w-10 items-center justify-center rounded-full border border-rosa/60 bg-blanco/70 text-granate transition hover:bg-rosa/30 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-coral/60"
      aria-haspopup="true"
      :aria-expanded="abierto"
      :aria-label="hayNoLeidas ? `Notificaciones, ${noLeidas} sin leer` : 'Notificaciones'"
      @click="alternar"
    >
      <svg class="h-5 w-5" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
        <path d="M14.857 17.082a23.848 23.848 0 0 0 5.454-1.31A8.967 8.967 0 0 1 18 9.75V9A6 6 0 0 0 6 9v.75a8.967 8.967 0 0 1-2.312 6.022c1.733.64 3.56 1.085 5.455 1.31m5.714 0a24.255 24.255 0 0 1-5.714 0m5.714 0a3 3 0 1 1-5.714 0" />
      </svg>
      <span
        v-if="hayNoLeidas"
        class="absolute -right-0.5 -top-0.5 flex h-5 min-w-5 items-center justify-center rounded-full bg-coral px-1 text-[11px] font-bold leading-none text-white shadow-sm"
        aria-hidden="true"
      >
        {{ etiquetaContador }}
      </span>
    </button>

    <div
      v-if="abierto"
      class="absolute right-0 z-50 mt-3 w-[min(24rem,calc(100vw-2rem))] overflow-hidden rounded-3xl border border-rosa/50 bg-blanco shadow-[0_18px_40px_rgba(74,29,46,0.18)]"
    >
      <div class="flex items-center justify-between gap-3 border-b border-rosa/40 px-4 py-3">
        <div>
          <p class="text-sm font-semibold text-granate">Notificaciones</p>
          <p class="text-xs text-cafe/60">
            {{ hayNoLeidas ? `${noLeidas} sin leer` : 'Estás al día' }}
          </p>
        </div>
        <button
          v-if="hayNoLeidas"
          type="button"
          class="rounded-full px-2.5 py-1 text-xs font-semibold text-coral transition hover:bg-rosa/30 disabled:cursor-not-allowed disabled:opacity-50"
          :disabled="marcandoTodas"
          @click="marcarTodas"
        >
          {{ marcandoTodas ? 'Marcando...' : 'Marcar todas' }}
        </button>
      </div>

      <div v-if="cargando && !hayNotificaciones" class="px-4 py-8 text-center text-sm text-cafe/60">
        Cargando notificaciones...
      </div>

      <div v-else-if="error && !hayNotificaciones" class="flex flex-col items-center gap-3 px-4 py-8 text-center">
        <p class="text-sm text-cafe/70">{{ error }}</p>
        <button
          type="button"
          class="rounded-full border border-rosa bg-blanco px-4 py-1.5 text-xs font-semibold text-granate transition hover:bg-rosa/20"
          @click="cargar()"
        >
          Reintentar
        </button>
      </div>

      <div v-else-if="!hayNotificaciones" class="px-4 py-10 text-center">
        <p class="text-sm font-medium text-granate">No tenés notificaciones.</p>
        <p class="mt-1 text-xs leading-relaxed text-cafe/60">
          Cuando recibas una solicitud, un pedido o una novedad, aparecerá acá.
        </p>
      </div>

      <ul v-else class="max-h-96 divide-y divide-rosa/30 overflow-y-auto">
        <li v-for="notificacion in notificaciones" :key="notificacion.idNotificacionUsuario">
          <button
            type="button"
            class="flex w-full items-start gap-3 px-4 py-3 text-left transition hover:bg-rosa/10 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-inset focus-visible:ring-coral/50"
            :class="notificacion.leida ? 'bg-blanco' : 'bg-rosa/10'"
            @click="seleccionar(notificacion)"
          >
            <span
              class="mt-1 flex h-8 w-8 shrink-0 items-center justify-center rounded-full"
              :class="notificacion.leida ? 'bg-rosa/20 text-cafe/60' : 'bg-coral/15 text-coral'"
              aria-hidden="true"
            >
              <svg class="h-4 w-4" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <path d="M9 12h6m-6 4h6m2 5H7a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h5.586a1 1 0 0 1 .707.293l5.414 5.414a1 1 0 0 1 .293.707V19a2 2 0 0 1-2 2Z" />
              </svg>
            </span>

            <span class="min-w-0 flex-1">
              <span class="flex items-start justify-between gap-2">
                <span class="text-sm font-semibold text-granate">{{ notificacion.nombre }}</span>
                <span
                  v-if="!notificacion.leida"
                  class="mt-1.5 h-2 w-2 shrink-0 rounded-full bg-coral"
                  aria-label="Sin leer"
                ></span>
              </span>
              <span class="mt-0.5 block text-xs leading-relaxed text-cafe/75">
                {{ notificacion.descripcion }}
              </span>
              <span class="mt-1 flex flex-wrap items-center gap-x-2 gap-y-0.5 text-[11px] text-cafe/50">
                <span>{{ formatearFecha(notificacion.fechaCreacion) }}</span>
                <span v-if="destinoDe(notificacion)" class="font-semibold text-coral">
                  {{ textoDestino(notificacion) }}
                </span>
              </span>
            </span>
          </button>
        </li>
      </ul>

      <div class="border-t border-rosa/40 px-4 py-2.5 text-center">
        <router-link
          :to="rutaNotificaciones"
          class="text-xs font-semibold text-coral transition hover:text-rojizo"
          @click="cerrar"
        >
          Ver todas las notificaciones
        </router-link>
      </div>
    </div>
  </div>
</template>
