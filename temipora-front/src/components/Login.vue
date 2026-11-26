<script setup>
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import api from '../servicios/api'
import { useSesion } from '../composables/useSesion'
import { obtenerContacto } from '../servicios/contacto'
import { formatearFecha, formatearFechaHora } from '../servicios/fechas'
import fondo from '../assets/fondos/prueba.webp'
import welcomeLogo from '../assets/welcome logo.png'

const router = useRouter()
const route = useRoute()
const { guardarSesion } = useSesion()
const nombreUsuario = ref('')
const contrasenia = ref('')
const error = ref('')
const cargando = ref(false)
const verContrasenia = ref(false)
const inhabilitada = ref(null)
const baja = ref(null)
const avisoBaja = ref(null)
const reactivando = ref(false)
const errorBaja = ref('')
const contactoRespaldo = ref('')

const contactoVisible = computed(
  () => baja.value?.contacto
    || inhabilitada.value?.contacto
    || contactoRespaldo.value
    || 'temiporasupport@gmail.com',
)

function cargarInhabilitada() {
  try {
    const raw = sessionStorage.getItem('cuentaInhabilitada')
    if (!raw) return
    const datos = JSON.parse(raw)
    if (datos?.codigo === 'CUENTA_DE_BAJA') {
      baja.value = datos
      inhabilitada.value = null
      return
    }
    inhabilitada.value = datos
    baja.value = null
  } catch {
    inhabilitada.value = null
    baja.value = null
  }
}

function cargarAvisoBaja() {
  try {
    const raw = sessionStorage.getItem('cuentaBaja')
    if (!raw) return
    avisoBaja.value = JSON.parse(raw)
    sessionStorage.removeItem('cuentaBaja')
  } catch {
    avisoBaja.value = null
  }
}

function formatearFechaFin(valor) {
  return formatearFechaHora(valor)
}

async function reactivarCuenta() {
  errorBaja.value = ''

  if (!nombreUsuario.value.trim()) {
    errorBaja.value = 'Ingresá tu nombre de usuario.'
    return
  }
  if (!contrasenia.value) {
    errorBaja.value = 'Ingresá tu contraseña para reactivar tu cuenta.'
    return
  }

  reactivando.value = true
  try {
    const res = await api.post('/auth/reactivar-cuenta', {
      nombreUsuario: nombreUsuario.value.trim(),
      contrasenia: contrasenia.value,
    })

    guardarSesion(res.data)
    sessionStorage.removeItem('cuentaInhabilitada')
    sessionStorage.removeItem('cuentaBaja')
    baja.value = null
    avisoBaja.value = null

    const rol = res.data.rol
    if (res.data.restringido) {
      router.push({ name: 'cuenta-restringida' })
    } else if (rol === 'ADMIN') {
      router.push({ name: 'admin-resumen' })
    } else {
      router.push({ name: 'explorar' })
    }
  } catch (e) {
    const data = e.response?.data
    if (e.response?.status === 403 && data?.codigo === 'REACTIVACION_VENCIDA') {
      baja.value = {
        ...baja.value,
        puedeReactivar: false,
        reactivarHasta: data.reactivarHasta || baja.value?.reactivarHasta || null,
        contacto: data.contacto || baja.value?.contacto || '',
        message: data.message || '',
      }
    } else {
      errorBaja.value = data?.message || 'No se pudo reactivar la cuenta.'
    }
  } finally {
    reactivando.value = false
  }
}

function alInhabilitar() {
  cargarInhabilitada()
}

