<script setup>
import { ref, computed, onMounted } from 'vue'
import api from '../servicios/api'

const nombreUsuario = localStorage.getItem('nombreUsuario') || ''

const cargando = ref(true)
const error = ref('')
const guardando = ref(false)
const mensaje = ref('')
const errorEdicion = ref('')

const idUsuario = ref(null)
const correoActual = ref('')
const nuevoCorreo = ref('')
const contraseniaActual = ref('')
const mostrarContrasenia = ref(false)
const correoEditable = ref(false)
const contraseniaActualEditable = ref(false)
const claveActualEditable = ref(false)
const claveNuevaEditable = ref(false)
const claveConfirmarEditable = ref(false)

const correoValido = computed(() => /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(nuevoCorreo.value.trim()))
const puedeGuardar = computed(
  () => correoValido.value && !!contraseniaActual.value && !guardando.value,
)

const claveActual = ref('')
const claveNueva = ref('')
const claveConfirmar = ref('')
const mostrarClaveActual = ref(false)
const mostrarClaveNueva = ref(false)
const mostrarClaveConfirmar = ref(false)
const guardandoClave = ref(false)
const mensajeClave = ref('')
const errorClave = ref('')

const nuevaValida = computed(() => {
  const c = claveNueva.value
  return c.length >= 8 && /[A-Z]/.test(c) && /\d/.test(c)
})
const clavesCoinciden = computed(() => !!claveNueva.value && claveNueva.value === claveConfirmar.value)
const esIgualALaActual = computed(
  () => !!claveActual.value && !!claveNueva.value && claveActual.value === claveNueva.value,
)
const fuerzaInfo = computed(() => {
  const c = claveNueva.value
  if (!c) return { label: '', clase: '', ancho: '0%' }
  let score = 0
  if (c.length >= 8) score++
  if (/[A-Z]/.test(c)) score++
  if (/\d/.test(c)) score++
  if (/[^A-Za-z0-9]/.test(c)) score++
  if (c.length >= 12) score++
  if (score <= 2) return { label: 'Débil', clase: 'bg-rojizo', ancho: '33%' }
  if (score <= 3) return { label: 'Media', clase: 'bg-coral', ancho: '66%' }
  return { label: 'Fuerte', clase: 'bg-green-600', ancho: '100%' }
})
const puedeCambiarClave = computed(
  () => nuevaValida.value && clavesCoinciden.value && !esIgualALaActual.value && !guardandoClave.value,
)

async function cargar() {
  cargando.value = true
  error.value = ''
  try {
    const res = await api.get(`/perfil/${nombreUsuario}`)
    idUsuario.value = res.data.idUsuario
    correoActual.value = res.data.correo || ''
  } catch (e) {
    error.value = e.response?.data?.message || 'No se pudo cargar la cuenta.'
  } finally {
    cargando.value = false
  }
}

async function guardar() {
  mensaje.value = ''
  errorEdicion.value = ''
  if (!nuevoCorreo.value.trim()) {
    errorEdicion.value = 'Ingresá el nuevo correo.'
    return
  }
  if (!correoValido.value) {
    errorEdicion.value = 'El correo no es válido.'
    return
  }
  if (!contraseniaActual.value) {
    errorEdicion.value = 'Ingresá tu contraseña actual para confirmar el cambio.'
    return
  }
  guardando.value = true
  try {
    const r = await api.put(`/usuarios/${idUsuario.value}/correo`, {
      correo: nuevoCorreo.value.trim(),
      contraseniaActual: contraseniaActual.value,
    })
    correoActual.value = r.data.correo || correoActual.value
    nuevoCorreo.value = ''
    contraseniaActual.value = ''
    mensaje.value = 'Correo actualizado correctamente.'
  } catch (e) {
    errorEdicion.value = e.response?.data?.message || 'No se pudo actualizar el correo.'
  } finally {
    guardando.value = false
  }
}

async function cambiarClave() {
  mensajeClave.value = ''
  errorClave.value = ''
  if (!claveActual.value) {
    errorClave.value = 'Ingresá tu contraseña actual.'
    return
  }
  if (!nuevaValida.value) {
    errorClave.value = 'La nueva contraseña debe tener al menos 8 caracteres, una mayúscula y un número.'
    return
  }
  if (!clavesCoinciden.value) {
    errorClave.value = 'Las contraseñas no coinciden.'
    return
  }
  if (esIgualALaActual.value) {
    errorClave.value = 'La nueva contraseña debe ser distinta a la actual.'
    return
  }
  guardandoClave.value = true
  try {
    await api.put(`/usuarios/${idUsuario.value}/contrasenia`, {
      contraseniaActual: claveActual.value,
      contraseniaNueva: claveNueva.value,
      confirmarContrasenia: claveConfirmar.value,
    })
    claveActual.value = ''
    claveNueva.value = ''
    claveConfirmar.value = ''
    mensajeClave.value = 'Contraseña actualizada correctamente.'
  } catch (e) {
    errorClave.value = e.response?.data?.message || 'No se pudo actualizar la contraseña.'
  } finally {
    guardandoClave.value = false
  }
}

