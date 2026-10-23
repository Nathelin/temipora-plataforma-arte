<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import api from '../servicios/api'
import { useNotificaciones } from '../composables/useNotificaciones'

const router = useRouter()
const { exito, error } = useNotificaciones()

const favoritos = ref([])
const cargando = ref(true)
const errorCarga = ref('')
const quitando = ref(null)
const orden = ref('recientes')

const favoritosOrdenados = computed(() => {
  const lista = [...favoritos.value]
  if (orden.value === 'alfabetico') {
    return lista.sort((a, b) => nombreArtista(a).localeCompare(nombreArtista(b), 'es', { sensitivity: 'base' }))
  }
  return lista.sort((a, b) => {
    const diferencia = fechaAgregado(b) - fechaAgregado(a)
    return diferencia !== 0
      ? diferencia
      : nombreArtista(a).localeCompare(nombreArtista(b), 'es', { sensitivity: 'base' })
  })
})

async function cargarFavoritos() {
  cargando.value = true
  errorCarga.value = ''
  try {
    const res = await api.get('/favoritos')
    favoritos.value = res.data || []
  } catch (e) {
    if (e.response?.status === 401) {
      router.push({ name: 'login' })
      return
    }
    errorCarga.value = 'No se pudieron cargar tus artistas favoritos.'
  } finally {
    cargando.value = false
  }
}

