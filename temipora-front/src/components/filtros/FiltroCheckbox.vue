<script setup>
import { computed } from 'vue'

const props = defineProps({
  opciones: { type: Array, required: true },
  seleccionados: { type: Array, default: () => [] },
  conteos: { type: Object, default: () => ({}) },
  claveValor: { type: String, default: 'valor' },
  claveTexto: { type: String, default: 'texto' },
})

const emit = defineEmits(['cambiar'])

const seleccion = computed(() => new Set((props.seleccionados || []).map(String)))

function cantidad(opcion) {
  const valor = props.conteos?.[String(opcion[props.claveValor])]
  return Number.isFinite(Number(valor)) ? Number(valor) : null
}

function deshabilitada(opcion) {
  if (seleccion.value.has(String(opcion[props.claveValor]))) return false
  const total = cantidad(opcion)
  return total === 0
}

function alternar(opcion) {
  const valor = String(opcion[props.claveValor])
  const siguientes = seleccion.value.has(valor)
    ? (props.seleccionados || []).filter((v) => String(v) !== valor)
    : [...(props.seleccionados || []), valor]
  emit('cambiar', siguientes)
}
</script>

<template>
  <ul class="space-y-0.5">
    <li v-for="opcion in opciones" :key="opcion[claveValor]">
      <label
        class="flex cursor-pointer items-start gap-2 rounded-lg px-1.5 py-1.5 text-sm transition"
        :class="deshabilitada(opcion)
          ? 'cursor-not-allowed text-cafe/35'
          : 'text-cafe hover:bg-rosa/20'"
      >
        <input
          type="checkbox"
          class="mt-0.5 h-4 w-4 shrink-0 rounded border-borde text-coral focus:ring-coral disabled:opacity-50"
          :checked="seleccion.has(String(opcion[claveValor]))"
          :disabled="deshabilitada(opcion)"
          @change="alternar(opcion)"
        />
        <span class="min-w-0 flex-1 break-words leading-snug">
          {{ opcion[claveTexto] }}
        </span>
        <span v-if="cantidad(opcion) !== null" class="shrink-0 text-[11px] text-cafe/45">
          ({{ cantidad(opcion) }})
        </span>
      </label>
    </li>
  </ul>
</template>
