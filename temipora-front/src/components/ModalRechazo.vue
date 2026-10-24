<script setup>
import { nextTick, ref, watch } from 'vue'

const MAXIMO = 200

const props = defineProps({
  abierto: { type: Boolean, default: false },
  cargando: { type: Boolean, default: false },
  error: { type: String, default: '' },
})

const emit = defineEmits(['cerrar', 'confirmar'])

const motivo = ref('')
const campoMotivo = ref(null)

watch(
  () => props.abierto,
  async (abierto) => {
    if (!abierto) return
    motivo.value = ''
    await nextTick()
    campoMotivo.value?.focus()
  },
)

function cerrar() {
  if (props.cargando) return
  emit('cerrar')
}

function confirmar() {
  if (props.cargando) return
  emit('confirmar', motivo.value.trim())
}
</script>

<template>
  <div
    v-if="abierto"
    class="fixed inset-0 z-[90] flex items-center justify-center p-4"
    role="dialog"
    aria-modal="true"
    aria-labelledby="titulo-modal-rechazo"
    @keydown.esc="cerrar"
  >
    <div class="absolute inset-0 bg-granate/40" aria-hidden="true" @click="cerrar"></div>

    <div class="relative w-full max-w-md rounded-xl border border-rosa bg-blanco p-6 shadow-xl shadow-granate/20">
      <h2 id="titulo-modal-rechazo" class="text-lg text-granate">Rechazar solicitud</h2>
      <p class="mt-2 text-sm leading-relaxed text-cafe/70">
        El cliente recibirá una notificación con tu decisión. Si no escribís un motivo,
        se le informará que preferiste no mencionarlo.
      </p>

      <label for="motivo-rechazo" class="mt-4 block text-sm font-medium text-cafe">
        Motivo <span class="font-normal text-cafe/50">(opcional)</span>
      </label>
      <textarea
        id="motivo-rechazo"
        ref="campoMotivo"
        v-model="motivo"
        rows="3"
        :maxlength="MAXIMO"
        placeholder="Contale al cliente por qué no podés tomar esta solicitud..."
        class="mt-2 w-full resize-none rounded-lg border border-rosa/70 bg-blanco px-3 py-2 text-sm text-cafe transition placeholder:text-cafe/40 focus:border-coral focus:outline-none focus:ring-2 focus:ring-coral/20"
      ></textarea>

      <div class="mt-1 flex items-start justify-between gap-3 text-[11px] text-cafe/50">
        <span>Este texto va a figurar en el detalle de la solicitud.</span>
        <span class="shrink-0">{{ motivo.length }}/{{ MAXIMO }}</span>
      </div>

      <p
        v-if="error"
        class="mt-3 rounded-lg border border-rojizo/25 bg-rojizo/5 px-3 py-2 text-sm text-rojizo"
        role="alert"
      >
        {{ error }}
      </p>

      <div class="mt-6 flex flex-col-reverse gap-3 sm:flex-row sm:justify-end">
        <button
          type="button"
          class="rounded-md border border-rosa px-4 py-2 text-sm text-cafe transition hover:bg-rosa/20 disabled:cursor-not-allowed disabled:opacity-50"
          :disabled="cargando"
          @click="cerrar"
        >
          Volver
        </button>
        <button
          type="button"
          class="rounded-md bg-rojizo px-5 py-2.5 text-sm font-semibold text-white shadow-sm transition hover:bg-granate disabled:cursor-not-allowed disabled:opacity-60"
          :disabled="cargando"
          @click="confirmar"
        >
          {{ cargando ? 'Rechazando...' : 'Rechazar solicitud' }}
        </button>
      </div>
    </div>
  </div>
</template>
