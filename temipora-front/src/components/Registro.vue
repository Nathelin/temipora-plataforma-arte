<script setup>
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import api from '../servicios/api'
import { acceptImagenes, validarImagen, procesarImagen, LIMITES_IMAGEN } from '../servicios/archivos'
import { useAvisoActualizacion } from '../composables/useAvisoActualizacion'
import fondo from '../assets/fondos/prueba.png'

const router = useRouter()
const {
  mostrar: avisoMostrar,
  fase: avisoFase,
  detalle: avisoDetalle,
  exito: avisoExito,
  fallo: avisoFallo,
} = useAvisoActualizacion()

const paso = ref(1)
const totalPasos = 4

// Paso 1: credenciales
const nombreUsuario = ref('')
const correo = ref('')
const contrasenia = ref('')
const confirmarContrasenia = ref('')
const acceptaTOS = ref(false)
const verContrasenia = ref(false)
const verConfirmar = ref(false)
const nombreUsuarioEnUso = ref(false)

// Paso 2: datos personales
const nombre = ref('')
const apellido = ref('')
const fechaNacimiento = ref('')

// Paso 3: rol
const rol = ref('')

// Paso 4: foto + bio (opcionales)
const biografia = ref('')
const archivoFoto = ref(null)
const urlFotoPerfil = ref('')
const previewFoto = ref('')

const cargando = ref(false)
const error = ref('')
const errores = ref([])
const fase = ref('')
const progreso = ref(0)

const etiquetaRegistro = computed(() => {
  if (fase.value === 'procesando') return 'Procesando imagen…'
  if (fase.value === 'subiendo') return `Subiendo foto… ${progreso.value}%`
  return cargando.value ? 'Registrando...' : 'Registrarse'
})

function validarPaso() {
  const errs = []
  if (paso.value === 1) {
    if (nombreUsuario.value.trim().length < 2) errs.push('El nombre de usuario debe tener al menos 2 caracteres.')
    if (nombreUsuarioEnUso.value) errs.push('El nombre de usuario ya está en uso.')
    if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(correo.value)) errs.push('El correo no es válido.')
    if (contrasenia.value.length < 8) errs.push('La contraseña debe tener al menos 8 caracteres.')
    if (!/[A-Z]/.test(contrasenia.value) || !/\d/.test(contrasenia.value)) errs.push('La contraseña debe incluir al menos una mayúscula y un número.')
    if (contrasenia.value !== confirmarContrasenia.value) errs.push('Las contraseñas no coinciden.')
    if (!acceptaTOS.value) errs.push('Debés aceptar los términos y condiciones.')
  }
  if (paso.value === 2) {
    if (nombre.value.trim().length < 2) errs.push('El nombre debe tener al menos 2 caracteres.')
    if (apellido.value.trim().length < 2) errs.push('El apellido debe tener al menos 2 caracteres.')
    if (!fechaNacimiento.value) errs.push('La fecha de nacimiento es obligatoria.')
  }
  if (paso.value === 3) {
    if (!rol.value) errs.push('Debés elegir una opción de rol.')
  }
  return errs
}

async function chequearNombreUsuario() {
  const valor = nombreUsuario.value.trim()
  if (valor.length < 2) {
    nombreUsuarioEnUso.value = false
    return
  }
  try {
    const res = await api.get('/checkNombreUsuario', {
      params: { nombreUsuario: valor },
    })
    nombreUsuarioEnUso.value = !res.data.disponible
  } catch (e) {
    nombreUsuarioEnUso.value = false
  }
}

function onNombreUsuarioBlur() {
  chequearNombreUsuario()
}

function siguiente() {
  const errs = validarPaso()
  if (errs.length > 0) {
    errores.value = errs
    return
  }
  errores.value = []
  if (paso.value < totalPasos) paso.value++
}

function anterior() {
  errores.value = []
  if (paso.value > 1) paso.value--
}

function onFileChange(event) {
  const file = event.target.files[0] || null
  const problema = file ? validarImagen(file) : ''
  if (problema) {
    archivoFoto.value = null
    previewFoto.value = ''
    error.value = problema
    event.target.value = ''
    return
  }

  error.value = ''
  archivoFoto.value = file
  if (file) {
    previewFoto.value = URL.createObjectURL(file)
  } else {
    previewFoto.value = ''
  }
}