async function iniciarSesion() {
  error.value = ''
  cargando.value = true
  try {
    const res = await api.post('/auth/login', {
      nombreUsuario: nombreUsuario.value,
      contrasenia: contrasenia.value,
    })

    const rol = res.data.rol
    guardarSesion(res.data)
    sessionStorage.removeItem('cuentaInhabilitada')
    inhabilitada.value = null
    baja.value = null
    avisoBaja.value = null
    errorBaja.value = ''

    if (res.data.restringido) {
      router.push({ name: 'cuenta-restringida' })
      return
    }

    const redirect = route.query.redirect
    if (redirect) {
      router.push(String(redirect))
    } else if (rol === 'ADMIN') {
      router.push({ name: 'admin-resumen' })
    } else {
      // Clientes y artistas entran directo al descubrimiento.
      router.push({ name: 'explorar' })
    }
  } catch (e) {
    const data = e.response?.data
    if (e.response?.status === 403 && data?.codigo === 'CUENTA_DE_BAJA') {
      baja.value = {
        codigo: data.codigo,
        fechaBaja: data.fechaBaja || null,
        reactivarHasta: data.reactivarHasta || null,
        puedeReactivar: data.puedeReactivar !== false,
        contacto: data.contacto || '',
        message: data.message || '',
      }
      inhabilitada.value = null
      errorBaja.value = ''
    } else if (e.response?.status === 403
        && (data?.codigo === 'CUENTA_SANCIONADA' || data?.codigo === 'CUENTA_INACTIVA')) {
      inhabilitada.value = {
        codigo: data.codigo,
        motivo: data.motivo || '',
        contacto: data.contacto || '',
        fechaFin: data.fechaFin || null,
        message: data.message || '',
      }
      baja.value = null
    } else {
      error.value = 'Nombre de usuario o contraseña incorrecta.'
    }
  } finally {
    cargando.value = false
  }
}

onMounted(async () => {
  cargarInhabilitada()
  cargarAvisoBaja()
  window.addEventListener('cuenta-inhabilitada', alInhabilitar)
  contactoRespaldo.value = await obtenerContacto()
})

onBeforeUnmount(() => {
  window.removeEventListener('cuenta-inhabilitada', alInhabilitar)
})
</script>

