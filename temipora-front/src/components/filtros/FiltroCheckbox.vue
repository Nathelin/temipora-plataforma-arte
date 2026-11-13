<script setup>
import { computed } from 'vue'

const props = defineProps({
  opciones: { type: Array, required: true },
  seleccionados: { type: Array, default: () => [] },
  conteos: { type: Object, default: () => ({}) },
  claveValor: { type: String, default: 'valor' },
  claveTexto: { type: String, default: 'texto' },
  /**
   * Habilita el botón "−" para excluir una opción, en vez de requerir un
   * segundo listado de checkboxes. `null` = sin exclusión.
   */
  excluidos: { type: Array, default: null },
})

const emit = defineEmits(['cambiar', 'cambiar-excluidos'])

const seleccion = computed(() => new Set((props.seleccionados || []).map(String)))
const exclusion = computed(() => new Set((props.excluidos || []).map(String)))
const admiteExclusion = computed(() => Array.isArray(props.excluidos))

function cantidad(opcion) {
  const valor = props.conteos?.[String(opcion[props.claveValor])]
  return Number.isFinite(Number(valor)) ? Number(valor) : null
}

function deshabilitada(opcion) {
  const valor = String(opcion[props.claveValor])
  if (seleccion.value.has(valor) || exclusion.value.has(valor)) return false
  return cantidad(opcion) === 0
}

function alternar(opcion) {
  const valor = String(opcion[props.claveValor])
  if (seleccion.value.has(valor)) {
    emit('cambiar', (props.seleccionados || []).filter((v) => String(v) !== valor))
    return
  }
  if (exclusion.value.has(valor)) {
    // Pasar de excluido a incluido: un valor no puede estar en las dos listas.
    emit('cambiar-excluidos',
      (props.excluidos || []).filter((v) => String(v) !== valor))
  }
  emit('cambiar', [...(props.seleccionados || []), valor])
}

function alternarExclusion(opcion) {
  const valor = String(opcion[props.claveValor])
  if (exclusion.value.has(valor)) {
    emit('cambiar-excluidos',
      (props.excluidos || []).filter((v) => String(v) !== valor))
    return
  }
  if (seleccion.value.has(valor)) {
    emit('cambiar', (props.seleccionados || []).filter((v) => String(v) !== valor))
  }
  emit('cambiar-excluidos', [...(props.excluidos || []), valor])
}
</script>

<template>
  <ul class="space-y-0.5">
    <li v-for="opcion in opciones" :key="opcion[claveValor]">
      <div
        class="flex items-center gap-1 rounded-lg px-1.5 py-1.5 transition"
        :class="deshabilitada(opcion) ? '' : 'hover:bg-rosa/20'"
      >
        <label
          class="flex min-w-0 flex-1 items-start gap-2 text-sm"
          :class="deshabilitada(opcion)
            ? 'cursor-not-allowed text-cafe/35'
            : 'cursor-pointer text-cafe'"
        >
          <input
            type="checkbox"
            class="mt-0.5 h-4 w-4 shrink-0 rounded border-borde text-coral focus:ring-coral disabled:opacity-50"
            :checked="seleccion.has(String(opcion[claveValor]))"
            :disabled="deshabilitada(opcion)"
            @change="alternar(opcion)"
          />
          <span
            class="min-w-0 flex-1 break-words leading-snug"
            :class="exclusion.has(String(opcion[claveValor])) ? 'line-through opacity-50' : ''"
          >
            {{ opcion[claveTexto] }}
          </span>
          <span v-if="cantidad(opcion) !== null" class="shrink-0 text-[11px] text-cafe/45">
            ({{ cantidad(opcion) }})
          </span>
        </label>
        <button
          v-if="admiteExclusion"
          type="button"
          class="inline-flex h-6 min-w-6 shrink-0 items-center justify-center rounded-full border text-xs font-bold transition focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-coral/60"
          :class="exclusion.has(String(opcion[claveValor]))
            ? 'border-rojizo bg-rojizo text-white'
            : 'border-coral/40 bg-coral/10 text-coral hover:border-coral hover:bg-coral hover:text-white'"
          :aria-label="exclusion.has(String(opcion[claveValor]))
            ? `Dejar de excluir ${opcion[claveTexto]}`
            : `Excluir ${opcion[claveTexto]}`"
          :title="exclusion.has(String(opcion[claveValor])) ? 'Dejar de excluir' : 'Excluir'"
          @click="alternarExclusion(opcion)"
        >
          −
        </button>
      </div>
    </li>
  </ul>
</template>
