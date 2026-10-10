<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import api from '../servicios/api'
import { acceptImagenes, validarImagen, procesarImagen, LIMITES_IMAGEN } from '../servicios/archivos'
import { useAvisoActualizacion } from '../composables/useAvisoActualizacion'

const router = useRouter()
const nombreUsuario = localStorage.getItem('nombreUsuario') || ''

const cargando = ref(true)
const error = ref('')
const guardando = ref(false)
const mensaje = ref('')
const errorEdicion = ref('')
const fase = ref('')
const progreso = ref(0)

const etiquetaBoton = computed(() => {
  if (fase.value === 'procesando') return 'Procesando imagen…'
  if (fase.value === 'subiendo-foto') return `Subiendo foto… ${progreso.value}%`
  if (fase.value === 'subiendo-banner') return `Subiendo banner… ${progreso.value}%`
  if (fase.value === 'guardando') return 'Guardando…'
  return 'Guardar cambios'
})

const form = ref({
  nombreDisplay: '',
  biografia: '',
  sexo: '',
  telefono: '',
  pais: '',
  urlFotoPerfil: '',
  urlBanner: '',
})

const editFoto = ref(null)
const editBanner = ref(null)
const previewFoto = ref('')
const previewBanner = ref('')
const idUsuario = ref(null)

const {
  mostrar: avisoMostrar,
  fase: avisoFase,
  detalle: avisoDetalle,
  exito: avisoExito,
  fallo: avisoFallo,
} = useAvisoActualizacion()

function irAPerfil() {
  router.push({ name: 'perfil', params: { nombreUsuario } })
}

async function cargar() {
  cargando.value = true
  error.value = ''
  try {
    const res = await api.get(`/perfil/${nombreUsuario}`)
    const d = res.data
    idUsuario.value = d.idUsuario
    form.value = {
      nombreDisplay: d.nombreDisplay || '',
      biografia: d.biografia || '',
      sexo: d.sexo || '',
      telefono: d.telefono || '',
      pais: d.pais || '',
      urlFotoPerfil: d.urlFotoPerfil || '',
      urlBanner: d.urlBanner || '',
    }
    previewFoto.value = ''
    previewBanner.value = ''
  } catch (e) {
    error.value = e.response?.data?.message || 'No se pudo cargar el perfil.'
  } finally {
    cargando.value = false
  }
}

function onFotoChange(e) {
  const f = e.target.files[0] || null
  const problema = f ? validarImagen(f) : ''
  if (problema) {
    editFoto.value = null
    previewFoto.value = ''
    errorEdicion.value = problema
    e.target.value = ''
    return
  }
  errorEdicion.value = ''
  editFoto.value = f
  previewFoto.value = f ? URL.createObjectURL(f) : ''
}

function onBannerChange(e) {
  const f = e.target.files[0] || null
  const problema = f ? validarImagen(f) : ''
  if (problema) {
    editBanner.value = null
    previewBanner.value = ''
    errorEdicion.value = problema
    e.target.value = ''
    return
  }
  errorEdicion.value = ''
  editBanner.value = f
  previewBanner.value = f ? URL.createObjectURL(f) : ''
}

async function subirArchivo(archivo, limite, faseSubida, etiqueta) {
  fase.value = 'procesando'
  avisoFase('procesando')
  avisoDetalle('Procesando la imagen…')
  const optimizado = await procesarImagen(archivo, limite)
  fase.value = faseSubida
  avisoFase('subiendo', 0)
  avisoDetalle(etiqueta)
  progreso.value = 0
  const fd = new FormData()
  fd.append('archivo', optimizado)
  const res = await api.post('/upload', fd, {
    onUploadProgress: (e) => {
      if (e.total) {
        const pct = Math.round((e.loaded * 100) / e.total)
        progreso.value = pct
        avisoFase('subiendo', pct)
      }
    },
  })
  return res.data.url
}

