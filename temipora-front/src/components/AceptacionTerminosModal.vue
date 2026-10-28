<script setup>
import { onMounted, ref } from 'vue'
import api from '../servicios/api'

const visible = ref(false)
const estado = ref(null)
const leido = ref(false)
const aceptando = ref(false)
const error = ref('')

async function verificar() {
  if (!localStorage.getItem('token')) return
  if (localStorage.getItem('rol') === 'ADMIN') return
  try {
    const res = await api.get('/terminos-plataforma/estado')
    if (res.data?.version && !res.data.aceptada) {
      estado.value = res.data
      visible.value = true
    }
  } catch {
    // Si no se puede verificar, no bloqueamos la navegación.
  }
}

async function aceptar() {
  if (!leido.value || aceptando.value) return
  aceptando.value = true
  error.value = ''
  try {
    await api.post('/terminos-plataforma/aceptar', {
      version: estado.value.version,
    })
    visible.value = false
  } catch (e) {
    error.value = e.response?.data?.message
      || 'No se pudo registrar la aceptación.'
  } finally {
    aceptando.value = false
  }
}

onMounted(verificar)
</script>

<template>
  <div
    v-if="visible"
    class="fixed inset-0 z-[60] flex items-center justify-center bg-granate/75 p-4 backdrop-blur-sm"
    role="dialog"
    aria-modal="true"
    aria-labelledby="tos-actualizados-titulo"
  >
    <div class="flex max-h-[90vh] w-full max-w-3xl flex-col overflow-hidden rounded-3xl bg-blanco shadow-2xl">
      <header class="border-b border-rosa/40 px-6 py-4">
        <h2 id="tos-actualizados-titulo" class="text-xl font-semibold text-granate">
          Términos y condiciones actualizados
        </h2>
        <p class="mt-1 text-sm leading-relaxed text-cafe/65">
          {{ estado.titulo }} · versión {{ estado.version }}.
          Para seguir usando Temipora, leelos y aceptalos.
        </p>
      </header>

      <div class="min-h-0 flex-1 overflow-y-auto px-6 py-4">
        <p class="whitespace-pre-line text-sm leading-relaxed text-cafe">
          {{ estado.contenido }}
        </p>
      </div>

      <footer class="border-t border-rosa/40 px-6 py-4">
        <label class="flex items-start gap-2 text-sm text-cafe/75">
          <input
            v-model="leido"
            type="checkbox"
            class="mt-0.5 h-4 w-4 rounded border-rosa text-coral focus:ring-coral"
          />
          <span>Leí y acepto los términos y condiciones de Temipora.</span>
        </label>

        <p v-if="error" class="mt-2 text-sm text-rojizo">{{ error }}</p>

        <div class="mt-3 flex justify-end">
          <button
            type="button"
            :disabled="!leido || aceptando"
            class="inline-flex min-h-10 items-center justify-center rounded-full bg-coral px-5 text-sm font-semibold text-white transition hover:bg-rojizo disabled:cursor-not-allowed disabled:bg-coral/40"
            @click="aceptar"
          >
            {{ aceptando ? 'Guardando...' : 'Aceptar y continuar' }}
          </button>
        </div>
      </footer>
    </div>
  </div>
</template>
