<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import api from '../servicios/api'

const router = useRouter()
const nombreUsuario = localStorage.getItem('nombreUsuario') || ''

const cargando = ref(true)
const error = ref('')
const guardando = ref(false)
const mensaje = ref('')
const errorEdicion = ref('')

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
  editFoto.value = f
  previewFoto.value = f ? URL.createObjectURL(f) : ''
}

function onBannerChange(e) {
  const f = e.target.files[0] || null
  editBanner.value = f
  previewBanner.value = f ? URL.createObjectURL(f) : ''
}

async function subirArchivo(archivo) {
  const fd = new FormData()
  fd.append('archivo', archivo)
  const res = await api.post('/upload', fd)
  return res.data.url
}

async function guardar() {
  guardando.value = true
  mensaje.value = ''
  errorEdicion.value = ''
  try {
    let urlFoto = null
    let urlBanner = null
    if (editFoto.value) urlFoto = await subirArchivo(editFoto.value)
    if (editBanner.value) urlBanner = await subirArchivo(editBanner.value)

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
    previewFoto.value = ''
    previewBanner.value = ''
    editFoto.value = null
    editBanner.value = null

    mensaje.value = 'Perfil actualizado correctamente.'
  } catch (e) {
    errorEdicion.value = e.response?.data?.message || 'No se pudo guardar.'
  } finally {
    guardando.value = false
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
          <input type="file" accept="image/*" @change="onFotoChange"class="sr-only" />

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
        <input type="file" accept="image/*" @change="onBannerChange" class="sr-only" />
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
          class="px-5 py-2.5 rounded-md text-sm text-white bg-coral hover:bg-rojizo disabled:opacity-50 transition shadow-sm"
          @click="guardar"
        >
          {{ guardando ? 'Guardando...' : 'Guardar cambios' }}
        </button>
      </div>
    </div>
  </div>
</template>