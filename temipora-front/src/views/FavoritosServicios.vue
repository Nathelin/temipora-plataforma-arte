<script setup>
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import api from '../servicios/api'
import FavoritosNav from '../components/FavoritosNav.vue'
import TarjetaServicioFavorito from '../components/TarjetaServicioFavorito.vue'
import { useNotificaciones } from '../composables/useNotificaciones'

const route = useRoute()
const router = useRouter()
const { exito, error } = useNotificaciones()

const TAMANIO = 12
const ORDENES = ['recientes', 'alfabetico']

function ordenInicial(valor) {
  return ORDENES.includes(valor) ? valor : 'recientes'
}

const disponibles = ref([])
const totalDisponibles = ref(0)
const pagina = ref(0)
const hayMas = ref(false)
const cargando = ref(true)
const cargandoMas = ref(false)
const errorCarga = ref('')

const noDisponibles = ref([])
const totalNoDisponibles = ref(0)
const paginaNoDisponibles = ref(0)
const hayMasNoDisponibles = ref(false)
const cargandoNoDisponibles = ref(false)
const cargandoMasNoDisponibles = ref(false)
const errorNoDisponibles = ref('')

const quitando = ref(null)
const busqueda = ref(typeof route.query.q === 'string' ? route.query.q : '')
const orden = ref(ordenInicial(route.query.orden))

const hayFiltros = computed(() => !!busqueda.value.trim())
const hayCambios = computed(() => hayFiltros.value || orden.value !== 'recientes')
const hayAlgunFavorito = computed(
  () => totalDisponibles.value + totalNoDisponibles.value > 0,
)
const mostrarFiltros = computed(() => hayAlgunFavorito.value || hayCambios.value)

let temporizador = null
let secuenciaDisponibles = 0
let secuenciaNoDisponibles = 0

function actualizarUrl() {
  const query = { ...route.query }
  const termino = busqueda.value.trim()
  if (termino) {
    query.q = termino
  } else {
    delete query.q
  }
  if (orden.value !== 'recientes') {
    query.orden = orden.value
  } else {
    delete query.orden
  }
  router.replace({ query })
}

function parametros(paginaSolicitada, estado) {
  const params = {
    pagina: paginaSolicitada,
    tamanio: TAMANIO,
    orden: orden.value,
    estado,
  }
  const termino = busqueda.value.trim()
  if (termino) params.q = termino
  return params
}

async function cargarDisponibles(paginaSolicitada = 0, acumular = false) {
  const miSecuencia = ++secuenciaDisponibles
  if (paginaSolicitada === 0) {
    cargando.value = true
  } else {
    cargandoMas.value = true
  }
  errorCarga.value = ''
  try {
    const res = await api.get('/favoritos/servicios', {
      params: parametros(paginaSolicitada, 'disponibles'),
    })
    if (miSecuencia !== secuenciaDisponibles) return
    const data = res.data || {}
    disponibles.value = acumular
      ? [...disponibles.value, ...(data.contenido || [])]
      : (data.contenido || [])
    totalDisponibles.value = data.totalElementos || 0
    pagina.value = data.pagina ?? paginaSolicitada
    hayMas.value = !!data.hayMas
  } catch (e) {
    if (miSecuencia !== secuenciaDisponibles) return
    if (e.response?.status === 401) {
      router.push({ name: 'login' })
      return
    }
    if (acumular && disponibles.value.length) {
      error('No se pudieron cargar más servicios favoritos.')
    } else {
      errorCarga.value = 'No se pudieron cargar tus servicios favoritos.'
    }
  } finally {
    if (miSecuencia === secuenciaDisponibles) {
      cargando.value = false
      cargandoMas.value = false
    }
  }
}

