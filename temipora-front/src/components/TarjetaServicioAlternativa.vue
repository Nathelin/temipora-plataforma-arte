<script setup>
import TarjetaServicio from './TarjetaServicio.vue'

defineProps({
  opcion: { type: Object, required: true },
  eligiendo: { type: Boolean, default: false },
})

const emit = defineEmits(['solicitar'])
</script>

<template>
  <TarjetaServicio
    :servicio="opcion.servicio"
    :motivo="opcion.motivo"
  >
    <template #acciones>
      <button
        type="button"
        class="inline-flex min-h-9 flex-1 items-center justify-center rounded-full bg-coral px-3 text-xs font-bold text-white transition hover:bg-rojizo focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-coral/60 disabled:cursor-not-allowed disabled:opacity-50"
        :disabled="!opcion.disponible || eligiendo"
        @click="emit('solicitar', opcion)"
      >
        {{ eligiendo ? 'Abriendo...' : 'Solicitar' }}
      </button>
      <router-link
        :to="{ name: 'servicio-detalle', params: { id: opcion.idServicio } }"
        class="inline-flex min-h-9 items-center justify-center rounded-full border border-coral/40 px-3 text-xs font-bold text-coral transition hover:bg-coral/5"
      >
        Ver servicio
      </router-link>
    </template>
  </TarjetaServicio>
</template>
