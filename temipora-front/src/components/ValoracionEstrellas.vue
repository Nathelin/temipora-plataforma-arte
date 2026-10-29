<script setup>
import { computed, useId } from 'vue'

const props = defineProps({
  modelValue: { type: Number, default: 0 },
  readonly: { type: Boolean, default: false },
  etiqueta: { type: String, default: 'Calificación' },
  tamano: { type: String, default: 'md' },
})

const emit = defineEmits(['update:modelValue'])

const nombreGrupo = `estrellas-${useId()}`
const claseTamano = computed(() => (props.tamano === 'sm' ? 'h-4 w-4' : 'h-7 w-7'))

function elegir(valor) {
  if (!props.readonly) emit('update:modelValue', valor)
}
</script>

<template>
  <fieldset class="inline-flex items-center">
    <legend class="sr-only">{{ etiqueta }}</legend>
    <div class="flex items-center gap-0.5">
      <template v-if="readonly">
        <span
          v-for="n in 5"
          :key="n"
          class="inline-flex items-center justify-center"
          aria-hidden="true"
        >
          <svg
            viewBox="0 0 20 20"
            fill="currentColor"
            :class="[claseTamano, n <= modelValue ? 'text-coral' : 'text-cafe/20']"
          >
            <path d="M10 1.5l2.6 5.27 5.82.85-4.21 4.1.99 5.79L10 14.78l-5.2 2.73.99-5.79L1.58 7.62l5.82-.85L10 1.5z" />
          </svg>
        </span>
        <span class="sr-only">{{ modelValue }} de 5 estrellas</span>
      </template>

      <template v-else>
        <label
          v-for="n in 5"
          :key="n"
          class="inline-flex cursor-pointer items-center justify-center rounded p-1 transition focus-within:ring-2 focus-within:ring-coral/60"
        >
          <input
            type="radio"
            class="sr-only"
            :name="nombreGrupo"
            :value="n"
            :checked="modelValue === n"
            @change="elegir(n)"
          />
          <svg
            viewBox="0 0 20 20"
            fill="currentColor"
            aria-hidden="true"
            :class="[claseTamano, n <= modelValue ? 'text-coral' : 'text-cafe/20']"
          >
            <path d="M10 1.5l2.6 5.27 5.82.85-4.21 4.1.99 5.79L10 14.78l-5.2 2.73.99-5.79L1.58 7.62l5.82-.85L10 1.5z" />
          </svg>
          <span class="sr-only">{{ n }} de 5 estrellas</span>
        </label>
      </template>
    </div>
  </fieldset>
</template>
