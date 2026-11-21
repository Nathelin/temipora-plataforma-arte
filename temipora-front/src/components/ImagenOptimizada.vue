<script setup>
import { computed } from 'vue'

const props = defineProps({
  src: { type: String, default: '' },
  alt: { type: String, default: '' },
  sizes: { type: String, default: '100vw' },
  prioridad: { type: Boolean, default: false },
  ancho: { type: [Number, String], default: null },
  alto: { type: [Number, String], default: null },
})

const ANCHOS = [128, 320, 480, 640, 960, 1280]
const PATRON = /^\/uploads\/([0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12})\.webp$/

// Solo reescribimos las imágenes públicas propias. Cualquier otra URL
// (galerías privadas, portadas externas) se muestra tal cual.
const variantes = computed(() => {
  const coincidencia = typeof props.src === 'string' ? props.src.match(PATRON) : null
  if (!coincidencia) return null
  return ANCHOS
    .map((ancho) => `/uploads/${ancho}/${coincidencia[1]}.webp ${ancho}w`)
    .join(', ')
})

const dimensiones = computed(() => {
  const valores = {}
  if (props.ancho != null) valores.width = props.ancho
  if (props.alto != null) valores.height = props.alto
  return valores
})
</script>

<template>
  <img
    v-bind="dimensiones"
    :src="src"
    :srcset="variantes || undefined"
    :sizes="variantes ? sizes : undefined"
    :alt="alt"
    :loading="prioridad ? 'eager' : 'lazy'"
    :fetchpriority="prioridad ? 'high' : undefined"
    decoding="async"
  />
</template>
