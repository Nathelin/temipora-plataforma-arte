<script setup>
import { onBeforeUnmount, ref, watch } from 'vue'
import api from '../servicios/api'

const props = defineProps({
  url: { type: String, default: '' },
  alt: { type: String, default: 'Imagen de la galería' },
  ajuste: { type: String, default: 'cover' },
})

const src = ref('')
const cargando = ref(false)
const error = ref('')
let urlTemporal = ''
let solicitudActual = 0
let desmontado = false

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
    error.value = 'No se pudo cargar la imagen.'
  } finally {
    if (!desmontado && solicitud === solicitudActual) cargando.value = false
  }
}

watch(() => props.url, cargar, { immediate: true })
onBeforeUnmount(() => {
  desmontado = true
  solicitudActual++
  revocar()
})
</script>

<template>
  <div class="flex h-full w-full items-center justify-center">
    <p v-if="cargando" class="px-3 text-xs text-cafe/50">Cargando...</p>
    <p v-else-if="error" class="px-3 text-xs text-cafe/50">{{ error }}</p>
    <img
      v-else-if="src"
      :src="src"
      :alt="alt"
      class="h-full w-full"
      :class="ajuste === 'contain' ? 'object-contain' : 'object-cover'"
    />
  </div>
</template>
