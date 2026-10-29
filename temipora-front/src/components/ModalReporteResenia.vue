<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { MOTIVOS_REPORTE, reportarResenia } from '../servicios/resenias'

const props = defineProps({
  abierto: { type: Boolean, default: false },
  resenia: { type: Object, default: null },
})

const emit = defineEmits(['cerrar', 'reportado'])

const motivo = ref('')
const comentario = ref('')
const error = ref('')
const enviando = ref(false)
const selectorMotivo = ref(null)

const autorMostrado = computed(() => {
  if (!props.resenia) return ''
  if (props.resenia.anonima) return 'Cliente verificado'
  return props.resenia.autor?.nombreDisplay
    || props.resenia.autor?.nombreUsuario
    || props.resenia.autorMostrado
    || ''
})

watch(
  () => props.abierto,
  (abierto) => {
    if (!abierto) return
    motivo.value = ''
    comentario.value = ''
    error.value = ''
    nextTick(() => selectorMotivo.value?.focus())
  },
)

function cerrar() {
  if (enviando.value) return
  emit('cerrar')
}

async function enviar() {
  if (enviando.value || !motivo.value || !props.resenia?.idResenia) return
  enviando.value = true
  error.value = ''
  try {
    await reportarResenia(props.resenia.idResenia, {
      motivo: motivo.value,
      comentario: comentario.value.trim() || null,
    })
    emit('reportado', props.resenia.idResenia)
    emit('cerrar')
  } catch (e) {
    error.value = e.response?.data?.message
      || 'No se pudo enviar el reporte. Intentá de nuevo.'
  } finally {
    enviando.value = false
  }
}

function manejarTecla(event) {
  if (event.key === 'Escape' && props.abierto) cerrar()
}

onMounted(() => document.addEventListener('keydown', manejarTecla))
onBeforeUnmount(() => document.removeEventListener('keydown', manejarTecla))
</script>

<template>
  <div
    v-if="abierto"
    class="fixed inset-0 z-[90] flex items-center justify-center p-4"
    role="dialog"
    aria-modal="true"
    aria-labelledby="titulo-reporte-resenia"
  >
    <div class="absolute inset-0 bg-granate/40" aria-hidden="true" @click="cerrar"></div>

    <form
      class="relative w-full max-w-lg rounded-2xl border border-rosa bg-blanco p-6 shadow-xl shadow-granate/20"
      @submit.prevent="enviar"
    >
      <h2 id="titulo-reporte-resenia" class="text-lg text-granate">Reportar reseña</h2>
      <p class="mt-1 text-xs leading-relaxed text-cafe/60">
        Un administrador va a revisar el reporte. El autor no sabe quién lo reportó.
      </p>

      <div v-if="resenia" class="mt-4 rounded-xl bg-rosa/10 px-3 py-2 text-xs text-cafe/70">
        <p class="font-semibold text-granate">{{ resenia.nombreServicio || 'Servicio' }}</p>
        <p>{{ autorMostrado }} · {{ resenia.calificacionGeneral }}/5</p>
      </div>

      <div class="mt-4">
        <label for="motivo-reporte" class="mb-2 block text-sm text-cafe">
          Motivo <span class="text-rojizo">*</span>
        </label>
        <select
          id="motivo-reporte"
          ref="selectorMotivo"
          v-model="motivo"
          required
          class="block w-full rounded-lg border-0 px-3 py-2.5 text-sm text-cafe shadow-sm ring-1 ring-inset ring-rosa focus:outline-none focus:ring-2 focus:ring-inset focus:ring-coral"
        >
          <option value="" disabled>Seleccioná un motivo</option>
          <option v-for="item in MOTIVOS_REPORTE" :key="item.valor" :value="item.valor">
            {{ item.etiqueta }}
          </option>
        </select>
      </div>

      <div class="mt-4">
        <label for="comentario-reporte" class="mb-2 block text-sm text-cafe">
          Comentario <span class="text-cafe/40">(opcional)</span>
        </label>
        <textarea
          id="comentario-reporte"
          v-model="comentario"
          rows="3"
          maxlength="500"
          class="block w-full resize-none rounded-lg border-0 px-3 py-2.5 text-sm text-cafe shadow-sm ring-1 ring-inset ring-rosa focus:outline-none focus:ring-2 focus:ring-inset focus:ring-coral"
          placeholder="Contanos brevemente qué problema tiene la reseña."
        ></textarea>
        <p class="mt-1 text-right text-[11px] text-cafe/40">{{ comentario.length }}/500</p>
      </div>

      <p v-if="error" role="alert" class="mt-3 text-sm text-rojizo">{{ error }}</p>

      <div class="mt-6 flex flex-col-reverse gap-3 sm:flex-row sm:justify-end">
        <button
          type="button"
          class="rounded-md border border-rosa px-4 py-2 text-sm text-cafe transition hover:bg-rosa/20 disabled:opacity-50"
          :disabled="enviando"
          @click="cerrar"
        >
          Cancelar
        </button>
        <button
          type="submit"
          class="rounded-md bg-rojizo px-5 py-2.5 text-sm text-white shadow-sm transition hover:bg-granate disabled:opacity-50"
          :disabled="enviando || !motivo"
        >
          {{ enviando ? 'Enviando...' : 'Enviar reporte' }}
        </button>
      </div>
    </form>
  </div>
</template>
