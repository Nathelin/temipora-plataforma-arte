<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import api from '../servicios/api'
import FavoritosNav from '../components/FavoritosNav.vue'
import TarjetaArtistaFavorito from '../components/TarjetaArtistaFavorito.vue'
import TarjetaServicioFavorito from '../components/TarjetaServicioFavorito.vue'
import { useNotificaciones } from '../composables/useNotificaciones'

const router = useRouter()
const { exito, error } = useNotificaciones()

const TAMANIO_ARTISTAS = 6
const TAMANIO_SERVICIOS = 3

const artistas = ref([])
const servicios = ref([])
const totalArtistas = ref(0)
const totalServicios = ref(0)
const cargando = ref(true)
const errorCarga = ref('')
const quitandoArtista = ref(null)
const quitandoServicio = ref(null)

const hayFavoritos = computed(() => totalArtistas.value + totalServicios.value > 0)

async function cargarFavoritos() {
  cargando.value = true
  errorCarga.value = ''
  try {
    const [resArtistas, resServicios] = await Promise.all([
      api.get('/favoritos/artistas', {
        params: { tamanio: TAMANIO_ARTISTAS, orden: 'recientes' },
      }),
      api.get('/favoritos/servicios', {
        params: { tamanio: TAMANIO_SERVICIOS, orden: 'recientes', estado: 'todos' },
      }),
    ])
    artistas.value = resArtistas.data?.contenido || []
    totalArtistas.value = resArtistas.data?.totalElementos || 0
    servicios.value = resServicios.data?.contenido || []
    totalServicios.value = resServicios.data?.totalElementos || 0
  } catch (e) {
    if (e.response?.status === 401) {
      router.push({ name: 'login' })
      return
    }
    errorCarga.value = 'No se pudieron cargar tus favoritos.'
  } finally {
    cargando.value = false
  }
}

async function quitarArtista(artista) {
  const confirmar = window.confirm(
    `¿Quitar a @${artista.nombreUsuario} de tus favoritos?`,
  )
  if (!confirmar || quitandoArtista.value) return

  quitandoArtista.value = artista.idUsuario
  try {
    await api.delete(`/favoritos/artistas/${encodeURIComponent(artista.nombreUsuario)}`)
    artistas.value = artistas.value.filter(
      (item) => item.idUsuario !== artista.idUsuario,
    )
    totalArtistas.value = Math.max(0, totalArtistas.value - 1)
    exito(`Quitaste a @${artista.nombreUsuario} de tus favoritos.`)
  } catch (e) {
    if (e.response?.status === 401) {
      router.push({ name: 'login' })
      return
    }
    error(e.response?.data?.message || 'No se pudo quitar al artista de tus favoritos.')
  } finally {
    quitandoArtista.value = null
  }
}

async function quitarServicio(favorito) {
  const nombre = favorito?.servicio?.nombre || 'este servicio'
  const confirmar = window.confirm(`¿Quitar "${nombre}" de tus favoritos?`)
  if (!confirmar || quitandoServicio.value) return

  quitandoServicio.value = favorito.idServicio
  try {
    await api.delete(`/favoritos/servicios/${favorito.idServicio}`)
    servicios.value = servicios.value.filter(
      (item) => item.idServicio !== favorito.idServicio,
    )
    totalServicios.value = Math.max(0, totalServicios.value - 1)
    exito(`Quitaste "${nombre}" de tus favoritos.`)
  } catch (e) {
    if (e.response?.status === 401) {
      router.push({ name: 'login' })
      return
    }
    error(e.response?.data?.message || 'No se pudo quitar el servicio de tus favoritos.')
  } finally {
    quitandoServicio.value = null
  }
}

onMounted(cargarFavoritos)
</script>

