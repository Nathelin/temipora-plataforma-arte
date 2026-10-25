<script setup>
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import api from '../servicios/api'
import FavoritosNav from '../components/FavoritosNav.vue'
import TarjetaArtistaFavorito from '../components/TarjetaArtistaFavorito.vue'
import { useNotificaciones } from '../composables/useNotificaciones'

const route = useRoute()
const router = useRouter()
const { exito, error } = useNotificaciones()

const TAMANIO = 24
const ORDENES = ['recientes', 'alfabetico']

function ordenInicial(valor) {
  return ORDENES.includes(valor) ? valor : 'recientes'
}

const artistas = ref([])
const total = ref(0)
const pagina = ref(0)
const hayMas = ref(false)
const cargando = ref(true)
const cargandoMas = ref(false)
const errorCarga = ref('')
const quitando = ref(null)
const busqueda = ref(typeof route.query.q === 'string' ? route.query.q : '')
const orden = ref(ordenInicial(route.query.orden))

const hayFiltros = computed(() => !!busqueda.value.trim())
const hayCambios = computed(() => hayFiltros.value || orden.value !== 'recientes')
const mostrarFiltros = computed(() => total.value > 0 || hayCambios.value)

let temporizador = null
let secuencia = 0

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

async function cargar(paginaSolicitada = 0, acumular = false) {
  const miSecuencia = ++secuencia
  if (paginaSolicitada === 0) {
    cargando.value = true
  } else {
    cargandoMas.value = true
  }
  errorCarga.value = ''
  try {
    const parametros = {
      pagina: paginaSolicitada,
      tamanio: TAMANIO,
      orden: orden.value,
    }
    const termino = busqueda.value.trim()
    if (termino) parametros.q = termino

    const res = await api.get('/favoritos/artistas', { params: parametros })
    if (miSecuencia !== secuencia) return
    const data = res.data || {}
    artistas.value = acumular
      ? [...artistas.value, ...(data.contenido || [])]
      : (data.contenido || [])
    total.value = data.totalElementos || 0
    pagina.value = data.pagina ?? paginaSolicitada
    hayMas.value = !!data.hayMas
  } catch (e) {
    if (miSecuencia !== secuencia) return
    if (e.response?.status === 401) {
      router.push({ name: 'login' })
      return
    }
    if (acumular && artistas.value.length) {
      error('No se pudieron cargar más artistas favoritos.')
    } else {
      errorCarga.value = 'No se pudieron cargar tus artistas favoritos.'
    }
  } finally {
    if (miSecuencia === secuencia) {
      cargando.value = false
      cargandoMas.value = false
    }
  }
}

function aplicarFiltros() {
  if (temporizador) {
    clearTimeout(temporizador)
    temporizador = null
  }
  actualizarUrl()
  void cargar(0)
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
  void cargar(pagina.value + 1, true)
}

function limpiarFiltros() {
  busqueda.value = ''
  orden.value = 'recientes'
}

async function quitar(artista) {
  const confirmar = window.confirm(
    `¿Quitar a @${artista.nombreUsuario} de tus favoritos?`,
  )
  if (!confirmar || quitando.value) return

  quitando.value = artista.idUsuario
  try {
    await api.delete(`/favoritos/artistas/${encodeURIComponent(artista.nombreUsuario)}`)
    artistas.value = artistas.value.filter(
      (item) => item.idUsuario !== artista.idUsuario,
    )
    total.value = Math.max(0, total.value - 1)
    exito(`Quitaste a @${artista.nombreUsuario} de tus favoritos.`)
    if (hayMas.value || (!artistas.value.length && total.value > 0)) {
      void cargar(0)
    }
  } catch (e) {
    if (e.response?.status === 401) {
      router.push({ name: 'login' })
      return
    }
    error(e.response?.data?.message || 'No se pudo quitar al artista de tus favoritos.')
  } finally {
    quitando.value = null
  }
}

onMounted(() => cargar(0))
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
          Artistas favoritos
        </h1>
        <p class="max-w-2xl text-sm leading-relaxed text-cafe/70">
          Los artistas que guardaste para volver a encontrar sus perfiles y servicios.
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
            aria-label="Buscar artistas favoritos"
            placeholder="Buscar por nombre o @usuario..."
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
          aria-label="Ordenar artistas favoritos"
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
      class="grid grid-cols-2 gap-x-3 gap-y-8 sm:grid-cols-3 sm:gap-x-6 sm:gap-y-10 lg:grid-cols-4 xl:grid-cols-6"
      aria-label="Cargando artistas favoritos"
    >
      <div
        v-for="marcador in 12"
        :key="marcador"
        class="flex animate-pulse flex-col items-center gap-3"
      >
        <div class="h-24 w-24 rounded-full bg-rosa/40 sm:h-28 sm:w-28"></div>
        <div class="h-3 w-24 max-w-full rounded-full bg-rosa/40"></div>
        <div class="h-2.5 w-16 max-w-full rounded-full bg-rosa/25"></div>
      </div>
    </div>

    <div
      v-else-if="errorCarga"
      class="rounded-3xl border border-rojizo/30 bg-superficie px-6 py-10 text-center"
    >
      <p class="text-sm text-rojizo">{{ errorCarga }}</p>
      <button
        type="button"
        class="mt-4 inline-flex min-h-10 items-center rounded-full border border-borde/60 bg-crema px-5 text-sm font-semibold text-granate transition hover:bg-rosa/25"
        @click="cargar(0)"
      >
        Reintentar
      </button>
    </div>

    <template v-else-if="artistas.length">
      <div class="mb-5 flex items-center justify-between gap-3">
        <p class="text-sm text-cafe/60">
          {{ total }} {{ total === 1 ? 'artista guardado' : 'artistas guardados' }}
        </p>
      </div>

      <ul class="grid grid-cols-2 gap-x-3 gap-y-8 sm:grid-cols-3 sm:gap-x-6 sm:gap-y-10 lg:grid-cols-4 xl:grid-cols-6">
        <TarjetaArtistaFavorito
          v-for="artista in artistas"
          :key="artista.idUsuario"
          :artista="artista"
          :quitando="quitando === artista.idUsuario"
          @quitar="quitar"
        />
      </ul>

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
        No encontramos artistas que coincidan con “{{ busqueda.trim() }}”.
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
      v-else
      class="rounded-3xl border border-dashed border-borde bg-superficie px-6 py-16 text-center"
    >
      <div class="mx-auto flex h-14 w-14 items-center justify-center rounded-full bg-rosa/40 text-granate">
        <svg class="h-7 w-7" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" aria-hidden="true">
          <path stroke-linecap="round" stroke-linejoin="round" d="m12 3 2.78 5.63 6.22.9-4.5 4.39 1.06 6.2L12 17.2l-5.56 2.92 1.06-6.2L3 9.53l6.22-.9L12 3Z" />
        </svg>
      </div>
      <h2 class="mt-4 font-display text-xl font-bold text-granate">Todavía no guardaste artistas</h2>
      <p class="mx-auto mt-2 max-w-md text-sm text-cafe/70">
        Tocá “Favorito” en el perfil de un artista para verlo acá.
      </p>
      <router-link
        to="/servicios"
        class="mt-6 inline-flex min-h-10 items-center rounded-full bg-coral px-5 text-sm font-bold text-white shadow-sm transition hover:bg-rojizo"
      >
        Explorar servicios
      </router-link>
    </div>
  </div>
</template>
