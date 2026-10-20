<script setup>
import { useTema } from '../composables/useTema'

const { esOscuro, alternarTema } = useTema()
</script>

<template>
  <button
    type="button"
    class="inline-flex h-9 w-9 shrink-0 items-center justify-center rounded-full border border-borde/60 bg-superficie/70 text-texto transition hover:bg-borde/30 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-acento/60 sm:h-10 sm:w-10"
    :aria-label="esOscuro ? 'Cambiar a modo claro' : 'Cambiar a modo oscuro'"
    :title="esOscuro ? 'Cambiar a modo claro' : 'Cambiar a modo oscuro'"
    @click="alternarTema"
  >
    <svg v-if="esOscuro" class="h-5 w-5" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
      <circle cx="12" cy="12" r="4" />
      <path d="M12 2v2M12 20v2M4.93 4.93l1.41 1.41M17.66 17.66l1.41 1.41M2 12h2M20 12h2M4.93 19.07l1.41-1.41M17.66 6.34l1.41-1.41" />
    </svg>
    <svg v-else class="h-5 w-5" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
      <path d="M21 12.79A9 9 0 1 1 11.21 3 7 7 0 0 0 21 12.79Z" />
    </svg>
  </button>
</template>