async function guardar() {
  guardando.value = true
  mensaje.value = ''
  errorEdicion.value = ''
  progreso.value = 0
  const hayImagenes = !!(editFoto.value || editBanner.value)
  if (hayImagenes) {
    avisoMostrar('Actualizando tu perfil…', 'Esto puede tardar un momento. Puedes seguir navegando.')
  }
  try {
    let urlFoto = null
    let urlBanner = null
    if (editFoto.value) urlFoto = await subirArchivo(editFoto.value, LIMITES_IMAGEN.avatar, 'subiendo-foto', 'Subiendo tu foto de perfil…')
    if (editBanner.value) urlBanner = await subirArchivo(editBanner.value, LIMITES_IMAGEN.banner, 'subiendo-banner', 'Subiendo tu banner…')
    fase.value = 'guardando'
    if (hayImagenes) {
      avisoFase('guardando')
      avisoDetalle('Guardando los cambios…')
    }

    const payload = {
      nombreDisplay: form.value.nombreDisplay,
      biografia: form.value.biografia,
      sexo: form.value.sexo,
      telefono: form.value.telefono,
      pais: form.value.pais,
      ...(urlFoto ? { urlFotoPerfil: urlFoto } : {}),
      ...(urlBanner ? { urlBanner } : {}),
    }

    const r = await api.put(`/usuarios/${idUsuario.value}/perfil`, payload)

    form.value.nombreDisplay = r.data.nombreDisplay
    form.value.biografia = r.data.biografia
    form.value.sexo = r.data.sexo || ''
    form.value.telefono = r.data.telefono || ''
    form.value.pais = r.data.pais || ''
    form.value.urlFotoPerfil = r.data.urlFotoPerfil || ''
    form.value.urlBanner = r.data.urlBanner || ''
    localStorage.setItem('urlFotoPerfil', form.value.urlFotoPerfil)
    window.dispatchEvent(new CustomEvent('perfil-foto-actualizada', {
      detail: { url: form.value.urlFotoPerfil },
    }))
    previewFoto.value = ''
    previewBanner.value = ''
    editFoto.value = null
    editBanner.value = null

    mensaje.value = 'Perfil actualizado correctamente.'
    if (hayImagenes) avisoExito('Tu perfil se actualizó correctamente.')
  } catch (e) {
    errorEdicion.value = e.isAxiosError
      ? (e.response?.data?.message || 'Ocurrió un error, inténtalo más tarde.')
      : (e.message || 'No se pudo guardar.')
    if (hayImagenes) avisoFallo(errorEdicion.value)
  } finally {
    guardando.value = false
    fase.value = ''
    progreso.value = 0
  }
}

onMounted(cargar)
</script>

