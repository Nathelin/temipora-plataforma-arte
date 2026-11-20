<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  CATEGORIAS_NOTIFICACION,
  destinoDe as destinoCompartido,
  textoDestino as textoCompartido,
} from '../servicios/notificaciones'
import { useNotificacionesSistema } from '../composables/useNotificacionesSistema'
import { useNotificaciones as useToast } from '../composables/useNotificaciones'

const route = useRoute()
const router = useRouter()
const { exito, error: notificarError } = useToast()
const {
  notificaciones,
  noLeidas,
  porCategoria,
  hayMas,
  total,
  cargando,
  cargandoMas,
  error,
  categoria,
  soloNoLeidas,
  cargar,
  cargarMas,
  marcarLeida,
  marcarTodasLeidas,
  cambiarCategoria,
  alternarSoloNoLeidas,
  establecerFiltros,
  sincronizarSesion,
  refrescarContador,
} = useNotificacionesSistema()

const marcandoTodas = ref(false)
const esAdmin = localStorage.getItem('rol') === 'ADMIN'
const esArtista = localStorage.getItem('rol') === 'ARTISTA'
const hayNotificaciones = computed(() => notificaciones.value.length > 0)
const hayNoLeidas = computed(() => noLeidas.value > 0)
const hayFiltro = computed(() => Boolean(categoria.value) || soloNoLeidas.value)

const CATEGORIAS_VALIDAS = new Set(CATEGORIAS_NOTIFICACION.map((item) => item.clave))

const rolActual = localStorage.getItem('rol') || ''

const opcionesCategoria = computed(() => [
  { clave: null, texto: 'Todas' },
  ...CATEGORIAS_NOTIFICACION,
])

const etiquetaCategoria = computed(
  () => CATEGORIAS_NOTIFICACION.find((item) => item.clave === categoria.value)?.texto || '',
)

const textoVacio = computed(() => {
  if (soloNoLeidas.value && categoria.value) {
    return `No tenés notificaciones sin leer de ${etiquetaCategoria.value.toLowerCase()}.`
  }
  if (soloNoLeidas.value) {
    return 'No tenés notificaciones sin leer.'
  }
  if (categoria.value) {
    return `No tenés notificaciones de ${etiquetaCategoria.value.toLowerCase()}.`
  }
  return 'Cuando recibas una solicitud, un pedido o una novedad importante, aparecerá en esta página.'
})

function contadorDe(clave) {
  if (!clave) return noLeidas.value
  return Number(porCategoria.value?.[clave] || 0)
}

function etiquetaContador(valor) {
  return valor > 9 ? '9+' : String(valor)
}

function filtrosDeUrl() {
  const pedida = String(route.query.categoria || '').toUpperCase()
  return {
    categoria: CATEGORIAS_VALIDAS.has(pedida) ? pedida : null,
    soloNoLeidas: route.query.sinLeer === '1',
  }
}

function sincronizarUrl() {
  const query = { ...route.query }
  delete query.categoria
  delete query.sinLeer
  if (categoria.value) query.categoria = categoria.value
  if (soloNoLeidas.value) query.sinLeer = '1'
  router.replace({ query })
}

function seleccionarCategoria(clave) {
  if (categoria.value === clave) return
  cambiarCategoria(clave)
  sincronizarUrl()
}