<template>
  <div
    class="min-h-screen w-full flex items-stretch justify-end px-4 py-4 sm:px-8 sm:py-6 sm:pr-10 bg-cover bg-center"
    :style="{ backgroundImage: `url(${fondo})` }"
  >
    <div class="w-full max-w-md rounded-3xl bg-superficie px-8 py-10 shadow-xl flex flex-col justify-start">
      <div class="sm:mx-auto sm:w-full sm:max-w-sm flex flex-col items-center mt-12">
        <img :src="welcomeLogo" alt="Bienvenido a Temipora" class="w-20 h-20 object-contain mb-4" />
        <h2 class="text-center text-2xl font-normal leading-9 tracking-tight text-granate">
          Inicia sesión
        </h2>
        <p class="text-center text-sm text-cafe/60 mt-1">Nos alegra verte de nuevo</p>
      </div>

      <div
        v-if="avisoBaja"
        class="mt-5 rounded-xl border border-coral/40 bg-coral/10 px-4 py-3 text-sm text-granate"
        role="status"
      >
        <p class="font-semibold">Tu cuenta quedó inactiva.</p>
        <p v-if="avisoBaja.reactivarHasta" class="mt-1">
          Podés reactivarla hasta el {{ formatearFecha(avisoBaja.reactivarHasta) }} iniciando sesión.
        </p>
        <p v-else class="mt-1">
          Podés reactivarla iniciando sesión dentro del plazo configurado.
        </p>
      </div>

      <form class="space-y-2 sm:pt-5" @submit.prevent="iniciarSesion">
        <div>
          <label for="nombreUsuario" class="block text-sm font-normal leading-6 text-cafe">
            Nombre de usuario
          </label>
          <div class="mt-3">
            <input
              id="nombreUsuario"
              v-model="nombreUsuario"
              name="nombreUsuario"
              type="text"
              autocomplete="username"
              required
              class="pl-4 pr-4 block w-full rounded-lg border-0 py-3 text-base text-cafe shadow-sm ring-1 ring-inset ring-rosa placeholder:text-cafe/40 focus:ring-2 focus:ring-inset focus:ring-coral"
            />
          </div>
        </div>

        <div>
          <label for="contrasenia" class="block text-sm font-normal leading-6 text-cafe">
            Contraseña
          </label>
          <div class="mt-3 relative">
            <input
              id="contrasenia"
              v-model="contrasenia"
              name="contrasenia"
              :type="verContrasenia ? 'text' : 'password'"
              autocomplete="current-password"
              required
              class="pl-4 pr-14 block w-full rounded-lg border-0 py-3 text-base text-cafe shadow-sm ring-1 ring-inset ring-rosa placeholder:text-cafe/40 focus:ring-2 focus:ring-inset focus:ring-coral"
            />
            <button
              type="button"
              class="absolute inset-y-0 right-0 flex items-center px-4 text-sm text-cafe/60 hover:text-rojizo transition"
              @click="verContrasenia = !verContrasenia"
            >
              {{ verContrasenia ? 'Ocultar' : 'Ver' }}
            </button>
          </div>
        </div>

        <div
          v-if="inhabilitada"
          class="rounded-xl border border-rojizo/30 bg-rosa/40 px-4 py-3 text-sm text-granate"
          role="alert"
        >
          <p class="font-semibold text-rojizo">
            {{ inhabilitada.message || 'Tu cuenta está inhabilitada.' }}
          </p>
          <p v-if="inhabilitada.motivo" class="mt-1">
            <span class="font-semibold">Motivo:</span> {{ inhabilitada.motivo }}
          </p>
          <p v-if="inhabilitada.fechaFin" class="mt-1">
            La sanción finaliza el {{ formatearFechaFin(inhabilitada.fechaFin) }}.
          </p>
          <p class="mt-1">
            Si creés que es un error, escribinos a
            <a :href="`mailto:${contactoVisible}`" class="underline">
              {{ contactoVisible }}
            </a>.
          </p>
        </div>

        <div
          v-if="baja"
          class="rounded-xl border border-rojizo/30 bg-durazno/30 px-4 py-3 text-sm text-granate"
          role="alert"
        >
          <p class="font-semibold text-rojizo">Tu cuenta está dada de baja.</p>
          <p v-if="baja.fechaBaja" class="mt-1">
            La dimos de baja el {{ formatearFecha(baja.fechaBaja) }}.
          </p>
          <p v-if="baja.puedeReactivar && baja.reactivarHasta" class="mt-1">
            Podés reactivarla hasta el {{ formatearFecha(baja.reactivarHasta) }}.
          </p>
          <p v-else class="mt-1">
            El plazo para reactivarla venció. Escribinos a
            <a :href="`mailto:${contactoVisible}`" class="underline">{{ contactoVisible }}</a>.
          </p>
          <button
            v-if="baja.puedeReactivar"
            type="button"
            :disabled="reactivando"
            class="mt-3 inline-flex w-full items-center justify-center rounded-lg bg-coral px-4 py-2.5 text-sm font-semibold text-white shadow-sm transition hover:bg-rojizo disabled:cursor-not-allowed disabled:opacity-50"
            @click="reactivarCuenta"
          >
            {{ reactivando ? 'Reactivando...' : 'Reactivar mi cuenta' }}
          </button>
          <p v-if="baja.puedeReactivar" class="mt-2 text-xs text-cafe/70">
            Usamos el nombre de usuario y la contraseña que ingresaste arriba.
          </p>
          <p v-if="errorBaja" class="mt-2 text-sm text-rojizo">{{ errorBaja }}</p>
        </div>

        <p v-if="error" class="rounded-md bg-rosa/40 px-3 py-2 text-sm text-rojizo">
          {{ error }}
        </p>

        <div class="pt-2">
          <button
            type="submit"
            :disabled="cargando"
            class="flex w-full justify-center rounded-lg bg-coral px-3 py-3 text-base font-normal leading-6 text-white shadow-sm hover:bg-rojizo disabled:opacity-50 transition"
          >
            {{ cargando ? 'Ingresando...' : 'Iniciar sesión' }}
          </button>
        </div>
      </form>

      <p class="mt-auto text-center text-sm text-cafe/70">
        ¿No tenés cuenta?
        <router-link to="/registrarse" class="text-rojizo hover:text-coral underline">
          Registrarse
        </router-link>
      </p>
    </div>
  </div>
</template>