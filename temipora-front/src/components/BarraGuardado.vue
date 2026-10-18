<script setup>
defineProps({
  hayCambios: { type: Boolean, default: false },
  guardando: { type: Boolean, default: false },
  bloqueado: { type: Boolean, default: false },
  texto: { type: String, default: 'Guardar cambios' },
  textoCambios: { type: String, default: 'Tenés cambios sin guardar' },
})

defineEmits(['guardar'])
</script>

<template>
  <div
    class="sticky bottom-0 z-10 -mx-6 sm:-mx-8 rounded-b-3xl border-t border-rosa/60 bg-blanco px-6 sm:px-8 py-4"
  >
    <div v-if="$slots.aviso" class="mb-3">
      <slot name="aviso" />
    </div>

    <div class="flex flex-col-reverse gap-3 sm:flex-row sm:items-center sm:justify-between">
      <p class="flex min-h-5 items-center gap-2 text-sm" aria-live="polite">
        <template v-if="hayCambios">
          <span class="inline-flex h-2 w-2 shrink-0 rounded-full bg-coral"></span>
          <span class="text-coral">{{ textoCambios }}</span>
        </template>
      </p>

      <div class="flex items-center justify-end gap-3">
        <slot />
        <button
          type="button"
          :disabled="!hayCambios || guardando || bloqueado"
          class="px-5 py-2.5 rounded-md text-sm text-white bg-coral hover:bg-rojizo disabled:opacity-50 disabled:cursor-not-allowed transition shadow-sm min-w-44"
          @click="$emit('guardar')"
        >
          {{ texto }}
        </button>
      </div>
    </div>
  </div>
</template>
