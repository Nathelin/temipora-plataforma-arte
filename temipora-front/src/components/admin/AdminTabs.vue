<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import { tabsDeRuta } from '../../servicios/adminSecciones'

const props = defineProps({
  tabs: { type: Array, default: null },
})

const route = useRoute()

const items = computed(() => props.tabs ?? tabsDeRuta(route.path))
const esActiva = (to) => route.path === to
</script>

<template>
  <nav v-if="items.length" class="mb-6 border-b border-rosa/50" aria-label="Secciones de la página">
    <ul class="-mb-px flex gap-1 overflow-x-auto">
      <li v-for="tab in items" :key="tab.to" class="shrink-0">
        <router-link
          :to="tab.to"
          class="relative inline-flex min-h-10 items-center gap-2 rounded-t-lg px-3 text-sm font-medium transition-colors motion-reduce:transition-none focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-inset focus-visible:ring-coral/60"
          :class="esActiva(tab.to) ? 'text-granate' : 'text-cafe/55 hover:text-granate'"
          :aria-current="esActiva(tab.to) ? 'page' : null"
        >
          {{ tab.etiqueta }}
          <span
            v-if="esActiva(tab.to)"
            class="absolute inset-x-2 -bottom-px h-0.5 rounded-full bg-coral"
            aria-hidden="true"
          ></span>
        </router-link>
      </li>
    </ul>
  </nav>
</template>
