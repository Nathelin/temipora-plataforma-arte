<script setup>
import TarjetaServicio from './TarjetaServicio.vue'

defineProps({
  favorito: { type: Object, required: true },
  quitando: { type: Boolean, default: false },
})

const emit = defineEmits(['quitar'])

function estadoServicio(favorito) {
  const estado = favorito?.servicio?.estado
  if (estado === 'PAUSADO') return 'Pausado por el artista'
  if (estado === 'ARCHIVADO') return 'Archivado'
  if (favorito?.servicio?.disponibleAhora === false) return 'Sin cupos'
  return 'No disponible'
}

function esSinCupos(favorito) {
  return favorito?.servicio?.estado === 'PUBLICADO'
    && favorito?.servicio?.disponibleAhora === false
}
</script>

<template>
  <TarjetaServicio
    v-if="favorito.disponible && favorito.servicio"
    :servicio="favorito.servicio"
    mostrar-favorito
    es-favorito
    :actualizando-favorito="quitando"
    @alternar-favorito="emit('quitar', favorito)"
  />

  <article
    v-else
    class="flex h-full flex-col overflow-hidden rounded-[22px] border border-borde/60 bg-superficie shadow-[0_10px_28px_rgba(74,29,46,0.08)]"
  >
    <div class="flex aspect-video items-center justify-center bg-rosa/15 px-6 text-center">
      <div>
        <div class="mx-auto flex h-10 w-10 items-center justify-center rounded-full bg-rosa/40 text-granate">
          <svg class="h-5 w-5" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" aria-hidden="true">
            <path stroke-linecap="round" stroke-linejoin="round" d="M12 9v4m0 4h.01M10.29 3.86 1.82 18a2 2 0 0 0 1.71 3h16.94a2 2 0 0 0 1.71-3L13.71 3.86a2 2 0 0 0-3.42 0Z" />
          </svg>
        </div>
        <p class="mt-2 text-sm font-semibold text-granate">Servicio no disponible</p>
        <span class="mt-1 inline-block rounded-full bg-rosa/40 px-3 py-1 text-xs text-cafe/70">
          {{ estadoServicio(favorito) }}
        </span>
      </div>
    </div>

    <div class="flex flex-1 flex-col p-4">
      <h3 class="line-clamp-1 font-display text-base font-bold leading-snug text-granate">
        {{ favorito.servicio?.nombre || 'Servicio' }}
      </h3>
      <router-link
        v-if="favorito.servicio?.nombreArtista"
        :to="{ name: 'perfil', params: { nombreUsuario: favorito.servicio.nombreArtista } }"
        class="mt-2 w-fit text-left text-xs font-semibold text-cafe/60 transition hover:text-coral"
      >
        Por @{{ favorito.servicio.nombreArtista }}
      </router-link>
      <p class="mt-2 text-xs leading-relaxed text-cafe/70">
        <template v-if="esSinCupos(favorito)">
          El artista no tiene cupos libres en este momento. Podés buscar otra
          propuesta parecida.
        </template>
        <template v-else>
          El artista pausó o archivó este servicio. Podés quitarlo de tus favoritos.
        </template>
      </p>

      <div class="mt-auto flex flex-wrap justify-end gap-2 border-t border-borde/40 pt-3">
        <router-link
          v-if="esSinCupos(favorito)"
          :to="{ name: 'servicio-alternativas', params: { id: favorito.idServicio }, query: { disparador: 'FAVORITOS' } }"
          class="inline-flex items-center gap-1.5 rounded-full bg-coral px-3 py-1.5 text-xs font-bold text-white transition hover:bg-rojizo"
        >
          Ver artistas con cupo
        </router-link>
        <button
          type="button"
          class="inline-flex items-center gap-1.5 rounded-full border border-coral/40 px-3 py-1.5 text-xs font-bold text-coral transition hover:bg-coral hover:text-white disabled:cursor-wait disabled:opacity-60"
          :disabled="quitando"
          @click="emit('quitar', favorito)"
        >
          {{ quitando ? 'Quitando...' : 'Quitar de favoritos' }}
        </button>
      </div>
    </div>
  </article>
</template>
