<script setup>
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import api from '../servicios/api'

const router = useRouter()
const terminos = ref(null)
const cargando = ref(true)
const error = ref('')

async function cargar() {
  cargando.value = true
  error.value = ''
  try {
    const res = await api.get('/terminos-plataforma')
    terminos.value = res.data
  } catch (e) {
    error.value = e.response?.status === 404
      ? 'La plataforma todavía no publicó sus términos y condiciones.'
      : (e.response?.data?.message || 'No se pudieron cargar los términos y condiciones.')
  } finally {
    cargando.value = false
  }
}

function formatearFecha(valor) {
  if (!valor) return '—'
  try {
    return new Date(valor).toLocaleString('es-AR')
  } catch {
    return valor
  }
}

onMounted(cargar)
</script>

<template>
  <div class="min-h-screen w-full bg-crema text-cafe flex flex-col">
    <header class="bg-blanco border-b border-rosa px-6 py-4 flex items-center justify-between sticky top-0 z-10">
      <h1 class="text-xl font-normal text-granate">Términos y condiciones</h1>
      <button
        class="bg-coral hover:bg-rojizo text-white px-3 py-1.5 rounded-md text-sm transition shadow-sm"
        @click="router.back()"
      >
        Volver
      </button>
    </header>

    <main class="flex-1 px-6 py-8 overflow-auto min-h-screen w-full">
      <article class=" bg-blanco text-cafe rounded-3xl p-8 shadow-xl shadow-rosa/40 leading-relaxed">
        <p v-if="cargando" class="py-10 text-center text-cafe/60">Cargando...</p>

        <p v-else-if="error" class="py-10 text-center text-cafe/70">{{ error }}</p>

        <template v-else>
          <h2 class="text-lg font-normal text-granate mb-4">{{ terminos.titulo }}</h2>

          <p class="mb-1 text-sm text-cafe/50">
            Versión de los términos y condiciones: {{ terminos.version }}
          </p>
          <p class="mb-4 text-sm text-cafe/50">
            Última actualización: {{ formatearFecha(terminos.fechaPublicacion) }}
          </p>

          <p class="whitespace-pre-line">{{ terminos.contenido }}</p>
        </template>
      </article>
    </main>
  </div>
</template>