<template>
  <div class="mx-auto max-w-screen-2xl px-4 py-10 sm:px-8 sm:py-14">
    <div class="mb-8 flex flex-col gap-6 sm:mb-10 sm:flex-row sm:items-end sm:justify-between">
      <div class="flex flex-col gap-2">
        <p class="text-sm font-semibold uppercase tracking-[0.18em] text-coral">Tu colección</p>
        <h1 class="font-display text-3xl font-extrabold tracking-[-0.02em] text-granate sm:text-4xl">
          Favoritos
        </h1>
        <p class="max-w-2xl text-sm leading-relaxed text-cafe/70">
          Tu selección personal de artistas y servicios, con accesos rápidos a cada colección.
        </p>
      </div>

      <FavoritosNav />
    </div>

    <div v-if="cargando" class="space-y-10" aria-label="Cargando favoritos">
      <div v-for="bloque in 2" :key="bloque" class="animate-pulse">
        <div class="h-5 w-32 rounded-full bg-rosa/40"></div>
        <div class="mt-4 grid grid-cols-2 gap-4 sm:grid-cols-3 lg:grid-cols-4 xl:grid-cols-6">
          <div v-for="item in 6" :key="item" class="h-24 rounded-2xl bg-rosa/25"></div>
        </div>
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
      v-else-if="!hayFavoritos"
      class="rounded-3xl border border-dashed border-borde bg-superficie px-6 py-16 text-center"
    >
      <div class="mx-auto flex h-14 w-14 items-center justify-center rounded-full bg-rosa/40 text-granate">
        <svg class="h-7 w-7" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" aria-hidden="true">
          <path stroke-linecap="round" stroke-linejoin="round" d="m12 3 2.78 5.63 6.22.9-4.5 4.39 1.06 6.2L12 17.2l-5.56 2.92 1.06-6.2L3 9.53l6.22-.9L12 3Z" />
        </svg>
      </div>
      <h2 class="mt-4 font-display text-xl font-bold text-granate">Todavía no tenés favoritos</h2>
      <p class="mx-auto mt-2 max-w-md text-sm text-cafe/70">
        Explorá artistas y servicios y tocá la estrella para guardarlos acá.
      </p>
      <router-link
        to="/servicios"
        class="mt-6 inline-flex min-h-10 items-center rounded-full bg-coral px-5 text-sm font-bold text-white shadow-sm transition hover:bg-rojizo"
      >
        Explorar servicios
      </router-link>
    </div>

    <template v-else>
      <section class="mb-12">
        <div class="mb-5 flex flex-wrap items-end justify-between gap-3">
          <div>
            <h2 class="font-display text-2xl font-bold text-granate">Artistas</h2>
            <p class="mt-1 text-sm text-cafe/60">
              {{ totalArtistas }}
              {{ totalArtistas === 1 ? 'artista guardado' : 'artistas guardados' }}
            </p>
          </div>

          <router-link
            v-if="totalArtistas > artistas.length"
            :to="{ name: 'favoritos-artistas' }"
            class="text-sm font-semibold text-coral underline decoration-coral/40 underline-offset-4 transition hover:text-rojizo"
          >
            Ver todos ({{ totalArtistas }}) →
          </router-link>
        </div>

        <ul
          v-if="artistas.length"
          class="grid grid-cols-2 gap-x-3 gap-y-8 sm:grid-cols-3 sm:gap-x-6 sm:gap-y-10 lg:grid-cols-4 xl:grid-cols-6"
        >
          <TarjetaArtistaFavorito
            v-for="artista in artistas"
            :key="artista.idUsuario"
            :artista="artista"
            :quitando="quitandoArtista === artista.idUsuario"
            @quitar="quitarArtista"
          />
        </ul>

        <p
          v-else
          class="rounded-2xl border border-dashed border-borde bg-superficie px-5 py-8 text-center text-sm text-cafe/60"
        >
          Todavía no guardaste artistas. Tocá “Favorito” en el perfil de un artista.
        </p>
      </section>

      <section>
        <div class="mb-5 flex flex-wrap items-end justify-between gap-3">
          <div>
            <h2 class="font-display text-2xl font-bold text-granate">Servicios</h2>
            <p class="mt-1 text-sm text-cafe/60">
              {{ totalServicios }}
              {{ totalServicios === 1 ? 'servicio guardado' : 'servicios guardados' }}
            </p>
          </div>

          <router-link
            v-if="totalServicios > servicios.length"
            :to="{ name: 'favoritos-servicios' }"
            class="text-sm font-semibold text-coral underline decoration-coral/40 underline-offset-4 transition hover:text-rojizo"
          >
            Ver todos ({{ totalServicios }}) →
          </router-link>
        </div>

        <div
          v-if="servicios.length"
          class="grid grid-cols-1 gap-6 sm:grid-cols-2 xl:grid-cols-3"
        >
          <TarjetaServicioFavorito
            v-for="favorito in servicios"
            :key="favorito.idServicio"
            :favorito="favorito"
            :quitando="quitandoServicio === favorito.idServicio"
            @quitar="quitarServicio"
          />
        </div>

        <p
          v-else
          class="rounded-2xl border border-dashed border-borde bg-superficie px-5 py-8 text-center text-sm text-cafe/60"
        >
          Todavía no guardaste servicios. Tocá la estrella en un servicio para verlo acá.
        </p>
      </section>
    </template>
  </div>
</template>
