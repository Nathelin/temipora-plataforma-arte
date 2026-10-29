<script setup>
import { computed, ref, watch } from 'vue'
import {
  cargarReputacionCliente,
  etiquetaReputacionDe,
  formatearPromedio,
} from '../servicios/resenias'

const props = defineProps({
  idCliente: { type: Number, default: null },
  nombre: { type: String, default: '' },
  reputacion: { type: Object, default: null },
})

const reputacion = ref(null)
const cargando = ref(false)
const error = ref('')

const info = computed(() => etiquetaReputacionDe(reputacion.value?.etiqueta))

const detalle = computed(() => {
  const datos = reputacion.value
  if (!datos) return ''
  if (!datos.hayDatos) {
    return `Se muestra a partir de ${datos.minimoResenias} reseñas de artistas.`
  }
  const cantidad = datos.cantidadResenias === 1
    ? '1 reseña de artistas'
    : `${datos.cantidadResenias} reseñas de artistas`
  return `${cantidad} · promedio ${formatearPromedio(datos.promedio)}/5`
})

async function cargar() {
  if (props.reputacion) {
    reputacion.value = props.reputacion
    error.value = ''
    return
  }
  if (!props.idCliente) {
    reputacion.value = null
    return
  }
  cargando.value = true
  error.value = ''
  try {
    const res = await cargarReputacionCliente(props.idCliente)
    reputacion.value = res.data
  } catch (e) {
    reputacion.value = null
    error.value = e.response?.status === 403
      ? 'Todavía no trabajaste con este cliente.'
      : (e.response?.data?.message || 'No se pudo cargar la reputación del cliente.')
  } finally {
    cargando.value = false
  }
}

watch(() => props.idCliente, cargar, { immediate: true })

watch(() => props.reputacion, (valor) => {
  if (valor) {
    reputacion.value = valor
    error.value = ''
  }
}, { immediate: true })
</script>

<template>
  <section class="rounded-2xl border border-rosa/50 bg-blanco p-5">
    <h2 class="text-base font-semibold text-granate">Reputación del cliente</h2>

    <p v-if="cargando" class="mt-3 text-xs text-cafe/50">Cargando...</p>
    <p v-else-if="error" class="mt-3 text-xs leading-relaxed text-cafe/60">{{ error }}</p>

    <div v-else-if="reputacion">
      <span
        class="mt-3 inline-flex rounded-full border px-3 py-1 text-xs font-semibold"
        :class="info.clase"
      >
        {{ info.etiqueta }}
      </span>
      <p class="mt-2 text-xs leading-relaxed text-cafe/60">{{ detalle }}</p>
      <p v-if="nombre" class="mt-1 text-xs text-cafe/45">{{ nombre }}</p>
    </div>

    <p class="mt-3 border-t border-rosa/40 pt-3 text-[11px] leading-relaxed text-cafe/40">
      Información interna: solo la ven los artistas.
    </p>
  </section>
</template>
