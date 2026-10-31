<script setup>
import { onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import api from '../servicios/api'

const route = useRoute()
const esArtista = localStorage.getItem('rol') === 'ARTISTA'

const cargando = ref(true)
const error = ref('')
const mensaje = ref('')
const errorAccion = ref('')
const cuenta = ref(null)
const conectando = ref(false)
const desconectando = ref(false)

async function cargar() {
  cargando.value = true
  error.value = ''
  try {
    const res = await api.get('/pagos/mercadopago/oauth/estado')
    cuenta.value = res.data
  } catch (e) {
    error.value = e.response?.data?.message
      || 'No se pudo consultar el estado de la cuenta.'
  } finally {
    cargando.value = false
  }
}

async function conectar() {
  if (conectando.value) return
  conectando.value = true
  errorAccion.value = ''
  try {
    const res = await api.get('/pagos/mercadopago/oauth/inicio')
    if (res.data?.url) {
      window.location.href = res.data.url
    } else {
      errorAccion.value = 'MercadoPago no devolvió la URL de autorización.'
    }
  } catch (e) {
    errorAccion.value = e.response?.data?.message
      || 'No se pudo iniciar la conexión con MercadoPago.'
  } finally {
    conectando.value = false
  }
}

async function desconectar() {
  const confirmar = window.confirm(
    '¿Desconectar tu cuenta de MercadoPago? No vas a poder recibir pagos hasta que la vuelvas a conectar.',
  )
  if (!confirmar) return
  desconectando.value = true
  errorAccion.value = ''
  mensaje.value = ''
  try {
    await api.delete('/pagos/mercadopago/oauth')
    mensaje.value = 'Cuenta de MercadoPago desconectada.'
    await cargar()
  } catch (e) {
    errorAccion.value = e.response?.data?.message
      || 'No se pudo desconectar la cuenta.'
  } finally {
    desconectando.value = false
  }
}

function formatearFecha(fecha) {
  if (!fecha) return null
  return new Date(fecha).toLocaleString('es-AR', {
    dateStyle: 'medium',
    timeStyle: 'short',
  })
}

onMounted(() => {
  if (!esArtista) {
    cargando.value = false
    return
  }
  const resultado = route.query.mp
  if (resultado === 'ok') {
    mensaje.value = '¡Cuenta de MercadoPago conectada correctamente!'
  } else if (resultado === 'error') {
    errorAccion.value = 'No se pudo conectar la cuenta de MercadoPago. Intentá de nuevo.'
  }
  cargar()
})
</script>

<template>
  <div class="rounded-3xl bg-blanco p-6 shadow-xl shadow-rosa/40 sm:p-8">
    <h2 class="text-lg font-bold text-granate">Cobros</h2>

    <p v-if="!esArtista" class="py-10 text-center text-cafe/60">
      Esta sección es solo para artistas.
    </p>

    <template v-else>
      <p class="mt-2 text-sm text-cafe/70">
        Conectá tu cuenta de MercadoPago para recibir los pagos de tus pedidos.
        Temipora retiene la comisión configurada y el resto se acredita en tu
        cuenta.
      </p>

      <p v-if="cargando" class="py-10 text-center text-cafe/60">Cargando...</p>
      <p v-else-if="error" class="py-10 text-center text-rojizo">{{ error }}</p>

      <template v-else>
        <div
          v-if="cuenta?.conectada"
          class="mt-5 rounded-xl border border-green-700/30 bg-green-700/5 px-4 py-4"
        >
          <p class="text-sm font-semibold text-green-700">
            Cuenta conectada
          </p>
          <p v-if="cuenta.idCuentaExterna" class="mt-1 text-sm text-cafe/70">
            Cuenta de MercadoPago: {{ cuenta.idCuentaExterna }}
          </p>
          <p v-if="cuenta.expiraEn" class="mt-1 text-xs text-cafe/50">
            Autorización válida hasta {{ formatearFecha(cuenta.expiraEn) }}.
          </p>
          <div class="mt-4 flex flex-wrap gap-3">
            <button
              type="button"
              :disabled="conectando"
              class="inline-flex min-h-10 items-center justify-center rounded-full border border-coral bg-coral/10 px-5 text-sm font-semibold text-granate transition hover:bg-coral/20 disabled:opacity-50"
              @click="conectar"
            >
              {{ conectando ? 'Redirigiendo...' : 'Reconectar cuenta' }}
            </button>
            <button
              type="button"
              :disabled="desconectando"
              class="inline-flex min-h-10 items-center justify-center rounded-full border border-rojizo/40 bg-rojizo/5 px-5 text-sm font-semibold text-rojizo transition hover:bg-rojizo/10 disabled:opacity-50"
              @click="desconectar"
            >
              {{ desconectando ? 'Desconectando...' : 'Desconectar' }}
            </button>
          </div>
        </div>

        <div v-else class="mt-5 rounded-xl border border-rosa/50 bg-rosa/10 px-4 py-4">
          <p class="text-sm font-semibold text-granate">
            Todavía no conectaste tu cuenta
          </p>
          <p class="mt-1 text-sm text-cafe/70">
            Sin una cuenta conectada no vas a poder recibir pagos con
            MercadoPago.
          </p>
          <button
            type="button"
            :disabled="conectando"
            class="mt-4 inline-flex min-h-11 items-center justify-center rounded-full bg-coral px-6 text-sm font-semibold text-white transition hover:bg-rojizo disabled:opacity-50"
            @click="conectar"
          >
            {{ conectando ? 'Redirigiendo...' : 'Conectar MercadoPago' }}
          </button>
        </div>

        <p v-if="errorAccion" class="mt-4 text-sm text-rojizo" role="alert">
          {{ errorAccion }}
        </p>
        <p v-if="mensaje" class="mt-4 text-sm text-green-700" role="status">
          {{ mensaje }}
        </p>
      </template>
    </template>
  </div>
</template>
