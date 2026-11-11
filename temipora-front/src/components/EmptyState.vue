<script setup>
defineProps({
  titulo: { type: String, required: true },
  detalle: { type: String, default: '' },
})
</script>

<template>
  <div class="mt-8 rounded-3xl border border-dashed border-rosa/70 bg-blanco px-6 py-16 text-center">
    <p class="text-lg font-semibold text-granate">{{ titulo }}</p>
    <p v-if="detalle" class="mx-auto mt-2 max-w-md text-sm leading-relaxed text-cafe/60">
      {{ detalle }}
    </p>
    <div v-if="$slots.accion" class="mt-5 flex justify-center">
      <slot name="accion" />
    </div>
  </div>
</template>
