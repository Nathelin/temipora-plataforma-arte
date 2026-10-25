<script setup>
defineProps({
  artista: { type: Object, required: true },
  quitando: { type: Boolean, default: false },
})

const emit = defineEmits(['quitar'])

function nombreArtista(artista) {
  return artista.nombreDisplay || artista.nombreUsuario || ''
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
</script>

<template>
  <li class="favorito-artista group flex min-w-0 flex-col items-center">
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
        :disabled="quitando"
        :aria-label="`Quitar a @${artista.nombreUsuario} de tus favoritos`"
        title="Quitar de favoritos"
        @click="emit('quitar', artista)"
      >
        <svg
          v-if="!quitando"
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