function alternarFiltroSinLeer() {
  alternarSoloNoLeidas()
  sincronizarUrl()
}

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
    const marcadas = await marcarTodasLeidas()
    if (marcadas > 0) {
      exito(
        marcadas === 1
          ? 'Marcaste 1 notificación como leída.'
          : `Marcaste ${marcadas} notificaciones como leídas.`,
      )
    }
  } catch (e) {
    if (e.response?.status !== 401) {
      notificarError(
        e.response?.data?.message || 'No se pudieron marcar las notificaciones.',
      )
    }
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

watch(
  () => [route.query.categoria, route.query.sinLeer],
  () => {
    const filtros = filtrosDeUrl()
    if (filtros.categoria === categoria.value
      && filtros.soloNoLeidas === soloNoLeidas.value) {
      return
    }
    establecerFiltros(filtros)
    void cargar({ reset: true })
  },
)

onMounted(() => {
  sincronizarSesion()
  establecerFiltros(filtrosDeUrl())
  void refrescarContador()
  cargar({ reset: true })
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

    <div class="-mx-4 mt-5 overflow-x-auto px-4 pb-1 sm:mx-0 sm:px-0">
      <div
        class="inline-flex min-w-full rounded-full border border-rosa bg-blanco p-1"
        role="tablist"
        aria-label="Categorías de notificaciones"
      >
        <button
          v-for="opcion in opcionesCategoria"
          :key="opcion.clave || 'todas'"
          type="button"
          role="tab"
          :aria-selected="categoria === opcion.clave"
          class="inline-flex shrink-0 items-center gap-2 rounded-full px-4 py-1.5 text-sm font-semibold transition"
          :class="categoria === opcion.clave
            ? 'bg-coral text-white shadow-sm'
            : 'text-cafe/65 hover:text-granate'"
          @click="seleccionarCategoria(opcion.clave)"
        >
          <span>{{ opcion.texto }}</span>
          <span
            v-if="contadorDe(opcion.clave)"
            class="inline-flex min-w-5 items-center justify-center rounded-full px-1.5 py-0.5 text-[10px] font-bold"
            :class="categoria === opcion.clave ? 'bg-white/25 text-white' : 'bg-coral/15 text-coral'"
          >
            {{ etiquetaContador(contadorDe(opcion.clave)) }}
          </span>
        </button>
      </div>
    </div>

    <div class="mt-3 flex flex-wrap items-center justify-between gap-3">
      <button
        type="button"
        role="switch"
        :aria-checked="soloNoLeidas"
        class="inline-flex items-center gap-2 rounded-full border px-3.5 py-1.5 text-xs font-semibold transition"
        :class="soloNoLeidas
          ? 'border-coral/40 bg-coral/10 text-coral'
          : 'border-rosa bg-blanco text-cafe/70 hover:text-granate'"
        @click="alternarFiltroSinLeer"
      >
        <span
          class="relative inline-flex h-4 w-7 items-center rounded-full transition"
          :class="soloNoLeidas ? 'bg-coral' : 'bg-borde'"
          aria-hidden="true"
        >
          <span
            class="inline-block h-3 w-3 rounded-full bg-white shadow-sm transition"
            :class="soloNoLeidas ? 'translate-x-3.5' : 'translate-x-0.5'"
          ></span>
        </span>
        Solo sin leer
      </button>

      <span v-if="!cargando" class="text-xs text-cafe/50">
        {{ total }} {{ total === 1 ? 'notificación' : 'notificaciones' }}
      </span>
    </div>

    <CargandoPagina
      v-if="cargando && !hayNotificaciones"
      tipo="lista"
      mensaje="Cargando notificaciones…"
      :tarjetas="6"
    />

    <div v-else-if="error && !hayNotificaciones" class="mt-8 rounded-2xl border border-rosa/50 bg-blanco px-6 py-12 text-center">
      <p class="text-sm text-cafe/70">{{ error }}</p>
      <button
        type="button"
        class="mt-4 rounded-full border border-rosa bg-blanco px-4 py-2 text-sm font-semibold text-granate transition hover:bg-rosa/20"
        @click="cargar({ reset: true })"
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
      <p class="mt-4 text-base font-semibold text-granate">
        {{ hayFiltro ? 'Nada por acá' : 'No tenés notificaciones.' }}
      </p>
      <p class="mx-auto mt-1 max-w-sm text-sm leading-relaxed text-cafe/60">
        {{ textoVacio }}
      </p>
    </div>

    <template v-else>
      <ul class="mt-8 space-y-3">
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

      <div v-if="hayMas" class="pt-6 text-center">
        <button
          type="button"
          class="min-h-10 rounded-full border border-rosa bg-blanco px-5 text-sm font-semibold text-granate transition hover:bg-rosa/20 disabled:cursor-wait disabled:opacity-60"
          :disabled="cargandoMas"
          @click="cargarMas"
        >
          {{ cargandoMas ? 'Cargando...' : 'Cargar más' }}
        </button>
        <p class="mt-2 text-xs text-cafe/45">
          Mostrando {{ notificaciones.length }} de {{ total }}
        </p>
      </div>
    </template>
  </div>
</template>
