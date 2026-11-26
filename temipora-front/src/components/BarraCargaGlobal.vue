<script setup>
import { useCargaGlobal } from '../composables/useCargaGlobal'

const { activa } = useCargaGlobal()
</script>

<template>
  <div
    v-if="activa"
    class="fixed inset-x-0 top-0 z-[120] h-0.5 overflow-hidden bg-rosa/40"
    role="status"
    aria-live="polite"
  >
    <div class="barra-carga-global h-full w-1/3 bg-coral"></div>
    <span class="sr-only">Cargando…</span>
  </div>
</template>
