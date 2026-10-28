<script setup>
import { onBeforeUnmount, ref, watch } from 'vue'
import api from '../servicios/api'

const props = defineProps({
  url: { type: String, default: '' },
  alt: { type: String, default: 'Archivo de la entrega' },
})

const emit = defineEmits(['abrir'])

const src = ref('')
const cargando = ref(false)
const error = ref('')
let urlTemporal = ''
let desmontado = false
let solicitudActual = 0

function revocar() {
  if (urlTemporal) {
    URL.revokeObjectURL(urlTemporal)
    urlTemporal = ''
  }
}

async function cargar() {
  const solicitud = ++solicitudActual
  revocar()
  src.value = ''
  error.value = ''
  if (!props.url) return

  cargando.value = true
  try {
    const res = await api.get(props.url, { responseType: 'blob' })
    if (desmontado || solicitud !== solicitudActual) return
    urlTemporal = URL.createObjectURL(res.data)
    src.value = urlTemporal
  } catch (e) {
    if (desmontado || solicitud !== solicitudActual) return
    error.value = e.response?.status === 403
      ? 'Se libera al pagar.'
      : 'No se pudo cargar.'
  } finally {
    if (!desmontado && solicitud === solicitudActual) {
      cargando.value = false
    }
  }
}

watch(() => props.url, cargar, { immediate: true })
onBeforeUnmount(() => {
  desmontado = true
  revocar()
})
</script>

<template>
  <div class="min-w-0">
    <p v-if="cargando" class="rounded-lg border border-rosa/40 bg-rosa/5 px-3 py-6 text-center text-xs text-cafe/50">
      Cargando...
    </p>
    <p v-else-if="error" class="rounded-lg border border-durazno bg-durazno/20 px-3 py-6 text-center text-xs text-cafe/60">
      {{ error }}
    </p>
    <button
      v-else-if="src"
      type="button"
      class="block w-full overflow-hidden rounded-xl border border-rosa/50 bg-blanco transition hover:border-coral/50"
      @click="emit('abrir', src)"
    >
      <img :src="src" :alt="alt" class="max-h-56 w-full object-contain" />
    </button>
  </div>
</template>