async function subirFoto() {
  if (!archivoFoto.value) return
  fase.value = 'procesando'
  avisoFase('procesando')
  avisoDetalle('Procesando la imagen…')
  const optimizado = await procesarImagen(archivoFoto.value, LIMITES_IMAGEN.avatar)
  fase.value = 'subiendo'
  avisoFase('subiendo', 0)
  progreso.value = 0
  const form = new FormData()
  form.append('archivo', optimizado)
  const res = await api.post('/upload', form, {
    onUploadProgress: (e) => {
      if (e.total) {
        const pct = Math.round((e.loaded * 100) / e.total)
        progreso.value = pct
        avisoFase('subiendo', pct)
      }
    },
  })
  urlFotoPerfil.value = res.data.url
}

async function registrarse() {
  cargando.value = true
  error.value = ''
  progreso.value = 0
  const conFoto = !!archivoFoto.value
  if (conFoto) {
    avisoMostrar('Completando tu registro…', 'Subiendo tu foto de perfil…')
  }
  try {
    if (archivoFoto.value) {
      await subirFoto()
    }
    fase.value = ''
    if (conFoto) {
      avisoFase('guardando')
      avisoDetalle('Creando tu cuenta…')
    }

    await api.post('/auth/registrarse', {
      nombreUsuario: nombreUsuario.value,
      correo: correo.value,
      contrasenia: contrasenia.value,
      confirmarContrasenia: confirmarContrasenia.value,
      acceptaTOS: acceptaTOS.value,
      nombre: nombre.value,
      apellido: apellido.value,
      fechaNacimiento: fechaNacimiento.value,
      biografia: biografia.value,
      rol: rol.value,
      urlFotoPerfil: urlFotoPerfil.value || null,
    })

    if (conFoto) avisoExito('Registro completado. Te llevamos al inicio de sesión.')
    router.push({ name: 'login' })
  } catch (e) {
    const data = e.response?.data
    if (data && data.message) {
      error.value = data.message
    } else if (data && typeof data === 'string') {
      error.value = data
    } else {
      error.value = 'No se pudo completar el registro. Intentalo de nuevo.'
    }
    if (conFoto) avisoFallo(error.value)
  } finally {
    cargando.value = false
    fase.value = ''
    progreso.value = 0
  }
}

const pasosRouter = [
  { n: 1, titulo: 'Cuenta' },
  { n: 2, titulo: 'Datos personales' },
  { n: 3, titulo: 'Rol' },
  { n: 4, titulo: 'Perfil' },
]
</script>

