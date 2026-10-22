<script setup>
import { ref, computed, onMounted } from 'vue'
import { parsePhoneNumberFromString, getExampleNumber } from 'libphonenumber-js'
import api from '../servicios/api'

const nombreUsuario = localStorage.getItem('nombreUsuario') || ''

const cargando = ref(true)
const error = ref('')
const guardando = ref(false)
const mensaje = ref('')
const errorEdicion = ref('')

const idUsuario = ref(null)
const correoActual = ref('')
const paises = ref([])
const idPais = ref(null)
const telefonoEnmascarado = ref('')
const tieneTelefono = ref(false)
const telefonoActual = ref('')
const telefonoNuevo = ref('')
const contraseniaTelefono = ref('')
const mostrarContraseniaTelefono = ref(false)
const modoTelefono = ref('')
const guardandoTelefono = ref(false)
const mensajeTelefono = ref('')
const errorTelefono = ref('')

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

const paisActual = computed(
  () => paises.value.find((p) => String(p.idPais) === String(idPais.value)) || null,
)
const prefijoActual = computed(() => paisActual.value?.prefijoTelefonico || '')
const isoActual = computed(() => paisActual.value?.codigoIso || undefined)
const ejemploTelefono = computed(() => {
  if (!isoActual.value) return ''
  try {
    const ejemplo = getExampleNumber(isoActual.value)
    return ejemplo ? ejemplo.formatNational() : ''
  } catch {
    return ''
  }
})

function parseNumero(valor, iso) {
  const tel = (valor || '').trim()
  if (!tel) return undefined
  try {
    return iso ? parsePhoneNumberFromString(tel, iso) : parsePhoneNumberFromString(tel)
  } catch {
    return undefined
  }
}

function parseTelefonoActual(valor) {
  const tel = (valor || '').trim()
  if (!tel) return undefined
  try {
    return tel.startsWith('+')
      ? parsePhoneNumberFromString(tel)
      : parsePhoneNumberFromString(tel, isoActual.value)
  } catch {
    return undefined
  }
}

const telefonoNuevoError = computed(() => {
  const tel = telefonoNuevo.value.trim()
  if (!tel) return ''
  const parsed = parseNumero(tel, isoActual.value)
  if (!parsed || !parsed.isValid()) {
    return isoActual.value
      ? 'El teléfono no es válido para el país seleccionado.'
      : 'Configurá tu país en Mi perfil o ingresá el número con prefijo internacional (ej. +1...).'
  }
  return ''
})

const telefonoActualError = computed(() => {
  if (!tieneTelefono.value) return ''
  const tel = telefonoActual.value.trim()
  if (!tel) return ''
  const parsed = parseTelefonoActual(tel)
  if (!parsed || !parsed.isValid()) {
    return 'Ingresá el teléfono actual completo y válido (con prefijo si es de otro país).'
  }
  return ''
})

const mismosTelefonos = computed(() => {
  if (!tieneTelefono.value) return false
  const actual = parseTelefonoActual(telefonoActual.value)
  const nuevo = parseNumero(telefonoNuevo.value, isoActual.value)
  return !!(actual?.isValid() && nuevo?.isValid() && actual.number === nuevo.number)
})

const puedeGuardarTelefono = computed(() => {
  if (guardandoTelefono.value) return false
  if (!telefonoNuevo.value.trim() || !contraseniaTelefono.value) return false
  if (tieneTelefono.value && !telefonoActual.value.trim()) return false
  if (telefonoNuevoError.value || telefonoActualError.value || mismosTelefonos.value) return false
  return true
})

const puedeQuitarTelefono = computed(
  () => tieneTelefono.value && !!contraseniaTelefono.value && !guardandoTelefono.value,
)

function limpiarFormularioTelefono() {
  telefonoActual.value = ''
  telefonoNuevo.value = ''
  contraseniaTelefono.value = ''
  mostrarContraseniaTelefono.value = false
  errorTelefono.value = ''
}

function abrirEdicionTelefono() {
  mensajeTelefono.value = ''
  limpiarFormularioTelefono()
  modoTelefono.value = 'editar'
}

function abrirQuitarTelefono() {
  mensajeTelefono.value = ''
  limpiarFormularioTelefono()
  modoTelefono.value = 'quitar'
}

function cancelarTelefono() {
  modoTelefono.value = ''
  limpiarFormularioTelefono()
}