async function cargarNoDisponibles(paginaSolicitada = 0, acumular = false) {
  const miSecuencia = ++secuenciaNoDisponibles
  if (paginaSolicitada === 0) {
    cargandoNoDisponibles.value = true
  } else {
    cargandoMasNoDisponibles.value = true
  }
  errorNoDisponibles.value = ''
  try {
    const res = await api.get('/favoritos/servicios', {
      params: parametros(paginaSolicitada, 'no-disponibles'),
    })
    if (miSecuencia !== secuenciaNoDisponibles) return
    const data = res.data || {}
    noDisponibles.value = acumular
      ? [...noDisponibles.value, ...(data.contenido || [])]
      : (data.contenido || [])
    totalNoDisponibles.value = data.totalElementos || 0
    paginaNoDisponibles.value = data.pagina ?? paginaSolicitada
    hayMasNoDisponibles.value = !!data.hayMas
  } catch (e) {
    if (miSecuencia !== secuenciaNoDisponibles) return
    if (e.response?.status === 401) {
      router.push({ name: 'login' })
      return
    }
    if (acumular && noDisponibles.value.length) {
      error('No se pudieron cargar más servicios no disponibles.')
    } else {
      errorNoDisponibles.value = 'No se pudieron cargar los servicios no disponibles.'
    }
  } finally {
    if (miSecuencia === secuenciaNoDisponibles) {
      cargandoNoDisponibles.value = false
      cargandoMasNoDisponibles.value = false
    }
  }
}

function aplicarFiltros() {
  if (temporizador) {
    clearTimeout(temporizador)
    temporizador = null
  }
  actualizarUrl()
  paginaNoDisponibles.value = 0
  hayMasNoDisponibles.value = false
  void cargarDisponibles(0)
  void cargarNoDisponibles(0)
}

watch(busqueda, () => {
  if (temporizador) clearTimeout(temporizador)
  temporizador = setTimeout(aplicarFiltros, 300)
})

watch(orden, () => aplicarFiltros())

onBeforeUnmount(() => {
  if (temporizador) clearTimeout(temporizador)
})

function cargarMas() {
  if (!hayMas.value || cargando.value || cargandoMas.value) return
  void cargarDisponibles(pagina.value + 1, true)
}

function cargarMasNoDisponibles() {
  if (!hayMasNoDisponibles.value || cargandoNoDisponibles.value || cargandoMasNoDisponibles.value) {
    return
  }
  void cargarNoDisponibles(paginaNoDisponibles.value + 1, true)
}

function limpiarFiltros() {
  busqueda.value = ''
  orden.value = 'recientes'
}

async function quitar(favorito) {
  const nombre = favorito?.servicio?.nombre || 'este servicio'
  const confirmar = window.confirm(`¿Quitar "${nombre}" de tus favoritos?`)
  if (!confirmar || quitando.value) return

  quitando.value = favorito.idServicio
  try {
    await api.delete(`/favoritos/servicios/${favorito.idServicio}`)
    disponibles.value = disponibles.value.filter(
      (item) => item.idServicio !== favorito.idServicio,
    )
    noDisponibles.value = noDisponibles.value.filter(
      (item) => item.idServicio !== favorito.idServicio,
    )

    if (favorito.disponible) {
      totalDisponibles.value = Math.max(0, totalDisponibles.value - 1)
      if (hayMas.value || (!disponibles.value.length && totalDisponibles.value > 0)) {
        void cargarDisponibles(0)
      }
    } else {
      totalNoDisponibles.value = Math.max(0, totalNoDisponibles.value - 1)
      if (hayMasNoDisponibles.value
        || (!noDisponibles.value.length && totalNoDisponibles.value > 0)) {
        void cargarNoDisponibles(0)
      }
    }

    exito(`Quitaste "${nombre}" de tus favoritos.`)
  } catch (e) {
    if (e.response?.status === 401) {
      router.push({ name: 'login' })
      return
    }
    error(e.response?.data?.message || 'No se pudo quitar el servicio de tus favoritos.')
  } finally {
    quitando.value = null
  }
}

onMounted(() => {
  void cargarDisponibles(0)
  void cargarNoDisponibles(0)
})
</script>

