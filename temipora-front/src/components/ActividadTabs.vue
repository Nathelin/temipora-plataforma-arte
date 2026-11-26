<script setup>
import { useRoute } from 'vue-router'

const route = useRoute()

const tabs = [
  { etiqueta: 'Resumen', to: '/mi-actividad', exacta: true },
  { etiqueta: 'Historial', to: '/mi-actividad/historial', exacta: false },
]

const esActiva = (tab) => (tab.exacta
  ? route.path === tab.to
  : route.path.startsWith(tab.to))
</script>

<template>
  <nav class="mb-6 border-b border-rosa/50" aria-label="Secciones de Mi actividad">
    <ul class="-mb-px flex gap-1 overflow-x-auto">
      <li v-for="tab in tabs" :key="tab.to" class="shrink-0">
        <router-link
          :to="tab.to"
          class="relative inline-flex min-h-10 items-center gap-2 rounded-t-lg px-3 text-sm font-medium transition-colors motion-reduce:transition-none focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-inset focus-visible:ring-coral/60"
          :class="esActiva(tab) ? 'text-granate' : 'text-cafe/55 hover:text-granate'"
          :aria-current="esActiva(tab) ? 'page' : null"
        >
          {{ tab.etiqueta }}
          <span
            v-if="esActiva(tab)"
            class="absolute inset-x-2 -bottom-px h-0.5 rounded-full bg-coral"
            aria-hidden="true"
          ></span>
        </router-link>
      </li>
    </ul>
  </nav>
</template>