<template>
  <div
    class="min-h-screen w-full flex items-stretch justify-end px-4 py-4 sm:px-8 sm:py-6 sm:pr-10 bg-cover bg-center"
    :style="{ backgroundImage: `url(${fondo})` }"
  >
    <div class="w-full max-w-md rounded-3xl bg-superficie px-8 py-10 shadow-xl flex flex-col justify-start">
      <button
        type="button"
        class="self-start flex items-center gap-1 text-sm text-cafe/60 hover:text-rojizo transition mb-2"
        @click="router.push({ name: 'login' })"
      >
        ← Volver
      </button>

      <div class="sm:mx-auto sm:w-full sm:max-w-sm">
        <h2 class="sm:pt-5 text-center text-2xl font-normal leading-9 tracking-tight text-granate">
          Crear cuenta
        </h2>
        <p class="text-center text-sm text-cafe/60 mt-1"></p>
      </div>

      <!-- Indicador de pasos -->
      <div class="flex items-center justify-center gap-2 mt-8 mb-8">
        <template v-for="p in pasosRouter" :key="p.n">
          <div
            class="w-8 h-8 rounded-full flex items-center justify-center text-sm font-normal transition"
            :class="paso === p.n ? 'bg-coral text-white' : 'bg-rosa/40 text-granate'"
          >
            {{ p.n }}
          </div>
          <div
            v-if="p.n < totalPasos"
            class="w-6 h-px"
            :class="paso > p.n ? 'bg-coral' : 'bg-rosa/60'"
          ></div>
        </template>
      </div>
      <p class="text-center text-sm text-cafe/60 mb-8">
        Paso {{ paso }} de {{ totalPasos }}
      </p>

      <!-- Paso 1 -->
      <div v-if="paso === 1" class="space-y-8 pt-10 sm:pt-14 ">
        <div>
          <label class="block text-sm text-cafe mb-2">Nombre de usuario<span class="text-rojizo">*</span></label>
          <input
            v-model="nombreUsuario"
            name="nombreUsuario"
            type="text"
            autocomplete="username"
            @blur="onNombreUsuarioBlur"
            @input="nombreUsuarioEnUso = false"
            class="pl-4 pr-4 block w-full rounded-lg border-0 py-3 text-base text-cafe shadow-sm ring-1 ring-inset ring-rosa focus:ring-2 focus:ring-inset focus:ring-coral focus:outline-none"
          />
          <p v-if="nombreUsuarioEnUso" class="mt-1 text-xs text-rojizo">Este nombre de usuario ya está en uso.</p>
        </div>
        <div>
          <label class="block text-sm text-cafe mb-2">Correo electrónico<span class="text-rojizo">*</span></label>
          <input v-model="correo" name="correo" type="email" autocomplete="email" class="pl-4 pr-4 block w-full rounded-lg border-0 py-3 text-base text-cafe shadow-sm ring-1 ring-inset ring-rosa focus:ring-2 focus:ring-inset focus:ring-coral focus:outline-none" />
        </div>
        <div>
          <label class="block text-sm text-cafe mb-2">Contraseña<span class="text-rojizo">*</span></label>
          <div class="relative">
            <input
              v-model="contrasenia"
              name="contrasenia"
              :type="verContrasenia ? 'text' : 'password'"
              autocomplete="new-password"
              class="pl-4 pr-14 block w-full rounded-lg border-0 py-3 text-base text-cafe shadow-sm ring-1 ring-inset ring-rosa focus:ring-2 focus:ring-inset focus:ring-coral focus:outline-none"
            />
            <button
              type="button"
              class="absolute inset-y-0 right-0 flex items-center px-3 text-sm text-cafe/60 hover:text-rojizo transition"
              @click="verContrasenia = !verContrasenia"
            >
              {{ verContrasenia ? 'Ocultar' : 'Ver' }}
            </button>
          </div>
          <p class="mt-1 text-xs text-cafe/60">Mínimo 8 caracteres, con al menos una mayúscula y un número.</p>
        </div>
        <div>
          <label class="block text-sm text-cafe mb-2">Confirmar contraseña<span class="text-rojizo">*</span></label>
          <div class="relative">
            <input
              v-model="confirmarContrasenia"
              name="confirmarContrasenia"
              :type="verConfirmar ? 'text' : 'password'"
              autocomplete="new-password"
              class="pl-4 pr-14 block w-full rounded-lg border-0 py-3 text-base text-cafe shadow-sm ring-1 ring-inset ring-rosa focus:ring-2 focus:ring-inset focus:ring-coral focus:outline-none"
            />
            <button
              type="button"
              class="absolute inset-y-0 right-0 flex items-center px-3 text-sm text-cafe/60 hover:text-rojizo transition"
              @click="verConfirmar = !verConfirmar"
            >
              {{ verConfirmar ? 'Ocultar' : 'Ver' }}
            </button>
          </div>
        </div>
        <label class="flex items-center gap-2 text-sm text-cafe">
          <input v-model="acceptaTOS" type="checkbox" class="accent-coral" />
          <span>
            Acepto los
            <router-link to="/terminos" class="text-rojizo hover:text-coral underline">
              términos y condiciones
            </router-link>
          </span>
        </label>
      </div>

      <!-- Paso 2 -->
      <div v-else-if="paso === 2" class="space-y-8 pt-10 sm:pt-14 ">
        <div>
          <label class="block text-sm text-cafe mb-2">Nombre<span class="text-rojizo">*</span></label>
          <input v-model="nombre" type="text" class="pl-4 pr-4 block w-full rounded-lg border-0 py-3 text-base text-cafe shadow-sm ring-1 ring-inset ring-rosa focus:ring-2 focus:ring-inset focus:ring-coral focus:outline-none" />
        </div>
        <div>
          <label class="block text-sm text-cafe mb-2">Apellido<span class="text-rojizo">*</span></label>
          <input v-model="apellido" type="text" class="pl-4 pr-4 block w-full rounded-lg border-0 py-3 text-base text-cafe shadow-sm ring-1 ring-inset ring-rosa focus:ring-2 focus:ring-inset focus:ring-coral focus:outline-none" />
        </div>
        <div>
          <label class="block text-sm text-cafe mb-2">Fecha de nacimiento<span class="text-rojizo">*</span></label>
          <input v-model="fechaNacimiento" type="date" class="pl-4 pr-4 block w-full rounded-lg border-0 py-3 text-base text-cafe shadow-sm ring-1 ring-inset ring-rosa focus:ring-2 focus:ring-inset focus:ring-coral focus:outline-none" />
        </div>
      </div>

      <!-- Paso 3 -->
      <div v-else-if="paso === 3" class="space-y-4 pt-10 sm:pt-14 ">
        <p class="text-sm text-cafe/70 mb-4">¿Qué te gustaría hacer en Temipora?</p>

        <label
          class="flex items-start gap-3 p-4 rounded-xl border cursor-pointer transition"
          :class="rol === 'CLIENTE' ? 'border-coral bg-rosa/20' : 'border-rosa bg-superficie hover:border-coral/50'"
        >
          <input v-model="rol" type="radio" value="CLIENTE" class="mt-1 accent-coral" />
          <span>
            <span class="font-normal text-granate block">Solo comprar arte</span>
          </span>
        </label>

        <label
          class="flex items-start gap-3 p-4 rounded-xl border cursor-pointer transition"
          :class="rol === 'ARTISTA' ? 'border-coral bg-rosa/20' : 'border-rosa bg-superficie hover:border-coral/50'"
        >
          <input v-model="rol" type="radio" value="ARTISTA" class="mt-1 accent-coral" />
          <span>
            <span class="font-normal text-granate block">Vender y comprar arte</span>
          </span>
        </label>
      </div>

      <!-- Paso 4 -->
      <div v-else-if="paso === 4" class="space-y-6 pt-10 sm:pt-10 ">
        <div class="flex flex-col items-center">
          <div class="w-28 h-28 rounded-full overflow-hidden border-2 border-coral bg-rosa/30 flex items-center justify-center mb-4">
            <img
              v-if="previewFoto"
              :src="previewFoto"
              alt="Vista previa"
              class="w-full h-full object-cover"
            />
            <span v-else class="text-granate/40 text-3xl font-light">+</span>
          </div>
          <label
            class="cursor-pointer px-5 py-2 rounded-md text-sm text-white bg-coral hover:bg-rojizo transition shadow-sm"
          >
            Elegir foto
            <input type="file" :accept="acceptImagenes" class="hidden" @change="onFileChange" />
          </label>
        </div>
        <div>
          <label class="block text-sm text-cafe mb-2">Biografía <span class="text-coral">(opcional)</span></label>
          <textarea v-model="biografia" rows="4" maxlength="500" class="pl-4 pr-4 py-3 block w-full rounded-lg border-0 text-base text-cafe shadow-sm ring-1 ring-inset ring-rosa focus:ring-2 focus:ring-inset focus:ring-coral focus:outline-none resize-none"></textarea>
          <p class="text-right text-xs text-cafe/50 mt-1">{{ biografia.length }}/500</p>
        </div>
      </div>

      <!-- Errores -->
      <div v-if="errores.length" class="mt-6 rounded-md bg-rosa/40 px-3 py-2">
        <p v-for="err in errores" :key="err" class="text-sm text-rojizo">{{ err }}</p>
      </div>

      <p v-if="error" class="mt-6 rounded-md bg-rosa/40 px-3 py-2 text-sm text-rojizo">{{ error }}</p>

      <!-- Botones -->
      <div class="flex items-center justify-between mt-8 gap-4">
        <button
          v-if="paso > 1"
          type="button"
          class="px-4 py-2 rounded-md text-sm font-normal text-cafe border border-rosa hover:bg-rosa/20 transition"
          @click="anterior"
        >
          Atrás
        </button>
        <span v-else></span>

        <button
          v-if="paso < totalPasos"
          type="button"
          class="px-6 py-2 rounded-md text-sm font-normal text-white bg-coral hover:bg-rojizo transition shadow-sm"
          @click="siguiente"
        >
          Siguiente
        </button>
        <button
          v-else
          type="button"
          :disabled="cargando"
          class="px-6 py-2 rounded-md text-sm font-normal text-white bg-coral hover:bg-rojizo disabled:opacity-50 transition shadow-sm min-w-44"
          @click="registrarse"
        >
          {{ etiquetaRegistro }}
        </button>
      </div>

      <div v-if="fase === 'subiendo'" class="mt-4 h-1.5 w-full bg-rosa rounded-full overflow-hidden">
        <div class="h-full bg-coral transition-all duration-150" :style="{ width: progreso + '%' }"></div>
      </div>
    </div>
  </div>
</template>
