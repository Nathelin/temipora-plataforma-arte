<script setup>
import { onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import api from '../servicios/api'
import BurbujaInfo from '../components/BurbujaInfo.vue'

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

onMounted(() => {
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
    <div class="flex items-start gap-2">
      <div>
        <h2 class="text-xl font-bold text-granate">Pagos y cobros</h2>
        <p class="mt-1 text-sm text-cafe/70">
          Comprar y cobrar son dos cosas distintas. Acá ves el estado de tu cuenta
          de MercadoPago y qué habilita cada cosa.
        </p>
      </div>
      <BurbujaInfo etiqueta="Cómo funcionan los pagos y los cobros">
        <strong class="block">Conectar tu cuenta no cambia cómo pagás: solo habilita a cobrar.</strong>
        <span class="mt-1 block">
          Comprar: elegís la pieza, te redirigimos a MercadoPago y pagás con
          tarjeta o con tu cuenta. No hace falta conectar nada.
          Cobrar: conectás tu cuenta una vez y el dinero de tus ventas y
          reventas se acredita ahí.
        </span>
      </BurbujaInfo>
    </div>

    <div class="mt-6 grid gap-5 lg:grid-cols-2">
      <section class="rounded-2xl border border-borde/60 bg-superficie p-5">
        <p class="text-xs font-bold uppercase tracking-[0.16em] text-coral">Pagos</p>
        <h3 class="mt-1 font-display text-lg font-bold text-granate">
          Lo que usás para comprar
        </h3>
        <p class="mt-3 text-sm leading-relaxed text-cafe/75">
          Al finalizar una compra te redirigimos al checkout de MercadoPago.
          Podés pagar con tarjeta o con tu cuenta de MercadoPago.
        </p>
        <p class="mt-3 rounded-xl bg-rosa/20 px-4 py-3 text-sm font-medium text-granate">
          No necesitás conectar tu cuenta para comprar. El pago siempre pasa por
          MercadoPago, tengas o no una cuenta conectada en Temipora.
        </p>
        <router-link
          to="/compras"
          class="mt-4 inline-flex text-sm font-semibold text-coral underline decoration-coral/40 underline-offset-4 transition hover:text-rojizo"
        >
          Ver mis compras →
        </router-link>
      </section>

      <section class="rounded-2xl border border-borde/60 bg-superficie p-5">
        <p class="text-xs font-bold uppercase tracking-[0.16em] text-coral">Cobros</p>
        <h3 class="mt-1 font-display text-lg font-bold text-granate">
          Lo que usás para recibir dinero
        </h3>
        <p class="mt-3 text-sm leading-relaxed text-cafe/75">
          Conectá tu cuenta de MercadoPago para cobrar tus ventas y reventas.
          Temipora descuenta su comisión y MercadoPago descuenta su arancel;
          el resto se acredita en tu cuenta.
        </p>
        <p v-if="!esArtista" class="mt-2 text-sm text-cafe/60">
          Solo la necesitás si vas a revender una pieza.
        </p>

        <p v-if="cargando" class="py-6 text-center text-sm text-cafe/60">
          Consultando tu cuenta...
        </p>
        <p v-else-if="error" class="py-6 text-sm text-rojizo">{{ error }}</p>

        <template v-else>
          <div
            v-if="cuenta?.conectada"
            class="mt-4 rounded-xl border border-green-700/30 bg-green-700/5 px-4 py-4"
          >
            <p class="text-sm font-semibold text-green-700">
              Cuenta conectada
            </p>
            <p class="mt-1 text-sm text-cafe/70">
              Vas a recibir los pagos de tus ventas y reventas en esta cuenta.
            </p>
            <div class="mt-4 flex flex-wrap gap-2">
              <button
                type="button"
                :disabled="conectando"
                class="inline-flex min-h-9 items-center justify-center rounded-full border border-coral bg-coral/10 px-4 text-sm font-semibold text-granate transition hover:bg-coral/20 disabled:opacity-50"
                @click="conectar"
              >
                {{ conectando ? 'Redirigiendo...' : 'Reconectar cuenta' }}
              </button>
              <button
                type="button"
                :disabled="desconectando"
                class="inline-flex min-h-9 items-center justify-center rounded-full border border-rojizo/40 bg-rojizo/5 px-4 text-sm font-semibold text-rojizo transition hover:bg-rojizo/10 disabled:opacity-50"
                @click="desconectar"
              >
                {{ desconectando ? 'Desconectando...' : 'Desconectar' }}
              </button>
            </div>
          </div>

          <div v-else class="mt-4 rounded-xl border border-rosa/50 bg-rosa/10 px-4 py-4">
            <p class="text-sm font-semibold text-granate">
              Todavía no conectaste tu cuenta
            </p>
            <p class="mt-1 text-sm text-cafe/70">
              Sin una cuenta conectada no vas a poder cobrar tus reventas.
            </p>
            <button
              type="button"
              :disabled="conectando"
              class="mt-4 inline-flex min-h-10 items-center justify-center rounded-full bg-coral px-5 text-sm font-semibold text-white transition hover:bg-rojizo disabled:opacity-50"
              @click="conectar"
            >
              {{ conectando ? 'Redirigiendo...' : 'Conectar MercadoPago' }}
            </button>
          </div>

          <router-link
            :to="esArtista ? '/panel/ventas' : '/reventa'"
            class="mt-4 inline-flex text-sm font-semibold text-coral underline decoration-coral/40 underline-offset-4 transition hover:text-rojizo"
          >
            {{ esArtista ? 'Ver mis ventas →' : 'Ver mis reventas →' }}
          </router-link>
        </template>

        <p v-if="errorAccion" class="mt-4 text-sm text-rojizo" role="alert">
          {{ errorAccion }}
        </p>
        <p v-if="mensaje" class="mt-4 text-sm text-green-700" role="status">
          {{ mensaje }}
        </p>
      </section>
    </div>
  </div>
</template>
