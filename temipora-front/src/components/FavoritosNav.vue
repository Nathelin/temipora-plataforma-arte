<script setup>
import { useRoute } from 'vue-router'

const route = useRoute()

const secciones = [
  { name: 'favoritos', etiqueta: 'Resumen' },
  { name: 'favoritos-artistas', etiqueta: 'Artistas' },
  { name: 'favoritos-servicios', etiqueta: 'Servicios' },
]

function activa(name) {
  return route.name === name
}
</script>

<template>
  <nav
    class="inline-flex flex-wrap rounded-full border border-borde/60 bg-superficie p-1 shadow-sm"
    aria-label="Secciones de favoritos"
  >
    <router-link
      v-for="seccion in secciones"
      :key="seccion.name"
      :to="{ name: seccion.name }"
      class="rounded-full px-3.5 py-1.5 text-xs font-semibold transition"
      :class="activa(seccion.name) ? 'bg-coral text-white shadow-sm' : 'text-cafe/60 hover:text-granate'"
      :aria-current="activa(seccion.name) ? 'page' : undefined"
    >
      {{ seccion.etiqueta }}
    </router-link>
  </nav>
</template>