<template>
  <div class="bg-blanco rounded-3xl shadow-xl shadow-rosa/40 p-6 sm:p-8">
    <h2 class="text-lg text-granate mb-6">Mi perfil</h2>

    <p v-if="cargando" class="text-cafe/60 text-center py-10">Cargando...</p>
    <p v-else-if="error" class="text-rojizo text-center py-10">{{ error }}</p>

    <div v-else class="space-y-6">
      <!-- Foto de perfil -->
      <div class="flex items-center gap-5">
        <div class="w-20 h-20 rounded-full overflow-hidden border-2 border-rosa bg-rosa/20 shrink-0">
          <img v-if="previewFoto" :src="previewFoto" alt="" class="w-full h-full object-cover" />
          <img v-else-if="form.urlFotoPerfil" :src="form.urlFotoPerfil" alt="" class="w-full h-full object-cover" />
          <span v-else class="w-full h-full flex items-center justify-center text-2xl text-granate">
            {{ (nombreUsuario || '?').charAt(0).toUpperCase() }}
          </span>
        </div>
        <div>
          <label class="block text-sm text-cafe mb-1">Foto de perfil</label>
          <input id="foto-perfil" type="file" :accept="acceptImagenes" class="sr-only" @change="onFotoChange" />

           <label
                  for="foto-perfil"
                  class="inline-flex cursor-pointer items-center rounded-md border border-coral bg-coral/10 px-4 py-2 text-sm font-medium text-granate transition hover:bg-coral/20 focus-within:outline-none focus-within:ring-2 focus-within:ring-coral focus-within:ring-offset-2"
                >
                  {{ editFoto ? 'Foto seleccionada' : 'Cambiar foto' }}
                </label>
                <p class="mt-1 text-xs text-cafe/60">JPG, PNG o WEBP</p>
        </div>
      </div>

      <!-- Banner -->
      <div>
        <label class="block text-sm text-cafe mb-2">Banner</label>
        <div v-if="previewBanner" class="mb-2 w-full h-28 rounded-lg overflow-hidden bg-rosa/20">
          <img :src="previewBanner" alt="" class="w-full h-full object-cover" />
        </div>
        <div v-else-if="form.urlBanner" class="mb-2 w-full h-28 rounded-lg overflow-hidden bg-rosa/20">
          <img :src="form.urlBanner" alt="" class="w-full h-full object-cover" />
        </div>
        <input id="banner-perfil" type="file" :accept="acceptImagenes" class="sr-only" @change="onBannerChange" />
          <label
            for="banner-perfil"
            class="inline-flex cursor-pointer items-center rounded-md border border-coral bg-coral/10 px-4 py-2 text-sm font-medium text-granate transition hover:bg-coral/20 focus-within:outline-none focus-within:ring-2 focus-within:ring-coral focus-within:ring-offset-2"
          >
            {{ editBanner ? 'Banner seleccionado' : 'Cambiar banner' }}
          </label>
          <p class="mt-1 text-xs text-cafe/60">JPG, PNG o WEBP</p>
        </div>


      <!-- Nombre de display -->
      <div>
        <label class="block text-sm text-cafe mb-2">Nombre de display</label>
        <input
          v-model="form.nombreDisplay"
          type="text"
          maxlength="25"
          class="pl-4 pr-4 block w-full rounded-lg border-0 py-2.5 text-base text-cafe shadow-sm ring-1 ring-inset ring-rosa focus:ring-2 focus:ring-inset focus:ring-coral focus:outline-none"
        />
      </div>

      <!-- Biografía -->
      <div>
        <label class="block text-sm text-cafe mb-2">Biografía</label>
        <textarea
          v-model="form.biografia"
          rows="4"
          maxlength="500"
          class="pl-4 pr-4 py-2.5 block w-full rounded-lg border-0 text-base text-cafe shadow-sm ring-1 ring-inset ring-rosa focus:ring-2 focus:ring-inset focus:ring-coral focus:outline-none resize-none"
        ></textarea>
        <p class="text-right text-xs text-cafe/50 mt-1">{{ form.biografia.length }}/500</p>
      </div>

      <!-- Datos personales -->
      <div class="grid grid-cols-1 sm:grid-cols-3 gap-4">
        <div>
          <label class="block text-sm text-cafe mb-2">Sexo</label>
          <select
            v-model="form.sexo"
            class="pl-4 pr-4 block w-full rounded-lg border-0 py-2.5 text-base text-cafe shadow-sm ring-1 ring-inset ring-rosa focus:ring-2 focus:ring-inset focus:ring-coral focus:outline-none"
          >
            <option value="">No especificado</option>
            <option value="Masculino">Masculino</option>
            <option value="Femenino">Femenino</option>
            <option value="Otro">Otro</option>
          </select>
        </div>
        <div>
          <label class="block text-sm text-cafe mb-2">Teléfono</label>
          <input
            v-model="form.telefono"
            type="text"
            maxlength="20"
            class="pl-4 pr-4 block w-full rounded-lg border-0 py-2.5 text-base text-cafe shadow-sm ring-1 ring-inset ring-rosa focus:ring-2 focus:ring-inset focus:ring-coral focus:outline-none"
          />
        </div>
        <div>
          <label class="block text-sm text-cafe mb-2">País</label>
          <input
            v-model="form.pais"
            type="text"
            maxlength="60"
            class="pl-4 pr-4 block w-full rounded-lg border-0 py-2.5 text-base text-cafe shadow-sm ring-1 ring-inset ring-rosa focus:ring-2 focus:ring-inset focus:ring-coral focus:outline-none"
          />
        </div>
      </div>

      <p v-if="errorEdicion" class="text-rojizo text-sm">{{ errorEdicion }}</p>
      <p v-if="mensaje" class="text-green-700 text-sm">{{ mensaje }}</p>

      <div class="flex items-center justify-end gap-3 pt-2">
        <button
          class="inline-flex cursor-pointer items-center rounded-md border border-coral bg-coral/10 px-4 py-2 text-sm font-medium text-granate transition hover:bg-coral/20"
          @click="irAPerfil"
        >
          Ver mi perfil
        </button>
        <button
          :disabled="guardando"
          class="px-5 py-2.5 rounded-md text-sm text-white bg-coral hover:bg-rojizo disabled:opacity-50 transition shadow-sm min-w-44"
          @click="guardar"
        >
          {{ etiquetaBoton }}
        </button>
      </div>

      <div v-if="fase.startsWith('subiendo')" class="h-1.5 w-full bg-rosa rounded-full overflow-hidden">
        <div class="h-full bg-coral transition-all duration-150" :style="{ width: progreso + '%' }"></div>
      </div>
    </div>
  </div>
</template>