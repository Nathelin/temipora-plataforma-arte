<script setup>
import { ref, onMounted } from 'vue'
import api from '../servicios/api'

const esArtista = localStorage.getItem('rol') === 'ARTISTA'

const cargando = ref(true)
const error = ref('')
const mensaje = ref('')
const errorAccion = ref('')
const misEtiquetas = ref([])

async function cargar() {
  cargando.value = true
  error.value = ''
  try {
    const res = await api.get('/etiquetas/mis')
    misEtiquetas.value = res.data
  } catch (e) {
    error.value = e.response?.data?.message || 'No se pudieron cargar tus etiquetas.'
  } finally {
    cargando.value = false
  }
}

async function reSolicitarEtiqueta(e) {
  const confirmar = window.confirm(
    `¿Re-solicitar la etiqueta "${e.nombre}" para que los administradores la reevalúen?`,
  )
  if (!confirmar) return

  mensaje.value = ''
  try {
    await api.post(`/etiquetas/${e.idEtiqueta}/re-solicitar`)
    await cargar()
    mensaje.value = `Se registró la re-solicitud de "${e.nombre}".`
  } catch (err) {
    await cargar()
    errorAccion.value = err.response?.data?.message || 'No se pudo re-solicitar la etiqueta.'
  }
}

function formatearFecha(fecha) {
  if (!fecha) return null
  return new Date(fecha).toLocaleString('es-AR', { dateStyle: 'medium', timeStyle: 'short' })
}

function chipEstadoEtiqueta(estado) {
  return {
    VALIDA: 'bg-green-500/10 text-green-700',
    PENDIENTE: 'bg-coral/15 text-coral',
    RECHAZADA: 'bg-rojizo/10 text-rojizo',
    INACTIVA: 'bg-cafe/10 text-cafe/60',
  }[estado] || 'bg-durazno/40 text-cafe'
}

function textoEstadoEtiqueta(estado) {
  return {
    VALIDA: 'Válida',
    PENDIENTE: 'Pendiente',
    RECHAZADA: 'Rechazada',
    INACTIVA: 'Dada de baja',
  }[estado] || estado
}

onMounted(() => {
  if (esArtista) {
    cargar()
  } else {
    cargando.value = false
  }
})
</script>

<template>
  <div class="bg-blanco rounded-3xl shadow-xl shadow-rosa/40 p-6 sm:p-8">
    <h2 class="text-lg text-granate mb-6">Mis etiquetas</h2>

    <p v-if="!esArtista" class="text-cafe/60 text-center py-10">
      Esta sección es solo para artistas.
    </p>

    <template v-else>
      <p v-if="mensaje" class="text-sm text-green-700 bg-green-500/10 border border-green-500/20 rounded-lg px-4 py-3 mb-4">
        {{ mensaje }}
      </p>
      <p v-if="errorAccion" class="text-sm text-rojizo bg-rojizo/5 border border-rojizo/20 rounded-lg px-4 py-3 mb-4">
        {{ errorAccion }}
      </p>

      <p v-if="cargando" class="text-sm text-cafe/60 py-6 text-center">
        Cargando tus etiquetas...
      </p>
      <p v-else-if="error" class="text-rojizo text-center py-10">{{ error }}</p>
      <template v-else>
        <p v-if="!misEtiquetas.length" class="text-sm text-cafe/60 py-6">
          Todavía no propusiste etiquetas. Puedes crear una al subir una imagen a tu portfolio.
        </p>
        <div v-else class="space-y-4">
          <div
            v-for="e in misEtiquetas"
            :key="e.idEtiqueta"
            class="bg-blanco border border-rosa rounded-xl p-5 flex flex-col sm:flex-row sm:items-center gap-4"
          >
            <div class="flex-1 min-w-0">
              <div class="flex items-center gap-3 flex-wrap">
                <h3 class="text-granate font-medium">{{ e.nombre }}</h3>
                <span class="px-2.5 py-0.5 rounded-full text-xs" :class="chipEstadoEtiqueta(e.estado)">
                  {{ textoEstadoEtiqueta(e.estado) }}
                </span>
              </div>
              <p class="text-sm text-cafe/70 mt-1">{{ e.descripcion }}</p>
              <p class="text-xs text-cafe/50 mt-2">
                {{ e.nombreCategoria }} · Propuesta el {{ formatearFecha(e.fechaCreacion) }}
                <template v-if="e.fechaEvaluacion">
                  · Evaluada el {{ formatearFecha(e.fechaEvaluacion) }}{{ e.evaluadoPor ? ` por @${e.evaluadoPor}` : '' }}
                </template>
              </p>
            </div>
            <button
              v-if="e.estado === 'RECHAZADA'"
              class="shrink-0 px-4 py-2 rounded-md text-sm text-white bg-coral hover:bg-rojizo transition shadow-sm"
              @click="reSolicitarEtiqueta(e)"
            >
              Re-solicitar
            </button>
          </div>
        </div>
      </template>
    </template>
  </div>
</template>