onMounted(cargar)
</script>

<template>
  <div class="bg-blanco rounded-3xl shadow-xl shadow-rosa/40 p-6 sm:p-8">
    <h2 class="text-lg text-granate mb-6">Cuenta y seguridad</h2>

    <p v-if="cargando" class="text-cafe/60 text-center py-10">Cargando...</p>
    <p v-else-if="error" class="text-rojizo text-center py-10">{{ error }}</p>

    <div v-else class="space-y-8">
      <!-- Correo actual -->
      <div>
        <h3 class="text-base text-granate mb-4 pt-5 pb-2">Correo electrónico</h3>
        <div class="rounded-xl border border-rosa/40 bg-rosa/10 px-4 py-3">
          <p class="text-xs text-cafe/50 mb-0.5">Correo actual</p>
          <p class="text-sm text-cafe">{{ correoActual || '—' }}</p>
        </div>
      </div>

      <!-- Cambio de correo -->
      <div>
        <h3 class="text-base text-granate mb-2 pt-6">Cambiar correo</h3>
        <p class="text-sm text-cafe/70 mb-5">
          Por seguridad, ingresa tu contraseña para confirmar el cambio de correo.
        </p>

        <div class="space-y-5">
          <div>
            <label class="block text-sm text-cafe mb-2 pt-4">Nuevo correo <span class="text-rojizo">*</span></label>
            <input
              v-model="nuevoCorreo"
              name="nuevoCorreo"
              type="email"
              maxlength="40"
              autocomplete="off"
              :readonly="!correoEditable"
              @focus="correoEditable = true"
              placeholder="nuevo@correo.com"
              class="pl-4 pr-4 block w-full rounded-lg border-0 py-2.5 text-base text-cafe shadow-sm ring-1 ring-inset ring-rosa focus:ring-2 focus:ring-inset focus:ring-coral focus:outline-none read-only:cursor-pointer"
            />
            <p v-if="nuevoCorreo.trim() && !correoValido" class="text-xs text-rojizo mt-1">
              El correo no es válido.
            </p>
          </div>

          <div>
            <label class="block text-sm text-cafe mb-2 pt-4">Contraseña actual <span class="text-rojizo">*</span></label>
            <div class="relative pb-2">
              <input
                v-model="contraseniaActual"
                name="contraseniaConfirmacionCorreo"
                :type="mostrarContrasenia ? 'text' : 'password'"
                autocomplete="new-password"
                :readonly="!contraseniaActualEditable"
                @focus="contraseniaActualEditable = true"
                class="pl-4 pr-20 block w-full rounded-lg border-0 py-2.5 text-base text-cafe shadow-sm ring-1 ring-inset ring-rosa focus:ring-2 focus:ring-inset focus:ring-coral focus:outline-none read-only:cursor-pointer"
              />
              <button
                type="button"
                class="absolute inset-y-0 right-3 flex items-center text-xs text-cafe/60 hover:text-cafe transition"
                @click="mostrarContrasenia = !mostrarContrasenia"
              >
                {{ mostrarContrasenia ? 'Ocultar' : 'Mostrar' }}
              </button>
            </div>
          </div>

          <p v-if="errorEdicion" class="text-rojizo text-sm">{{ errorEdicion }}</p>
          <p v-if="mensaje" class="text-green-700 text-sm">{{ mensaje }}</p>

          <div class="flex justify-end pt-1">
            <button
              :disabled="!puedeGuardar"
              class="px-5 py-2.5 rounded-md text-sm text-white bg-coral hover:bg-rojizo disabled:opacity-50 disabled:cursor-not-allowed transition shadow-sm"
              @click="guardar"
            >
              {{ guardando ? 'Guardando...' : 'Actualizar correo' }}
            </button>
          </div>
        </div>
      </div>

      <!-- Cambio de contraseña -->
      <div>
        <h3 class="text-base text-granate mb-2 pt-6">Contraseña</h3>
        <p class="text-sm text-cafe/70 mb-5">
          Debe tener al menos 8 caracteres e incluir una mayúscula y un número.
        </p>

        <div class="space-y-5">
          <div>
            <label class="block text-sm text-cafe mb-2 pt-4">Contraseña actual <span class="text-rojizo">*</span></label>
            <div class="relative">
              <input
                v-model="claveActual"
                name="contraseniaActual"
                :type="mostrarClaveActual ? 'text' : 'password'"
                autocomplete="new-password"
                :readonly="!claveActualEditable"
                @focus="claveActualEditable = true"
                class="pl-4 pr-20 block w-full rounded-lg border-0 py-2.5 text-base text-cafe shadow-sm ring-1 ring-inset ring-rosa focus:ring-2 focus:ring-inset focus:ring-coral focus:outline-none read-only:cursor-pointer"
              />
              <button
                type="button"
                class="absolute inset-y-0 right-3 flex items-center text-xs text-cafe/60 hover:text-cafe transition"
                @click="mostrarClaveActual = !mostrarClaveActual"
              >
                {{ mostrarClaveActual ? 'Ocultar' : 'Mostrar' }}
              </button>
            </div>
          </div>

          <div>
            <label class="block text-sm text-cafe mb-2 pt-4">Nueva contraseña <span class="text-rojizo">*</span></label>
            <div class="relative">
              <input
                v-model="claveNueva"
                name="contraseniaNueva"
                :type="mostrarClaveNueva ? 'text' : 'password'"
                autocomplete="new-password"
                :readonly="!claveNuevaEditable"
                @focus="claveNuevaEditable = true"
                class="pl-4 pr-20 block w-full rounded-lg border-0 py-2.5 text-base text-cafe shadow-sm ring-1 ring-inset ring-rosa focus:ring-2 focus:ring-inset focus:ring-coral focus:outline-none read-only:cursor-pointer"
              />
              <button
                type="button"
                class="absolute inset-y-0 right-3 flex items-center text-xs text-cafe/60 hover:text-cafe transition"
                @click="mostrarClaveNueva = !mostrarClaveNueva"
              >
                {{ mostrarClaveNueva ? 'Ocultar' : 'Mostrar' }}
              </button>
            </div>
            <div v-if="claveNueva" class="mt-2">
              <div class="h-1.5 w-full overflow-hidden rounded-full bg-rosa/40">
                <div class="h-full transition-all" :class="fuerzaInfo.clase" :style="{ width: fuerzaInfo.ancho }"></div>
              </div>
              <p class="mt-1 text-xs" :class="nuevaValida ? 'text-green-700' : 'text-rojizo'">
                Seguridad: {{ fuerzaInfo.label }}
              </p>
            </div>
          </div>

          <div>
            <label class="block text-sm text-cafe mb-2 pt-4">Confirmar nueva contraseña <span class="text-rojizo">*</span></label>
            <div class="relative">
              <input
                v-model="claveConfirmar"
                name="confirmarContrasenia"
                :type="mostrarClaveConfirmar ? 'text' : 'password'"
                autocomplete="new-password"
                :readonly="!claveConfirmarEditable"
                @focus="claveConfirmarEditable = true"
                class="pl-4 pr-20 block w-full rounded-lg border-0 py-2.5 text-base text-cafe shadow-sm ring-1 ring-inset ring-rosa focus:ring-2 focus:ring-inset focus:ring-coral focus:outline-none read-only:cursor-pointer"
              />
              <button
                type="button"
                class="absolute inset-y-0 right-3 flex items-center text-xs text-cafe/60 hover:text-cafe transition"
                @click="mostrarClaveConfirmar = !mostrarClaveConfirmar"
              >
                {{ mostrarClaveConfirmar ? 'Ocultar' : 'Mostrar' }}
              </button>
            </div>
            <p v-if="claveConfirmar && !clavesCoinciden" class="mt-1 text-xs text-rojizo">
              Las contraseñas no coinciden.
            </p>
          </div>

          <p v-if="errorClave" class="text-rojizo text-sm">{{ errorClave }}</p>
          <p v-if="mensajeClave" class="text-green-700 text-sm">{{ mensajeClave }}</p>

          <div class="flex justify-end pt-1">
            <button
              :disabled="!puedeCambiarClave"
              class="px-5 py-2.5 rounded-md text-sm text-white bg-coral hover:bg-rojizo disabled:opacity-50 disabled:cursor-not-allowed transition shadow-sm"
              @click="cambiarClave"
            >
              {{ guardandoClave ? 'Guardando...' : 'Actualizar contraseña' }}
            </button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>