async function quitarFavorito(artista) {
  const confirmar = window.confirm(
    `¿Quitar a @${artista.nombreUsuario} de tus favoritos?`,
  )
  if (!confirmar || quitando.value) return

  quitando.value = artista.idUsuario
  try {
    await api.delete(`/favoritos/${encodeURIComponent(artista.nombreUsuario)}`)
    favoritos.value = favoritos.value.filter(
      (favorito) => favorito.idUsuario !== artista.idUsuario,
    )
    exito(`Quitaste a @${artista.nombreUsuario} de tus favoritos.`)
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

function nombreArtista(artista) {
  return artista.nombreDisplay || artista.nombreUsuario || ''
}

function fechaAgregado(artista) {
  const fecha = Date.parse(artista.fechaAgregado)
  return Number.isNaN(fecha) ? 0 : fecha
}

function lineaArtista(artista) {
  const partes = [artista.tipoArtePrincipal, artista.medioPrincipal].filter(Boolean)
  if (partes.length) return partes.join(' · ')
  return 'Artista de Temipora'
}

function etiquetaDestacada(artista) {
  const etiqueta = artista.etiquetasDestacadas?.[0]
  if (!etiqueta) return ''
  const partes = [artista.tipoArtePrincipal, artista.medioPrincipal]
  const repetida = partes.some(
    (parte) => parte && parte.toLowerCase() === etiqueta.toLowerCase(),
  )
  return repetida ? '' : etiqueta
}

function fotoPerfil(artista) {
  return artista.urlFotoPerfilMiniatura || artista.urlFotoPerfil || ''
}

function fotoInicial(nombre) {
  return (nombre || '?').charAt(0).toUpperCase()
}

onMounted(cargarFavoritos)
</script>

<template>
  <div class="mx-auto max-w-screen-2xl px-4 py-10 sm:px-8 sm:py-14">
    <div class="mb-8 flex flex-col gap-6 sm:mb-10 sm:flex-row sm:items-end sm:justify-between">
      <div class="flex flex-col gap-2">
        <p class="text-sm font-semibold uppercase tracking-[0.18em] text-coral">Tu colección</p>
        <h1 class="font-display text-3xl font-extrabold tracking-[-0.02em] text-granate sm:text-4xl">
          Artistas favoritos
        </h1>
        <p class="max-w-2xl text-sm leading-relaxed text-cafe/70">
          Tu selección personal de artistas.
        </p>
      </div>

      <div
        v-if="!cargando && !errorCarga && favoritos.length"
        class="flex flex-wrap items-center gap-3"
      >
        <p class="text-sm text-cafe/55">
          {{ favoritos.length }} {{ favoritos.length === 1 ? 'artista' : 'artistas' }}
        </p>
        <div
          class="inline-flex rounded-full border border-borde/60 bg-superficie p-1 shadow-sm"
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
      </div>
    </div>

    <div
      v-if="cargando"
      class="grid grid-cols-2 gap-x-3 gap-y-8 sm:grid-cols-3 sm:gap-x-6 sm:gap-y-10 lg:grid-cols-4 xl:grid-cols-5"
      aria-label="Cargando artistas favoritos"
    >
      <div
        v-for="marcador in 10"
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
        @click="cargarFavoritos"
      >
        Reintentar
      </button>
    </div>

    <div
      v-else-if="!favoritos.length"
      class="rounded-3xl border border-dashed border-borde bg-superficie px-6 py-16 text-center"
    >
      <div class="mx-auto flex h-14 w-14 items-center justify-center rounded-full bg-rosa/40 text-granate">
        <svg class="h-7 w-7" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" aria-hidden="true">
          <path stroke-linecap="round" stroke-linejoin="round" d="m12 3 2.78 5.63 6.22.9-4.5 4.39 1.06 6.2L12 17.2l-5.56 2.92 1.06-6.2L3 9.53l6.22-.9L12 3Z" />
        </svg>
      </div>
      <h2 class="mt-4 font-display text-xl font-bold text-granate">Todavía no tenés favoritos</h2>
      <p class="mx-auto mt-2 max-w-md text-sm text-cafe/70">
        Explorá artistas y tocá “Favorito” en su perfil para guardarlos acá.
      </p>
      <router-link
        to="/servicios"
        class="mt-6 inline-flex min-h-10 items-center rounded-full bg-coral px-5 text-sm font-bold text-white shadow-sm transition hover:bg-rojizo"
      >
        Explorar artistas
      </router-link>
    </div>

    <ul
      v-else
      class="grid grid-cols-2 gap-x-3 gap-y-8 sm:grid-cols-3 sm:gap-x-6 sm:gap-y-10 lg:grid-cols-4 xl:grid-cols-5"
    >
      <li
        v-for="artista in favoritosOrdenados"
        :key="artista.idUsuario"
        class="favorito-artista group flex min-w-0 flex-col items-center"
      >
        <div class="relative">
          <router-link
            :to="{ name: 'perfil', params: { nombreUsuario: artista.nombreUsuario } }"
            class="block rounded-full focus-visible:outline-none focus-visible:ring-4 focus-visible:ring-coral/40 focus-visible:ring-offset-4 focus-visible:ring-offset-crema"
            :aria-label="`Ver el perfil de @${artista.nombreUsuario}`"
          >
            <span class="block rounded-full bg-linear-to-br from-coral/55 via-durazno/75 to-mantequilla p-[3px] transition duration-300 group-hover:scale-[1.04] group-hover:shadow-[0_16px_30px_rgba(74,29,46,0.16)]">
              <span class="block h-24 w-24 overflow-hidden rounded-full border-4 border-crema bg-coral/20 sm:h-28 sm:w-28">
                <img
                  v-if="fotoPerfil(artista)"
                  :src="fotoPerfil(artista)"
                  :alt="`Foto de perfil de ${nombreArtista(artista)}`"
                  class="h-full w-full object-cover"
                  loading="lazy"
                  decoding="async"
                />
                <span
                  v-else
                  class="flex h-full w-full items-center justify-center font-display text-3xl font-extrabold text-granate"
                >
                  {{ fotoInicial(artista.nombreUsuario) }}
                </span>
              </span>
            </span>
          </router-link>

          <button
            type="button"
            class="favorito-quitar absolute -right-1 -top-1 z-10 flex h-8 w-8 items-center justify-center rounded-full border border-coral/30 bg-superficie/95 text-coral shadow-sm transition hover:scale-105 hover:bg-coral hover:text-white focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-coral/60 focus-visible:ring-offset-2 focus-visible:ring-offset-crema disabled:cursor-wait disabled:opacity-60"
            :disabled="quitando === artista.idUsuario"
            :aria-label="`Quitar a @${artista.nombreUsuario} de tus favoritos`"
            title="Quitar de favoritos"
            @click="quitarFavorito(artista)"
          >
            <svg
              v-if="quitando !== artista.idUsuario"
              class="h-4 w-4"
              viewBox="0 0 24 24"
              fill="currentColor"
              aria-hidden="true"
            >
              <path d="m12 3 2.78 5.63 6.22.9-4.5 4.39 1.06 6.2L12 17.2l-5.56 2.92 1.06-6.2L3 9.53l6.22-.9L12 3Z" />
            </svg>
            <span v-else class="text-[10px] font-bold" aria-hidden="true">…</span>
          </button>
        </div>

        <router-link
          :to="{ name: 'perfil', params: { nombreUsuario: artista.nombreUsuario } }"
          class="mt-3 flex w-full min-w-0 flex-col items-center rounded-2xl px-1 py-1 text-center focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-coral/50 focus-visible:ring-offset-2 focus-visible:ring-offset-crema"
        >
          <span class="w-full truncate font-display text-base font-bold leading-tight text-granate transition group-hover:text-coral sm:text-lg">
            {{ nombreArtista(artista) }}
          </span>
          <span class="mt-0.5 w-full truncate text-xs text-cafe/55 sm:text-sm">
            @{{ artista.nombreUsuario }}
          </span>
          <span class="mt-1.5 w-full truncate text-[11px] font-medium text-coral sm:text-xs">
            {{ lineaArtista(artista) }}
          </span>
          <span
            v-if="etiquetaDestacada(artista)"
            class="mt-2 inline-block max-w-full truncate rounded-full bg-rosa/35 px-2.5 py-0.5 text-[10px] font-medium text-cafe/75 sm:text-[11px]"
          >
            #{{ etiquetaDestacada(artista) }}
          </span>
        </router-link>
      </li>
    </ul>
  </div>
</template>

<style scoped>
@media (hover: hover) {
  .favorito-quitar {
    opacity: 0;
  }

  .favorito-artista:hover .favorito-quitar,
  .favorito-artista:focus-within .favorito-quitar {
    opacity: 1;
  }
}
</style>
