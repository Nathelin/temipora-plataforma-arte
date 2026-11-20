<script setup>
defineProps({
  visible: {
    type: Boolean,
    default: false,
  },
  detalle: {
    type: String,
    default: '',
  },
})
</script>

<template>
  <div
    v-if="visible"
    class="fixed inset-0 z-[100] flex items-center justify-center bg-granate/40 p-4 backdrop-blur-sm"
    role="status"
    aria-live="polite"
  >
    <div class="w-full max-w-sm rounded-2xl border border-rosa bg-blanco p-6 text-center shadow-2xl">
      <div
        class="mx-auto h-9 w-9 animate-spin rounded-full border-[3px] border-rosa border-t-coral"
        aria-hidden="true"
      ></div>
      <p class="mt-4 font-display text-base font-bold text-granate">
        Te estamos redirigiendo a MercadoPago...
      </p>
      <p class="mt-1 text-sm leading-relaxed text-cafe/65">
        No cierres esta ventana.
        <template v-if="detalle"> {{ detalle }}</template>
      </p>
    </div>
  </div>
</template>