<template>
  <div class="mx-auto max-w-screen-2xl px-4 py-10 sm:px-8 sm:py-14">
    <div class="mb-8 flex flex-col gap-6 sm:mb-10 sm:flex-row sm:items-end sm:justify-between">
      <div class="flex flex-col gap-2">
        <router-link
          :to="{ name: 'favoritos' }"
          class="text-sm font-semibold text-coral transition hover:text-rojizo"
        >
          ← Volver al resumen
        </router-link>
        <h1 class="font-display text-3xl font-extrabold tracking-[-0.02em] text-granate sm:text-4xl">
          Servicios favoritos
        </h1>
        <p class="max-w-2xl text-sm leading-relaxed text-cafe/70">
          Los servicios que guardaste para volver a ellos o solicitarlos más adelante.
        </p>
      </div>

      <FavoritosNav />
    </div>

    <div
      v-if="mostrarFiltros"
      class="mb-8 rounded-[28px] border border-borde/60 bg-superficie p-4 shadow-[0_14px_36px_rgba(74,29,46,0.08)] sm:p-5"
    >
      <div class="flex flex-wrap items-center gap-3">
        <div class="relative min-w-44 flex-1">
          <svg class="pointer-events-none absolute left-4 top-1/2 h-4 w-4 -translate-y-1/2 text-cafe/40" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" aria-hidden="true">
            <circle cx="11" cy="11" r="7" />
            <path d="m20 20-3.5-3.5" />
          </svg>
          <input
            v-model="busqueda"
            type="search"
            aria-label="Buscar servicios favoritos"
            placeholder="Buscar por servicio o @artista..."
            class="block w-full rounded-full border-0 bg-crema py-2.5 pl-11 pr-10 text-sm text-texto shadow-none ring-1 ring-inset ring-borde/60 placeholder:text-cafe/40 focus:ring-2 focus:ring-inset focus:ring-coral focus:outline-none"
          />
          <button
            v-if="busqueda"
            type="button"
            class="absolute right-3.5 top-1/2 -translate-y-1/2 text-cafe/40 transition hover:text-granate"
            aria-label="Limpiar búsqueda"
            @click="busqueda = ''"
          >
            ✕
          </button>
        </div>

        <div
          class="inline-flex rounded-full border border-borde/60 bg-crema p-1"
          role="group"
          aria-label="Ordenar servicios favoritos"
        >
          <button
            type="button"
            class="rounded-full px-3 py-1.5 text-xs font-semibold transition"
            :class="orden === 'recientes' ? 'bg-coral text-white shadow-sm' : 'text-cafe/60 hover:text-granate'"
            :aria-pressed="orden === 'recientes'"
            @click="orden = 'recientes'"
          >
            Recientes
          </button>
          <button
            type="button"
            class="rounded-full px-3 py-1.5 text-xs font-semibold transition"
            :class="orden === 'alfabetico' ? 'bg-coral text-white shadow-sm' : 'text-cafe/60 hover:text-granate'"
            :aria-pressed="orden === 'alfabetico'"
            @click="orden = 'alfabetico'"
          >
            A–Z
          </button>
        </div>

        <button
          v-if="hayCambios"
          type="button"
          class="inline-flex min-h-10 items-center rounded-full border border-borde/60 bg-crema px-4 text-sm font-semibold text-cafe transition hover:bg-rosa/30 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-coral/60"
          @click="limpiarFiltros"
        >
          Limpiar
        </button>
      </div>
    </div>

    <div
      v-if="cargando"
      class="grid grid-cols-1 gap-6 sm:grid-cols-2 xl:grid-cols-3"
      aria-label="Cargando servicios favoritos"
    >
      <div
        v-for="marcador in 6"
        :key="marcador"
        class="h-80 animate-pulse rounded-[26px] bg-rosa/25"
      ></div>
    </div>

    <div
      v-else-if="errorCarga"
      class="rounded-3xl border border-rojizo/30 bg-superficie px-6 py-10 text-center"
    >
      <p class="text-sm text-rojizo">{{ errorCarga }}</p>
      <button
        type="button"
        class="mt-4 inline-flex min-h-10 items-center rounded-full border border-borde/60 bg-crema px-5 text-sm font-semibold text-granate transition hover:bg-rosa/25"
        @click="cargarDisponibles(0)"
      >
        Reintentar
      </button>
    </div>

    <template v-else>
      <template v-if="disponibles.length">
        <div class="mb-5 flex items-center justify-between gap-3">
          <p class="text-sm text-cafe/60">
            {{ totalDisponibles }}
            {{ totalDisponibles === 1 ? 'servicio disponible' : 'servicios disponibles' }}
          </p>
        </div>

        <div class="grid grid-cols-1 gap-6 sm:grid-cols-2 xl:grid-cols-3">
          <TarjetaServicioFavorito
            v-for="favorito in disponibles"
            :key="favorito.idServicio"
            :favorito="favorito"
            :quitando="quitando === favorito.idServicio"
            @quitar="quitar"
          />
        </div>

        <div v-if="hayMas" class="mt-10 flex justify-center">
          <button
            type="button"
            class="inline-flex min-h-10 items-center rounded-full border border-borde/60 bg-crema px-6 text-sm font-semibold text-granate transition hover:bg-rosa/25 disabled:cursor-wait disabled:opacity-60"
            :disabled="cargandoMas"
            @click="cargarMas"
          >
            {{ cargandoMas ? 'Cargando...' : 'Cargar más' }}
          </button>
        </div>
      </template>

      <div
        v-else-if="hayFiltros"
        class="rounded-3xl border border-dashed border-borde bg-superficie px-6 py-14 text-center"
      >
        <p class="text-sm text-cafe/60">
          No encontramos servicios disponibles que coincidan con “{{ busqueda.trim() }}”.
        </p>
        <button
          type="button"
          class="mt-4 inline-flex min-h-10 items-center rounded-full border border-borde/60 bg-crema px-5 text-sm font-semibold text-granate transition hover:bg-rosa/25"
          @click="busqueda = ''"
        >
          Limpiar búsqueda
        </button>
      </div>

      <div
        v-else-if="hayAlgunFavorito"
        class="rounded-3xl border border-dashed border-borde bg-superficie px-6 py-14 text-center"
      >
        <p class="text-sm text-cafe/60">
          No tenés servicios disponibles. Podés revisar los no disponibles acá abajo.
        </p>
      </div>

      <div
        v-else
        class="rounded-3xl border border-dashed border-borde bg-superficie px-6 py-16 text-center"
      >
        <div class="mx-auto flex h-14 w-14 items-center justify-center rounded-full bg-rosa/40 text-granate">
          <svg class="h-7 w-7" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" aria-hidden="true">
            <path stroke-linecap="round" stroke-linejoin="round" d="m12 3 2.78 5.63 6.22.9-4.5 4.39 1.06 6.2L12 17.2l-5.56 2.92 1.06-6.2L3 9.53l6.22-.9L12 3Z" />
          </svg>
        </div>
        <h2 class="mt-4 font-display text-xl font-bold text-granate">Todavía no guardaste servicios</h2>
        <p class="mx-auto mt-2 max-w-md text-sm text-cafe/70">
          Tocá la estrella en un servicio para verlo acá.
        </p>
        <router-link
          to="/servicios"
          class="mt-6 inline-flex min-h-10 items-center rounded-full bg-coral px-5 text-sm font-bold text-white shadow-sm transition hover:bg-rojizo"
        >
          Explorar servicios
        </router-link>
      </div>

      <details
        v-if="totalNoDisponibles || errorNoDisponibles"
        class="mt-12 rounded-3xl border border-borde/60 bg-superficie p-5 shadow-sm sm:p-6"
      >
        <summary class="cursor-pointer select-none text-sm font-semibold text-granate">
          No disponibles ({{ totalNoDisponibles }})
        </summary>

        <div v-if="cargandoNoDisponibles" class="mt-5 text-sm text-cafe/60">
          Cargando servicios no disponibles...
        </div>

        <div
          v-else-if="errorNoDisponibles"
          class="mt-5 rounded-2xl border border-rojizo/30 bg-rojizo/5 px-4 py-5 text-center"
        >
          <p class="text-sm text-rojizo">{{ errorNoDisponibles }}</p>
          <button
            type="button"
            class="mt-3 inline-flex min-h-10 items-center rounded-full border border-borde/60 bg-crema px-4 text-sm font-semibold text-granate transition hover:bg-rosa/25"
            @click="cargarNoDisponibles(0)"
          >
            Reintentar
          </button>
        </div>

        <template v-else>
          <div
            v-if="noDisponibles.length"
            class="mt-5 grid grid-cols-1 gap-6 sm:grid-cols-2 xl:grid-cols-3"
          >
            <TarjetaServicioFavorito
              v-for="favorito in noDisponibles"
              :key="favorito.idServicio"
              :favorito="favorito"
              :quitando="quitando === favorito.idServicio"
              @quitar="quitar"
            />
          </div>

          <p v-else class="mt-5 text-sm text-cafe/60">
            No hay servicios no disponibles que coincidan con la búsqueda.
          </p>

          <div v-if="hayMasNoDisponibles" class="mt-6 flex justify-center">
            <button
              type="button"
              class="inline-flex min-h-10 items-center rounded-full border border-borde/60 bg-crema px-5 text-sm font-semibold text-granate transition hover:bg-rosa/25 disabled:cursor-wait disabled:opacity-60"
              :disabled="cargandoMasNoDisponibles"
              @click="cargarMasNoDisponibles"
            >
              {{ cargandoMasNoDisponibles ? 'Cargando...' : 'Cargar más no disponibles' }}
            </button>
          </div>
        </template>
      </details>
    </template>
  </div>
</template>