async function cargar() {
  cargando.value = true
  error.value = ''
  try {
    const [res, paisesRes] = await Promise.all([
      api.get(`/perfil/${nombreUsuario}`),
      api.get('/paises').catch(() => ({ data: [] })),
    ])
    idUsuario.value = res.data.idUsuario
    correoActual.value = res.data.correo || ''
    paises.value = Array.isArray(paisesRes.data) ? paisesRes.data : []
    idPais.value = res.data.idPais ?? null
    telefonoEnmascarado.value = res.data.telefonoEnmascarado || ''
    tieneTelefono.value = !!res.data.tieneTelefono
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

function telefonoNuevoNormalizado() {
  const parsed = parseNumero(telefonoNuevo.value, isoActual.value)
  return parsed?.isValid() ? parsed.number : telefonoNuevo.value.trim()
}

function telefonoActualNormalizado() {
  const parsed = parseTelefonoActual(telefonoActual.value)
  return parsed?.isValid() ? parsed.number : telefonoActual.value.trim()
}

async function guardarTelefono() {
  mensajeTelefono.value = ''
  errorTelefono.value = ''
  if (tieneTelefono.value && !telefonoActual.value.trim()) {
    errorTelefono.value = 'Ingresá tu teléfono actual.'
    return
  }
  if (telefonoActualError.value) {
    errorTelefono.value = telefonoActualError.value
    return
  }
  if (telefonoNuevoError.value) {
    errorTelefono.value = telefonoNuevoError.value
    return
  }
  if (!contraseniaTelefono.value) {
    errorTelefono.value = 'Ingresá tu contraseña actual para confirmar el cambio.'
    return
  }
  if (mismosTelefonos.value) {
    errorTelefono.value = 'El nuevo teléfono debe ser distinto al actual.'
    return
  }

  guardandoTelefono.value = true
  try {
    const payload = {
      telefonoNuevo: telefonoNuevoNormalizado(),
      contraseniaActual: contraseniaTelefono.value,
    }
    if (tieneTelefono.value) {
      payload.telefonoActual = telefonoActualNormalizado()
    }
    const r = await api.put(`/usuarios/${idUsuario.value}/telefono`, payload)
    telefonoEnmascarado.value = r.data.telefonoEnmascarado || ''
    tieneTelefono.value = !!r.data.tieneTelefono
    modoTelefono.value = ''
    limpiarFormularioTelefono()
    mensajeTelefono.value = 'Teléfono actualizado correctamente.'
  } catch (e) {
    errorTelefono.value = e.response?.data?.message || 'No se pudo actualizar el teléfono.'
  } finally {
    guardandoTelefono.value = false
  }
}

async function quitarTelefono() {
  mensajeTelefono.value = ''
  errorTelefono.value = ''
  if (!contraseniaTelefono.value) {
    errorTelefono.value = 'Ingresá tu contraseña actual para quitar el teléfono.'
    return
  }

  guardandoTelefono.value = true
  try {
    const r = await api.delete(`/usuarios/${idUsuario.value}/telefono`, {
      data: { contraseniaActual: contraseniaTelefono.value },
    })
    telefonoEnmascarado.value = r.data.telefonoEnmascarado || ''
    tieneTelefono.value = !!r.data.tieneTelefono
    modoTelefono.value = ''
    limpiarFormularioTelefono()
    mensajeTelefono.value = 'Teléfono eliminado correctamente.'
  } catch (e) {
    errorTelefono.value = e.response?.data?.message || 'No se pudo quitar el teléfono.'
  } finally {
    guardandoTelefono.value = false
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
    <h2 class="text-lg text-granate font-bold">Cuenta y seguridad</h2>

    <p v-if="cargando" class="text-cafe/60 text-center py-10">Cargando...</p>
    <p v-else-if="error" class="text-rojizo text-center py-10">{{ error }}</p>

    <div v-else class="space-y-4">
      <!-- Correo actual -->
      <div>
        <h3 class="text-base text-granate pt-5">Correo electrónico</h3>
        <div class="rounded-xl border border-rosa/40 bg-rosa/10 px-4 py-3">
          <p class="text-xs text-cafe/50 mb-0.5">Correo actual</p>
          <p class="text-sm text-cafe">{{ correoActual || '—' }}</p>
        </div>
      </div>

      <!-- Cambio de correo -->
      <div>
        <h3 class="text-base text-granate pt-2">Cambiar correo</h3>
        <p class="text-sm text-cafe/70">
          Por seguridad, ingresa tu contraseña para confirmar el cambio de correo.
        </p>

        <div class="space-y-5">
          <div>
            <label class="block text-sm text-cafe pt-4">Nuevo correo <span class="text-rojizo">*</span></label>
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
            <label class="block text-sm text-cafe">Contraseña actual <span class="text-rojizo">*</span></label>
            <div class="relative">
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

      <!-- Teléfono -->
      <div>
        <h3 class="text-base text-granate pt-2">Teléfono</h3>
        <p class="text-sm text-cafe/70">
          Por seguridad, tu número se muestra enmascarado. Para cambiarlo o quitarlo se requiere tu contraseña actual.
        </p>

        <div class="rounded-xl border border-rosa/40 bg-rosa/10 px-4 py-3 mt-4">
          <p class="text-xs text-cafe/50 mb-0.5">Teléfono actual</p>
          <p v-if="tieneTelefono" class="text-sm text-cafe">{{ telefonoEnmascarado }}</p>
          <p v-else class="text-sm text-cafe/60">Sin teléfono registrado</p>
          <p v-if="tieneTelefono" class="text-xs text-cafe/50 mt-1">Solo mostramos los últimos 4 dígitos.</p>
        </div>

        <div class="flex flex-wrap gap-3 pt-4">
          <button
            type="button"
            class="inline-flex cursor-pointer items-center rounded-md border border-coral bg-coral/10 px-4 py-2 text-sm font-medium text-granate transition hover:bg-coral/20"
            @click="abrirEdicionTelefono"
          >
            {{ tieneTelefono ? 'Cambiar teléfono' : 'Agregar teléfono' }}
          </button>
          <button
            v-if="tieneTelefono"
            type="button"
            class="inline-flex cursor-pointer items-center rounded-md border border-rojizo/40 bg-rojizo/5 px-4 py-2 text-sm font-medium text-rojizo transition hover:bg-rojizo/10"
            @click="abrirQuitarTelefono"
          >
            Quitar teléfono
          </button>
        </div>

        <div v-if="modoTelefono === 'editar'" class="space-y-5 pt-4">
          <div v-if="tieneTelefono">
            <label class="block text-sm text-cafe">Teléfono actual <span class="text-rojizo">*</span></label>
            <input
              v-model="telefonoActual"
              name="telefonoActual"
              type="tel"
              autocomplete="off"
              placeholder="+54 9 11 2345-6789"
              class="pl-4 pr-4 block w-full rounded-lg border-0 py-2.5 text-base text-cafe shadow-sm ring-1 ring-inset ring-rosa focus:ring-2 focus:ring-inset focus:ring-coral focus:outline-none"
            />
            <p v-if="telefonoActualError" class="text-xs text-rojizo mt-1">{{ telefonoActualError }}</p>
            <p v-else class="text-xs text-cafe/50 mt-1">
              Ingresalo completo. Si cambiaste de país, usá el prefijo internacional.
            </p>
          </div>

          <div>
            <label class="block text-sm text-cafe">Nuevo teléfono <span class="text-rojizo">*</span></label>
            <div class="flex">
              <span
                v-if="prefijoActual"
                class="inline-flex items-center rounded-l-lg border border-r-0 border-rosa bg-rosa/20 px-3 text-base text-cafe"
              >
                {{ prefijoActual }}
              </span>
              <input
                v-model="telefonoNuevo"
                name="telefonoNuevo"
                type="tel"
                autocomplete="off"
                :placeholder="ejemploTelefono || 'Número de teléfono'"
                class="pl-4 pr-4 block w-full border-0 py-2.5 text-base text-cafe shadow-sm ring-1 ring-inset focus:ring-2 focus:ring-inset focus:outline-none"
                :class="[
                  prefijoActual ? 'rounded-r-lg rounded-l-none' : 'rounded-lg',
                  telefonoNuevoError ? 'ring-rojizo focus:ring-rojizo' : 'ring-rosa focus:ring-coral',
                ]"
              />
            </div>
            <p v-if="telefonoNuevoError" class="text-xs text-rojizo mt-1">{{ telefonoNuevoError }}</p>
            <p v-else-if="mismosTelefonos" class="text-xs text-rojizo mt-1">
              El nuevo teléfono debe ser distinto al actual.
            </p>
            <p v-else class="text-xs text-cafe/50 mt-1">Se guarda en formato internacional.</p>
          </div>

          <div>
            <label class="block text-sm text-cafe">Contraseña actual <span class="text-rojizo">*</span></label>
            <div class="relative">
              <input
                v-model="contraseniaTelefono"
                name="contraseniaConfirmacionTelefono"
                :type="mostrarContraseniaTelefono ? 'text' : 'password'"
                autocomplete="new-password"
                class="pl-4 pr-20 block w-full rounded-lg border-0 py-2.5 text-base text-cafe shadow-sm ring-1 ring-inset ring-rosa focus:ring-2 focus:ring-inset focus:ring-coral focus:outline-none"
              />
              <button
                type="button"
                class="absolute inset-y-0 right-3 flex items-center text-xs text-cafe/60 hover:text-cafe transition"
                @click="mostrarContraseniaTelefono = !mostrarContraseniaTelefono"
              >
                {{ mostrarContraseniaTelefono ? 'Ocultar' : 'Mostrar' }}
              </button>
            </div>
          </div>

          <p v-if="errorTelefono" class="text-rojizo text-sm">{{ errorTelefono }}</p>

          <div class="flex justify-end gap-3 pt-1">
            <button
              type="button"
              class="px-4 py-2.5 rounded-md text-sm text-cafe border border-rosa hover:bg-rosa/10 transition"
              @click="cancelarTelefono"
            >
              Cancelar
            </button>
            <button
              type="button"
              :disabled="!puedeGuardarTelefono"
              class="px-5 py-2.5 rounded-md text-sm text-white bg-coral hover:bg-rojizo disabled:opacity-50 disabled:cursor-not-allowed transition shadow-sm"
              @click="guardarTelefono"
            >
              {{ guardandoTelefono ? 'Guardando...' : 'Guardar teléfono' }}
            </button>
          </div>
        </div>

        <div v-if="modoTelefono === 'quitar'" class="space-y-5 pt-4">
          <p class="text-sm text-cafe/70">
            Para quitar el teléfono, ingresá tu contraseña actual. Esta acción no se puede deshacer.
          </p>

          <div>
            <label class="block text-sm text-cafe">Contraseña actual <span class="text-rojizo">*</span></label>
            <div class="relative">
              <input
                v-model="contraseniaTelefono"
                name="contraseniaQuitarTelefono"
                :type="mostrarContraseniaTelefono ? 'text' : 'password'"
                autocomplete="new-password"
                class="pl-4 pr-20 block w-full rounded-lg border-0 py-2.5 text-base text-cafe shadow-sm ring-1 ring-inset ring-rosa focus:ring-2 focus:ring-inset focus:ring-coral focus:outline-none"
              />
              <button
                type="button"
                class="absolute inset-y-0 right-3 flex items-center text-xs text-cafe/60 hover:text-cafe transition"
                @click="mostrarContraseniaTelefono = !mostrarContraseniaTelefono"
              >
                {{ mostrarContraseniaTelefono ? 'Ocultar' : 'Mostrar' }}
              </button>
            </div>
          </div>

          <p v-if="errorTelefono" class="text-rojizo text-sm">{{ errorTelefono }}</p>

          <div class="flex justify-end gap-3 pt-1">
            <button
              type="button"
              class="px-4 py-2.5 rounded-md text-sm text-cafe border border-rosa hover:bg-rosa/10 transition"
              @click="cancelarTelefono"
            >
              Cancelar
            </button>
            <button
              type="button"
              :disabled="!puedeQuitarTelefono"
              class="px-5 py-2.5 rounded-md text-sm text-white bg-rojizo hover:bg-granate disabled:opacity-50 disabled:cursor-not-allowed transition shadow-sm"
              @click="quitarTelefono"
            >
              {{ guardandoTelefono ? 'Quitando...' : 'Confirmar quita' }}
            </button>
          </div>
        </div>

        <p v-if="mensajeTelefono" class="text-green-700 text-sm pt-3">{{ mensajeTelefono }}</p>
      </div>

      <!-- Cambio de contraseña -->
      <div>
        <h3 class="text-base text-granate ">Contraseña</h3>
        <p class="text-sm text-cafe/70">
          Debe tener al menos 8 caracteres e incluir una mayúscula y un número.
        </p>

        <div class="space-y-5">
          <div>
            <label class="block text-sm text-cafe pt-4">Contraseña actual <span class="text-rojizo">*</span></label>
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
            <label class="block text-sm text-cafe ">Nueva contraseña <span class="text-rojizo">*</span></label>
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
            <label class="block text-sm text-cafe">Confirmar nueva contraseña <span class="text-rojizo">*</span></label>
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
