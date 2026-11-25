<script setup>
import { computed } from 'vue'

const props = defineProps({
  tamano: { type: String, default: 'sm' },
  etiqueta: { type: String, default: 'Cargando' },
})

const TAMANIOS = {
  sm: 'h-4 w-4 border-2',
  md: 'h-5 w-5 border-2',
  lg: 'h-8 w-8 border-[3px]',
}

const clases = computed(() => TAMANIOS[props.tamano] || TAMANIOS.sm)
</script>

<template>
  <span
    class="inline-block animate-spin rounded-full border-current border-t-transparent motion-reduce:animate-none"
    :class="clases"
    role="status"
    :aria-label="etiqueta"
  ></span>
</template>
