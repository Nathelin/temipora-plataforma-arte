<script setup>
import { computed, nextTick, ref, watch } from 'vue'
import { formatearFecha, sumarDiasFecha } from '../servicios/pedidos'

const MAX_MOTIVO = 500
const MAX_DIAS = 365

const props = defineProps({
  abierto: { type: Boolean, default: false },
  pedido: { type: Object, default: null },
  procesando: { type: Boolean, default: false },
  error: { type: String, default: '' },
})

const emit = defineEmits(['cerrar', 'solicitar'])

const dias = ref('7')
const motivo = ref('')
const errorLocal = ref('')
const campoDias = ref(null)

const entregaVigente = computed(
  () => props.pedido?.fechaEntregaPrevista || props.pedido?.limiteEntrega || null,
)
const nuevaEntrega = computed(() => {
  const valor = Number(dias.value)
  if (!Number.isInteger(valor) || valor < 1 || valor > MAX_DIAS) return null
  return sumarDiasFecha(entregaVigente.value, valor)
})

watch(
  () => props.abierto,
  async (abierto) => {
    if (!abierto) return
    dias.value = '7'
    motivo.value = ''
    errorLocal.value = ''
    await nextTick()
    campoDias.value?.focus()
  },
)

function cerrar() {
  if (props.procesando) return
  emit('cerrar')
}

function enviar() {
  if (props.procesando) return
  errorLocal.value = ''
  const valor = Number(dias.value)
  if (!Number.isInteger(valor) || valor < 1 || valor > MAX_DIAS) {
    errorLocal.value = `Indicá un plazo entre 1 y ${MAX_DIAS} días.`
    return
  }
  if (!motivo.value.trim()) {
    errorLocal.value = 'Contale al cliente por qué necesitás más plazo.'
    return
  }
  emit('solicitar', { dias: valor, motivo: motivo.value.trim() })
}
</script>

<template>
  <div
    v-if="abierto"
    class="fixed inset-0 z-[95] flex items-center justify-center p-4"
    role="dialog"
    aria-modal="true"
    aria-labelledby="titulo-solicitar-prorroga"
  >
    <div class="absolute inset-0 bg-granate/50" aria-hidden="true" @click="cerrar"></div>

    <div class="relative w-full max-w-lg rounded-2xl border border-rosa bg-blanco p-6 shadow-xl shadow-granate/20">
      <p class="text-xs font-semibold uppercase tracking-[0.16em] text-coral">
        Plazo del pedido
      </p>
      <h2 id="titulo-solicitar-prorroga" class="mt-1 font-display text-xl font-bold text-granate">
        Solicitar más plazo
      </h2>
      <p class="mt-2 text-sm leading-relaxed text-cafe/70">
        El cliente tiene que aceptar la nueva fecha. Si no responde, se mantiene el plazo
        original. Solo se puede pedir una prórroga por pedido.
      </p>

      <div class="mt-4 grid gap-2 rounded-xl bg-rosa/10 p-4 text-sm sm:grid-cols-2">
        <div>
          <p class="text-xs text-cafe/50">Entrega vigente</p>
          <p class="mt-0.5 font-semibold text-cafe">
            {{ entregaVigente ? formatearFecha(entregaVigente) : 'A confirmar' }}
          </p>
        </div>
        <div>
          <p class="text-xs text-cafe/50">Nueva fecha propuesta</p>
          <p class="mt-0.5 font-semibold text-granate">
            {{ nuevaEntrega ? formatearFecha(nuevaEntrega) : '—' }}
          </p>
        </div>
      </div>

      <label for="prorroga-dias" class="mt-4 block text-sm font-medium text-cafe">
        Días adicionales <span class="text-rojizo">*</span>
      </label>
      <input
        id="prorroga-dias"
        ref="campoDias"
        v-model="dias"
        type="number"
        min="1"
        :max="MAX_DIAS"
        step="1"
        class="mt-2 block w-full rounded-lg border-0 px-3 py-2 text-sm text-cafe shadow-sm ring-1 ring-inset ring-rosa focus:outline-none focus:ring-2 focus:ring-inset focus:ring-coral"
        @keydown.enter.prevent="enviar"
      />

      <label for="prorroga-motivo" class="mt-4 block text-sm font-medium text-cafe">
        Motivo para el cliente <span class="text-rojizo">*</span>
      </label>
      <textarea
        id="prorroga-motivo"
        v-model="motivo"
        rows="3"
        :maxlength="MAX_MOTIVO"
        placeholder="Contale por qué necesitás más tiempo para terminar la pieza..."
        class="mt-2 w-full resize-none rounded-lg border border-rosa/70 bg-blanco px-3 py-2 text-sm text-cafe transition placeholder:text-cafe/40 focus:border-coral focus:outline-none focus:ring-2 focus:ring-coral/20"
      ></textarea>
      <div class="mt-1 flex justify-end text-[11px] text-cafe/50">
        <span>{{ motivo.length }}/{{ MAX_MOTIVO }}</span>
      </div>

      <p
        v-if="errorLocal || error"
        class="mt-3 rounded-lg border border-rojizo/25 bg-rojizo/5 px-3 py-2 text-sm text-rojizo"
        role="alert"
      >
        {{ errorLocal || error }}
      </p>

      <div class="mt-6 flex flex-col-reverse gap-3 sm:flex-row sm:justify-end">
        <button
          type="button"
          class="rounded-full border border-rosa px-4 py-2 text-sm font-semibold text-cafe transition hover:bg-rosa/20 disabled:opacity-50"
          :disabled="procesando"
          @click="cerrar"
        >
          Cancelar
        </button>
        <button
          type="button"
          class="rounded-full bg-coral px-5 py-2.5 text-sm font-semibold text-white shadow-sm transition hover:bg-rojizo disabled:cursor-not-allowed disabled:opacity-60"
          :disabled="procesando"
          @click="enviar"
        >
          {{ procesando ? 'Enviando...' : 'Enviar al cliente' }}
        </button>
      </div>
    </div>
  </div>
</template>
