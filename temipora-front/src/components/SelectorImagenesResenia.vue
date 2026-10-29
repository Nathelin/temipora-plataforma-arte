<script setup>
import ImagenGaleria from './ImagenGaleria.vue'

const props = defineProps({
  modelValue: { type: Array, default: () => [] },
  imagenes: { type: Array, default: () => [] },
  max: { type: Number, default: 0 },
})

const emit = defineEmits(['update:modelValue'])

function seleccionada(idArchivo) {
  return props.modelValue.includes(idArchivo)
}

function alternar(idArchivo) {
  const actuales = [...props.modelValue]
  const indice = actuales.indexOf(idArchivo)
  if (indice >= 0) {
    actuales.splice(indice, 1)
  } else {
    if (props.max > 0 && actuales.length >= props.max) return
    actuales.push(idArchivo)
  }
  emit('update:modelValue', actuales)
}
</script>

<template>
  <div>
    <div class="flex items-baseline justify-between gap-2">
      <p class="text-sm text-cafe">
        Imágenes del pedido <span class="text-cafe/40">(opcional)</span>
      </p>
      <p class="text-xs text-cafe/50">{{ modelValue.length }}/{{ max }}</p>
    </div>
    <p class="mt-1 text-xs leading-relaxed text-cafe/55">
      Elegí hasta {{ max }} imágenes de la entrega final. Se van a mostrar junto a tu reseña.
    </p>

    <div class="mt-3 grid grid-cols-3 gap-2 sm:grid-cols-4">
      <button
        v-for="imagen in imagenes"
        :key="imagen.idArchivo"
        type="button"
        class="relative aspect-square overflow-hidden rounded-lg border-2 transition disabled:cursor-not-allowed disabled:opacity-50"
        :class="seleccionada(imagen.idArchivo)
          ? 'border-coral ring-2 ring-coral/30'
          : 'border-transparent hover:border-rosa'"
        :aria-pressed="seleccionada(imagen.idArchivo)"
        :disabled="!seleccionada(imagen.idArchivo) && modelValue.length >= max"
        @click="alternar(imagen.idArchivo)"
      >
        <ImagenGaleria :url="imagen.urlPreview" alt="" class="h-full w-full" />
        <span
          v-if="seleccionada(imagen.idArchivo)"
          class="absolute right-1 top-1 flex h-5 w-5 items-center justify-center rounded-full bg-coral text-[11px] font-bold text-white"
        >
          ✓
        </span>
      </button>
    </div>
  </div>
</template>
