<script setup>
import { computed } from 'vue'
import { etiquetaReputacionDe } from '../servicios/resenias'

const props = defineProps({
  reputacion: { type: Object, default: null },
})

const info = computed(() => etiquetaReputacionDe(props.reputacion?.etiqueta))
</script>

<template>
  <span
    v-if="reputacion"
    class="inline-flex items-center gap-1 rounded-full border px-2.5 py-0.5 text-[11px] font-semibold"
    :class="info.clase"
    :title="`Basado en ${reputacion.cantidadResenias} reseña(s) de artistas. Los comentarios son privados.`"
  >
    {{ info.etiqueta }}
    <span class="font-normal opacity-75">{{ reputacion.cantidadResenias }}</span>
  </span>
</template>
